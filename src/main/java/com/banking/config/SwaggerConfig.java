package com.banking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger / OpenAPI 3.0
 * Ajoute le bouton "Authorize" avec support JWT Bearer token
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Bancaire Sécurisée",
                version = "1.0.0",
                description = "API REST de gestion de comptes bancaires — dépôt, retrait, consultation",
                contact = @Contact(
                        name = "Équipe Bancaire",
                        email = "contact@banking-api.com"
                )
        ),
        servers = {
                @Server(url = "/api/v1", description = "Serveur local / Render")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Entrez votre token JWT : Bearer <token>",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {
    // La configuration est portée par les annotations ci-dessus
}
