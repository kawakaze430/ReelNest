# Native catalog preview clients — Build 34

These are the first **functional read-only catalog clients** for Android/Android TV
and iOS/tvOS. They do NOT implement protected OTT playback, device authentication,
CDM / FairPlay licences, offline viewing, or payment.

Android: install JDK 17 and Android SDK, then use Gradle from `apps/mobile/android`:
`gradle :app:assembleDebug -PreelnestApiOrigin=https://api.your-approved-domain.example`.
This includes touch and focusable D-pad catalog items for TV shells. Android Gradle
Plugin 8.7.3 and Kotlin 2.0.21 are pinned, but the APK has **not** been built or
tested on a TV or Android handset in this handoff.

iOS / tvOS: install Xcode and XcodeGen on a macOS development host; from
`apps/mobile/ios`, run `xcodegen generate` and then build the
`ReelNestCatalogPreview` or `ReelNestTVPreview` scheme in Xcode. Replace the
`ReelNestAPIOrigin` Info.plist build setting with an approved HTTPS origin.
Neither Xcode build, device signing nor App Store/TestFlight review occurred.

All clients consume only `GET /api/v1/catalog` with no credentials. They never
use private manifests, reuse browser bearer tokens, or pretend that a public
title card grants streaming rights.

**Outstanding for M-02/M-03/M-07/M-08:** OIDC PKCE and secure device credentials,
parental PIN enforcement, protected HLS entitlement, Media3/AVPlayer device
playback, vendor DRM approval, captions, screen-reader/focus tests, physical
device compatibility matrix, signing and store certification.
