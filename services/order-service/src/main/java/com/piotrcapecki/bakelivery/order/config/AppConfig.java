package com.piotrcapecki.bakelivery.order.config;

import com.piotrcapecki.bakelivery.common.jwt.JwtUtil;
import com.piotrcapecki.bakelivery.order.client.CatalogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.access-ttl-millis}") long accessTtlMillis) {
        return new JwtUtil(secret, accessTtlMillis);
    }

    @Bean
    public CatalogClient catalogClient(@Value("${catalog.service.url:http://localhost:8083}") String baseUrl) {
        RestClient restClient = RestClient.create(baseUrl);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient)).build();
        return factory.createClient(CatalogClient.class);
    }
}
