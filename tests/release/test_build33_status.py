"""Build33 backlog audit: code scaffolding never equals a production acceptance."""
import csv
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]

def load(name):
    with (ROOT/"docs"/name).open(newline="",encoding="utf-8") as file:
        return list(csv.DictReader(file))

def test_preserve_all_282_original_task_ids_and_acceptance_state():
    old,new=load("BUILD32_282_TASKS.csv"),load("BUILD33_282_TASKS.csv")
    assert len(old)==len(new)==282
    assert [r["task_id"] for r in old]==[r["task_id"] for r in new]
    assert len({r["task_id"] for r in new})==282
    assert all(r["acceptance_verified"]=="NO" for r in new)
    for before,after in zip(old,new):
        for key,value in before.items():
            assert after[key]==value,(before["task_id"],key)

def test_only_three_not_started_tasks_advance_to_partial():
    old={r["task_id"]:r for r in load("BUILD32_282_TASKS.csv")}
    new={r["task_id"]:r for r in load("BUILD33_282_TASKS.csv")}
    promoted={id for id in old if old[id]["build32_status"]!=new[id]["build33_status"]}
    assert promoted=={"Q-06","G-13","G-16"}
    for id in promoted:
        assert old[id]["build32_status"]=="Not started / no audited implementation evidence"
        assert new[id]["build33_status"]=="Partially implemented"
        assert new[id]["build33_evidence_and_remaining"]
    assert dict(Counter(v["build33_status"] for v in new.values()))=={
        "Component CI verified — production acceptance pending":5,
        "Source implemented — runtime unverified":67,
        "Partially implemented":134,
        "Blocked — external execution / operator verification":31,
        "Blocked — external approval / account":17,
        "Not started / no audited implementation evidence":28
    }

def test_milestones_are_opt_in_private_and_non_monetary():
    controller=(ROOT/"apps/api/src/main/java/com/reelnest/loyalty/ViewerAchievementsController.java").read_text()
    assert "p.id=? AND p.user_id=?" in controller
    assert "state[0]||!state[1]" in controller
    assert "COUNT(DISTINCT title_id)" in controller
    assert "PLAY_COMPLETE" in controller
    assert "90 days" in controller
    assert "non-redeemable" in controller

def test_disabled_foreign_checkout_and_no_rental_entitlement():
    market=(ROOT/"apps/api/src/main/java/com/reelnest/billing/MarketEligibilityController.java").read_text()
    player=(ROOT/"apps/api/src/main/java/com/reelnest/playback/PlaybackService.java").read_text()
    rental=(ROOT/"apps/api/src/main/java/com/reelnest/billing/RentalWindowPolicy.java").read_text()
    billing=(ROOT/"apps/web/src/components/billing-panel.tsx").read_text()
    assert '!"IN".equals(country)' in market
    assert "List.of()" in market
    assert "market?.checkoutAvailable" in billing
    assert "market.visiblePlans.includes(code)" in billing
    assert "paidAt==null" in rental and "licensedUntil==null" in rental
    assert "RentalWindowPolicy" not in player
