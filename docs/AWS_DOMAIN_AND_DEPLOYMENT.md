# ReelNest — AWS and your own domain: exact operator guide

**Release status: only the STATIC public preview can be launched directly from this ZIP.** The full authenticated OTT application remains a staging candidate, not a verified public production application. No AWS account, payment account, domain, certificate or DNS entry has been created for you.

## Option A — put your website online today, without server/database (recommended first)

This path publishes a **clearly labelled interactive demonstration**, with a fictional catalog and an original 8-second test clip. It is not a subscription site. The upload-ready file is **`deploy/reelnest-amplify-preview.zip`** (inside the main cumulative ZIP). Do **not** upload the big source-code ZIP to Amplify Hosting as a manual static deployment.

1. Sign in to [AWS Console](https://console.aws.amazon.com/). Enable MFA on the root account, work from a non-root administrative identity, and set up AWS Budgets with billing alerts. Confirm charges before creating resources.
2. Open **AWS Amplify > Create new app > Deploy without Git**. Choose an app name such as `reelnest-preview`, branch `main`, method **Drag and drop**, then upload **`reelnest-amplify-preview.zip`**. The ZIP deliberately contains `index.html` **at its root**.
3. Select **Save and deploy**, then open the generated `https://main.<generated-id>.amplifyapp.com` URL provided by AWS. Check that search, genre filters, My List and sample-video playback work. All films in this preview are fictional; favorites are stored only in the user's browser.
4. Obtain a custom domain. In [Route 53 > Registered domains](https://console.aws.amazon.com/route53/) choose **Register domains**, search for an available domain (e.g. `reelnest-example.com`, not a claimed available name), verify contact information and finish purchase. You can instead buy from another registrar and retain it; Route 53 **registration is not required** for AWS hosting.
5. In **Amplify > your app > Hosting > Custom domains > Add domain**, enter the domain you actually own. Choose the `main` branch for `www` or the apex/root as preferred. Use an Amplify-managed HTTPS certificate. If the domain DNS is hosted by Route 53, Amplify can create records; with an external registrar/DNS provider, copy the exact validation and CNAME/ALIAS records that Amplify displays. Do **not** invent CloudFront endpoint names.
6. Wait for the console to report the domain **Available/Verified**, then check `https://www.yourdomain.example` or your chosen apex. HTTPS must be valid. DNS/certificate propagation can take time.
7. Whenever the static preview changes, regenerate it: `python3 scripts/prepare_public_preview.py`, then re-upload the produced nested ZIP. Verify again. It is normal for Amplify manual deployment to be a separate workflow from the dynamic Next.js application.

AWS official reference: https://docs.aws.amazon.com/amplify/latest/userguide/manual-deploys.html and https://docs.aws.amazon.com/amplify/latest/userguide/custom-domains.html

**Costs:** Amplify Hosting, data transfer and DNS/domain registration can incur charges. Monitor AWS Budgets. A domain is not automatically included with an AWS account or ChatGPT subscription.

## Option B — full web + API + videos + login on AWS (STAGING first)

### Intended public hostnames

| DNS name | Service | Where it runs |
|---|---|---|
| `www.yourdomain.example` | Static marketing/demo preview | Amplify Hosting (Option A) |
| `app.yourdomain.example` | Real Next.js viewer/creator/admin app | ECS Fargate behind HTTPS ALB |
| `api.yourdomain.example` | Spring Boot application API | ECS Fargate behind HTTPS ALB |
| `stream.yourdomain.example` | Private, signed HLS video | CloudFront + private S3 OAC |
| `identity.yourdomain.example` | External production OIDC/Keycloak | **Not provisioned by this Terraform** |

### Prerequisites (do not skip)

- Verified AWS account, MFA, least-privileged IAM, cost budget and alerts; choose AWS region (Terraform defaults to `ap-south-1` Mumbai). Availability and charges vary by account/region.
- Registered domain and a **Route 53 public hosted zone** for `yourdomain.example`. If domain was registered outside AWS and you're moving its DNS to Route 53, update nameservers at your registrar to the four in the new hosted zone. **Preserve existing MX/TXT and other records before changing nameservers.** Buying or changing registrar is optional.
- Valid ACM public certificate in **the ALB region** for `app.yourdomain.example` and `api.yourdomain.example`. Validate ownership via DNS.
- A **separate** ACM certificate in **us-east-1** for `stream.yourdomain.example`, which CloudFront requires for a custom HTTPS host.
- External persistent, secured HTTPS OIDC provider (Keycloak or compatible) with correct realm, redirect URIs, issuer/JWKS, token audience and account recovery. The local `start-dev` Keycloak is **NOT suitable for public deployment**. Its production deployment is unfinished.
- Proper streaming rights for every uploaded film, sound recording, artwork, trailer and subtitle, plus age/classification and customer/creator terms. No real payments until compliance and technical reviews pass.
- Workstation or CI runner with Docker/Compose, Java 21, Maven, Node 22, npm, Python 3, Terraform, AWS CLI and FFmpeg, plus credentials with constrained permissions. Do not put access keys into code.

### 1. Verify the code in local development

```bash
cd reelnest
python3 scripts/deployment_preflight.py
python3 scripts/release_gate.py --mode offline
(cd apps/api && mvn --batch-mode verify)
(cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build)
docker compose config
docker compose up --build -d
docker compose ps
python3 scripts/live_smoke.py
```

Investigate every failing check; run actual creator upload, media processing, admin review, playback and Razorpay TEST webhooks. The project is **not** proven runnable until these pass. `docker compose` is for local development only and exposes known development credentials on localhost; it must not be deployed to EC2 or the public internet unchanged.

### 2. Prepare a safe infrastructure plan

The Terraform **scaffold** is `infrastructure/aws`. Review `network.tf`, `media.tf`, `services.tf`, `variables.tf` and `versions.tf`. It includes costly resources even when ECS services are off: NAT gateway + Elastic IP, ALB, RDS, ElastiCache, CloudFront and WAF. Review estimated charges with the AWS Pricing Calculator before applying. It has **not** passed actual provider validation or live AWS provisioning in this authoring environment.

**Never run `terraform apply` blindly.** A proper staging workflow is:

```bash
cd infrastructure/aws
terraform fmt -recursive
terraform init -backend=false
terraform validate
cp terraform.tfvars.example terraform.tfvars
# Edit values ONLY after checking your own domain, AWS IDs, certs, images and OIDC.
terraform plan -var-file=terraform.tfvars -out=staging.tfplan
terraform show staging.tfplan
```

Do not commit `terraform.tfvars`, `.tfplan`, state files or private keys to Git. Before shared/team use, configure encrypted remote Terraform state, appropriate locking, limited IAM permissions and a documented secret procedure.

`enable_services = false` intentionally prevents ECS services, but **does not prevent provisioning of chargeable infrastructure**. A plan is not a deployment. Review and obtain approval before applying.

### 3. Make and store CloudFront signing keys, carefully

CloudFront custom-policy signed cookies use an asymmetric signing key. On an authorized machine:

```bash
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out reelnest-cf-private.pem
openssl pkey -in reelnest-cf-private.pem -pubout -out reelnest-cf-public.pem
```

Store the **private key outside the repository**, then upload it as a secret value only after the Terraform Secrets Manager secret is created. The **public** key is safe to use for `cdn_public_key_pem` in the Terraform variables, but check its PEM formatting. Rotate keys on a documented schedule. Do not paste a private key into ChatGPT or GitHub. Signed cookies alone are **not DRM**.

### 4. Build and publish separate application images (after local builds pass)

When the three ECR repositories have been created, discover their exact URLs from `terraform output -json ecr_repositories`. Use AWS CLI authentication to your own ECR account:

```bash
AWS_REGION=ap-south-1
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
```

At repository root, build images locally. Example values below must be replaced with your real, validated domains. For production Next.js public environment variables are **embedded at build time**; updating ECS environment after building **does not rewrite them**:

```bash
docker build -t reelnest-api:staging ./apps/api
docker build -t reelnest-worker:staging ./apps/video-worker
docker build --target production \
  --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.yourdomain.example \
  --build-arg NEXT_PUBLIC_AUTHORITY=https://identity.yourdomain.example/realms/reelnest \
  --build-arg NEXT_PUBLIC_APP_ORIGIN=https://app.yourdomain.example \
  --build-arg NEXT_PUBLIC_OIDC_CLIENT_ID=reelnest-web \
  -t reelnest-web:staging ./apps/web
```

Tag and push these images to the ECR repositories in **your account**. Collect immutable image digests and set `api_image`, `web_image` and `worker_image` to the digest-pinned URLs. A tag like `latest` is not a reproducible release. Keep `enable_services=false` until the next prerequisites are complete.

### 5. Database migrations, roles and secrets

The runtime app config expects `reelnest_app` and a Secrets Manager password. **Terraform creates the empty secret entry, not the SQL role or a working password value**. Follow `scripts/sql/provision_runtime_role.sql` after applying V1–V8 Flyway migrations with a privileged migration principal via a secure, private, audited connection (e.g. temporary ECS migration job with approved credentials). Review the SQL grants for all existing schema objects and sequences, future tables, and the worker's DB writes.

Verify that runtime app credentials **cannot** run arbitrary DDL. Do not accidentally disable migrations in the migration job (`SPRING_FLYWAY_ENABLED` should be enabled only for the privileged migration step). The API ECS task intentionally has no access to the RDS master-user password. Use Secrets Manager and IAM roles; never paste secrets into Terraform state or Docker build arguments.

### 6. OIDC, CDN geofencing and enabling services

The Terraform does not create a production Keycloak service. Deploy and test one separately or use a compatible managed OIDC service after making the required client/issuer changes. The issuer/JWKS must be reachable from ECS and must match tokens issued to the browser.

CloudFront restricts video delivery by viewer IP country, and the backend uses a configured country (default India) for rights queries. **That backend constant is not a cryptographically verified viewer territory.** The geographic integration, VPN risk review and edge bypass tests are **still required**. Never set `reviewed_geo_enforcement=true` purely to make a deployment start.

Test CORS credentials, signed cookies on `stream`, private origin access, media manifest/segment MIME types, refresh, age restrictions, rights expiry and revocation. When evidence and external approval exist, enable services in reviewed Terraform change and apply to **staging**, then run end-to-end tests.

### 7. Domain DNS and HTTPS checks

The Terraform intends to create Route 53 **A alias records** for `app` and `api` pointing to the Application Load Balancer, and `stream` pointing to CloudFront. Make sure the hosted zone is delegated and certificate subject names cover those exact hosts.

From a client machine, check:

```bash
# Replace the examples with real deployed names:
nslookup app.yourdomain.example
curl -I https://app.yourdomain.example
curl -i https://api.yourdomain.example/actuator/health
# Without authorization, direct HLS requests must be denied:
curl -i https://stream.yourdomain.example/nonexistent/master.m3u8
```

A 404 from a nonexistent file is NOT sufficient proof private S3 is protected: test a **known existing protected manifest** without cookies (must not return the content) and with valid/expired credentials. Never publicly expose the source bucket or staging development accounts.

### 8. Razorpay and legal checks before real customers

Use Razorpay **TEST mode**; generate test key/secret and webhook secret in the merchant dashboard, configure the exact public HTTPS staging webhook and supported event subscriptions, and run refund, failure, dispute, renewal, idempotency and expiry tests. Do not put secrets in the browser or committed `.env`. Only after passing merchant TEST and security review can you plan live payment credentials.

Independently complete privacy erasure (the current endpoint only creates a review request), administration MFA, restore rehearsals, uptime monitoring and alerts, vulnerability scans, content ownership, age classification and relevant Indian consumer/OTT/privacy compliance.

### 9. Do not overpromise production readiness

- The preview is a **functional visual demo**, not live authenticated OTT.
- The **full application is source implemented but not end-to-end verified**. Terraform is a scaffold, not a deployed cluster.
- Production launch requires operator-controlled resources, genuine secrets, a domain, licensed content and tests; these cannot be completed offline or from a ZIP alone.
- Do not share AWS account credentials, API secrets, payment keys or private signing keys in chat.

## Official AWS documentation

- Amplify manual static deployment: https://docs.aws.amazon.com/amplify/latest/userguide/manual-deploys.html
- Amplify custom domains: https://docs.aws.amazon.com/amplify/latest/userguide/custom-domains.html
- Route 53 domain registration: https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/domain-register.html
- Route 53 external-DNS delegation: https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/dns-configuring-new-domain.html
- CloudFront ACM certificates: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/cnames-and-https-requirements.html
- Razorpay test/webhook validation: https://razorpay.com/docs/webhooks/validate-test/
