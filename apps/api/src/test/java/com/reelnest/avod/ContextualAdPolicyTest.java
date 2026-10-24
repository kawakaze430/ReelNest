package com.reelnest.avod;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ContextualAdPolicyTest {
  @Test void noConsentOrChildOrPaidOrUnlicensedNeverReceivesAdPreview() {
    assertTrue(ContextualAdPolicy.previewAllowed(true,false,false,true,true,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(false,false,false,true,true,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,true,false,true,true,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,false,true,true,true,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,false,false,false,true,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,false,false,true,false,true,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,false,false,true,true,false,true));
    assertFalse(ContextualAdPolicy.previewAllowed(true,false,false,true,true,true,false));
  }
  @Test void measurementMustBeSeparatelyOptedInAndOnlyForEligibleAdult() {
    assertTrue(ContextualAdPolicy.measureAllowed(true,false,true,true));
    assertFalse(ContextualAdPolicy.measureAllowed(true,false,false,true));
    assertFalse(ContextualAdPolicy.measureAllowed(true,true,true,true));
    assertFalse(ContextualAdPolicy.measureAllowed(true,false,true,false));
  }
  @Test void creativeContentRejectsUnsafeMarkup() {
    assertTrue(ContextualAdPolicy.safeText("Original sponsor",100));
    assertFalse(ContextualAdPolicy.safeText("<script>alert(1)</script>",100));
    assertFalse(ContextualAdPolicy.safeText("a",100));
    assertFalse(ContextualAdPolicy.safeText("a\nb",100));
  }
}
