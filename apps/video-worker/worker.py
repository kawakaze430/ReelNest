"""Durable DB-queued media worker, separate process from Spring API.

DB polling (SKIP LOCKED) is the queue; completed-upload transaction creates job.
Claim leases + heartbeats + retries tolerate worker crashes without RabbitMQ loss.
"""
from __future__ import annotations
import logging
import os
from pathlib import Path
import socket
import tempfile
import threading
import time
from uuid import uuid4

import boto3
import psycopg
from psycopg.rows import dict_row
from pipeline import InvalidMedia, encode, content_type, validate_package
from caption_packaging import package_captions

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"),
                    format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("reelnest.worker")
DB_DSN = os.environ.get("DATABASE_URL", "postgresql://reelnest:local_dev_only@postgres:5432/reelnest")
SOURCE_BUCKET = os.getenv("SOURCE_BUCKET", "reelnest-source-private")
PROCESSED_BUCKET = os.getenv("PROCESSED_BUCKET", "reelnest-processed-private")
WORKER_ID = f"{socket.gethostname()}-{uuid4()}"
MAX_DURATION = int(os.getenv("MAX_VIDEO_SECONDS", "14400"))
MAX_ENCODE_SECONDS = int(os.getenv("MAX_ENCODE_SECONDS", "3600"))
LEASE_SECONDS = 90
HEARTBEAT_SECONDS = 20


def connection():
    if os.getenv("DATABASE_URL"):
        return psycopg.connect(DB_DSN, row_factory=dict_row, connect_timeout=10)
    return psycopg.connect(host=os.environ["DB_HOST"], user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"], dbname=os.getenv("DB_NAME", "reelnest"),
        row_factory=dict_row, connect_timeout=10, sslmode="require")


def s3_client():
    opts = {"region_name": os.getenv("S3_REGION", "us-east-1")}
    endpoint = os.getenv("S3_ENDPOINT", "")
    if endpoint:
        opts["endpoint_url"] = endpoint
    key, secret = os.getenv("S3_ACCESS_KEY", ""), os.getenv("S3_SECRET_KEY", "")
    if bool(key) != bool(secret):
        raise RuntimeError("Storage key and secret must be provided together")
    if key:
        opts.update(aws_access_key_id=key, aws_secret_access_key=secret)
    # Cloud deployment uses ECS task IAM role. No static AWS secrets required.
    return boto3.client("s3", **opts)


def claim(conn):
    with conn.transaction():
        # A crashed worker on the final attempt must not leave a RUNNING row forever.
        exhausted = conn.execute("""
            UPDATE media_processing_jobs SET state='FAILED',progress_stage='FAILED',worker_id=NULL,
                lease_until=NULL,finished_at=NOW(),updated_at=NOW(),
                last_error='Processing lease expired after final attempt'
            WHERE state='RUNNING' AND lease_until<NOW() AND attempts>=max_attempts
            RETURNING media_asset_id
        """).fetchall()
        for entry in exhausted:
            conn.execute("""
                UPDATE media_assets SET processing_status='FAILED'
                WHERE id=%s AND processing_status<>'READY'
            """, (entry['media_asset_id'],))
        return conn.execute("""
          WITH chosen AS (
            SELECT id FROM media_processing_jobs
            WHERE ((state IN ('QUEUED','RETRY_WAIT') AND available_at <= NOW())
                 OR (state='RUNNING' AND lease_until < NOW()))
              AND attempts < max_attempts
            ORDER BY available_at ASC, created_at ASC
            FOR UPDATE SKIP LOCKED LIMIT 1
          )
          UPDATE media_processing_jobs AS j
          SET state='RUNNING', progress_stage='PROBING',progress_percent=1,attempts=attempts+1,
              worker_id=%s, lease_until=NOW()+(%s * interval '1 second'),
              started_at=COALESCE(started_at,NOW()), updated_at=NOW()
          FROM chosen WHERE j.id=chosen.id
          RETURNING j.id,j.media_asset_id,j.attempts,j.max_attempts
        """, (WORKER_ID, LEASE_SECONDS)).fetchone()


