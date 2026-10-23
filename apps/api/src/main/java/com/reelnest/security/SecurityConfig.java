package com.reelnest.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,StringRedisTemplate redis) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Stateless bearer-token API, not cookie authentication.
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog", "/api/v1/catalog/**", "/api/v1/billing/plans").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/billing/webhooks/razorpay").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
            .addFilterAfter(new AdmissionRateLimiter(redis),BearerTokenAuthenticationFilter.class)
            .build();
    }

    @Bean
    JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
            Object realm = jwt.getClaim("realm_access");
            if (realm instanceof Map<?, ?> realmMap && realmMap.get("roles") instanceof List<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String name) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowCredentials(true); // CDN auth relies on cross-subdomain Set-Cookie.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Profile-Pin"));
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
