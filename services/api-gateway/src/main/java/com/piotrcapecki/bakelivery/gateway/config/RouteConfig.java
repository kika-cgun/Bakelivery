package com.piotrcapecki.bakelivery.gateway.config;

import com.piotrcapecki.bakelivery.gateway.filter.JwtPropagationFilter;
import com.piotrcapecki.bakelivery.gateway.filter.RateLimitFilter;
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

    @Value("${gateway.routes.dispatching-service.uri:http://localhost:8085}")
    private String dispatchingServiceUri;

    @Value("${gateway.routes.routing-service.uri:http://localhost:8086}")
    private String routingServiceUri;

    private final JwtPropagationFilter jwtPropagationFilter;
    private final RateLimitFilter rateLimitFilter;

    public RouteConfig(JwtPropagationFilter jwtPropagationFilter, RateLimitFilter rateLimitFilter) {
        this.jwtPropagationFilter = jwtPropagationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth-service")
                .path("/api/auth/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(authServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> customerServiceRoute() {
        return GatewayRouterFunctions.route("customer-service")
                .path("/api/customer/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(customerServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> catalogServiceRoute() {
        return GatewayRouterFunctions.route("catalog-service")
                .path("/api/catalog/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
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

    @Bean
    public RouterFunction<ServerResponse> dispatchingServiceRoute() {
        return GatewayRouterFunctions.route("dispatching-service")
                .path("/api/dispatch/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(dispatchingServiceUri))
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routingServiceRoute() {
        return GatewayRouterFunctions.route("routing-service")
                .path("/api/routing/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), HandlerFunctions.http(routingServiceUri))
                )
                .build();
    }
}
