package com.reelnest.devices;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Ephemeral code never grants authentication or media entitlement. */
public final class DeviceHandoffPolicy {
    private static final SecureRandom RANDOM = new SecureRandom();
    private DeviceHandoffPolicy() {}
    public static String newCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static boolean validCode(String code) {
        return code!=null && code.matches("[A-Za-z0-9_-]{32}");
    }
    public static String digest(String code) {
        if(!validCode(code))throw new IllegalArgumentException("Invalid handoff code");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.US_ASCII)));
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
