package com.piotrcapecki.bakelivery.gateway.config;

import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import com.piotrcapecki.bakelivery.gateway.filter.JwtPropagationFilter;
import com.piotrcapecki.bakelivery.gateway.filter.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class GatewayConfig implements WebMvcConfigurer {

    private final SecurityHeadersFilter securityHeadersFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private List<String> allowedOrigins;

    public GatewayConfig(SecurityHeadersFilter securityHeadersFilter) {
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeadersFilter);
    }

    @Bean
    public JwtUtil jwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-millis}") long accessTtlMillis) {
        return new JwtUtil(secret, accessTtlMillis);
    }

    @Bean
    public JwtPropagationFilter jwtPropagationFilter(JwtUtil jwtUtil) {
        return new JwtPropagationFilter(jwtUtil);
    }
}
