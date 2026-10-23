# Build 22 — External Verification Queue

The 40 local/Codex execution blockers and 17 accounts/approvals blockers are carried forward unchanged from Build 21. See `BUILD21_OPERATOR_CODEX_BLOCKERS.md` for the full task-ID-by-task-ID queue.

Additional Build 22 integration acceptance:

- Compile Spring Boot + Next.js and test Flyway `V21` / `V22` against a new and upgraded PostgreSQL database.
- Login as two viewers: ensure users cannot read, mark or delete each other's notifications.
- Verify rights-expiry job idempotency and daily scheduler behavior.
- Ensure creator performance cannot list another creator's titles; low-cardinality aggregates suppressed.
- Ensure admin analytics cannot be queried by viewer/creator tokens; no viewer IDs in responses.
- Validate semantic search with mocked recommender invalid IDs, duplicate IDs, timeout and unsupported content rights.
- Confirm preview promotions do not modify provider prices or entitlements.
- Test opt-in TV D-pad mode and keyboard input focus with real browser/device.

No changes to the status of AWS, Razorpay, content licences or production staging.
