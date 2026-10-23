package com.reelnest.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class ApiJwtDecoderConfig {
    /** Keycloak audience mapper must emit this audience in each access token. */
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
            @Value("${app.auth.required-audience:reelnest-api}") String audience,
            @Value("${app.auth.required-client:reelnest-web}") String client) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> recipient = jwt -> {
            if (!TokenClaimsPolicy.permitsAudience(jwt.getAudience(), audience))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong API audience", null));
            if (!TokenClaimsPolicy.permitsClient(jwt.getClaimAsString("azp"), client))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong OAuth client", null));
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), recipient));
        return decoder;
    }
}
