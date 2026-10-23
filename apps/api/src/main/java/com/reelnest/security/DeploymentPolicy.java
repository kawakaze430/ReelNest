package com.reelnest.security;

/** Production configuration gate, deliberately dependency-free for JVM offline tests. */
public final class DeploymentPolicy {
    private DeploymentPolicy() {}
    public static void validate(String mode, boolean devProxy, boolean geoReviewed,
            String cdnBaseUrl, String country, String s3Endpoint) {
        if (!"development".equals(mode) && !"staging".equals(mode)
                && !"production".equals(mode))
            throw new IllegalStateException("Deployment mode must be development, staging, or production");
        if (!"production".equals(mode)) return;
        if (devProxy) throw new IllegalStateException("Development streaming proxy cannot run in production");
        if (!geoReviewed) throw new IllegalStateException("Production geofencing review is required");
        if (cdnBaseUrl == null || !cdnBaseUrl.matches("https://[a-zA-Z0-9.-]+"))
            throw new IllegalStateException("A HTTPS CloudFront hostname is required");
        if (country == null || !country.matches("[A-Z]{2}"))
            throw new IllegalStateException("Approved country code is required");
        if (s3Endpoint != null && !s3Endpoint.isBlank())
            throw new IllegalStateException("Production must use AWS S3 IAM, not a developer S3 endpoint");
    }
}
