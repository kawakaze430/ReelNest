package com.reelnest.devices;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DeviceHandoffPolicyTest {
    @Test void secureCodesAreFixedLengthAndNeverPersistedRaw() {
        var first=DeviceHandoffPolicy.newCode();
        var second=DeviceHandoffPolicy.newCode();
        assertTrue(DeviceHandoffPolicy.validCode(first));
        assertTrue(DeviceHandoffPolicy.validCode(second));
        assertNotEquals(first,second);
        assertEquals(64,DeviceHandoffPolicy.digest(first).length());
        assertNotEquals(first,DeviceHandoffPolicy.digest(first));
        assertNotEquals(DeviceHandoffPolicy.digest(first),DeviceHandoffPolicy.digest(second));
    }
    @Test void invalidOrAmbiguousCodesAreRejected() {
        for(String value:new String[]{null,"","../bad","a".repeat(31),"a".repeat(33),"abcdef 12345678abcdef123456789012"}) {
            assertFalse(DeviceHandoffPolicy.validCode(value));
            assertThrows(IllegalArgumentException.class,()->DeviceHandoffPolicy.digest(value));
        }
    }
}