def heartbeat(stop: threading.Event, job_id, lost: threading.Event):
    while not stop.wait(HEARTBEAT_SECONDS):
        try:
            with connection() as conn:
                with conn.transaction():
                    row = conn.execute("""
                      UPDATE media_processing_jobs
                      SET lease_until=NOW()+(%s * interval '1 second'),updated_at=NOW()
                      WHERE id=%s AND worker_id=%s AND state='RUNNING'
                      RETURNING id
                    """, (LEASE_SECONDS, job_id, WORKER_ID)).fetchone()
                if row is None:
                    lost.set()
                    return
        except Exception:
            # Do not claim success after the lease expires. Mark job as uncertain.
            log.exception("Heartbeat failed for %s", job_id)
            lost.set()
            return


def update_progress(job, stage: str, percent: int) -> bool:
    """Only the live lease holder may publish monotonic progress for an attempt."""
    if stage not in {"PROBING", "ENCODING", "UPLOADING", "VERIFYING"} or not 1 <= percent <= 99:
        raise ValueError("Invalid progress milestone")
    with connection() as conn:
        with conn.transaction():
            result = conn.execute("""
                UPDATE media_processing_jobs SET progress_stage=%s,progress_percent=%s,updated_at=NOW()
                WHERE id=%s AND worker_id=%s AND state='RUNNING' AND lease_until>NOW()
                  AND progress_percent<=%s
                RETURNING id
            """, (stage,percent,job["id"],WORKER_ID,percent)).fetchone()
    return result is not None


def asset_details(conn, asset_id):
    return conn.execute("""
      SELECT m.id, m.title_id, m.source_key, m.processing_status,
             u.expected_bytes
      FROM media_assets m JOIN upload_intents u ON m.source_key=u.object_key
      WHERE m.id=%s AND u.status='COMPLETED'
    """, (asset_id,)).fetchone()


