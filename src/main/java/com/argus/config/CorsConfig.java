package com.argus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOriginsString;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethodsString;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeadersString;

    @Value("${cors.exposed-headers:Authorization,Content-Type,X-Total-Count}")
    private String exposedHeadersString;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    private String[] allowedOrigins;
    private String[] allowedMethods;
    private String[] allowedHeaders;
    private String[] exposedHeaders;

    @PostConstruct
    public void init() {
        allowedOrigins = splitAndTrim(allowedOriginsString);
        allowedMethods = splitAndTrim(allowedMethodsString);
        allowedHeaders = splitAndTrim(allowedHeadersString);
        exposedHeaders = splitAndTrim(exposedHeadersString);

        log.info("CORS configured with origins: {}", Arrays.toString(allowedOrigins));
    }

    private String[] splitAndTrim(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of(allowedMethods));
        configuration.setAllowedHeaders(List.of(allowedHeaders));
        configuration.setExposedHeaders(List.of(exposedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
