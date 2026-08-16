package com.tiny.url.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!prod")
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI slashUrlOpenApi() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development");

        Contact contact = new Contact()
                .name("SlashURL")
                .email("support@slashurl.com");

        Info info = new Info()
                .title("SlashURL API")
                .version("1.0.0")
                .contact(contact)
                .description("URL shortening API")
                .license(new License().name("MIT License").url("https://choosealicense.com/licenses/mit/"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
