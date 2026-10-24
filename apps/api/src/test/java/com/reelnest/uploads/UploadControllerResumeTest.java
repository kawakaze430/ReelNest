package com.reelnest.uploads;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

class UploadControllerResumeTest {
    @Test void partListingNeverCachesPresignedMetadata() {
        UploadService service=mock(UploadService.class);
        var controller=new UploadController(service);
        UUID title=UUID.randomUUID(),upload=UUID.randomUUID();
        Jwt jwt=Jwt.withTokenValue("test").header("alg","none").claim("sub","demo").build();
        var returned=List.of(new UploadService.UploadedPart(1,"\"etag\"",500));
        when(service.uploadedParts(jwt,title,upload)).thenReturn(returned);
        var response=controller.uploadedParts(jwt,title,upload);
        assertEquals(HttpStatus.OK,response.getStatusCode());
        assertEquals(returned,response.getBody());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        verify(service).uploadedParts(jwt,title,upload);
        verifyNoMoreInteractions(service);
    }
}
