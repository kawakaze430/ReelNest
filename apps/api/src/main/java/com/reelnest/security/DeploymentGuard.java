package com.reelnest.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Refuse dangerous deployment configuration during application startup. */
@Component
public class DeploymentGuard {
    @Value("${app.deploy.mode:development}") private String mode;
    @Value("${app.playback.dev-proxy-enabled:false}") private boolean devProxy;
    @Value("${app.playback.cdn-geo-reviewed:false}") private boolean geoReviewed;
    @Value("${app.playback.cdn-base-url:}") private String cdnBaseUrl;
    @Value("${app.playback.trusted-country:IN}") private String country;
    @Value("${app.storage.endpoint:}") private String storageEndpoint;

    @PostConstruct
    public void validate() {
        DeploymentPolicy.validate(mode,devProxy,geoReviewed,cdnBaseUrl,country,storageEndpoint);
    }
}
