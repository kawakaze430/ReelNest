# Build 14 — Ten Netflix-style playback capabilities

Base: Build 13 cumulative project. This document distinguishes source delivery from end-to-end proof. No account keys or copyrighted movie files were added.

## Source implemented (10 tracked tasks)

| Task | Feature | Source change | Verification still required |
|---|---|---|---|
| E-04 | Transport controls | Play/pause, ±10 seconds, volume/mute slider and fullscreen, disabled until playback ready | Browser keyboard/video tests |
| E-05 | ABR and quality | Hls.js automatic and manual quality, current-resolution readout | Real rendition switching and network shaping |
| E-06 | Audio/subtitle selectors | Dynamic alternate-audio and timed-text tracks from authorized HLS; native Safari text-track handling | Multilingual encoded HLS on Safari + Chrome |
| E-07 | Subtitle styling | Caption sizing and high contrast using CSS `::cue` | Native browser compatibility and accessibility review |
| E-08 | Playback speed | 0.5–2x playback rate; device-local per-profile persistence | Refresh/profile switch browser test |
| E-14 | Accessible keyboard player | K/space, J/L, arrows, M, F, C and focus-visible controls | Screen reader and real keyboard checks |
| E-15 | Touch/rotation handling | Two-tap left/right seek and landscape-aware responsive video height | Real mobile double-tap/native controls and rotation tests |
| E-16 | Picture-in-picture | API capability check, enter/exit and state-change labels | Desktop and mobile PiP capability checks |
| E-19 | Playback error UX | Distinct entitlement error text, browser decode/network feedback, retry requests fresh session | Force 401/402/403, network failures, browser decode errors |
| E-21 | Data-saver mode | Hls.js ABR ceiling at or below 480p when available; protects manual selection | Measured bandwidth and player-switch test |

Extra: E-22 **partially implemented** — audio/subtitle preferences plus caption and speed settings are stored per profile on this device. Cross-device settings synchronization and native audio-track selection remain unimplemented. Do not check E-22 as globally complete.

## Known limitations

- These changes have not been verified with real Docker/Next.js builds, live authentication, CDN cookies, DRM, Safari devices or authorized 2+ track media. The release is not production-approved.
- Native Safari HLS controls ABR itself. It may not expose manual quality, native alternate audio or a data-saver cap through the JavaScript media element. Data saver is correctly labeled as unavailable there.
- Caption `::cue` styling is browser dependent. Some OS/native closed-caption displays override CSS.
- Device-local preferences use only whitelisted values and keyed localStorage, never authentication tokens. They don't sync between devices or log in to a central server.
- The source video must already have timed-text and audio renditions. This build does not synthesize subtitles or dub films.
- The sample clips in the repository are demo media; current catalog and entitlement restrictions are unchanged.

## Test evidence

Run `python3 -m pytest tests/release/test_build14_player.py -q` for compiler-backed pure helper tests and source-control assertions. Run `python3 scripts/release_gate.py --mode offline` for inherited checks. These are not equivalent to playback integration or device certification.

## Required next device test

1. Start Docker stack, authorize a known published movie, and open `/watch/{titleId}` on desktop and phone.
2. Test every button, keyboard shortcut and fullscreen/PiP support; verify failed playback cannot bypass entitlement.
3. Generate sample rights-owned assets with two audio renditions and at least two WebVTT subtitles; validate selection and persistence.
4. Throttle network to 3G/slow broadband; verify 480p cap when data saver is enabled and adaptive fallback under congestion.
5. Verify Safari uses signed-cookie CDN playback, not bearer-auth local proxy; test captions on device.
6. Upload logs and screenshots to the local/Codex evidence register; repair any discovered regressions.

## External launch gates

Maven build, Next.js production typecheck/build, Docker integration, provider payment tests, AWS staging, rights review and device playback are still NOT VERIFIED. Never treat source implementation as a cleared public release gate.
