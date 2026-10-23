package com.reelnest.admin;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Worker-owned review-state transition. Re-evaluates rights at execution time. */
@Component
public class PublicationScheduler {
 private final JdbcTemplate db;
 public PublicationScheduler(JdbcTemplate db){this.db=db;}
 public static boolean eligible(JdbcTemplate db,UUID titleId){
    Integer count=db.queryForObject("""
      SELECT COUNT(*) FROM titles t WHERE t.id=? AND t.status='IN_REVIEW'
      AND EXISTS(SELECT 1 FROM creators c WHERE c.id=t.creator_id AND c.status='APPROVED')
      AND EXISTS(SELECT 1 FROM content_rights r WHERE r.title_id=t.id AND r.status='APPROVED'
        AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE
        AND EXISTS(SELECT 1 FROM rights_territories rt WHERE rt.rights_id=r.id))
      AND EXISTS(SELECT 1 FROM media_assets m WHERE m.title_id=t.id
        AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
      """,Integer.class,titleId);
    return count!=null && count==1;
 }
 @Scheduled(fixedDelayString="${app.publication.poll-ms:60000}")
 @Transactional
 public void publishDue(){
    // SKIP LOCKED avoids double actions on multi-instance application deployment.
    List<UUID> due=db.query("""
      SELECT title_id FROM title_publication_schedules
      WHERE state='SCHEDULED' AND publish_at<=NOW()
      ORDER BY publish_at LIMIT 50 FOR UPDATE SKIP LOCKED
      """,(r,i)->r.getObject(1,UUID.class));
    for(UUID id:due){
      if(eligible(db,id)){
        db.update("UPDATE titles SET status='PUBLISHED' WHERE id=? AND status='IN_REVIEW'",id);
        db.update("UPDATE title_publication_schedules SET state='PUBLISHED',updated_at=NOW() WHERE title_id=?",id);
        db.update("""
           INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
           SELECT ?,requested_by,'SCHEDULED_TITLE_PUBLISHED','title',title_id,
             'Scheduled publish after validating rights and media'
           FROM title_publication_schedules WHERE title_id=?
           """,UUID.randomUUID(),id);
      }else{
        db.update("""
          UPDATE title_publication_schedules SET state='BLOCKED',
          last_error='Rights, creator, title state or READY media missing at publish time',updated_at=NOW()
          WHERE title_id=?
          """,id);
      }
    }
 }
 @Scheduled(cron="0 25 0 * * *",zone="UTC")
 @Transactional
 public void retireExpiredRights(){
   List<UUID> ids=db.query("""
      SELECT t.id FROM titles t WHERE t.status='PUBLISHED' AND t.creator_id IS NOT NULL
      AND NOT EXISTS(SELECT 1 FROM content_rights r WHERE r.title_id=t.id
        AND r.status='APPROVED' AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE)
      LIMIT 1000 FOR UPDATE OF t SKIP LOCKED
      """,(r,i)->r.getObject(1,UUID.class));
   for(UUID id:ids){
     db.update("UPDATE titles SET status='UNPUBLISHED',seo_index_approved=FALSE WHERE id=?",id);
     db.update("""
       UPDATE playback_sessions SET status='REVOKED',expires_at=NOW()
       WHERE title_id=? AND status='ACTIVE'
       """,id);
     db.update("""
       INSERT INTO system_publication_events(id,title_id,event_type,detail)
       VALUES(?,?,'RIGHTS_AUTO_UNPUBLISHED','Rights no longer active (UTC date)')
       """,UUID.randomUUID(),id);
   }
 }
}
