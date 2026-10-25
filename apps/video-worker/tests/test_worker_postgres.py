"""Opt-in real PostgreSQL claim/lease transitions, not storage/media acceptance.

Uses only the dedicated disposable database and this run's generated rows. The
stale-handle test deliberately prevents pipeline/storage work; READY here is a
synthetic SQL fixture, never evidence of an encoded or uploaded film.
"""
from concurrent.futures import ThreadPoolExecutor
import importlib.util
import os
from pathlib import Path
import threading
from urllib.parse import urlsplit
from uuid import uuid4

import pytest


WORKER_ROOT = Path(__file__).resolve().parents[1]


def disposable_dsn():
    dsn = os.getenv('WORKER_TEST_DB_DSN', '')
    if not dsn or os.getenv('REELNEST_DISPOSABLE_DB') != 'true':
        pytest.skip('Real worker PostgreSQL tests require an explicitly disposable dedicated database')
    parsed = urlsplit(dsn)
    assert (parsed.scheme in ('postgres', 'postgresql')
            and parsed.hostname in ('127.0.0.1', 'localhost')
            and parsed.port is not None
            and parsed.path == '/reelnest_worker_test'
            and not parsed.query and not parsed.fragment), 'Refusing a nonlocal or nondedicated worker test database'
    return dsn


@pytest.fixture
def db(monkeypatch):
    dsn = disposable_dsn()
    import psycopg
    from psycopg.rows import dict_row

    monkeypatch.setenv('DATABASE_URL', dsn)
    monkeypatch.setenv('PGOPTIONS', '-c statement_timeout=10000 -c lock_timeout=5000')
    monkeypatch.syspath_prepend(str(WORKER_ROOT))
    spec = importlib.util.spec_from_file_location('worker_postgres_subject', WORKER_ROOT / 'worker.py')
    worker = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(worker)
    monkeypatch.setattr(worker, 'WORKER_ID', 'worker-test-' + uuid4().hex)
    conn = psycopg.connect(dsn, autocommit=True, row_factory=dict_row, connect_timeout=10)
    owner, creator, title = uuid4(), uuid4(), uuid4()
    assets = []

    class Database:
        def __init__(self):
            self.worker = worker
            self.conn = conn

        def add_job(self, max_attempts=3):
            asset, job = uuid4(), uuid4()
            conn.execute("""
                INSERT INTO media_assets(id,title_id,source_key,processing_status)
                VALUES (%s,%s,%s,'QUEUED')
            """, (asset, title, 'worker-test/' + str(asset)))
            assets.append(asset)
            conn.execute("""
                INSERT INTO media_processing_jobs(id,media_asset_id,max_attempts)
                VALUES (%s,%s,%s)
            """, (job, asset, max_attempts))
            return job

        def claim(self):
            with worker.connection() as claim_conn:
                return worker.claim(claim_conn)

        def row(self, job_id):
            return conn.execute("""
                SELECT j.*,m.processing_status,m.manifest_key,m.duration_seconds,m.processing_details
                FROM media_processing_jobs j JOIN media_assets m ON m.id=j.media_asset_id
                WHERE j.id=%s
            """, (job_id,)).fetchone()

        def expire(self, job):
            conn.execute("UPDATE media_processing_jobs SET lease_until=NOW()-interval '1 second' WHERE id=%s", (job['id'],))

    try:
        conn.execute("INSERT INTO users(id,identity_subject,email) VALUES (%s,%s,%s)",
                     (owner, 'worker-test-' + str(owner), 'worker-test@example.invalid'))
        conn.execute("""
            INSERT INTO creators(id,user_id,display_name,legal_name,contact_email,application_statement,status)
            VALUES (%s,%s,'Worker fixture','Synthetic fixture','worker-test@example.invalid','No actual media','APPROVED')
        """, (creator, owner))
        conn.execute("""
            INSERT INTO titles(id,slug,name,type,genre,language,status,age_rating,art_tone,creator_id)
            VALUES (%s,%s,'Worker SQL fixture','MOVIE','Drama','en','DRAFT','U','blue',%s)
        """, (title, 'worker-test-' + str(title), creator))
        yield Database()
    finally:
        for asset in assets:
            conn.execute('DELETE FROM media_processing_jobs WHERE media_asset_id=%s', (asset,))
            conn.execute('DELETE FROM media_assets WHERE id=%s', (asset,))
        conn.execute('DELETE FROM titles WHERE id=%s', (title,))
        conn.execute('DELETE FROM creators WHERE id=%s', (creator,))
        conn.execute('DELETE FROM users WHERE id=%s', (owner,))
        conn.close()


def test_concurrent_claims_skip_locked_rows_and_never_duplicate(db):
    expected = {db.add_job(), db.add_job()}
    start = threading.Barrier(2)
    claimed = threading.Barrier(2)

    def claim_while_holding_transaction():
        with db.worker.connection() as conn:
            with conn.transaction():
                start.wait(timeout=10)
                job = db.worker.claim(conn)
                # Both claims must complete before either releases its row lock.
                claimed.wait(timeout=10)
                return job

    with ThreadPoolExecutor(max_workers=2) as pool:
        futures = [pool.submit(claim_while_holding_transaction) for _ in range(2)]
        jobs = [future.result(timeout=15) for future in futures]
    assert {job['id'] for job in jobs} == expected
    assert all(db.row(job['id'])['attempts'] == 1 for job in jobs)
    assert db.claim() is None


