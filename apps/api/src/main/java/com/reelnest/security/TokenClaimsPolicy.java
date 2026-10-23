package com.reelnest.security;

import java.util.Collection;
import java.time.Instant;

/** Defense in depth: reject well-signed tokens minted for a different API/client. */
public final class TokenClaimsPolicy {
    private TokenClaimsPolicy() {}
    public static boolean permitsAudience(Collection<String> audiences, String expected) {
        return expected != null && !expected.isBlank() && audiences != null
            && audiences.stream().anyMatch(expected::equals);
    }
    public static boolean issuedAfterRevocation(Instant issuedAt, Instant cutoff) {
        return cutoff == null || issuedAt != null && issuedAt.isAfter(cutoff);
    }
    public static boolean permitsClient(String authorizedParty, String expected) {
        return expected != null && !expected.isBlank() && expected.equals(authorizedParty);
    }
}
