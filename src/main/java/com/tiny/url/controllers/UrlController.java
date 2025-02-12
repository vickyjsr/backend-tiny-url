package com.tiny.url.controllers;

import com.tiny.url.services.TinyUrlService;
import com.tiny.url.dto.UrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

import java.net.URI;

/**
 * REST Controller for URL shortening operations.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@Validated
@Tag(name = "URL Shortener API", description = "API endpoints for URL shortening operations")
public class UrlController {

    private final TinyUrlService tinyUrlService;

    @Autowired
    public UrlController(TinyUrlService tinyUrlService) {
        this.tinyUrlService = tinyUrlService;
    }

    @Operation(summary = "Create a shortened URL")
    @ApiResponse(responseCode = "200", description = "URL successfully shortened")
    @ApiResponse(responseCode = "400", description = "Invalid URL provided")
    @PostMapping("/newTinyUrl")
    public ResponseEntity<UrlResponse> saveUrl(
            @RequestParam(value = "originalUrl") @NotBlank String originalUrl) {
        log.info("Received request to shorten URL: {}", originalUrl);
        UrlResponse response = tinyUrlService.shortenUrl(originalUrl);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Retrieve original URL")
    @ApiResponse(responseCode = "200", description = "Original URL found")
    @ApiResponse(responseCode = "404", description = "URL not found")
    @GetMapping("/getUrl")
    public ResponseEntity<UrlResponse> getOriginalUrl(
            @RequestParam(value = "tinyUrl") @NotBlank String tinyUrl) {
        log.info("Received request to retrieve URL for code: {}", tinyUrl);
        UrlResponse response = tinyUrlService.getUrl(tinyUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/redirect/{code}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String code) {
        UrlResponse response = tinyUrlService.getUrl(code);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(response.getOriginalUrl()))
            .build();
    }
}
