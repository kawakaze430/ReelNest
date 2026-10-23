package com.reelnest.admin;
import java.time.Instant;
import java.util.UUID;
/** No blind admin bulk writes. Distinct requester/reviewer, digest and short expiry. */
public final class CatalogBatchPolicy {
    private CatalogBatchPolicy(){}
    public static boolean canApprove(String status, UUID requester, UUID reviewer, Instant expires, Instant now){
        return "PENDING".equals(status) && requester!=null && reviewer!=null && !requester.equals(reviewer)
            && expires!=null && now.isBefore(expires);
    }
    public static boolean canApply(String status,UUID requester,UUID reviewer,Instant expires,Instant now,boolean checksumValid){
        return "APPROVED".equals(status) && requester!=null && reviewer!=null && !requester.equals(reviewer)
            && expires!=null && now.isBefore(expires) && checksumValid;
    }
    public static String digest(String text){
        try {
            byte[] result=java.security.MessageDigest.getInstance("SHA-256")
                 .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(result);
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }
}
