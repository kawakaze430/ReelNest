package com.reelnest.support;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Two-person internal refund *review* only. Real Razorpay refund must use separately verified gateway controls. */
@RestController
@RequestMapping("/api/v1/admin/refund-reviews")
@PreAuthorize("hasRole('ADMIN')")
public class RefundReviewController {
 private final JdbcTemplate db;private final CurrentUserService users;
 public RefundReviewController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
 public record Reason(@NotBlank @Size(min=20,max=1000) String reason){}
 public record Review(UUID id,UUID supportCaseId,String status,String reason){}
 @PostMapping("/cases/{caseId}")
 @Transactional
 @ResponseStatus(HttpStatus.CREATED)
 public Review nominate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID caseId,@Valid @RequestBody Reason input){
    String[] state=db.query("SELECT category,status FROM support_cases WHERE id=? FOR UPDATE",
     rs->rs.next()?new String[]{rs.getString(1),rs.getString(2)}:null,caseId);
    if(state==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Billing support case not found");
    if(!RefundReviewPolicy.mayRequest(state[0],state[1]))
       throw new ResponseStatusException(HttpStatus.CONFLICT,"Only reviewed billing cases qualify");
    UUID id=UUID.randomUUID();
    db.update("""
      INSERT INTO refund_review_requests(id,support_case_id,requested_by,reason)
      VALUES(?,?,?,?)
      """,id,caseId,users.resolve(jwt).getId(),input.reason().strip());
    return new Review(id,caseId,"PENDING",input.reason().strip());
 }
 @GetMapping
 public List<Review> list(){return db.query("""
    SELECT id,support_case_id,status,reason FROM refund_review_requests
    ORDER BY created_at DESC LIMIT 100
    """,(r,i)->new Review(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4)));}
 @PostMapping("/{id}/approve")
 @Transactional
 public Review approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
    UUID reviewer=users.resolve(jwt).getId();
    var entry=db.query("""
      SELECT requested_by,support_case_id,status,reason FROM refund_review_requests
      WHERE id=? FOR UPDATE
      """,(r,i)->new Object[]{r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4)},id);
    if(entry.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found");
    Object[] row=entry.get(0);
    if(!RefundReviewPolicy.mayApprove((String)row[2],(UUID)row[0],reviewer))
       throw new ResponseStatusException(HttpStatus.CONFLICT,"A different administrator must review");
    db.update("""
      UPDATE refund_review_requests SET status='APPROVED_FOR_MANUAL_REVIEW',approved_by=?,reviewed_at=NOW()
      WHERE id=?
      """,reviewer,id);
    db.update("""
      INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
      VALUES(?,?,'REFUND_REVIEW_APPROVED','refund_review',?,?)
      """,UUID.randomUUID(),reviewer,id,"Human review only, no provider refund executed");
    return new Review(id,(UUID)row[1],"APPROVED_FOR_MANUAL_REVIEW",(String)row[3]);
 }
}
