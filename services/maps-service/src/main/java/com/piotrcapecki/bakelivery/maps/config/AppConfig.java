package com.piotrcapecki.bakelivery.maps.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${maps.osrm.url}")
    private String osrmUrl;

    @Value("${maps.nominatim.url}")
    private String nominatimUrl;

    @Value("${maps.nominatim.user-agent:bakelivery/1.0}")
    private String nominatimUserAgent;

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }

    @Bean(name = "osrmRestClient")
    public RestClient osrmRestClient() {
        return RestClient.builder()
            .baseUrl(osrmUrl)
            .build();
    }

    @Bean(name = "nominatimRestClient")
    public RestClient nominatimRestClient() {
        return RestClient.builder()
            .baseUrl(nominatimUrl)
            .defaultHeader("User-Agent", nominatimUserAgent)
            .defaultHeader("Accept", "application/json")
            .build();
    }
}
