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
                        "http://www.slashurl.com",
                        "http://slashurl.com",
                        "https://apis.slashurl.com",
                        "http://apis.slashurl.com",
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "http://3.109.139.251",
                        "http://3.109.139.251:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
