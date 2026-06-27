package com.choosephd.config;

import com.choosephd.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/regions",
                        "/api/v1/subjects",
                        "/api/v1/countries",
                        "/api/v1/sources",
                        "/api/v1/sources/*",
                        "/api/v1/sources/*/entries",
                        "/api/v1/sources/*/years",
                        "/api/v1/universities",
                        "/api/v1/universities/*",
                        "/api/v1/universities/*/rankings",
                        "/api/v1/university-tags",
                        "/api/v1/university-tags/**",
                        "/api/v1/trends/**",
                        "/api/v1/stats/**",
                        "/api/v1/geo/**",
                        "/api/v1/admin/import/status"
                );
    }
}
