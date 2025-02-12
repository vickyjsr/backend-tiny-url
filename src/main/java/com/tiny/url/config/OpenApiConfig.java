package com.tiny.url.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// todo currently not in used
// todo will disable prod swagger, once testing is over
//@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Development server");

        Server prodServer = new Server()
                .description("Production server");

        Contact contact = new Contact()
                .name("URL Shortener Team")
                .email("gouravmodi010@gmail.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("URL Shortener API")
                .version("1.0.0")
                .contact(contact)
                .description("This API exposes endpoints to manage URL shortening operations.")
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer));
    }
} 