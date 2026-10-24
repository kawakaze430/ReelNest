#!/usr/bin/env python3
"""Disposable PostgreSQL constraints: opt-in, two-person review and preview dedup.

No payment, actual advertisements, production subscriptions or media playback.
"""
from __future__ import annotations
import os,sys,uuid
from datetime import datetime,timedelta,timezone
from urllib.parse import urlsplit

def permitted(dsn: str) -> bool:
    try:
        u=urlsplit(dsn)
        return (u.scheme in {"postgres","postgresql"} and
            u.hostname in {"localhost","127.0.0.1"} and
            u.port==5432 and u.path=="/reelnest" and
            os.getenv("REELNEST_DISPOSABLE_DB")=="true")
    except ValueError:return False

def main() -> int:
    dsn=os.getenv("REELNEST_TEST_DB_DSN","")
    if not permitted(dsn):
        print("BLOCKED: explicitly disposable PostgreSQL localhost only",file=sys.stderr)
        return 3
    import psycopg
    from psycopg.errors import CheckViolation
    a,b,adult,child,creative,event=(uuid.uuid4() for _ in range(6))
    now=datetime.now(timezone.utc)
    with psycopg.connect(dsn,connect_timeout=8,autocommit=True,
                         options="-c statement_timeout=15000") as conn:
        try:
            for i,uid in enumerate((a,b)):
                conn.execute("INSERT INTO users(id,identity_subject,email) VALUES(%s,%s,%s)",
                    (uid,"avod-schema-"+str(uid),f"avod{i}@example.invalid"))
            conn.execute("INSERT INTO profiles(id,user_id,name,is_child) VALUES(%s,%s,%s,FALSE)",
                (adult,a,"Adult preview"))
            conn.execute("INSERT INTO profiles(id,user_id,name,is_child) VALUES(%s,%s,%s,TRUE)",
                (child,a,"Child preview"))
            prefs=conn.execute("SELECT COUNT(*) FROM avod_profile_preferences WHERE profile_id=%s",(adult,)).fetchone()[0]
            assert prefs==0,"Profile consent must be absent and therefore OFF by default"
            conn.execute("INSERT INTO avod_profile_preferences(profile_id) VALUES(%s)",(adult,))
            assert conn.execute("""
              SELECT contextual_previews_opt_in,anonymous_measurement_opt_in
              FROM avod_profile_preferences WHERE profile_id=%s
              """,(adult,)).fetchone()==(False,False),"Opt-in default must be false"
            conn.execute("""
              INSERT INTO avod_creatives(id,advertiser,message,country_code,
                                         starts_at,ends_at,submitted_by)
              VALUES(%s,'CI original sponsor','Disposable sponsor message','IN',%s,%s,%s)
              """,(creative,now,now+timedelta(days=1),a))
            try:
                conn.execute("""
                  UPDATE avod_creatives SET status='REVIEWED',reviewed_by=%s WHERE id=%s
                  """,(a,creative))
            except CheckViolation:
                pass
            else:
                raise AssertionError("Same-person creative approval must be rejected")
            conn.execute("UPDATE avod_creatives SET status='REVIEWED',reviewed_by=%s WHERE id=%s",
                         (b,creative))
            assert conn.execute("SELECT status FROM avod_creatives WHERE id=%s",
                                (creative,)).fetchone()[0]=="REVIEWED"
            conn.execute("INSERT INTO avod_preview_events(id,profile_id,creative_id) VALUES(%s,%s,%s)",
                         (event,adult,creative))
            inserted=conn.execute("""
              INSERT INTO avod_preview_events(id,profile_id,creative_id)
              VALUES(%s,%s,%s) ON CONFLICT DO NOTHING
              RETURNING id
              """,(event,adult,creative)).fetchall()
            assert not inserted,"Repeat client event must deduplicate"
            conn.execute("DELETE FROM profiles WHERE id=%s",(adult,))
            assert conn.execute("SELECT COUNT(*) FROM avod_preview_events WHERE id=%s",
                                (event,)).fetchone()[0]==0,"Profile erasure must cascade preview events"
            print("PASS real V27 schema: OFF default, second reviewer, event deduplication, deletion cascade")
        finally:
            conn.execute("DELETE FROM avod_preview_events WHERE creative_id=%s",(creative,))
            conn.execute("DELETE FROM avod_creatives WHERE id=%s",(creative,))
            conn.execute("DELETE FROM profiles WHERE id IN (%s,%s)",(adult,child))
            conn.execute("DELETE FROM users WHERE id IN (%s,%s)",(a,b))
    return 0

if __name__=="__main__":raise SystemExit(main())
