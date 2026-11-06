package edu.kuet.classsync.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Keeps frontend (separate origin) able to call /api/v1/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origin}") private String origin;

    @Override
    public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/v1/**").allowedOrigins(origin).allowedMethods("*");
    }
}
