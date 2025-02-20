package com.tiny.url.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://www.slashurl.com",
                        "https://slashurl.com",
                        "slashurl.com",
                        "slashurl.com/",
                        "http://www.slashurl.com",
                        "http://slashurl.com",
                        "https://apis.slashurl.com",
                        "http://apis.slashurl.com",
                        "apis.slashurl.com",
                        "http://localhost:3000",
                        "http://143.110.255.217:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
