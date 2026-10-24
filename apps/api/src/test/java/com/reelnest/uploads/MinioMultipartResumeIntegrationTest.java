package com.reelnest.uploads;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Real disposable MinIO multipart test: no Keycloak/SQL/browser or production bucket.
 * Never run against a remote endpoint or without explicit test opt-in.
 */
class MinioMultipartResumeIntegrationTest {
    private static final String ENDPOINT="http://127.0.0.1:9000";
    private static final long CHUNK=UploadValidation.PART_SIZE;

    @Test
    void resumeFromStorageAndFinishWithoutMixingParts() {
        assumeTrue("true".equals(System.getenv("REELNEST_DISPOSABLE_MINIO"))
            && ENDPOINT.equals(System.getenv("MINIO_TEST_ENDPOINT")),
            "Real S3 integration requires loopback-only disposable MinIO opt-in");
        String access=System.getenv("MINIO_TEST_ACCESS_KEY");
        String secret=System.getenv("MINIO_TEST_SECRET_KEY");
        assumeTrue(access!=null && !access.isBlank() && secret!=null && !secret.isBlank(),
            "Disposable MinIO credentials must be set by CI");
        String bucket="reelnest-ci-"+UUID.randomUUID().toString().replace("-","");
        try (S3Client s3=S3Client.builder()
            .endpointOverride(URI.create(ENDPOINT))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret)))
            .build()) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            try {
                multipartRoundTrip(s3,bucket);
                cancellation(s3,bucket);
                invalidLengthFailsClosed(s3,bucket);
            } finally {
                // Only ephemeral resources created by this test, never project media buckets.
                try { s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key("original.mp4").build()); }
                catch (S3Exception ignored) { }
                s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
            }
        }
    }

    private static String start(S3Client s3,String bucket,String key) {
        return s3.createMultipartUpload(CreateMultipartUploadRequest.builder()
            .bucket(bucket).key(key).contentType("video/mp4").build()).uploadId();
    }

    private static void multipartRoundTrip(S3Client s3,String bucket) {
        String key="original.mp4";
        String uploadId=start(s3,bucket,key);
        boolean completed=false;
        try {
            byte[] first=new byte[(int)CHUNK];
            Arrays.fill(first,(byte)0x51);
            var up1=s3.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key)
                .uploadId(uploadId).partNumber(1).contentLength(CHUNK).build(),
                RequestBody.fromBytes(first));
            // Simulated internet/browser interruption: new request lists only persisted parts.
            var paused=s3.listParts(ListPartsRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build());
            var accepted=UploadPartSnapshot.validate(paused,CHUNK+89,2);
            assertEquals(1,accepted.size());
            assertEquals(1,accepted.getFirst().partNumber());
            assertEquals(up1.eTag(),accepted.getFirst().eTag());
            assertEquals(CHUNK,accepted.getFirst().sizeBytes());
            byte[] last=new byte[89];
            Arrays.fill(last,(byte)0x32);
            var up2=s3.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key)
                .uploadId(uploadId).partNumber(2).contentLength((long)last.length).build(),
                RequestBody.fromBytes(last));
            var both=UploadPartSnapshot.validate(s3.listParts(ListPartsRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId).build()),CHUNK+last.length,2);
            assertEquals(List.of(1,2),both.stream().map(UploadPartSnapshot.PartInfo::partNumber).toList());
            assertEquals(up2.eTag(),both.get(1).eTag());
            // Confirm the object-store tags rather than accepting fabricated browser claims.
            assertNotEquals("\"forged-etag\"",both.get(1).eTag());
            s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(
                    CompletedPart.builder().partNumber(1).eTag(both.get(0).eTag()).build(),
                    CompletedPart.builder().partNumber(2).eTag(both.get(1).eTag()).build()).build())
                .build());
            completed=true;
            assertEquals(CHUNK+last.length,
                s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentLength());
        } finally {
            if(!completed)abortQuietly(s3,bucket,key,uploadId);
        }
    }

    private static void cancellation(S3Client s3,String bucket) {
        String key="cancelled.mp4", id=start(s3,bucket,key);
        s3.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key).uploadId(id)
            .partNumber(1).contentLength(CHUNK).build(),
            RequestBody.fromBytes(new byte[(int)CHUNK]));
        s3.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(id).build());
        assertThrows(S3Exception.class,()->s3.listParts(ListPartsRequest.builder()
            .bucket(bucket).key(key).uploadId(id).build()));
    }

    private static void invalidLengthFailsClosed(S3Client s3,String bucket) {
        String key="invalid.mp4",id=start(s3,bucket,key);
        try {
            s3.uploadPart(UploadPartRequest.builder().bucket(bucket).key(key).uploadId(id)
                .partNumber(1).contentLength(11L).build(),
                RequestBody.fromBytes(new byte[11]));
            var remote=s3.listParts(ListPartsRequest.builder().bucket(bucket).key(key).uploadId(id).build());
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->UploadPartSnapshot.validate(remote,CHUNK+7,2));
        } finally {abortQuietly(s3,bucket,key,id);}
    }

    private static void abortQuietly(S3Client s3,String bucket,String key,String id) {
        try {s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
            .bucket(bucket).key(key).uploadId(id).build());}
        catch (S3Exception ignored) {}
    }
}
