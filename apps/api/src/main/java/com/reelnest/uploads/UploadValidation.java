package com.reelnest.uploads;

import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class UploadValidation {
    public static final long PART_SIZE = 16L * 1024 * 1024;
    public static final long MAX_BYTES = 5L * 1024 * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
        "mp4", "video/mp4", "mov", "video/quicktime", "mkv", "video/x-matroska");
    private UploadValidation() {}

    public static String extension(String name, String type, long bytes) {
        if (name == null || name.isBlank() || name.length() > 240 ||
            name.contains("/") || name.contains("\\") || name.contains("..") ||
            name.chars().anyMatch(c -> c < 32))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!TYPES.containsKey(ext) || !TYPES.get(ext).equals(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supported media: MP4, MOV or MKV with matching MIME type");
        if (bytes < 1 || bytes > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds configured 5 GiB upload limit");
        return ext;
    }
    public static int partCount(long bytes) {
        return Math.toIntExact((bytes + PART_SIZE - 1) / PART_SIZE);
    }
}
