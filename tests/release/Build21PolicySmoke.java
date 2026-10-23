import com.reelnest.profiles.ProfilePinPolicy;
import com.reelnest.admin.CatalogBatchPolicy;
import com.reelnest.admin.PublicationSchedulePolicy;
import com.reelnest.support.RefundReviewPolicy;
import com.reelnest.catalog.SearchTextPolicy;
import java.time.Instant;
import java.util.UUID;
public class Build21PolicySmoke {
 static int count=0;
 static void yes(boolean ok,String reason){if(!ok)throw new AssertionError(reason);count++;}
 public static void main(String[] args) {
  var p1=UUID.randomUUID();var p2=UUID.randomUUID();Instant now=Instant.now();
  yes(!ProfilePinPolicy.valid("1234"),"short PIN");
  yes(!ProfilePinPolicy.valid("111111"),"trivial PIN");
  yes(ProfilePinPolicy.valid("123456"),"valid PIN");
  var salt=ProfilePinPolicy.salt();yes(salt.length==24,"salt length");
  var digest=ProfilePinPolicy.derive("123456",salt);
  yes(digest.length==32,"digest length");
  yes(ProfilePinPolicy.matches("123456",salt,digest),"matching PIN");
  yes(!ProfilePinPolicy.matches("123457",salt,digest),"wrong PIN");
  yes(!ProfilePinPolicy.matches(null,salt,digest),"missing PIN");
  yes(!ProfilePinPolicy.locked(now,now.minusSeconds(1)),"expired lock");
  yes(ProfilePinPolicy.locked(now,now.plusSeconds(1)),"active lock");
  yes(ProfilePinPolicy.nextLock(now,4)==null,"first four attempts");
  yes(ProfilePinPolicy.nextLock(now,5).isAfter(now),"fifth attempt locks");
  yes(CatalogBatchPolicy.canApprove("PENDING",p1,p2,now.plusSeconds(60),now),"different approver");
  yes(!CatalogBatchPolicy.canApprove("PENDING",p1,p1,now.plusSeconds(60),now),"same actor blocked");
  yes(!CatalogBatchPolicy.canApprove("PENDING",p1,p2,now.minusSeconds(1),now),"expired approval");
  yes(CatalogBatchPolicy.canApply("APPROVED",p1,p2,now.plusSeconds(60),now,true),"valid batch");
  yes(!CatalogBatchPolicy.canApply("APPROVED",p1,p2,now.plusSeconds(60),now,false),"tamper block");
  yes(!CatalogBatchPolicy.canApply("PENDING",p1,p2,now.plusSeconds(60),now,true),"unapproved block");
  yes(CatalogBatchPolicy.digest("csv").length()==64,"digest size");
  yes(!CatalogBatchPolicy.digest("csv").equals(CatalogBatchPolicy.digest("other")),"digest changes");
  yes(PublicationSchedulePolicy.canSchedule("IN_REVIEW",now.plusSeconds(3600),now),"schedule review");
  yes(!PublicationSchedulePolicy.canSchedule("PUBLISHED",now.plusSeconds(3600),now),"no published reschedule");
  yes(!PublicationSchedulePolicy.canSchedule("IN_REVIEW",now.plusSeconds(3),now),"minimum buffer");
  yes(!PublicationSchedulePolicy.canSchedule("IN_REVIEW",now.plusSeconds(400L*86400),now),"maximum horizon");
  yes(PublicationSchedulePolicy.canPublish("IN_REVIEW",true,true,true),"ready and licensed");
  yes(!PublicationSchedulePolicy.canPublish("IN_REVIEW",true,false,true),"expired rights");
  yes(!PublicationSchedulePolicy.canPublish("DRAFT",true,true,true),"unreviewed");
  yes(RefundReviewPolicy.mayRequest("BILLING","RESOLVED"),"billing case");
  yes(!RefundReviewPolicy.mayRequest("TECHNICAL","RESOLVED"),"no technical refunds");
  yes(RefundReviewPolicy.mayApprove("PENDING",p1,p2),"different reviewer");
  yes(!RefundReviewPolicy.mayApprove("PENDING",p1,p1),"same reviewer rejected");
  yes(SearchTextPolicy.likeLiteral("a%b_c!").equals("%a!%b!_c!!%"),"escaping wildcards");
  yes(SearchTextPolicy.foldLatin("Caf\u00e9").equals("cafe"),"accent fold");
  System.out.println(count+" Build 21 policy assertions passed");
 }
}
