package com.reelnest.uploads;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class UploadValidationTest {
    @Test void acceptsExpectedVideoContainers() {
        assertEquals("mp4", UploadValidation.extension("studio.mp4","video/mp4",1));
        assertEquals("mov", UploadValidation.extension("movie.MOV","video/quicktime",1024));
        assertEquals("mkv", UploadValidation.extension("movie.mkv","video/x-matroska",1024));
    }
    @Test void blocksPathTraversalAndSpoofedMediaTypes() {
        assertThrows(ResponseStatusException.class,
            ()->UploadValidation.extension("../secrets.mp4","video/mp4",100));
        assertThrows(ResponseStatusException.class,
            ()->UploadValidation.extension("film.mp4","application/octet-stream",100));
        assertThrows(ResponseStatusException.class,
            ()->UploadValidation.extension("film.exe","video/mp4",100));
        assertThrows(ResponseStatusException.class,
            ()->UploadValidation.extension("film.mp4","video/mp4",0));
        assertThrows(ResponseStatusException.class,
            ()->UploadValidation.extension("film.mp4","video/mp4",UploadValidation.MAX_BYTES+1));
    }
    @Test void correctlyDeterminesMultipartBoundaries() {
        assertEquals(1,UploadValidation.partCount(1));
        assertEquals(1,UploadValidation.partCount(UploadValidation.PART_SIZE));
        assertEquals(2,UploadValidation.partCount(UploadValidation.PART_SIZE+1));
        assertEquals(320,UploadValidation.partCount(UploadValidation.MAX_BYTES));
    }
}
