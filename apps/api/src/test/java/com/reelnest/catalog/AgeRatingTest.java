package com.reelnest.catalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AgeRatingTest {
    @Test void creatorCodesMapToCanonicalCatalogLabels() {
        assertEquals("U/A 7+",AgeRating.canonical("UA7"));
        assertEquals("U/A 13+",AgeRating.canonical("UA13"));
        assertEquals("U/A 16+",AgeRating.canonical("UA16"));
        assertEquals("U/A 7+",AgeRating.canonical("U/A 7+"));
        assertThrows(IllegalArgumentException.class,()->AgeRating.canonical("unknown"));
    }
}
