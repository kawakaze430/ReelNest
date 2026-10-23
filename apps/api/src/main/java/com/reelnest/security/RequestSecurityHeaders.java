package com.reelnest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Common correlation and defensive headers; not a replacement for CSP on Next.js. */
@Component
public class RequestSecurityHeaders extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,
       FilterChain chain) throws ServletException, IOException {
        String provided=request.getHeader("X-Request-ID");
        String requestId=provided!=null && provided.matches("[A-Za-z0-9_-]{8,64}")
            ? provided : UUID.randomUUID().toString();
        MDC.put("requestId",requestId);
        response.setHeader("X-Request-ID",requestId);
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("X-Frame-Options","DENY");
        response.setHeader("Referrer-Policy","no-referrer");
        response.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");
        String path=request.getRequestURI();
        if (path.startsWith("/api/v1/privacy") || path.startsWith("/api/v1/billing")
            || path.startsWith("/api/v1/me") || path.startsWith("/api/v1/profiles"))
            response.setHeader("Cache-Control","no-store");
        try { chain.doFilter(request,response); }
        finally { MDC.remove("requestId"); }
    }
}
