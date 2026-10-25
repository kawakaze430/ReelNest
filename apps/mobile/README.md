# Native catalog preview clients — Build 34

These are the first **functional read-only catalog clients** for Android/Android TV
and iOS/tvOS. They do NOT implement protected OTT playback, device authentication,
CDM / FairPlay licences, offline viewing, or payment.

Android requires JDK **17**, Gradle **8.9**, Android platform **35** and AGP's
build tools **34.0.0**. Android Gradle Plugin 8.7.3 and Kotlin 2.0.21 are pinned.
From `apps/mobile/android`, run:

```bash
gradle --no-daemon --console=plain :app:assembleDebug :app:assembleRelease :app:lintDebug \
  -PreelnestApiOrigin=https://api.example.org
```

The placeholder origin deliberately disables catalog network requests. Replace it
with a reviewed real HTTPS origin only for authorized functional testing. The
debug APK uses a generated development signature; the release APK is unsigned
because no release signing configuration is provided. This compiles touch and
focusable D-pad catalog UI, not a protected TV player. The launcher has original
banner artwork and explicitly makes touchscreen hardware optional; these satisfy
Android TV manifest lint requirements without suppressions.

iOS / tvOS requires Xcode and XcodeGen **2.44.1** on macOS. From
`apps/mobile/ios`, run `xcodegen generate --spec project.yml`, then:

```bash
xcodebuild -project ReelNestCatalogPreview.xcodeproj -scheme ReelNestCatalogPreview \
  -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY='' build
xcodebuild -project ReelNestCatalogPreview.xcodeproj -scheme ReelNestTVPreview \
  -configuration Debug -sdk appletvsimulator -destination 'generic/platform=tvOS Simulator' \
  -derivedDataPath build/tvos CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY='' build
```

The project uses Swift language mode **5.0**, not compiler-release number 5.9.
XcodeGen creates typed Info.plist properties, including the catalog URL-scheme
array. Its placeholder `ReelNestAPIOrigin` can be replaced in the two target
`info.properties` dictionaries for authorized testing. Generated project,
Info.plist and build outputs are local artifacts; do not commit them.

## Hosted native compilation

`.github/workflows/native-clients.yml` runs Android APK compilation and real lint
on `main` changes to the mobile source or workflow. It pins the Gradle/JDK/SDK
requirements above and retains compiler/lint reports and APK artifacts for seven
days. A debug APK signature is not production signing or installation evidence.

Unsigned iOS/tvOS simulator compilation is available only by manually dispatching
that workflow with `apple_simulators=true` (default **false**). This selects only
the Apple job and skips Android, so a missing Apple check does not repeat an
already-passing Android build. Normal source pushes and a manual dispatch with
the input false select Android. The Apple job uses one macOS
runner, checksum-pinned XcodeGen 2.44.1, records the installed Xcode/SDK versions,
and checks both actual built app plists. It consumes additional private CI
minutes; no Apple job is automatically dispatched. No developer account,
certificate, provisioning profile, device installation or store upload is used.

At commit `c55dea46a588a8f76422ed98a4d2f855640dc5bd`, the actual Android compilation
and lint job [passed in GitHub Actions](https://github.com/kawakaze430/ReelNest/actions/runs/40197670752).
The separate Apple-only run at `d4ae145a5b3c79104e623957ee2856ce2ab87265`
[passed both unsigned iOS/tvOS simulator builds and built-app plist checks](https://github.com/kawakaze430/ReelNest/actions/runs/40211313374).
GitHub API metadata is now accessible from the managed Linux environment, while
artifact-download hosts still require additional network access. Configuration
checks do not prove compiler or device behavior. Inspect exact-commit job results
before claiming a newer build passed.

All clients consume only `GET /api/v1/catalog` with no credentials. They never
use private manifests, reuse browser bearer tokens, or pretend that a public
title card grants streaming rights.

**Outstanding for M-02/M-03/M-07/M-08:** OIDC PKCE and secure device credentials,
parental PIN enforcement, protected HLS entitlement, Media3/AVPlayer device
playback, vendor DRM approval, captions, screen-reader/focus tests, physical
device compatibility matrix, signing and store certification.
