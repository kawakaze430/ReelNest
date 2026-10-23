package com.reelnest.playback;

/** Deterministic resume policy. Avoid resuming into credits or beyond the media end. */
public final class ResumePolicy {
    private ResumePolicy() {}
    public static int resumePosition(int storedSeconds,int durationSeconds) {
        if (storedSeconds <= 0 || durationSeconds <= 0) return 0;
        int bounded=Math.min(storedSeconds,durationSeconds);
        // Do not reopen a completed movie in its final 5% or last 30 seconds.
        int nearEnd=Math.max(1,Math.max((int)Math.ceil(durationSeconds*0.95),durationSeconds-30));
        if (bounded >= nearEnd) return 0;
        return bounded;
    }
    public static int boundedProgress(int requestedSeconds,int durationSeconds) {
        if (requestedSeconds < 0 || durationSeconds <= 0 || requestedSeconds > durationSeconds+15)
            throw new IllegalArgumentException("Invalid progress or duration");
        return Math.min(requestedSeconds,durationSeconds);
    }
}
