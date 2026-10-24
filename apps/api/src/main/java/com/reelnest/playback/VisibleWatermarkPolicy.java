package com.reelnest.playback;

/** A visible browser overlay is a deterrent, NOT forensic video watermarking. */
public final class VisibleWatermarkPolicy {
    private VisibleWatermarkPolicy(){}
    public static boolean requiredFor(String approvedRightsBasis) {
        // LICENSED_DISTRIBUTOR means this platform does not own the film.
        // Owned originals may still need stronger vendor watermarking later.
        return "LICENSED_DISTRIBUTOR".equals(approvedRightsBasis);
    }
}
