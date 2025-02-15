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
                        "http://apis.slashurl.com",
                        "https://apis.slashurl.com",
                        "http://localhost:3000",
                        "http://143.110.255.217:3000",
                        "http://www.slashurl.com",
                        "www.slashurl.com",
                        "slashurl.com",
                        "http://slashurl.com",
                        "https://slashurl.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Accept", "Content-Type", "Origin",
                        "Authorization", "X-Requested-With", "Cache-Control",
                        "Pragma", "Referer", "User-Agent", "sec-ch-ua",
                        "sec-ch-ua-mobile", "sec-ch-ua-platform")
                .exposedHeaders("Access-Control-Allow-Origin")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
