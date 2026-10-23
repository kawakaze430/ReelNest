package com.reelnest.playback;

/** Editorial playback markers measured in seconds from the start of an encoded asset.
 * These are UI hints, never a source of playback entitlement.
 */
public record PlaybackCues(Integer introStartSeconds, Integer introEndSeconds,
                           Integer recapStartSeconds, Integer recapEndSeconds,
                           Integer creditsStartSeconds) {
    public static final PlaybackCues EMPTY = new PlaybackCues(null,null,null,null,null);

    public boolean validFor(int durationSeconds) {
        if (durationSeconds <= 0) return false;
        return validRange(introStartSeconds,introEndSeconds,durationSeconds)
            && validRange(recapStartSeconds,recapEndSeconds,durationSeconds)
            && (creditsStartSeconds == null || (creditsStartSeconds >= 0 && creditsStartSeconds < durationSeconds));
    }

    private static boolean validRange(Integer start, Integer end, int duration) {
        if (start == null || end == null) return start == null && end == null;
        return start >= 0 && end > start && end <= duration;
    }
}
