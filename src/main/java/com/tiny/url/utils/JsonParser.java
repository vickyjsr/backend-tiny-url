package com.tiny.url.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts any POJO to JSON string.
     *
     * @param obj the object to convert
     * @return JSON string representation of the object, or null if conversion fails
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error serializing object to JSON: {}", e.getMessage());
            return String.valueOf(obj);
        }
    }

    /**
     * Converts JSON string to specified class type.
     *
     * @param json JSON string to convert
     * @param clazz target class
     * @return instance of specified class, or null if conversion fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error converting JSON to object: {}", e.getMessage(), e);
            return null;
        }
    }

    private JsonParser() {
        // Private constructor to prevent instantiation
    }
} 