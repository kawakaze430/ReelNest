package com.reelnest.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TitleResponseTest {
    @Test
    void mapsTitleFields() {
        TitleEntity entity = new TitleEntity();
        ReflectionTestUtils.setField(entity, "name", "Demo Feature");
        ReflectionTestUtils.setField(entity, "genre", "Drama");
        ReflectionTestUtils.setField(entity, "featured", true);
        TitleResponse result = TitleResponse.from(entity);
        assertEquals("Demo Feature", result.name());
        assertEquals("Drama", result.genre());
        assertTrue(result.featured());
    }
}
