package com.tiny.url;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class TinyUrlBackendApplicationTests {

    @Test
    void contextLoads() {
        // Smoke: application context starts with profile "dev"
    }
}
