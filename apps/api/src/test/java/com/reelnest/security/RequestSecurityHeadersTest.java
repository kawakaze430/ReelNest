package com.reelnest.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSecurityHeadersTest {
    private static MockHttpServletResponse check(String path) throws Exception {
        var req=new MockHttpServletRequest("GET",path);
        var res=new MockHttpServletResponse();
        new RequestSecurityHeaders().doFilter(req,res,new MockFilterChain());
        return res;
    }
    @Test void rightsSensitiveCatalogDiscoveryAndProfileMetadataNeverCache() throws Exception {
        for(String path:new String[]{
            "/api/v1/catalog","/api/v1/catalog/00000000-0000-4000-8000-000000000001",
            "/api/v1/catalog/suggest","/api/v1/discovery/home",
            "/api/v1/profiles","/api/v1/billing/plans","/api/v1/avod/preview"}) {
            var response=check(path);
            assertEquals("no-store",response.getHeader("Cache-Control"),path);
            assertEquals("nosniff",response.getHeader("X-Content-Type-Options"));
        }
    }
    @Test void healthEndpointDoesNotGetRestrictedCatalogHeaders() throws Exception {
        assertNull(check("/actuator/health").getHeader("Cache-Control"));
    }
}
