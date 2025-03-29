package com.tech_mel.tech_mel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TechMel API")
                .version("1.0")
                .description("API para monitoramento de colmeias")
                .summary("API para monitoramento de colmeias e gestão de dados relacionados à apicultura.")
                .termsOfService("https://www.techmel.com/terms")
        );
    }
}
