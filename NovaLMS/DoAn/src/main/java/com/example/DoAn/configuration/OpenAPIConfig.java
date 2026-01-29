package com.example.DoAn.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!prod")
public class OpenAPIConfig {

    @Bean
    public GroupedOpenApi publicApi(@Value("${openapi.service.api-docs:/v3/api-docs}") String apiDocs) {
        return GroupedOpenApi.builder()
                .group("public-api")
                .packagesToScan("com.example.DoAn.controller") // Chú ý: Sửa lại đúng package controller của bạn
                .build();
    }

    @Bean
    public OpenAPI openAPI(
            @Value("${openapi.service.title:Nova API}") String title,
            @Value("${openapi.service.version:1.0.0}") String version,
            @Value("${openapi.service.server-url:http://localhost:8080}") String serverUrl) {

        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl).description("Local Server")))
                .info(new Info().title(title)
                        .description("API documents for Nova Learning Management System")
                        .version(version)
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}