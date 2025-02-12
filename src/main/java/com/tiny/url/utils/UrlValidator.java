package com.tiny.url.utils;

import com.tiny.url.exception.InvalidUrlException;

import java.net.URL;
import java.net.MalformedURLException;

public class UrlValidator {

    public static boolean isValidUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return false;
            }

            // Add malicious URL checking
            if (isMaliciousUrl(url)) {
                throw new InvalidUrlException("Potentially malicious URL detected");
            }

            // Handle URLs that are just domains with optional trailing slash
            if (url.endsWith("/") && url.split("/").length == 4) {
                url = url.substring(0, url.length() - 1);
            }

            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol();
            
            // Restrict to only http/https
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }

            // Add length validation
            if (url.length() > 2048) {
                return false;
            }

            // Check if host is not empty
            String host = urlObj.getHost();
            return host != null && !host.trim().isEmpty();

            // Additional validation if needed
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static boolean isMaliciousUrl(String url) {
        // Add checks against known malicious domains
        // Implement connection to security services API
        return false;
    }
} 