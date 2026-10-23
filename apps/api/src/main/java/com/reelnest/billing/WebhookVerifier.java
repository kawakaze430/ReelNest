package com.reelnest.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verify HMAC over the unmodified raw request body, never a parsed or reserialized object. */
public final class WebhookVerifier {
    private WebhookVerifier() {}
    public static boolean verify(byte[] raw, String signature, String secret) {
        if (raw==null || signature==null || secret==null || secret.isBlank() || !signature.matches("[0-9a-fA-F]{64}")) return false;
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            byte[] expected=mac.doFinal(raw);
            byte[] supplied=new byte[32];
            for(int i=0;i<32;i++) supplied[i]=(byte)Integer.parseInt(signature.substring(i*2,i*2+2),16);
            return MessageDigest.isEqual(expected,supplied);
        } catch (Exception ex) { return false; }
    }
}
