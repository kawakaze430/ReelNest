package com.reelnest.admin;

import com.reelnest.users.CurrentUserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Two-person bulk metadata process. Applies only to DRAFT titles; not publication, rights or pricing. */
@RestController
@RequestMapping("/api/v1/admin/catalog/import")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogBatchController {
    private final JdbcTemplate db;private final CurrentUserService users;
    public CatalogBatchController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record Batch(UUID id,String status,int rows,String digest,Instant expiresAt){}
    public record ApplyResult(UUID batchId,int updated){}
    private record DbBatch(UUID requester,UUID reviewer,String payload,String digest,String fingerprint,String status,Instant expires){}
    private DbBatch load(UUID id){
       List<DbBatch> rows=db.query("""
         SELECT requested_by,approved_by,payload,payload_sha256,base_fingerprint,status,expires_at
         FROM catalog_import_batches WHERE id=? FOR UPDATE
         """,(r,i)->new DbBatch(r.getObject(1,UUID.class),r.getObject(2,UUID.class),
           r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getTimestamp(7).toInstant()),id);
       if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Import batch not found");
       return rows.get(0);
    }
    private String snapshot(List<CatalogImportPreview.Row> rows){
       StringBuilder raw=new StringBuilder();
       for(var row:rows){
           List<List<String>> versions=db.query("""
             SELECT name,genre,language,age_rating,status FROM titles
             WHERE id=? FOR SHARE
             """,(r,i)->Arrays.asList(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)),row.id());
           if(versions.size()!=1 || !"DRAFT".equals(versions.get(0).get(4)))
               throw new ResponseStatusException(HttpStatus.CONFLICT,"All imported titles must remain DRAFT");
           raw.append(row.id()).append('|');
           for(String cell:versions.get(0)){
               if(cell==null)throw new ResponseStatusException(HttpStatus.CONFLICT,
                   "Draft metadata contains null values; correct it before staging");
               raw.append(cell.length()).append(':').append(cell).append('|');
           }
       }
       return CatalogBatchPolicy.digest(raw.toString());
    }
    @PostMapping(value="/stage",consumes="text/csv")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Batch stage(@AuthenticationPrincipal Jwt jwt,@RequestBody String csv){
       var parsed=CatalogImportPreview.parse(csv);
       if(!parsed.valid())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
           "Invalid import preview: "+String.join("; ",parsed.errors()));
       // Never stage unknown IDs or live catalog edits: this is only a DRAFT metadata editor.
       for(var row:parsed.rows()){
          var status=db.query("SELECT status FROM titles WHERE id=?",rs->rs.next()?rs.getString(1):null,row.id());
          if(!"DRAFT".equals(status))throw new ResponseStatusException(HttpStatus.CONFLICT,
                "All rows must reference existing DRAFT titles");
       }
       UUID id=UUID.randomUUID(),actor=users.resolve(jwt).getId();
       Instant expiry=Instant.now().plus(30,ChronoUnit.MINUTES);
       String hash=CatalogBatchPolicy.digest(csv);
       String baseFingerprint=snapshot(parsed.rows());
       db.update("""
         INSERT INTO catalog_import_batches(id,requested_by,payload,payload_sha256,base_fingerprint,expires_at)
         VALUES (?,?,?,?,?,?)
         """,id,actor,csv,hash,baseFingerprint,java.sql.Timestamp.from(expiry));
       return new Batch(id,"PENDING",parsed.rows().size(),hash,expiry);
    }
    @PostMapping("/{id}/approve")
    @Transactional
    public Batch approve(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
       UUID reviewer=users.resolve(jwt).getId();DbBatch b=load(id);
       if(!CatalogBatchPolicy.canApprove(b.status(),b.requester(),reviewer,b.expires(),Instant.now()))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"A different administrator must approve before expiry");
       db.update("UPDATE catalog_import_batches SET approved_by=?,status='APPROVED',approved_at=NOW() WHERE id=?",reviewer,id);
       return new Batch(id,"APPROVED",CatalogImportPreview.parse(b.payload()).rows().size(),b.digest(),b.expires());
    }
    @PostMapping("/{id}/apply")
    @Transactional
    public ApplyResult apply(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
       UUID actor=users.resolve(jwt).getId();DbBatch b=load(id);
       if(!CatalogBatchPolicy.canApply(b.status(),b.requester(),b.reviewer(),b.expires(),Instant.now(),
           CatalogBatchPolicy.digest(b.payload()).equals(b.digest())))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Import not approved, expired or tampered with");
       var parsed=CatalogImportPreview.parse(b.payload());
       if(!parsed.valid())throw new ResponseStatusException(HttpStatus.CONFLICT,"Staged CSV no longer valid");
       if(!snapshot(parsed.rows()).equals(b.fingerprint()))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Draft metadata changed since staging; restage and reapprove");
       // Avoid inconsistent partial edits: any row changed from DRAFT invalidates entire transaction.
       int updated=0;
       for(var row:parsed.rows()) {
           int n=db.update("""
             UPDATE titles SET name=?,genre=?,language=?,age_rating=? WHERE id=? AND status='DRAFT'
             """,row.name(),row.genre(),row.language(),com.reelnest.catalog.AgeRating.canonical(row.ageRating()),row.id());
           if(n!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Title changed since preview; batch not applied");
           updated+=n;
       }
       db.update("UPDATE catalog_import_batches SET status='APPLIED',applied_at=NOW() WHERE id=?",id);
       db.update("""
         INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
         VALUES(?,?,'CATALOG_BULK_APPLIED','catalog_batch',?,?)
         """,UUID.randomUUID(),actor,id,"Rows="+updated+", sha256="+b.digest());
       return new ApplyResult(id,updated);
    }
    @PostMapping("/{id}/reject")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
       UUID actor=users.resolve(jwt).getId();DbBatch b=load(id);
       if(!"PENDING".equals(b.status()) && !"APPROVED".equals(b.status()))
          throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot reject this batch");
       db.update("UPDATE catalog_import_batches SET status='REJECTED' WHERE id=?",id);
       db.update("""
         INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
         VALUES(?,?,'CATALOG_IMPORT_REJECTED','catalog_batch',?,?)
         """,UUID.randomUUID(),actor,id,"Rejected by admin");
    }
}
