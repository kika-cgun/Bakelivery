package com.piotrcapecki.bakelivery.order.config;

import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.access-ttl-millis}") long accessTtlMillis) {
        return new JwtUtil(secret, accessTtlMillis);
    }
}
