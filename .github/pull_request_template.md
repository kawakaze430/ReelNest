## Purpose and linked checklist items

- [ ] Ticket IDs recorded (A-*, B-*)
- [ ] Security impact and threat model considered
- [ ] No secrets, tokens, private keys or customer data committed
- [ ] No unreviewed destructive migration

## Validation evidence

- [ ] `mvn -B verify` (when Maven available)
- [ ] `npm ci && npm run typecheck && npm run build` (when lockfile exists)
- [ ] `docker compose config --quiet` and live integration when needed
- [ ] `python3 scripts/check_env_contracts.py`
- [ ] `python3 -m pytest tests/release/test_build19_contracts.py -q`

Exact commands, timestamps, pass/fail, screenshots/logs (redacted):

## Rollback/incident plan

- Expected deployment blast radius:
- Backward-compatibility and migration notes:
- Rollback procedure:
