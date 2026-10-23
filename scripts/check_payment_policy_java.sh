#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
cp "$ROOT/apps/api/src/main/java/com/reelnest/billing/WebhookVerifier.java" "$TMP/"
cp "$ROOT/apps/api/src/main/java/com/reelnest/billing/EntitlementPolicy.java" "$TMP/"
mkdir -p "$TMP/classes"
cat > "$TMP/PaymentCheck.java" <<'JAVA'
import com.reelnest.billing.WebhookVerifier;
import com.reelnest.billing.EntitlementPolicy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
public class PaymentCheck {
  static void check(boolean b) {if(!b)throw new AssertionError("Security policy failed");}
  public static void main(String[]args) throws Exception {
    byte[] raw="{\"event\":\"subscription.charged\"}".getBytes(StandardCharsets.UTF_8);
    Mac mac=Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("demo-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
    String signature=HexFormat.of().formatHex(mac.doFinal(raw));
    check(WebhookVerifier.verify(raw,signature,"demo-secret"));
    check(!WebhookVerifier.verify(raw,signature,"wrong"));
    check(!WebhookVerifier.verify("{}".getBytes(StandardCharsets.UTF_8),signature,"demo-secret"));
    check(!WebhookVerifier.verify(raw,"z".repeat(64),"demo-secret"));
    Instant now=Instant.parse("2026-10-21T13:00:00Z");
    check(EntitlementPolicy.hasPremium("ACTIVE","STANDARD",now.plusSeconds(100),now));
    check(EntitlementPolicy.hasPremium("CANCEL_SCHEDULED","PREMIUM",now.plusSeconds(1),now));
    check(!EntitlementPolicy.hasPremium("ACTIVE","FREE",now.plusSeconds(100),now));
    check(!EntitlementPolicy.hasPremium("PENDING","PREMIUM",now.plusSeconds(100),now));
    check(!EntitlementPolicy.hasPremium("ACTIVE","PREMIUM",now,now));
    check(!EntitlementPolicy.hasPremium("ACTIVE","PREMIUM",null,now));
    check("PAST_DUE".equals(EntitlementPolicy.mappedProviderStatus("halted",false)));
    check("CANCEL_SCHEDULED".equals(EntitlementPolicy.mappedProviderStatus("active",true)));
    System.out.println("PASS: Java payment signature and entitlement policy assertions");
  }
}
JAVA
javac -d "$TMP/classes" "$TMP/WebhookVerifier.java" "$TMP/EntitlementPolicy.java" "$TMP/PaymentCheck.java"
java -cp "$TMP/classes" PaymentCheck
