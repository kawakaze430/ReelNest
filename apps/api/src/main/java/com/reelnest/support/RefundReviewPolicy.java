package com.reelnest.support;
import java.util.UUID;
/** Manual operations review. Never authorizes disbursement or alters billing entitlements. */
public final class RefundReviewPolicy {
 private RefundReviewPolicy(){}
 public static boolean mayRequest(String category,String status){
  return "BILLING".equals(category) && ("IN_REVIEW".equals(status)||"RESOLVED".equals(status));
 }
 public static boolean mayApprove(String state,UUID requester,UUID reviewer){
  return "PENDING".equals(state) && requester!=null && reviewer!=null && !requester.equals(reviewer);
 }
}
