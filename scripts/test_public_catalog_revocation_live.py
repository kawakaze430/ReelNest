#!/usr/bin/env python3
"""Exercise actual public catalog API with committed disposable DB fixture.

NO AWS, paid providers or content media. Refuses anything except the isolated,
locally booted PostgreSQL and HTTP API from .github/workflows/postgres-flyway.yml.
"""
from __future__ import annotations

from datetime import date,timedelta
from urllib.parse import urlsplit,quote
from urllib.request import urlopen
from urllib.error import HTTPError
import json
import os
import sys
import uuid

API="http://127.0.0.1:8080"
DEMO_ID="00000000-0000-4000-8000-000000000001"

def safe_dsn(dsn):
    parsed=urlsplit(dsn)
    return (parsed.scheme in ("postgres","postgresql") and
        parsed.hostname in ("127.0.0.1","localhost") and
        parsed.port==5432 and parsed.path=="/reelnest")

def get_json(url):
    try:
        with urlopen(url,timeout=7) as response:
            if response.status!=200:
                raise AssertionError("Unexpected catalog HTTP response")
            return response.status,json.load(response)
    except HTTPError as error:
        return error.code,None

def main():
    dsn=os.getenv("REELNEST_TEST_DB_DSN","")
    if os.getenv("REELNEST_DISPOSABLE_DB")!="true" or not safe_dsn(dsn):
        print("BLOCKED: disposable loopback PostgreSQL opt-in required",file=sys.stderr)
        return 3
    import psycopg
    nonce=uuid.uuid4().hex
    user,creator,title,rights,asset=(uuid.uuid4() for _ in range(5))
    name="Build35-Catalog-"+nonce
    slug="build35-"+nonce
    alias="licensed-alias-"+nonce
    baseline=f"{API}/api/v1/catalog"
    def visible():
        status,data=get_json(baseline+"?q="+quote(name)+"&size=20")
        assert status==200,(status,"catalog list status")
        return any(x["id"]==str(title) for x in data["content"])
    def specific(expected):
        status,_=get_json(baseline+"/"+str(title))
        assert status==expected,("title ID",status,expected)
        status,_=get_json(baseline+"/by-slug/"+slug)
        assert status==expected,("slug",status,expected)
    with psycopg.connect(dsn,autocommit=True,connect_timeout=8,
                         options="-c statement_timeout=15000") as conn:
        try:
            conn.execute("INSERT INTO users(id,identity_subject,email) VALUES(%s,%s,%s)",
                         (user,"build35-"+nonce,"build35@example.invalid"))
            conn.execute("""
                INSERT INTO creators(id,user_id,display_name,legal_name,contact_email,
                                     application_statement,status)
                VALUES(%s,%s,%s,%s,%s,%s,'APPROVED')
                """,(creator,user,"CI Studio","CI Studio",
                      "ci@example.invalid","Disposable rights-owned film"))
            conn.execute("""
                INSERT INTO titles(id,creator_id,slug,name,synopsis,type,genre,language,
                                   status,age_rating,runtime_minutes,art_tone,featured,access_tier)
                VALUES(%s,%s,%s,%s,%s,'MOVIE','Drama','en','PUBLISHED','U',2,'blue',FALSE,'FREE')
                """,(title,creator,slug,name,"Original disposable synthetic test video"))
            conn.execute("""
                INSERT INTO content_rights(id,title_id,creator_id,rights_basis,
                                           proof_reference,agreement_notes,
                                           valid_from,valid_until,status)
                VALUES(%s,%s,%s,'OWNER','CI generated','Disposable verification',
                       %s,%s,'APPROVED')
                """,(rights,title,creator,date.today()-timedelta(days=1),
                      date.today()+timedelta(days=10)))
            conn.execute("INSERT INTO rights_territories(rights_id,country_code) VALUES(%s,'IN')",(rights,))
            conn.execute("""
                INSERT INTO media_assets(id,title_id,source_key,manifest_key,processing_status,duration_seconds)
                VALUES(%s,%s,%s,%s,'READY',120)
                """,(asset,title,"ci-originals/"+nonce+".mp4",
                      "ci-processed/"+nonce+"/master.m3u8"))
            conn.execute("INSERT INTO title_search_aliases(title_id,language,alias) VALUES(%s,'en',%s)",(title,alias))
            assert visible(),"Licensed READY title is absent from public browse"
            specific(200)
            # The main catalog search must match reviewed aliases, not only /suggest.
            status,matching=get_json(baseline+"?q="+quote(alias)+"&size=20")
            assert status==200 and any(x["id"]==str(title) for x in matching["content"]),(
                "reviewed title aliases not searchable")
            conn.execute("UPDATE creators SET status='SUSPENDED' WHERE id=%s",(creator,))
            assert not visible(),"Suspended creator leaked into public catalog"
            specific(404)
            conn.execute("UPDATE creators SET status='APPROVED' WHERE id=%s",(creator,))
            conn.execute("UPDATE content_rights SET status='REJECTED' WHERE id=%s",(rights,))
            assert not visible(),"Revoked rights leaked into public catalog"
            specific(404)
            conn.execute("UPDATE content_rights SET status='APPROVED' WHERE id=%s",(rights,))
            conn.execute("DELETE FROM rights_territories WHERE rights_id=%s",(rights,))
            assert not visible(),"Territory deletion leaked public catalog"
            specific(404)
            conn.execute("INSERT INTO rights_territories(rights_id,country_code) VALUES(%s,'IN')",(rights,))
            conn.execute("UPDATE media_assets SET processing_status='FAILED' WHERE id=%s",(asset,))
            assert not visible(),"No READY media but public catalog leaked"
            specific(404)
            conn.execute("UPDATE media_assets SET processing_status='READY' WHERE id=%s",(asset,))
            assert visible(),"Title did not reappear when all eligibility restored"
            status,_=get_json(baseline+"/"+DEMO_ID)
            assert status==404,"Unlicensed fictional demo metadata must not expose as playable public title"
            print("PASS live public catalog / ID / slug / alias: revoked creator, rights, territory and READY media denied")
        finally:
            # Scoped to unique synthetic IDs, in dependency order.
            conn.execute("DELETE FROM title_search_aliases WHERE title_id=%s",(title,))
            conn.execute("DELETE FROM media_assets WHERE id=%s",(asset,))
            conn.execute("DELETE FROM rights_territories WHERE rights_id=%s",(rights,))
            conn.execute("DELETE FROM content_rights WHERE id=%s",(rights,))
            conn.execute("DELETE FROM titles WHERE id=%s",(title,))
            conn.execute("DELETE FROM creators WHERE id=%s",(creator,))
            conn.execute("DELETE FROM users WHERE id=%s",(user,))
    return 0

if __name__=="__main__":
    raise SystemExit(main())