def process_job(job, s3):
    with connection() as conn:
        asset = asset_details(conn, job["media_asset_id"])
    if asset is None:
        raise InvalidMedia("Missing completed source upload for media asset")
    source_key = asset["source_key"]
    original_size = s3.head_object(Bucket=SOURCE_BUCKET, Key=source_key)["ContentLength"]
    if original_size != asset["expected_bytes"]:
        raise InvalidMedia("Source object size no longer matches completed upload")
    # Deterministic, isolated output prefix per attempt avoids stale worker corruption.
    prefix = f"processed/{asset['id']}/{job['id']}/attempt-{job['attempts']}/"
    if not update_progress(job, "PROBING", 5):
        raise RuntimeError("Worker lease lost before media processing")
    with tempfile.TemporaryDirectory(prefix="reelnest-media-") as workspace:
        source = Path(workspace) / "input.media"
        output = Path(workspace) / "hls"
        s3.download_file(SOURCE_BUCKET, source_key, str(source))
        if source.stat().st_size != original_size:
            raise InvalidMedia("Incomplete source download")
        if not update_progress(job, "ENCODING", 15):
            raise RuntimeError("Worker lease lost before transcoding")
        manifest, info, variants = encode(source, output,
                    max_duration=MAX_DURATION, timeout_seconds=MAX_ENCODE_SECONDS)
        with connection() as conn:
            caption_sources=conn.execute("""
                SELECT language,srt FROM title_caption_sources WHERE title_id=%s ORDER BY language
                """,(asset["title_id"],)).fetchall()
        if caption_sources:
            package_captions(output,info.duration,caption_sources)
            validate_package(output,decode_samples=False)
        files = sorted(p for p in output.rglob("*") if p.is_file())
        if len(files) < 3:  # master + variant + segment(s)
            raise RuntimeError("HLS output missing files")
        if not update_progress(job, "UPLOADING", 70):
            raise RuntimeError("Worker lease lost before upload")
        # Do not set READY until *every* upload succeeds.
        for index, path in enumerate(files):
            relative = path.relative_to(output).as_posix()
            s3.upload_file(str(path), PROCESSED_BUCKET, prefix + relative,
                ExtraArgs={"ContentType": content_type(relative),
                           "CacheControl": "private, max-age=3600"})
            # Limit database traffic for long films; never publish 100% before READY.
            milestone = 70 + (index + 1) * 25 // len(files)
            if milestone > 70 and (index == len(files)-1 or index % max(1, len(files)//8) == 0):
                if not update_progress(job, "UPLOADING", milestone):
                    raise RuntimeError("Worker lease lost during upload")
        if not update_progress(job, "VERIFYING", 98):
            raise RuntimeError("Worker lease lost before output verification")
        key = prefix + manifest
        if s3.head_object(Bucket=PROCESSED_BUCKET, Key=key)["ContentLength"] < 10:
            raise RuntimeError("Master manifest not uploaded correctly")
        return key, int(info.duration), variants


def mark_success(job, manifest_key, duration, variants):
    import json
    with connection() as conn:
        with conn.transaction():
            updated = conn.execute("""
              UPDATE media_processing_jobs SET state='SUCCEEDED',progress_stage='READY',progress_percent=100,lease_until=NULL,
                  worker_id=NULL,last_error=NULL,finished_at=NOW(),updated_at=NOW()
              WHERE id=%s AND state='RUNNING' AND worker_id=%s
                AND lease_until > NOW() RETURNING media_asset_id
            """, (job["id"], WORKER_ID)).fetchone()
            if not updated:
                log.warning("Lease lost; refusing READY transition for %s", job["id"])
                return False
            conn.execute("""
              UPDATE media_assets SET manifest_key=%s,duration_seconds=%s,
                  processing_status='READY',processing_details=%s::jsonb
              WHERE id=%s
            """, (manifest_key,duration,json.dumps({"variants":variants}),job["media_asset_id"]))
    return True


def mark_failure(job, error, permanent=False):
    message = str(error).replace("\x00", "")[:800]
    with connection() as conn:
        with conn.transaction():
            # A stale worker cannot change the state of a newer lease holder.
            current = conn.execute("""
              SELECT attempts,max_attempts FROM media_processing_jobs
              WHERE id=%s AND state='RUNNING' AND worker_id=%s AND lease_until>NOW()
              FOR UPDATE
            """, (job["id"], WORKER_ID)).fetchone()
            if not current:
                return
            terminal = permanent or current["attempts"] >= current["max_attempts"]
            next_state = 'REJECTED' if permanent else ('FAILED' if terminal else 'RETRY_WAIT')
            # Exponential backoff with a ceiling, delay next attempt unless terminal.
            delay = min(1800, 30 * 2 ** min(6,current["attempts"] - 1))
            conn.execute("""
              UPDATE media_processing_jobs SET state=%s,progress_stage='FAILED',worker_id=NULL,lease_until=NULL,
                  last_error=%s,available_at=NOW()+(%s * interval '1 second'),
                  finished_at=CASE WHEN %s THEN NOW() ELSE NULL END,updated_at=NOW()
              WHERE id=%s
            """, (next_state,message,delay,terminal,job["id"]))
            conn.execute("""
              UPDATE media_assets SET processing_status=%s
              WHERE id=%s
            """, (next_state if terminal else 'RETRY_WAIT',job["media_asset_id"]))


def handle(job, s3):
    log.info("Processing media=%s attempt=%s job=%s",job["media_asset_id"],job["attempts"],job["id"])
    stop = threading.Event()
    lost = threading.Event()
    thread = threading.Thread(target=heartbeat, args=(stop,job["id"],lost), daemon=True)
    thread.start()
    try:
        with connection() as conn:
            with conn.transaction():
                conn.execute("UPDATE media_assets SET processing_status='PROCESSING' WHERE id=%s",
                             (job["media_asset_id"],))
        manifest, duration, variants = process_job(job,s3)
        if lost.is_set():
            raise RuntimeError("Processing lease heartbeat was lost; retry safely")
        if mark_success(job, manifest, duration, variants):
            log.info("Ready media=%s manifest=%s variants=%s",job["media_asset_id"],manifest,len(variants))
    except InvalidMedia as exc:
        log.warning("Rejected invalid media=%s: %s",job["media_asset_id"],exc)
        mark_failure(job,exc,permanent=True)
    except Exception as exc:
        log.exception("Processing failed media=%s",job["media_asset_id"])
        mark_failure(job,exc)
    finally:
        stop.set()
        thread.join(timeout=2)


def reconcile_uploads(s3):
    """Repair S3 multipart completion that succeeded before the API SQL commit.

    Only recover source objects whose size matches their authenticated upload intent.
    Incomplete expired multipart uploads are aborted and marked ABORTED.
    """
    from botocore.exceptions import ClientError
    with connection() as conn:
        intents=conn.execute("""
            SELECT id,title_id,object_key,provider_upload_id,expected_bytes,expires_at,status
            FROM upload_intents
            WHERE status IN ('INITIATED','COMPLETING')
              AND created_at < NOW() - interval '5 minutes'
            ORDER BY created_at LIMIT 40
        """).fetchall()
    for item in intents:
        assembled=False
        try:
            actual=s3.head_object(Bucket=SOURCE_BUCKET,Key=item['object_key'])['ContentLength']
            assembled=(actual == item['expected_bytes'])
            if not assembled:
                log.warning("Source mismatch, not auto-recovering upload=%s",item['id'])
                continue
        except ClientError as exc:
            if exc.response.get('ResponseMetadata',{}).get('HTTPStatusCode')!=404:
                log.warning("Could not inspect upload=%s: %s",item['id'],exc)
                continue
        if assembled:
            with connection() as conn:
                with conn.transaction():
                    # A title can only be submitted after upload completion; lock upload.
                    locked=conn.execute("SELECT status FROM upload_intents WHERE id=%s FOR UPDATE",
                                        (item['id'],)).fetchone()
                    if not locked or locked['status'] not in ('INITIATED','COMPLETING'):
                        continue
                    conn.execute("UPDATE upload_intents SET status='COMPLETED',completed_at=NOW() WHERE id=%s",
                                 (item['id'],))
                    media=conn.execute("""
                        INSERT INTO media_assets(id,title_id,source_key,manifest_key,processing_status,created_at)
                        VALUES (%s,%s,%s,NULL,'QUEUED',NOW())
                        ON CONFLICT (source_key) DO UPDATE SET source_key=EXCLUDED.source_key
                        RETURNING id
                    """,(uuid4(),item['title_id'],item['object_key'])).fetchone()
                    conn.execute("""
                        INSERT INTO media_processing_jobs(id,media_asset_id,state)
                        VALUES (%s,%s,'QUEUED') ON CONFLICT (media_asset_id) DO NOTHING
                    """,(uuid4(),media['id']))
            log.info("Recovered assembled upload=%s and enqueued media processing",item['id'])
        elif item['expires_at'].timestamp()<time.time():
            try:
                s3.abort_multipart_upload(Bucket=SOURCE_BUCKET,Key=item['object_key'],
                                          UploadId=item['provider_upload_id'])
            except ClientError as exc:
                # Already gone is not a reason to keep an expired upload active.
                if exc.response.get('ResponseMetadata',{}).get('HTTPStatusCode')!=404:
                    log.warning("Could not abort expired upload=%s: %s",item['id'],exc)
                    continue
            with connection() as conn:
                with conn.transaction():
                    conn.execute("""
                        UPDATE upload_intents SET status='ABORTED' WHERE id=%s
                        AND status IN ('INITIATED','COMPLETING')
                    """,(item['id'],))
            log.info("Aborted expired upload=%s",item['id'])


def main():
    s3=s3_client()
    log.info("Started worker %s", WORKER_ID)
    next_cleanup=0.0
    while True:
        try:
            if time.monotonic()>=next_cleanup:
                next_cleanup=time.monotonic()+300
                try:
                    reconcile_uploads(s3)
                except Exception:
                    log.exception("Upload reconciliation failed; processing queue remains available")
            with connection() as conn:
                job=claim(conn)
            if job:
                handle(job,s3)
            else:
                time.sleep(3)
        except KeyboardInterrupt:
            log.info("Worker stopped")
            return
        except Exception:
            log.exception("Worker poll failed; retrying")
            time.sleep(8)


if __name__ == "__main__":
    main()
