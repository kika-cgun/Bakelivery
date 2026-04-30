package com.piotrcapecki.bakelivery.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public com.piotrcapecki.bakelivery.common.jwt.JwtUtil jwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-millis}") long ttl) {
        return new com.piotrcapecki.bakelivery.common.jwt.JwtUtil(secret, ttl);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
