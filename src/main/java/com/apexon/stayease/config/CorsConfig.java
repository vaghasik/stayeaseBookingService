package com.apexon.stayease.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS Configuration
 * Configures Cross-Origin Resource Sharing for the application.
 *
 */
@Configuration
public class CorsConfig {

    /**
     * Extra origins allowed in addition to the local dev ports (e.g. the deployed
     * frontend URL). Set via the CORS_ALLOWED_ORIGINS env var as a comma-separated
     * list, e.g. "https://stayease-team45.onrender.com".
     */
    @Value("${app.cors.allowed-origins:}")
    private String additionalAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins =
                new ArrayList<>(
                        Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:5173",
                                "http://localhost:4173",
                                "http://localhost:4174"));
        if (additionalAllowedOrigins != null && !additionalAllowedOrigins.isBlank()) {
            for (String origin : additionalAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
