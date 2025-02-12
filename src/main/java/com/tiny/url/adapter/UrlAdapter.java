package com.tiny.url.adapter;

import com.tiny.url.dto.UrlResponse;
import com.tiny.url.models.Url;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UrlAdapter {

    public UrlResponse toUrlResponse(Url url) {
        if (url == null) {
            log.warn("Attempting to convert null URL to response");
            return null;
        }
        
        try {
            return UrlResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .tinyUrl(url.getTinyUrl())
                    .originalUrl(url.getOriginalUrl())
                    .build();
        } catch (Exception e) {
            log.error("Error converting URL to response: {}", e.getMessage());
            throw new RuntimeException("Error converting URL to response", e);
        }
    }
} 