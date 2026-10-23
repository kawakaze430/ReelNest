package com.reelnest.playback;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Private CloudFront HLS: brief path-scoped signed cookies, never an S3 public URL.
 * Configure CloudFront trusted key group and OAC independently. PKCS#8 PEM required.
 */
@Component
public class CdnCookieSigner {
    private final String baseUrl, domain, keyId, pem;
    public CdnCookieSigner(@Value("${app.playback.cdn-base-url:}") String baseUrl,
      @Value("${app.playback.cdn-cookie-domain:}") String domain,
      @Value("${app.playback.cdn-key-pair-id:}") String keyId,
      @Value("${app.playback.cdn-private-key-pem:}") String pem) {
        this.baseUrl=baseUrl.replaceAll("/$", "");
        this.domain=domain;
        this.keyId=keyId;
        this.pem=pem;
        if (!this.baseUrl.isEmpty() && (!this.baseUrl.matches("https://[a-zA-Z0-9.-]+")
            || !domain.matches("\\.[a-zA-Z0-9.-]+") || keyId.isBlank() || pem.isBlank()))
            throw new IllegalStateException("Incomplete CloudFront signed-cookie configuration");
    }
    public boolean enabled() { return !baseUrl.isEmpty(); }
    public String manifestUrl(String key) { return baseUrl + "/" + key; }

    public void grant(HttpServletResponse response,String manifestKey) {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CDN signing unavailable");
        if (manifestKey==null || !manifestKey.matches("processed/[a-zA-Z0-9/_-]+/master\\.m3u8"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid processed-media path");
        String prefix=manifestKey.substring(0,manifestKey.length()-"master.m3u8".length());
        String resource=baseUrl+"/"+prefix+"*";
        long expiry=Instant.now().plusSeconds(120).getEpochSecond();
        String policy="{\"Statement\":[{\"Resource\":\""+resource
          +"\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"+expiry+"}}}]}";
        try {
            String plain=pem.replace("\\n", "\n").replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            PrivateKey key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(plain)));
            Signature rsa=Signature.getInstance("SHA1withRSA"); // Required by CloudFront RSA signed URLs/cookies
            rsa.initSign(key);
            rsa.update(policy.getBytes(StandardCharsets.UTF_8));
            cookie(response,"CloudFront-Policy",encode(policy.getBytes(StandardCharsets.UTF_8)));
            cookie(response,"CloudFront-Signature",encode(rsa.sign()));
            cookie(response,"CloudFront-Key-Pair-Id",keyId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"CDN signing failed",ex);
        }
    }
    private void cookie(HttpServletResponse response,String name,String value) {
        response.addHeader(HttpHeaders.SET_COOKIE,name+"="+value+"; Path=/; Domain="+domain
          +"; Max-Age=120; Secure; HttpOnly; SameSite=None");
    }
    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes)
            .replace('+','-').replace('=','_').replace('/','~');
    }
}
