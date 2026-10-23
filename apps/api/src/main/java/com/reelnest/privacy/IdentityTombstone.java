package com.reelnest.privacy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IdentityTombstone {
    private IdentityTombstone() {}
    public static String digest(String subject) {
        if(subject==null || subject.isBlank())throw new IllegalArgumentException("Identity subject required");
        try {
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(subject.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
}
