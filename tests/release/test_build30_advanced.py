"""Build30 optional feature evidence; no cloud provisioning or paid traffic."""
from __future__ import annotations
import gzip
import importlib.util
import json
from pathlib import Path
import pytest

ROOT=Path(__file__).resolve().parents[2]

def load(name):
    p=ROOT/"scripts"/"advanced"/(name+".py")
    spec=importlib.util.spec_from_file_location("reelnest_"+name,p)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def test_recovery_measurement_and_no_false_rpo():
    m=load("recovery_objectives")
    evidence=m.assess(backup_completed="2026-10-24T00:00:00Z",
        incident="2026-10-24T00:05:00Z",
        restore_started="2026-10-24T00:06:00Z",
        restore_verified="2026-10-24T00:07:00Z",
        max_rto_seconds=180,max_backup_age_seconds=600)
    assert evidence["restore_time_from_incident_seconds"]==120
    assert evidence["backup_age_at_incident_seconds"]==300
    assert evidence["rto_target_met"] and evidence["backup_age_proxy_target_met"]
    assert evidence["actual_rpo_verified"] is False
    assert evidence["actual_recovery_point_loss_seconds"] is None

def test_recovery_rejects_out_of_order_timestamps():
    m=load("recovery_objectives")
    with pytest.raises(ValueError):
        m.assess(backup_completed="2026-10-24T00:09:00Z",
            incident="2026-10-24T00:05:00Z",
            restore_started="2026-10-24T00:06:00Z",
            restore_verified="2026-10-24T00:07:00Z",
            max_rto_seconds=100,max_backup_age_seconds=3600)
    with pytest.raises(ValueError):m.parse_utc("2026-10-24T00:00:00+05:30")

def _log():
    return ("#Version: 1.0\n#Fields: date\ttime\tx-edge-result-type\tsc-bytes\tsc-status\n"
      "2026-10-24\t10:00:00\tHit\t1000\t200\n"
      "2026-10-24\t10:00:01\tMiss\t2000\t200\n"
      "2026-10-24\t10:00:02\tError\t300\t500\n")

def test_cdn_metrics_without_claiming_origin_egress(tmp_path):
    m=load("cdn_log_metrics")
    p=tmp_path/"cf.log"
    p.write_text(_log(),encoding="utf-8")
    result=m.measure(p)
    assert result["cache_hit_rate_pct"]==50
    assert result["requests"]==3
    assert result["viewer_response_bytes"]==3300
    assert result["viewer_5xx"]==1
    assert result["origin_egress_bytes"] is None
    assert result["origin_egress_verified"] is False
    gz=tmp_path/"cf.log.gz"
    with gzip.open(gz,"wt",encoding="utf-8") as stream:stream.write(_log())
    assert m.measure(gz)==result

def test_cdn_rejects_malformed_and_missing_header(tmp_path):
    m=load("cdn_log_metrics")
    p=tmp_path/"cf.log"
    p.write_text("Hello\tWorld\n")
    with pytest.raises(ValueError):m.measure(p)
    p.write_text(_log().replace("\t2000\t200","\tINVALID\t200"))
    with pytest.raises(ValueError):m.measure(p)

def test_scaling_advice_fail_closed_without_evidence():
    m=load("rollout_gate")
    example={
      "observed_hours":168,"requests":100000,"cache_hit_rate_pct":55,
      "origin_egress_gb_24h":120,"p95_origin_latency_ms":700,
      "estimated_monthly_savings_usd":200,
    }
    result=m.review(example)
    assert result["origin_shield_candidate"]
    assert result["autoscaling_review_candidate"]
    assert not result["regional_routing_candidate"]
    assert result["changes_applied"] is False
    example["observed_hours"]=24
    assert not m.review(example)["origin_shield_candidate"]
    example["observed_hours"]=168
    example["estimated_monthly_savings_usd"]=float("nan")
    with pytest.raises(ValueError):m.review(example)

def test_restore_script_writes_only_disposable_metrics():
    script=(ROOT/"scripts/test_restore_disposable.sh").read_text()
    assert "scripts/advanced/recovery_objectives.py" in script
    assert "recovery-objectives.local.json" in script
    assert "RESTORE_VERIFIED_UTC" in script
    assert "dropdb --if-exists" in script
