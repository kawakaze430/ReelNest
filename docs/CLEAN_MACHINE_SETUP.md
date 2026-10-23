# ReelNest clean-machine setup (Build 19)

## Supported development host
Windows 11 + WSL2 Ubuntu + Docker Desktop/WSL integration, or native Linux. Use Java 21, Maven >=3.9, Node 22, Python 3, FFmpeg and Docker Compose. Never use production credentials locally.

## Bootstrap
```bash
cd reelnest
python3 scripts/codex_preflight.py
python3 scripts/check_env_contracts.py
bash scripts/test_token_claims_java.sh
python3 -m pytest tests/release/test_build19_contracts.py -q
```

## Dependency lockfile — operator requires internet
The inherited ZIP has **no package-lock.json**. In a connected development machine:
```bash
cd apps/web
npm install --package-lock-only --no-audit --no-fund
npm ci
npm audit --omit=dev --audit-level=high
npm run typecheck
npm run build
```
Review advisories, resolve vulnerabilities, commit the *generated and reviewed* package-lock.json, then use `npm ci` for reproducible deployment. **Do not invent a fake lockfile**. The Dockerfile temporarily supports install bootstrap before a lockfile is committed.

## Java
```bash
cd apps/api
mvn -B --no-transfer-progress verify
```

## Docker (developer account only)
```bash
cd reelnest
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=120 api keycloak postgres web
bash scripts/validate_local_stack.sh --smoke
```
Do not run `docker compose down -v` on anything containing important data. On an isolated test stack run full creator-to-HLS workflow and billing TEST transactions only.

## DB migration verification
Check `flyway_schema_history` all V1–V15 applied with `success=true`. Test clean DB and upgrade-copy DB. Save redacted evidence. Be aware V15 adds JWT revocation cutoff and requires all in-use app tokens to have aud/azp claims.

## Private Git and branch protection
Create **private** GitHub repository, push your reviewed commits, turn on required PR reviews, status checks (`api`, `web`, `worker`), secret scanning and branch protection. `.github/CODEOWNERS` must be replaced with real handles. No credential files should be committed.

## Troubleshooting
- `401 Wrong API audience`: confirm Keycloak imported the mapper, sign out, login again for a fresh token.
- `401 Session revoked`: perform a fresh login; IdP SSO may still be active.
- `Keycloak SMTP`: registration is intentionally disabled in staging until working mail and verification are evidenced.
- `npm ci` failure: regenerate and review lockfile on connected host.
- Docker not present: tasks A-08–A-11 cannot be certified. Use Codex against your local Docker runtime, then share sanitized logs or diff.
