package com.projexa.coaching.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(redis);

    @Test
    void signupIsLimitedByTheSharedRedisCounter() throws Exception {
        org.mockito.stubbing.OngoingStubbing<Long> counter =
            when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString()));
        counter.thenReturn(1L, 2L, 3L, 4L, 5L, 6L);
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);

        for (int i=0;i<5;i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/auth/signup"),response,chain);
            assertEquals(200,response.getStatus());
        }
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/auth/signup"),limited,chain);
        assertEquals(429,limited.getStatus());
        assertEquals("900",limited.getHeader("Retry-After"));
        verify(chain,times(5)).doFilter(any(),any());
        verify(redis,times(6)).execute(any(DefaultRedisScript.class),anyList(),anyString());
    }

    @Test
    void nonAuthenticationRoutesAreNotThrottled() throws Exception {
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);
        for (int i=0;i<30;i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/students"),response,chain);
            assertEquals(200,response.getStatus());
        }
        verify(chain,times(30)).doFilter(any(),any());
        verifyNoInteractions(redis);
    }

    @Test
    void authenticationFailsClosedWhenSharedLimiterIsUnavailable() throws Exception {
        when(redis.execute(any(DefaultRedisScript.class),anyList(),anyString()))
            .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/auth/login"),response,mock(jakarta.servlet.FilterChain.class));
        assertEquals(503,response.getStatus());
        assertEquals("60",response.getHeader("Retry-After"));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",path);
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
