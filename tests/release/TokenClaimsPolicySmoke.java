import com.reelnest.security.TokenClaimsPolicy;
import java.util.List;
import java.time.Instant;
public class TokenClaimsPolicySmoke {
  public static void main(String[] args) {
    if(!TokenClaimsPolicy.permitsAudience(List.of("reelnest-api","account"),"reelnest-api"))throw new AssertionError("missing valid aud");
    if(TokenClaimsPolicy.permitsAudience(List.of("account"),"reelnest-api"))throw new AssertionError("accepted wrong aud");
    if(TokenClaimsPolicy.permitsAudience(null,"reelnest-api"))throw new AssertionError("accepted empty aud");
    if(TokenClaimsPolicy.permitsClient("other-client","reelnest-web"))throw new AssertionError("accepted wrong azp");
    if(!TokenClaimsPolicy.permitsClient("reelnest-web","reelnest-web"))throw new AssertionError("rejected valid azp");
    var t=Instant.parse("2026-10-22T00:00:00Z");
    if(TokenClaimsPolicy.issuedAfterRevocation(t,t))throw new AssertionError("accepted token issued at cutoff");
    if(TokenClaimsPolicy.issuedAfterRevocation(t.minusSeconds(1),t))throw new AssertionError("accepted revoked token");
    if(!TokenClaimsPolicy.issuedAfterRevocation(t.plusSeconds(1),t))throw new AssertionError("rejected fresh token");
    if(TokenClaimsPolicy.issuedAfterRevocation(null,t))throw new AssertionError("accepted token without issue time");
    System.out.println("Token audience/client/revocation assertions: PASS");
  }
}
