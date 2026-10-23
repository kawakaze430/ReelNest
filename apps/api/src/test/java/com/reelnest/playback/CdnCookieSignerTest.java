package com.reelnest.playback;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CdnCookieSignerTest {
    @Test void disabledWithoutConfig() {
        var signer=new CdnCookieSigner("","","","");
        assertFalse(signer.enabled());
    }
    @Test void invalidCookieConfigurationFailsClosed() {
        assertThrows(IllegalStateException.class,()->new CdnCookieSigner(
            "http://not-secure",".example.com","KEY","secret"));
        assertThrows(IllegalStateException.class,()->new CdnCookieSigner(
            "https://stream.example.com","","KEY","secret"));
    }
}
