# Dependency inventory and compatibility matrix (Build 19)

| Layer | Declared | Reviewed minimum / operator action |
|---|---|---|
| JVM | Java 21 | 21 LTS (compilation in Docker Maven image requires internet) |
| Spring Boot | 3.5.16 | Verified published release; last OSS 3.5.x. Plan reviewed upgrade to supported 4.x before public launch; do not blindly bump across migration |
| Maven build | 3.9.11 Docker image | Real `mvn verify` required |
| Web | Next.js `^16.4.1` | 16.4.1 is minimum patched for October 2026 critical advisories. Review newer patches when generating lockfile |
| React | `^19.2.0` | Verify resolved version and React RSC advisories in live npm audit |
| Node.js | 22 Docker image | Use supported Node.js LTS and `npm ci` after reviewing lockfile |
| Keycloak | 26.8.1 | Verify exact image and realm-import / audience mapper on local stack |
| PostgreSQL | 17 Alpine | Test migrations V1–V15 and backups |
| RabbitMQ | 4.1 Alpine | Pin reviewed immutable image digest before production |
| Redis | 7.4 Alpine | Authenticate/restrict to private network |
| MinIO | latest in local Compose | Development only; pin digest and switch to private S3 for deployment |
| Python API | requirements in `apps/recommendation-api/` | Use pip audit and pinned lockfiles for production |
| Video worker | Python + FFmpeg | Verify codec support, media isolation and CVEs |
| AWS S3 SDK | 2.31.58 BOM | Review against current supported dependency versions |
| Terraform | external CLI | `terraform init -backend=false`, `terraform validate`, plan review required |

## External security evidence

As of 22 Oct 2026, Next.js 16.x below 16.4.1 is affected by critical remote execution and other issues. Sources:
- https://nextjs.org/blog/nextjs-security-update-october-14-2026
- https://www.csa.gov.sg/alerts-and-advisories/alerts/al-2026-148/

Spring Boot 3.5.16 is the last open-source supported release of its 3.5.x line; future migration is needed:
- https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/

**No dependency scan was performed against installed packages here** because npm/Maven registries were unavailable. Dependabot and a real npm audit in CI provide future evidence, not retrospective clearance.
