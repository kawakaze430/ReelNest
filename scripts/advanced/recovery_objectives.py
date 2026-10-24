#!/usr/bin/env python3
"""Measure a disposable logical restore without claiming full RPO for real traffic.

RTO observed = incident/start-of-restore to successful validation.
Backup age at simulated incident is an RPO *exposure proxy*, NOT actual data loss.
"""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys

def parse_utc(value: str) -> datetime:
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ValueError("Expected ISO-8601 UTC ending in Z")
    try:
        timestamp = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError("Invalid timestamp") from exc
    if timestamp.utcoffset().total_seconds() != 0:
        raise ValueError("Timestamp must be UTC")
    return timestamp

def assess(*, backup_completed: str, incident: str,
           restore_started: str, restore_verified: str,
           max_rto_seconds: int, max_backup_age_seconds: int) -> dict:
    dates=[parse_utc(x) for x in (backup_completed,incident,restore_started,restore_verified)]
    if dates != sorted(dates):
        raise ValueError("Required order: backup completion <= incident <= restore start <= verified")
    if not 1 <= max_rto_seconds <= 604800 or not 1 <= max_backup_age_seconds <= 604800:
        raise ValueError("Thresholds must be positive and bounded to seven days")
    exposure=(dates[1]-dates[0]).total_seconds()
    rto=(dates[3]-dates[1]).total_seconds()
    return {
        "schema_version": 1,
        "evidence_type": "disposable-logical-backup-restore",
        "backup_age_at_incident_seconds": exposure,
        "restore_time_from_incident_seconds": rto,
        "restore_execution_seconds": (dates[3]-dates[2]).total_seconds(),
        "backup_age_proxy_target_met": exposure <= max_backup_age_seconds,
        "rto_target_met": rto <= max_rto_seconds,
        "actual_recovery_point_loss_seconds": None,
        "actual_rpo_verified": False,
        "scope": "Synthetic interruption; no customer writes, failover, storage replication or live payment data.",
    }

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ("backup-completed","incident","restore-started","restore-verified"):
        parser.add_argument("--"+name,required=True)
    parser.add_argument("--max-rto-seconds",type=int,default=1800)
    parser.add_argument("--max-backup-age-seconds",type=int,default=3600)
    parser.add_argument("--output",type=Path)
    args=parser.parse_args(argv)
    try:
        report=assess(backup_completed=args.backup_completed,incident=args.incident,
            restore_started=args.restore_started,restore_verified=args.restore_verified,
            max_rto_seconds=args.max_rto_seconds,max_backup_age_seconds=args.max_backup_age_seconds)
    except ValueError as exc:
        print("INVALID: "+str(exc),file=sys.stderr)
        return 2
    text=json.dumps(report,indent=2,sort_keys=True)
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(text+"\n",encoding="utf-8")
    print(text)
    return 0 if report["rto_target_met"] and report["backup_age_proxy_target_met"] else 1

if __name__=="__main__":
    raise SystemExit(main())
