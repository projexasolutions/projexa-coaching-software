package com.projexa.coaching.social.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class SocialContentAiServiceTest {
    @Test
    void returnsServiceUnavailableWhenProviderKeyIsMissing() {
        SocialContentAiService service = new SocialContentAiService();
        ReflectionTestUtils.setField(service, "apiKey", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.generate("Study habits", "EDUCATIONAL", "Carousel",
                "Example Institute", "School students", "Friendly", "Learn more"));

        assertEquals(503, error.getStatusCode().value());
        assertTrue(error.getReason().contains("OPENAI_API_KEY"));
    }

    @Test
    void returnsServiceUnavailableWhenProviderKeyIsNull() {
        SocialContentAiService service = new SocialContentAiService();
        ReflectionTestUtils.setField(service, "apiKey", null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.generate("Study habits", "EDUCATIONAL", "Carousel",
                null, null, null, null));

        assertEquals(503, error.getStatusCode().value());
    }
}
