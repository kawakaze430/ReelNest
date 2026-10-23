import com.reelnest.notifications.NotificationPolicy;
import com.reelnest.analytics.AggregatePrivacyPolicy;
import com.reelnest.creators.RoyaltyEstimatePolicy;
import com.reelnest.billing.PromotionPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Build22PolicySmoke {
    static int n=0;
    static void check(boolean good,String message) {n++;if(!good)throw new AssertionError(message);}
    static void rejects(Runnable r){n++;try{r.run();throw new AssertionError("Expected exception");}catch(IllegalArgumentException ok){}}
    public static void main(String[] args) {
        var id=UUID.randomUUID();
        check(NotificationPolicy.expiryKey(id,LocalDate.of(2027,1,1),7).contains("rights:"),"expiry key");
        rejects(()->NotificationPolicy.expiryKey(id,LocalDate.now(),99));
        rejects(()->NotificationPolicy.bounded("x\nheader",100));
        check(AggregatePrivacyPolicy.safe(5,12),"k threshold");
        check(!AggregatePrivacyPolicy.safe(4,100),"no small cohorts");
        rejects(()->AggregatePrivacyPolicy.days(0));
        rejects(()->AggregatePrivacyPolicy.days(91));
        check(RoyaltyEstimatePolicy.shareMinorUnits(101,new BigDecimal("50"))==50,"bankers rounding");
        rejects(()->RoyaltyEstimatePolicy.shareMinorUnits(100,new BigDecimal("101")));
        check(PromotionPolicy.quoteMinor(14900,10)==13410,"quote calculation");
        check(PromotionPolicy.quoteMinor(29900,50)==14950,"quote half price");
        rejects(()->PromotionPolicy.quoteMinor(14900,95));
        rejects(()->PromotionPolicy.checkedCode("a' OR 1=1"));
        check("AUTUMN10".equals(PromotionPolicy.checkedCode("AUTUMN10")),"normalized code");
        check(!PromotionPolicy.active(Instant.now(),Instant.now().plusSeconds(10),Instant.now().plusSeconds(50)),"future promotion");
        System.out.println("Build22 standalone Java policy assertions passed: "+n);
    }
}
