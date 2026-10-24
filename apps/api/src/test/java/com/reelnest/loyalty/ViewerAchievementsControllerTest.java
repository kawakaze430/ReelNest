package com.reelnest.loyalty;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ViewerAchievementsControllerTest {
    @Test void noMoneyOrAwardsWithoutOptIn() {
        var hidden=ViewerAchievementsController.summary(false,100);
        assertFalse(hidden.enabled());
        assertEquals(0,hidden.completedDistinctTitles());
        assertTrue(hidden.badges().stream().noneMatch(ViewerAchievementsController.Badge::earned));
        var one=ViewerAchievementsController.summary(true,1);
        assertTrue(one.badges().get(0).earned());
        assertFalse(one.badges().get(1).earned());
        var five=ViewerAchievementsController.summary(true,5);
        assertTrue(five.badges().stream().allMatch(ViewerAchievementsController.Badge::earned));
        assertTrue(five.scope().contains("non-redeemable"));
    }
}
