package com.reelnest.uploads;

import com.reelnest.catalog.TitleRepository;
import com.reelnest.creators.CreatorService;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

@Service
public class UploadService {
    private final CreatorService creators;
    private final TitleRepository titles;
    private final JdbcTemplate jdbc;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final TransactionTemplate tx;
    public UploadService(CreatorService creators, TitleRepository titles, JdbcTemplate jdbc,
        S3Client s3, S3Presigner presigner, PlatformTransactionManager transactionManager,
        @Value("${app.storage.bucket}") String bucket) {
        this.creators = creators; this.titles = titles; this.jdbc = jdbc;
        this.s3 = s3; this.presigner = presigner; this.bucket = bucket;
        this.tx = new TransactionTemplate(transactionManager);
    }
    public record BeginRequest(@NotBlank String filename, @NotBlank String contentType,
                               @Min(1) @Max(5368709120L) long sizeBytes) {}
    public record Intent(UUID id, UUID titleId, String filename, String status, long expectedBytes,
                         long partSizeBytes, int partCount, Instant expiresAt) {}
    public record PartUrl(int partNumber, String url, int expiresInSeconds) {}
    public record ClientPart(@Min(1) int partNumber, @NotBlank @Size(max = 128) String eTag) {}
    public record CompleteRequest(@NotEmpty List<@jakarta.validation.Valid ClientPart> parts) {}

    private record Row(UUID id, UUID titleId, UUID creatorId, String key, String providerId,
                       String filename, String type, long bytes, int partCount, String status, Instant expires) {}

