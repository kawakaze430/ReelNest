package com.reelnest.playback;


/** Small deterministic safety decisions, independently unit-tested. */
public final class PlaybackPolicy {
    private PlaybackPolicy() {}

    public static boolean allowedAge(boolean child, String ageRating) {
        if (ageRating == null) return false;
        if (!child) return true;
        return "U".equals(ageRating) || "U/A 7+".equals(ageRating) || "UA7".equals(ageRating);
    }

    /** Only files produced by our own HLS worker may be fetched by the dev proxy.
     * A generic .ts/.m3u8 suffix would allow arbitrary objects within a media prefix.
     */
    public static boolean safeRelativeMediaPath(String relative) {
        if (relative == null || relative.length() > 100) return false;
        return relative.equals("master.m3u8")
            || relative.matches("[0-9]{2,4}p/index\\.m3u8")
            || relative.matches("[0-9]{2,4}p/seg_[0-9]{5}\\.ts")
            || relative.equals("previews/preview.vtt")
            || relative.equals("previews/sprites.vtt")
            || relative.matches("previews/thumb_[0-9]{4}\\.jpg")
            || relative.matches("previews/sprite_[0-9]{2}\\.jpg")
            || relative.equals("artwork/poster.jpg")
            || relative.matches("audio/a0[0-7]/index\\.m3u8")
            || relative.matches("audio/a0[0-7]/seg_[0-9]{5}\\.ts")
            || relative.matches("captions/[a-z]{2,3}(-[a-z]{2,8})?/index\\.m3u8")
            || relative.matches("captions/[a-z]{2,3}(-[a-z]{2,8})?/captions\\.vtt");
    }

    public static boolean isFree(String tier) { return "FREE".equals(tier); }
}
