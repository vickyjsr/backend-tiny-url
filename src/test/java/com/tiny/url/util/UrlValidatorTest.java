package com.tiny.url.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlValidatorTest {

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertTrue(UrlValidator.isValidUrl("https://slashurl.com"));
        assertTrue(UrlValidator.isValidUrl("http://example.com/path?q=1"));
    }

    @Test
    void rejectsBlankAndUnsupportedProtocols() {
        assertFalse(UrlValidator.isValidUrl(null));
        assertFalse(UrlValidator.isValidUrl(""));
        assertFalse(UrlValidator.isValidUrl("   "));
        assertFalse(UrlValidator.isValidUrl("ftp://example.com/file"));
        assertFalse(UrlValidator.isValidUrl("not-a-url"));
    }
}
