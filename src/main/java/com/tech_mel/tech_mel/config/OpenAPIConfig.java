package com.tech_mel.tech_mel.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechMel API")
                        .version("1.0.0")
                        .description("""
                                API para monitoramento de colmeias e gestão de dados relacionados à apicultura.
                                
                                **Funcionalidades principais:**
                                - Autenticação de usuários (local e OAuth2 com Google)
                                - Gestão de colmeias
                                - Monitoramento de dados dos sensores
                                - Diferentes níveis de acesso (COMMON, TECHNICIAN, ADMIN)
                                
                                **Como usar:**
                                1. Registre-se ou faça login para obter um token de acesso
                                2. Use o botão "Authorize" para inserir seu token no formato: Bearer {seu_token}
                                3. Acesse os endpoints de acordo com seu nível de permissão
                                """)
                        .termsOfService("https://www.techmel.com/terms")
                        .contact(new Contact()
                                .name("Equipe TechMel")
                                .email("contato@techmel.com")
                                .url("https://www.techmel.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desenvolvimento"),
                        new Server()
                                .url("https://api.techmel.com")
                                .description("Servidor de Produção")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT no formato: Bearer {token}")
                        )
                );
    }
}
