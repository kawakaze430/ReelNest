# ReelNest: DRM / SPEKE integration architecture (source design, NOT enabled)

**Status:** Build 30 architecture review draft — no keys, licenses, DRM-enabled playable media, or vendor provisioning. Tracks task **F-12** and dependencies F-13 through F-18.

## Service boundaries
1. A rights-verified title with confirmed tenant, territory and access tier is the *only* eligible source for optional encrypted renditions.
2. The worker requests a short-lived packaging job from a separate audited orchestration service, never raw content keys. A selected, contracted packager talks to an approved KMS/DRM key provider using a version-pinned SPEKE v2/CPIX-compatible interface. The KMS/DRM trust boundary must be independently reviewed.
3. Encrypted CMAF/fMP4 segments and DASH/HLS manifests are stored in the private processed origin with per-title/version provenance. Reject clear-key URLs, external manifest references, plaintext key logging, and any publicly readable origin.
4. The playback API remains the *authorization decision point*: it validates user, active subscription, profile child restrictions, active rights territory/time window, asset variant, session concurrency and revocation before issuing a **device-bound, scope-limited license request** to a trusted license proxy.
5. The license proxy has independently rotated service credentials, minimal client-visible metadata, rate limits and detailed revocation logs. No licence is issued solely because a player requests it; each request must re-check backend entitlements.
6. All capability negotiation occurs **after** a verified licence origin/provider is configured. If a device lacks the required EME/CDM platform (Widevine for suitable Android/Chromium; FairPlay for iOS/Safari/tvOS; PlayReady for certified Windows/TV), the client fails closed with an unsupported-device message; **never** silently switch a premium title to a clear stream.
7. Offline licences and downloads are disabled by default. They require content-contract approval, licence expiry/renewal caps, per-device key storage and assured deletion/revocation tests.
8. Forensic watermarking is an independent licensed vendor track with imperceptibility, robustness, disclosure and privacy review. Do not embed personally identifying details or secrets in overlays.

## Explicit vendor dependencies
- Content-owner contract allowing the proposed DRM systems, devices, territories, HDCP policy, offline viewing and forensic watermarking.
- Packager/key provider and individual DRM operator accounts/contracts; platform certificates/key requests and SDK approval where applicable.
- KMS IAM, secret rotation, origin-CDN security review, per-playback licence proxy attestation, backup and incident procedures.
- Real Widevine/FairPlay/PlayReady devices with positive/negative tests, device-loss and session-revocation proofs, license-expiry clock skew testing, threat review and representative video QA.

## Non-goals and fail-closed criteria
Do not call this feature complete until end-to-end encrypted packing and licensed playback, entitled/unauthorized clients, offline denial, revoked sessions and CDN-origin bypass tests are independently logged. No public free-tier demo ever proves DRM. No raw content keys, real certificates or private vendor credentials belong in Git or CI logs.

## Next code dependency graph
`rights+assets entitlement` -> `DRM policy model` -> `key provider/KMS` -> `packager SPEKE adapter` -> `trusted license proxy` -> `EME/native player adapters` -> `device & denial matrix` -> `operational runbooks`. Each step must have test-only keys and vendor account approval before live use.
