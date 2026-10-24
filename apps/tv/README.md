# ReelNest Tizen & webOS catalog preview clients (Build 34)

These are **platform packaging starters with usable, read-only catalog navigation**, NOT
production Tizen/webOS app signoff. Never claim media playback, device OIDC,
commercial DRM, HDCP, offline licences, or app-store certification.

Run `python3 scripts/package_tv_clients.py --api-origin https://api.example.org --out build-evidence/tv-preview`
to generate both platform folders; provide an authorized **real** HTTPS API domain for an actual deployment.
The example domain is only for packaging tests.

Remote arrow keys move between cards, Enter selects, Escape/Backspace returns.
All untrusted catalog fields are rendered via `textContent`; no protected URLs
or tokens are fetched. Both clients use the public `GET /api/v1/catalog` API
and purposefully refuse to play media. An HTTPS-only CSP is created per package.
Run `node apps/tv/tests/catalog.test.cjs` for strict origin/title/focus rules.

**Still necessary:** platform developer accounts, TV simulator/hardware, genuine
Tizen web-app signing and webOS developer packaging, certificate/DRM partner
approval, Keycloak native/device flow, secure token storage, per-device playback
policy, captions, remote accessibility, store review, and multi-version QA.
Do not link a user's paid session to either preview shell.
