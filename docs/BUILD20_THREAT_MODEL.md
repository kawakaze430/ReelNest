# ReelNest security threat model — implementation-focused draft

**Status:** engineering draft, NOT an independent security assessment. Review after live integration.

## Trust boundaries

1. Public browser / mobile clients are untrusted. Every JWT must be verified by the API. Every `profileId`, `titleId`, `caseId`, session ID, and upload ID is attacker-controlled input until ownership checks pass.
2. Keycloak is a separate security authority. API role checks are independent of what the frontend hides. MFA is required for staff in production.
3. API ⇄ PostgreSQL holds identity mapping, subscriber status, rights grants and audit records. Restrict DB credentials to a runtime role, parameterize SQL, migrate via a separate role, encrypt connections and backups.
4. Creator uploads are untrusted binary input. Validate type/duration, decode in isolated workers, constrain CPU/memory/disk/time, never execute embedded scripts or expose uploaded originals.
5. S3 source/processed assets are private; application-generated playback sessions have bounded lifetimes. CDN cookies are *not* DRM and cannot assure instantaneous segment revocation before cookie expiry.
6. Payment webhooks enter from the public internet: verify signature over raw body, deduplicate event IDs, reconcile charge/refund/dispute states independently from checkout success callbacks.
7. Rights/territory decisions must rely on a verified server/edge source. Never trust caller-supplied country headers. Revocation must affect search, recommendations, new sessions and existing application sessions.
8. Support cases contain private messages and possible copyrighted-rights evidence. Never show them to another user; restrict reviews to staff, log privileged unpublishes and apply retention/deletion review.
9. ML ranking receives only pre-filtered eligible content IDs; reject unknown IDs and re-filter at the final API boundary.

## Highest-severity test scenarios

- User A reads User B's profile, support case, watchlist or payment through guessed UUID.
- Spoofed JWT with wrong audience, altered role, or revoked issuance cutoff.
- Child profile uses an adult profile identifier to bypass age rules (B-10 PIN/profile unlock remains outstanding).
- Direct or cached CDN access after rights/tier/session revocation; review signed cookie TTL and emergency invalidation.
- Spoofed or replayed Razorpay webhook grants paid access.
- Malicious subtitle/HLS playlist path traversal, external URI or oversized media upload.
- Moderator unpublishes a film without administrator rights; support complaint automatically unpublishes without review.
- Support/analytics data leaks to public SEO endpoints, frontend error pages or logs.

## Release controls

Run API integration tests, dependency audits, DAST with allowed targets, signed-media denial checks, Keycloak failover, backup restoration, incident rehearsal and a qualified independent penetration test before paid public launch.

**Owners:** Platform engineering for code and evidence; project owner for policies, licences and independent approval.
