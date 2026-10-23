package com.reelnest.profiles;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PINs are not an alternative to OIDC; server-enforced extra verification for sensitive actions. */
public final class ProfilePinPolicy {
    private static final SecureRandom RANDOM=new SecureRandom();
    private ProfilePinPolicy() {}
    public static boolean valid(String pin) {return pin!=null && pin.matches("[0-9]{6,12}") && !pin.chars().allMatch(c->c==pin.charAt(0));}
    public static byte[] salt(){byte[] bytes=new byte[24];RANDOM.nextBytes(bytes);return bytes;}
    public static byte[] derive(String pin,byte[] salt) {
        if(!valid(pin) || salt==null || salt.length!=24)throw new IllegalArgumentException("Invalid PIN or salt");
        try {
            KeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,210_000,256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch(java.security.GeneralSecurityException e){throw new IllegalStateException("PIN protection unavailable",e);}
    }
    public static boolean matches(String pin,byte[] salt,byte[] hash){
        if(!valid(pin) || salt==null || hash==null || hash.length!=32)return false;
        return MessageDigest.isEqual(derive(pin,salt),hash);
    }
    public static boolean locked(java.time.Instant now,java.time.Instant until){return until!=null && now.isBefore(until);}
    public static java.time.Instant nextLock(java.time.Instant now,int failed){
        return failed>=5 ? now.plusSeconds(Math.min(3600L,60L*(1L<<Math.min(6,failed-5)))) : null;
    }
}