def test_stale_handle_cannot_overwrite_newer_success_or_start_pipeline(db, monkeypatch):
    db.add_job()
    stale = db.claim()
    stale_owner = db.worker.WORKER_ID
    db.expire(stale)
    monkeypatch.setattr(db.worker, 'WORKER_ID', 'replacement-' + uuid4().hex)
    replacement = db.claim()
    assert replacement['attempts'] == 2
    assert db.worker.mark_success(replacement, 'synthetic/master.m3u8', 60, ['480p'])
    monkeypatch.setattr(db.worker, 'WORKER_ID', stale_owner)
    pipeline_calls = []

    def forbidden_pipeline(*args):
        pipeline_calls.append(True)
        raise RuntimeError('Stale handle must not begin media work')

    monkeypatch.setattr(db.worker, 'process_job', forbidden_pipeline)
    db.worker.handle(stale, object())
    row = db.row(stale['id'])
    assert row['state'] == 'SUCCEEDED'
    assert row['processing_status'] == 'READY', 'Stale handle regressed completed media to PROCESSING'
    assert row['manifest_key'] == 'synthetic/master.m3u8'
    assert not pipeline_calls, 'Stale handle started pipeline/storage work'


def test_replacement_lease_fences_stale_progress_failure_and_success(db, monkeypatch):
    db.add_job()
    stale = db.claim()
    stale_owner = db.worker.WORKER_ID
    db.expire(stale)
    replacement_owner = 'replacement-' + uuid4().hex
    monkeypatch.setattr(db.worker, 'WORKER_ID', replacement_owner)
    replacement = db.claim()
    monkeypatch.setattr(db.worker, 'WORKER_ID', stale_owner)
    assert not db.worker.update_progress(stale, 'UPLOADING', 90)
    db.worker.mark_failure(stale, RuntimeError('stale failure'))
    assert not db.worker.mark_success(stale, 'stale/master.m3u8', 60, [])
    row = db.row(stale['id'])
    assert row['state'] == 'RUNNING' and row['worker_id'] == replacement_owner
    assert row['attempts'] == replacement['attempts'] == 2
    assert row['manifest_key'] is None


def test_progress_is_monotonic_and_requires_a_live_lease(db):
    db.add_job()
    job = db.claim()
    assert db.worker.update_progress(job, 'ENCODING', 40)
    assert not db.worker.update_progress(job, 'PROBING', 20)
    assert db.row(job['id'])['progress_percent'] == 40
    db.expire(job)
    assert not db.worker.update_progress(job, 'UPLOADING', 80)
    db.worker.mark_failure(job, RuntimeError('expired'))
    assert not db.worker.mark_success(job, 'expired/master.m3u8', 60, [])
    assert db.row(job['id'])['state'] == 'RUNNING'


def test_expired_final_attempt_is_terminal_and_not_claimed_again(db):
    db.add_job(max_attempts=1)
    job = db.claim()
    db.expire(job)
    assert db.claim() is None
    row = db.row(job['id'])
    assert row['state'] == row['processing_status'] == 'FAILED'
    assert row['progress_stage'] == 'FAILED'
    assert row['worker_id'] is row['lease_until'] is None
    assert row['finished_at'] is not None


def test_transient_failure_backs_off_then_exhausts_retries(db):
    db.add_job(max_attempts=2)
    first = db.claim()
    db.worker.mark_failure(first, RuntimeError('temporary'))
    row = db.row(first['id'])
    assert row['state'] == row['processing_status'] == 'RETRY_WAIT'
    assert row['finished_at'] is None and row['lease_until'] is None
    assert db.claim() is None, 'Backoff was ignored'
    db.conn.execute("UPDATE media_processing_jobs SET available_at=NOW()-interval '1 second' WHERE id=%s", (first['id'],))
    second = db.claim()
    assert second['attempts'] == 2
    db.worker.mark_failure(second, RuntimeError('still unavailable'))
    row = db.row(first['id'])
    assert row['state'] == row['processing_status'] == 'FAILED'
    assert row['finished_at'] is not None and db.claim() is None


def test_invalid_media_is_permanently_rejected_without_retry(db):
    db.add_job()
    job = db.claim()
    db.worker.mark_failure(job, db.worker.InvalidMedia('synthetic invalid fixture'), permanent=True)
    row = db.row(job['id'])
    assert row['state'] == row['processing_status'] == 'REJECTED'
    assert row['attempts'] == 1 and row['finished_at'] is not None
    assert db.claim() is None


def test_live_success_commits_job_and_media_together(db):
    db.add_job()
    job = db.claim()
    assert db.worker.mark_success(job, 'synthetic/master.m3u8', 42, ['480p'])
    row = db.row(job['id'])
    assert row['state'] == 'SUCCEEDED' and row['processing_status'] == 'READY'
    assert row['progress_stage'] == 'READY' and row['progress_percent'] == 100
    assert row['manifest_key'] == 'synthetic/master.m3u8' and row['duration_seconds'] == 42
    assert row['processing_details'] == {'variants': ['480p']}
    assert row['lease_until'] is row['worker_id'] is None
    assert row['finished_at'] is not None and db.claim() is None


def test_live_handle_starts_pipeline_and_can_publish_after_fenced_transition(db, monkeypatch):
    db.add_job()
    job = db.claim()
    calls = []

    def synthetic_pipeline(actual_job, storage):
        assert actual_job['id'] == job['id']
        assert db.row(job['id'])['processing_status'] == 'PROCESSING'
        calls.append(True)
        return 'synthetic/handled/master.m3u8', 42, ['480p']

    monkeypatch.setattr(db.worker, 'process_job', synthetic_pipeline)
    db.worker.handle(job, object())
    row = db.row(job['id'])
    assert calls == [True], 'Live claim was incorrectly refused'
    assert row['state'] == 'SUCCEEDED' and row['processing_status'] == 'READY'
    assert row['manifest_key'] == 'synthetic/handled/master.m3u8'
