package com.tiny.url.mapper;

import com.tiny.url.dto.UrlResponse;
import com.tiny.url.entity.Url;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlMapperTest {

    private final UrlMapper urlMapper = new UrlMapper();

    @Test
    void mapsEntityToResponse() {
        Url url = Url.builder()
                .tinyUrl("AbCd1234")
                .originalUrl("https://example.com")
                .build();

        UrlResponse response = urlMapper.toUrlResponse(url);

        assertEquals(200, response.getStatusCode());
        assertEquals("AbCd1234", response.getTinyUrl());
        assertEquals("https://example.com", response.getOriginalUrl());
    }

    @Test
    void returnsNullForNullEntity() {
        assertNull(urlMapper.toUrlResponse(null));
    }
}
