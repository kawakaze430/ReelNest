# 01 — Project charter and final product goal

**Product:** ReelNest, an ORIGINAL, commercially viable OTT/VOD service inspired by the capabilities (not copyrighted assets, logos, source code or unlicensed catalog) of Netflix and Prime Video.

**Owner's objective:** Eventually deploy a functional global streaming service on AWS, using a purchased domain, with browser, mobile and TV apps, multi-language adaptive playback, legitimate films, creator onboarding, subscriptions, robust authorization/DRM when needed, personalization, analytics, administration, availability and customer support.

**Product shape:** Marketplace/OTT hybrid. Independently licensed films and series are supplied by verified creators; administrators check the rights and classification; viewers with applicable entitlements can discover and watch permitted titles.

**Core business workflow (must actually work):**
1. Creator registers, is reviewed and creates a movie draft with metadata and signed streaming rights.
2. Creator multipart-uploads source video to private storage; worker verifies and transcodes to adaptive HLS, protecting posters, captions and alternate audio.
3. Admin reviews media quality, content classification and territorial/temporal licences, then publishes.
4. Viewer creates an account/profile; searches or browses a permitted title; optionally pays through a verified provider checkout.
5. API checks user/profile age, territory, licence, media readiness, active entitlement and concurrency; issues short-lived CDN access; HLS video plays and resume state is saved.
6. Operations can revoke access, handle copyright complaints, refund/dispute events, erasure, monitoring, incidents and licence expiry.

**Advanced end-state:** seasons/episodes; skip-intro/next-episode; 4K/HDR where codecs and rights allow; multiple audio/subtitles; trick-play previews; watch parties; EPG/optional live FAST; quality-of-experience analytics; full-text/semantic/ML recommendations; native Android/iOS/Android TV/tvOS and other TV clients; commercial DRM (Widevine/FairPlay/PlayReady) where contracts require; offline licensed playback; creator royalties/monetization; international currency, tax, privacy and language policies; multi-region resilience when data/scale justify.

**Non-negotiables:** no unauthorized copyrighted media, no deceptive Netflix branding, no circumvention of DRM; never treat UI hiding as server authorization; no live payments without verified sandbox tests; no irreversible cloud or financial actions without owner approval. **Worldwide technical reach is not worldwide content rights.**

**Out of scope until foundation passes:** public paid launch, global marketing claims, commercial DRM licensing, production key distribution, real creator payouts, genuine app-store approvals, production deployment. All require real external testing/approvals.
