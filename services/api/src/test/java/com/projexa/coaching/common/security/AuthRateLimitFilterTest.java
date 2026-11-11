package com.projexa.coaching.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    @Test
    void signupIsLimitedToFiveRequestsPerIpWindow() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i=0;i<5;i++) {
            MockHttpServletRequest request = request("/api/v1/auth/signup");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request,response,chain);
            assertEquals(200,response.getStatus());
        }
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/auth/signup"),limited,chain);
        assertEquals(429,limited.getStatus());
        assertEquals("900",limited.getHeader("Retry-After"));
        verify(chain,times(5)).doFilter(any(),any());
    }

    @Test
    void nonAuthenticationRoutesAreNotThrottledByThisFilter() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i=0;i<30;i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/students"),response,chain);
            assertEquals(200,response.getStatus());
        }
        verify(chain,times(30)).doFilter(any(),any());
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",path);
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
