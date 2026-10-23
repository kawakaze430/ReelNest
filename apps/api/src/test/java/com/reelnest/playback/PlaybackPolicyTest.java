package com.reelnest.playback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackPolicyTest {
    @Test void childRatingsFailClosed() {
        assertTrue(PlaybackPolicy.allowedAge(true,"U"));
        assertTrue(PlaybackPolicy.allowedAge(true,"U/A 7+"));
        assertTrue(PlaybackPolicy.allowedAge(true,"UA7"));
        assertFalse(PlaybackPolicy.allowedAge(true,"U/A 13+"));
        assertFalse(PlaybackPolicy.allowedAge(true,"U/A 16+"));
        assertFalse(PlaybackPolicy.allowedAge(true,"A"));
        assertFalse(PlaybackPolicy.allowedAge(true,null));
        assertTrue(PlaybackPolicy.allowedAge(false,"U/A 16+"));
    }
    @Test void mediaPathNeverEscapesPrefix() {
        assertTrue(PlaybackPolicy.safeRelativeMediaPath("master.m3u8"));
        assertTrue(PlaybackPolicy.safeRelativeMediaPath("360p/seg_00003.ts"));
        for (String bad : new String[]{"../secret.ts","foo/../secret.ts","/private/file.ts",
            "foo//seg.ts","foo/%2e%2e/secret.ts","https://other/a.ts","foo\\evil.ts",
            "not-video.mp4","./master.m3u8",""})
            assertFalse(PlaybackPolicy.safeRelativeMediaPath(bad),bad);
    }
    @Test void noPremiumWithoutBilling() {
        assertTrue(PlaybackPolicy.isFree("FREE"));
        assertFalse(PlaybackPolicy.isFree("PREMIUM"));
        assertFalse(PlaybackPolicy.isFree(null));
    }
}
