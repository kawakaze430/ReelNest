package com.reelnest.playback;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class VisibleWatermarkPolicyTest {
  @Test void markLicensedDistributionButNeverTrustClientFlags() {
    assertTrue(VisibleWatermarkPolicy.requiredFor("LICENSED_DISTRIBUTOR"));
    assertFalse(VisibleWatermarkPolicy.requiredFor("OWNER"));
    assertFalse(VisibleWatermarkPolicy.requiredFor("unknown"));
    assertFalse(VisibleWatermarkPolicy.requiredFor(null));
  }
}
