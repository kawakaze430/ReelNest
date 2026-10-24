package com.reelnest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fail-closed Redis admission limit for particularly costly authenticated actions. */
public class AdmissionRateLimiter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> SCRIPT=new DefaultRedisScript<>("""
        local n=redis.call('INCR',KEYS[1]);
        if n == 1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end;
        return n;
        """,Long.class);
    public AdmissionRateLimiter(StringRedisTemplate redis) { this.redis=redis; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse response,
      FilterChain chain) throws IOException,ServletException {
        String path=req.getRequestURI();
        if (!"POST".equals(req.getMethod())) {chain.doFilter(req,response);return;}
        int limit=0;
        if(path.equals("/api/v1/billing/subscriptions"))limit=5;
        else if(path.equals("/api/v1/playback/sessions"))limit=12;
        else if(path.equals("/api/v1/privacy/deletion-request"))limit=2;
        else if(path.equals("/api/v1/me/sessions/revoke-all"))limit=3;
        else if(path.equals("/api/v1/profiles"))limit=8;
        else if(path.equals("/api/v1/device-handoff"))limit=5;
        else if(path.equals("/api/v1/device-handoff/redeem"))limit=10;
        else if(path.equals("/api/v1/support/cases"))limit=5;
        else if(path.matches("/api/v1/support/cases/[0-9a-fA-F-]{36}/appeal"))limit=3;
        if(limit==0) {chain.doFilter(req,response);return;}
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) {chain.doFilter(req,response);return;}
        try {
            Long count=redis.execute(SCRIPT,
                List.of("reelnest:admission:"+path+":"+auth.getName()),"60");
            if(count==null || count>limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After","60");
                return;
            }
        } catch(Exception unavailable) {
            // Avoid letting Redis downtime become unlimited paid subscription admission.
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }
        chain.doFilter(req,response);
    }
}
