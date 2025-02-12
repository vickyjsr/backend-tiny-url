package com.tiny.url.controllers;

import com.tiny.url.services.TinyUrlService;
import com.tiny.url.dto.UrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
//@RequestMapping("/v1")
@Validated
@Tag(name = "URL Shortener", description = "URL Shortener API endpoints")
public class UrlController {

    private final TinyUrlService tinyUrlService;

    @Autowired
    public UrlController(TinyUrlService tinyUrlService) {
        this.tinyUrlService = tinyUrlService;
    }

    @Operation(
        summary = "Create a shortened URL",
        description = "Takes a long URL and returns a shortened version"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "URL successfully shortened",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UrlResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid URL provided",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content
        )
    })
    @PostMapping("/new")
    public ResponseEntity<UrlResponse> saveUrl(
            @Parameter(
                description = "Original URL to be shortened",
                required = true,
                example = "https://www.example.com/very/long/url"
            )
            @RequestParam(value = "originalUrl") @NotBlank String originalUrl) {
        log.info("Received request to shorten URL: {}", originalUrl);
        UrlResponse response = tinyUrlService.shortenUrl(originalUrl);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Retrieve original URL",
        description = "Retrieves the original URL using the shortened code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Original URL found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UrlResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "URL not found",
            content = @Content
        )
    })
    @GetMapping("/get")
    public ResponseEntity<UrlResponse> getOriginalUrl(
            @Parameter(
                description = "Shortened URL code",
                required = true,
                example = "abc123"
            )
            @RequestParam(value = "tinyUrl") @NotBlank String tinyUrl) {
        log.info("Received request to retrieve URL for code: {}", tinyUrl);
        UrlResponse response = tinyUrlService.getUrl(tinyUrl);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Redirect to original URL",
        description = "Redirects to the original URL using the shortened code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "302",
            description = "Redirect to original URL",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "URL not found",
            content = @Content
        )
    })
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @Parameter(
                description = "Shortened URL code",
                required = true,
                example = "abc123"
            )
            @PathVariable String code) {
        UrlResponse response = tinyUrlService.getUrl(code);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(response.getOriginalUrl()))
            .build();
    }
}
