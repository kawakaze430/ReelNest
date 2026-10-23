# Keycloak security operations and break-glass runbook

1. Generate an isolated staging realm: `python3 scripts/generate_keycloak_staging_realm.py --web-origin https://app.staging.example.com --output build-evidence/reelnest-staging-realm.json`.
2. Inspect generated realm. No seeded users. Registration remains disabled until external SMTP verification is proven. User enrollment must require TOTP; independently verify OTP actually executes after import.
3. Configure Keycloak hostname with HTTPS, trusted proxies, issuer URL, private PostgreSQL, backups and tested SSL. Do not reuse local demo passwords.
4. Test authorization-code flow with S256 PKCE, `aud=reelnest-api`, `azp=reelnest-web`, `iss` equal to the external canonical issuer. Test JWKS key rotation and 401 for wrong audience/issuer.
5. Require administrative accounts to enroll MFA. Restrict realm-admin to designated identities. Configure audit events, alert on privilege changes and repeated OTP failures.
6. Keep two monitored emergency administrator identities in secure vault; use only for outages, require documented approvals, and rotate/review after every use. The repository contains **no emergency passwords**.
7. Test identity backup restore and administrator lockout in non-production staging. Save sanitized timestamps, PASS/FAIL and incident owner to private `build-evidence/`.
8. The application revoke-all endpoint denies previously issued API access JWTs and cancels playback sessions. It does **not** enumerate or terminate Keycloak SSO or refresh tokens. Use OIDC RP-initiated logout, and complete Keycloak Admin session revocation as a separate acceptance gate.
9. In any Keycloak outage, fail closed for new JWT validations (except cached keys until expiration policy) and never bypass profile or entitlement checks.
10. For production, prefer a separate Keycloak deployment behind HTTPS and a private database. Never import the local developer realm to a public environment.