    private Row row(UUID id) {
        return jdbc.query("""
            SELECT id,title_id,creator_id,object_key,provider_upload_id,original_filename,
                   content_type,expected_bytes,part_count,status,expires_at
            FROM upload_intents WHERE id=?
            """, rs -> rs.next() ? new Row(rs.getObject("id", UUID.class),
                    rs.getObject("title_id", UUID.class), rs.getObject("creator_id", UUID.class),
                    rs.getString("object_key"),rs.getString("provider_upload_id"),
                    rs.getString("original_filename"),rs.getString("content_type"),
                    rs.getLong("expected_bytes"),rs.getInt("part_count"),
                    rs.getString("status"),rs.getTimestamp("expires_at").toInstant()) : null, id);
    }
    private Row owned(Jwt jwt, UUID titleId, UUID id) {
        var creator = creators.requireApproved(jwt);
        titles.findByIdAndCreatorId(titleId, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Row result = row(id);
        if (result == null || !result.titleId().equals(titleId) || !result.creatorId().equals(creator.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found");
        return result;
    }
    private void ensureDraft(UUID titleId, UUID creatorId) {
        var title = titles.findByIdAndCreatorId(titleId, creatorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!"DRAFT".equals(title.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uploads allowed only for drafts");
    }
    @Transactional
    public Intent begin(Jwt jwt, UUID titleId, BeginRequest input) {
        var creator = creators.requireApproved(jwt);
        ensureDraft(titleId, creator.getId());
        String ext = UploadValidation.extension(input.filename(), input.contentType(), input.sizeBytes());
        if (jdbc.queryForObject("SELECT COUNT(*) FROM upload_intents WHERE title_id=? AND status IN ('INITIATED','COMPLETING')", Long.class, titleId) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A multipart upload is already active for this title");
        Long pendingMedia = jdbc.queryForObject("""
            SELECT COUNT(*) FROM media_assets WHERE title_id=?
              AND processing_status IN ('QUEUED','PROCESSING','RETRY_WAIT','READY')
            """, Long.class,titleId);
        if (pendingMedia != null && pendingMedia > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An existing asset is processing or READY");
        UUID id = UUID.randomUUID();
        String key = "source/" + creator.getId() + "/" + titleId + "/" + id + "/original." + ext;
        var response = s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
            .bucket(bucket).key(key).contentType(input.contentType()).build());
        Instant expires = Instant.now().plus(Duration.ofHours(24));
        jdbc.update("""
            INSERT INTO upload_intents(id,title_id,creator_id,object_key,provider_upload_id,original_filename,
                content_type,expected_bytes,part_size_bytes,part_count,status,expires_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,'INITIATED',?)
            """, id, titleId, creator.getId(), key, response.uploadId(),
            input.filename(), input.contentType(), input.sizeBytes(), UploadValidation.PART_SIZE,
            UploadValidation.partCount(input.sizeBytes()), java.sql.Timestamp.from(expires));
        creators.audit(creator.getUserId(), "UPLOAD_INITIATED", "title", titleId, id.toString());
        return new Intent(id, titleId, input.filename(), "INITIATED", input.sizeBytes(),
            UploadValidation.PART_SIZE, UploadValidation.partCount(input.sizeBytes()), expires);
    }
    @Transactional
    public Intent get(Jwt jwt, UUID titleId, UUID id) {
        Row r = owned(jwt,titleId,id);
        return new Intent(r.id(),r.titleId(),r.filename(),r.status(),r.bytes(),UploadValidation.PART_SIZE,r.partCount(),r.expires());
    }
    @Transactional
    public List<Intent> list(Jwt jwt, UUID titleId) {
        var c = creators.requireApproved(jwt);
        titles.findByIdAndCreatorId(titleId,c.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        return jdbc.query("""
            SELECT id,original_filename,status,expected_bytes,part_size_bytes,part_count,expires_at
            FROM upload_intents WHERE title_id=? ORDER BY created_at DESC
            """,(rs,n)->new Intent(rs.getObject("id",UUID.class),titleId,
                rs.getString("original_filename"),rs.getString("status"),rs.getLong("expected_bytes"),
                rs.getLong("part_size_bytes"),rs.getInt("part_count"),rs.getTimestamp("expires_at").toInstant()),titleId);
    }
    @Transactional
    public PartUrl part(Jwt jwt, UUID titleId, UUID id, int number) {
        Row r = owned(jwt,titleId,id);
        if (!"INITIATED".equals(r.status()) || Instant.now().isAfter(r.expires()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload is no longer open");
        if (number < 1 || number > r.partCount())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid part number");
        var request = UploadPartRequest.builder().bucket(bucket).key(r.key()).uploadId(r.providerId())
            .partNumber(number).build();
        var signed = presigner.presignUploadPart(UploadPartPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15)).uploadPartRequest(request).build());
        return new PartUrl(number,signed.url().toString(),900);
    }
    // IMPORTANT: S3 finalize is NOT inside a JDBC transaction. If SQL persistence fails,
    // retry HEAD of the already-assembled S3 object and atomically schedule the SQL job.
    // This is an explicit reconciliation path for S3-success/DB-failure.
    public Intent complete(Jwt jwt, UUID titleId, UUID id, CompleteRequest input) {
        Row r = owned(jwt,titleId,id);
        if ("COMPLETED".equals(r.status())) return get(jwt,titleId,id);
        ensureDraft(titleId,r.creatorId());
        if (!"INITIATED".equals(r.status()) && !"COMPLETING".equals(r.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Upload cannot be completed");
        if (Instant.now().isAfter(r.expires()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Upload expired");
        if (input.parts().size()!=r.partCount())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"All numbered parts are required");
        List<ClientPart> sorted = input.parts().stream().sorted(Comparator.comparingInt(ClientPart::partNumber)).toList();
        for (int i=0;i<sorted.size();i++) {
            if (sorted.get(i).partNumber()!=i+1 || sorted.get(i).eTag().contains("\n") || sorted.get(i).eTag().contains("\r"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or duplicate multipart part");
        }
        boolean alreadyAssembled = false;
        try {
            var assembled = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(r.key()).build());
            alreadyAssembled = assembled.contentLength()==r.bytes();
            if (!alreadyAssembled)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Source object size mismatch");
        } catch (NoSuchKeyException ignored) {
            // Multipart is still in progress. Some S3 services return 404 S3Exception instead.
        } catch (S3Exception error) {
            if (error.statusCode()!=404) throw error;
        }
        if (!alreadyAssembled) {
            var remoteParts = s3.listParts(ListPartsRequest.builder().bucket(bucket).key(r.key())
                .uploadId(r.providerId()).build()).parts();
            if (remoteParts.size() != r.partCount())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Object storage has not received every part");
            long verifiedBytes=0;
            for (int i=0;i<sorted.size();i++) {
                var remote=remoteParts.get(i);
                if (remote.partNumber()!=i+1 || !remote.eTag().equals(sorted.get(i).eTag()) ||
                    remote.size() != Math.min(UploadValidation.PART_SIZE, r.bytes()-(long)i*UploadValidation.PART_SIZE))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Part ETag or length did not match object storage");
                verifiedBytes += remote.size();
            }
            if (verifiedBytes!=r.bytes())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uploaded byte count mismatch");
            var completed = sorted.stream().map(p->CompletedPart.builder().partNumber(p.partNumber())
                .eTag(p.eTag()).build()).toList();
            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder().bucket(bucket).key(r.key())
                .uploadId(r.providerId()).multipartUpload(CompletedMultipartUpload.builder().parts(completed).build()).build());
        }
        var head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(r.key()).build());
        if (head.contentLength()!=r.bytes())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Source size verification failed");

        tx.executeWithoutResult(status -> {
            // This lock serializes concurrent retries of the same upload id.
            String state = jdbc.queryForObject(
                "SELECT status FROM upload_intents WHERE id=? FOR UPDATE",String.class,id);
            if ("COMPLETED".equals(state)) return;
            if (!"INITIATED".equals(state) && !"COMPLETING".equals(state))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Upload changed state");
            jdbc.update("UPDATE upload_intents SET status='COMPLETED',completed_at=NOW() WHERE id=?",id);
            UUID assetId = jdbc.queryForObject("""
                INSERT INTO media_assets(id,title_id,source_key,manifest_key,processing_status,created_at)
                VALUES (?,?,?,NULL,'QUEUED',NOW())
                ON CONFLICT (source_key) DO UPDATE SET source_key=EXCLUDED.source_key
                RETURNING id
                """, UUID.class, UUID.randomUUID(), titleId, r.key());
            jdbc.update("""
                INSERT INTO media_processing_jobs(id,media_asset_id,state,available_at)
                VALUES (?,?,'QUEUED',NOW())
                ON CONFLICT (media_asset_id) DO NOTHING
                """,UUID.randomUUID(),assetId);
            creators.audit(creators.requireApproved(jwt).getUserId(),
                "UPLOAD_COMPLETED_QUEUED", "title", titleId, id.toString());
        });
        return get(jwt,titleId,id);
    }
    @Transactional
    public void abort(Jwt jwt, UUID titleId, UUID id) {
        Row r = owned(jwt,titleId,id);
        if ("ABORTED".equals(r.status())) return;
        if (!"INITIATED".equals(r.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only active uploads can be aborted");
        s3.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(r.key())
            .uploadId(r.providerId()).build());
        jdbc.update("UPDATE upload_intents SET status='ABORTED' WHERE id=?",id);
        creators.audit(creators.requireApproved(jwt).getUserId(),"UPLOAD_ABORTED","title",titleId,id.toString());
    }
}
