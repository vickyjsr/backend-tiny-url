package com.tiny.url.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlResponse implements Serializable {
    @JsonProperty("statusCode")
    private int statusCode;
    
    @JsonProperty("originalUrl")
    private String originalUrl;
    
    @JsonProperty("tinyUrl")
    private String tinyUrl;
} 