package com.piotrcapecki.bakelivery.gateway.config;

import com.piotrcapecki.bakelivery.gateway.filter.JwtPropagationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class RouteConfig {

    @Value("${gateway.routes.auth-service.uri:http://localhost:8081}")
    private String authServiceUri;

    @Value("${gateway.routes.customer-service.uri:http://localhost:8082}")
    private String customerServiceUri;

    @Value("${gateway.routes.catalog-service.uri:http://localhost:8083}")
    private String catalogServiceUri;

    @Value("${gateway.routes.order-service.uri:http://localhost:8084}")
    private String orderServiceUri;

    private final JwtPropagationFilter jwtPropagationFilter;

    public RouteConfig(JwtPropagationFilter jwtPropagationFilter) {
        this.jwtPropagationFilter = jwtPropagationFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth-service")
                .path("/api/auth/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(authServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> customerServiceRoute() {
        return GatewayRouterFunctions.route("customer-service")
                .path("/api/customer/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(customerServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> catalogServiceRoute() {
        return GatewayRouterFunctions.route("catalog-service")
                .path("/api/catalog/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(catalogServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute() {
        return GatewayRouterFunctions.route("order-service")
                .path("/api/orders/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(orderServiceUri))
                )
                .build();
    }
}
