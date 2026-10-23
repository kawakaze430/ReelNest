package com.reelnest.uploads;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class StorageConfig {
    private AwsCredentialsProvider credentials(String key, String secret) {
        if (key.isBlank() != secret.isBlank())
            throw new IllegalStateException("Both storage key and secret are required, or neither");
        return key.isBlank() ? DefaultCredentialsProvider.create()
            : StaticCredentialsProvider.create(AwsBasicCredentials.create(key,secret));
    }
    @Bean
    S3Client s3Client(@Value("${app.storage.endpoint:}") String endpoint,
        @Value("${app.storage.access-key:}") String key,
        @Value("${app.storage.secret-key:}") String secret,
        @Value("${app.storage.region}") String region) {
        var builder=S3Client.builder().region(Region.of(region)).credentialsProvider(credentials(key,secret));
        if(!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        return builder.build();
    }
    @Bean
    S3Presigner s3Presigner(@Value("${app.storage.public-endpoint:}") String publicEndpoint,
        @Value("${app.storage.access-key:}") String key,
        @Value("${app.storage.secret-key:}") String secret,
        @Value("${app.storage.region}") String region) {
        var builder=S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials(key,secret));
        if(!publicEndpoint.isBlank()) builder.endpointOverride(URI.create(publicEndpoint))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        return builder.build();
    }
}
