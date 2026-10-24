# ReelNest Build 34 — implementation-focused cross-device and media extension

**GitHub** `kawakaze430/ReelNest`, `main`. Created October 24, 2026.

## Eight feature foundations (some grouped by platform)

1. **M-12 authenticated device profile handoff** — `POST /api/v1/device-handoff` creates a high-entropy, one-use 5-minute code for an owned profile. Only a device already signed in as the **same account** may redeem using `POST /redeem`. Redis stores SHA-256 of the code; an atomic Lua compare-owner-and-delete operation prevents replay and wrong-user code consumption. Redis admission rate limits apply. The web Profiles page has issue/redeem UI. The transfer chooses only a profile, **not a bearer token, session, TV login, manifest URL, or premium entitlement**. Full TV activation/PKCE/casting and real browser E2E remain.
2. **M-09 Tizen preview**, **M-10 webOS preview** — shared HTML/JS TV catalog with D-pad browsing, strongly validated HTTPS API origin and CSP, safe `textContent`, no cookies or private playback. `scripts/package_tv_clients.py` produces platform-specific preview manifests and files. Neither client is app-store signed, certified, hardware-tested, or capable of protected playback.
3. **D-13 opt-in 4K SDR H.264** — media-worker HLS ladder adds 2160p **only** when a genuine 3840×2160 SDR input is provided and `REELNEST_ENABLE_4K_SDR_H264=true`. Default maximum remains 1080p. FFprobe color-transfer metadata checks reject HDR rather than silently mishandling luminance/colors. This is not HEVC/AV1/HDR mastering/4K device qualification.
4. **E-17 Safari native HLS AirPlay chooser** — available only if the device advertises AirPlay and the player has an active authorized native HLS session. It never shares an OIDC token or media URL directly. AirPlay receiver entitlement behavior/hardware has not been tested; Chromecast is not integrated.

## Verified results and what they mean
- Normal GitHub API, Web, Worker and Docker-image CI ran on the implementation commits. Dedicated Build 34 offline guards/TV package tests are wired into `.github/workflows/checks.yml`. Refer to newest GitHub Actions run for exact results.
- No real TV device package certification, AirPlay receiver, 4K media decode/encode long-running E2E, or cross-device UI authentication acceptance has been completed.
- We keep the existing 282 task IDs, all older status columns and all production acceptance flags. We reclassify only 8 tasks from not-started to partial.

## Build 34 282-task audit
| Category | Tasks |
|---|---:|
| Component CI verified — production acceptance pending | 5 |
| Source implemented — runtime unverified | 67 |
| Partially implemented | 142 |
| Blocked external execution | 31 |
| Blocked external account/approval | 17 |
| **Not started/no audited implementation** | **20** |
| **Total** | **282** |

**The 20 still-unstarted features have NOT been completed.** They include Android and iOS native clients, Android/Apple TV hardware apps, DRM key-provider contracts, trusted offline license/download architecture, watermarking vendors, plan-change/proration, multi-region RDS consistency, advertising/ad fraud, live ingest, multi-tenant data isolation, HDR/Dolby mastering, and royalty payout systems. `docs/BUILD30_39_ADVANCED_AUDIT.json` describes dependencies for each.

## Release verdict
**NO-GO**. G1 full verified creator→HLS→licensed viewer workflow, G2 sandbox money/entitlements, G3 AWS staging, G4 security and licensed distribution, and G5 country-by-country launch remain open. No AWS, money movement, app-store publication, third-party DRM keys, or production data changes occurred.

**Next engineering priority:** finish the existing G1 integration, then security-tested same-account pairing on real devices, and turn the TV previews into certified, DRM-integrated clients. No source work is equivalent to paid playback rights.

## Native client source projects (M-02 and M-03)
- `apps/mobile/android`: a Java 17/Android Gradle Plugin native Kotlin `Activity`, real read-only public catalog request through HTTPS, touch UI and basic Android/Android-TV D-pad focus. No native Media3 protected playback, PKCE, certificate pinning, DRM/HDCP or installed-device test. Android API/Gradle compile remains unverified.
- `apps/mobile/ios`: SwiftUI public catalog navigation, `URLSession` HTTPS GET and signed-off playback-disabled detail; `project.yml` can be expanded with XcodeGen for iOS/tvOS. No Xcode or device build, FairPlay/AVPlayer licensed HLS, login or App Store/TestFlight acceptance.
- These are real source foundations only, not completed native OTT clients. Task M-07 (TV player) and M-08 (tvOS player) **remain not started** because the native previews do not implement a TV media player.

## G-10 subscription plan-change review (new)
The secured endpoint `GET /api/v1/billing/plan-change-preview?targetPlan=PREMIUM` requires a single active, provider-confirmed paid subscription, checks current and requested active INR plans, and returns the current list-price difference. It explicitly returns `automaticChangeAvailable=false` and `proratedAmountPaise=null`. The membership page offers a read-only preview and support link. No upgrade, downgrade, immediate charge, cancellation, refund, or entitlement mutation is permitted from this route. Merchant-approved proration, period boundaries, stateful intent, payment webhooks and E2E still need implementation and sandbox proof.
