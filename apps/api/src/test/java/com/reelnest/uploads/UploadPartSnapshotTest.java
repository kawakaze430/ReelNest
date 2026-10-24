package com.reelnest.uploads;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;

class UploadPartSnapshotTest {
    private static final long SIZE=UploadValidation.PART_SIZE*2+7;
    private static Part part(int n,long bytes,String tag) {
        return Part.builder().partNumber(n).size(bytes).eTag(tag).build();
    }
    private static ListPartsResponse response(Part... parts) {
        return ListPartsResponse.builder().parts(List.of(parts)).isTruncated(false).build();
    }
    @Test void emptySnapshotCanResumeFromStart() {
        assertTrue(UploadPartSnapshot.validate(response(),SIZE,3).isEmpty());
    }
    @Test void returnsOnlyRealUploadedPartsInOrder() {
        var list=UploadPartSnapshot.validate(response(part(1,UploadValidation.PART_SIZE,"\"one\""),
             part(3,7,"\"three\"")),SIZE,3);
        assertEquals(List.of(1,3),list.stream().map(UploadPartSnapshot.PartInfo::partNumber).toList());
        assertEquals("\"one\"",list.getFirst().eTag());
    }
    @Test void rejectsTruncatedRemoteSnapshot() {
        var truncated=ListPartsResponse.builder().parts(part(1,UploadValidation.PART_SIZE,"ok"))
            .isTruncated(true).build();
        assertThrows(ResponseStatusException.class,()->UploadPartSnapshot.validate(truncated,SIZE,3));
    }
    @Test void rejectsRepeatedOutOfOrderOrUnexpectedPartNumbers() {
        for (var bad:new ListPartsResponse[]{
            response(part(1,UploadValidation.PART_SIZE,"a"),part(1,UploadValidation.PART_SIZE,"b")),
            response(part(3,7,"a"),part(1,UploadValidation.PART_SIZE,"b")),
            response(part(4,7,"c"))
        }) assertThrows(ResponseStatusException.class,
            ()->UploadPartSnapshot.validate(bad,SIZE,3));
    }
    @Test void rejectsWrongLengthsAndInvalidEtags() {
        for(var bad:new Part[]{part(1,9,"ok"),part(3,8,"ok"),part(2,UploadValidation.PART_SIZE,"\r\n"),
                part(1,UploadValidation.PART_SIZE,"")})
            assertThrows(ResponseStatusException.class,
                ()->UploadPartSnapshot.validate(response(bad),SIZE,3));
    }
    @Test void rejectsInconsistentExpectedCount() {
        assertThrows(ResponseStatusException.class,()->UploadPartSnapshot.validate(response(),SIZE,2));
    }
}
