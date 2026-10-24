package com.reelnest.uploads;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;

/** Strict server-side validation of the single (at most 320-part) S3 multipart snapshot. */
public final class UploadPartSnapshot {
    private UploadPartSnapshot() {}

    public record PartInfo(int partNumber,String eTag,long sizeBytes) {}

    public static List<PartInfo> validate(ListPartsResponse response,long expectedBytes,int expectedPartCount) {
        if (response==null || expectedBytes<1 || expectedBytes>UploadValidation.MAX_BYTES
            || expectedPartCount!=UploadValidation.partCount(expectedBytes) || response.isTruncated())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Incomplete storage part listing");
        List<PartInfo> verified=new ArrayList<>();
        int previous=0;
        for (var part:response.parts()) {
            Integer number=part.partNumber();
            Long size=part.size();
            String tag=part.eTag();
            if (number==null || number<=previous || number>expectedPartCount
                || size==null || tag==null || tag.isBlank() || tag.length()>128
                || tag.contains("\r") || tag.contains("\n"))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid storage multipart metadata");
            long sizeExpected=Math.min(UploadValidation.PART_SIZE,
                expectedBytes-(number.longValue()-1)*UploadValidation.PART_SIZE);
            if (size!=sizeExpected)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Stored part length mismatch");
            verified.add(new PartInfo(number,tag,size));
            previous=number;
        }
        return List.copyOf(verified);
    }
}
