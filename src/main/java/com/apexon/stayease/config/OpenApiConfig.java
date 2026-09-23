package com.apexon.stayease.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI / Swagger Configuration
 * Provides API documentation with JWT Bearer token support.
 *
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stayEaseOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("StayEase Booking API")
                                .description(
                                        "REST API for StayEase Booking Service. "
                                                + "Allows guests to browse hotels, check room availability, "
                                                + "and confirm bookings. Managers manage rooms and bookings. "
                                                + "Admins manage the hotel directory.")
                                .version("1.0.0")
                                .contact(new Contact().name("Team 45")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "Bearer Authentication",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Enter JWT token obtained from /api/auth/login or /api/auth/register")));
    }
}
