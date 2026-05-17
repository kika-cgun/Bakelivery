package com.piotrcapecki.bakelivery.gateway.config;

import com.piotrcapecki.bakelivery.gateway.filter.JwtPropagationFilter;
import com.piotrcapecki.bakelivery.gateway.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

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

    @Value("${gateway.routes.driver-ops-service.uri:http://localhost:8087}")
    private String driverOpsServiceUri;

    @Value("${gateway.routes.messaging-service.uri:http://localhost:8088}")
    private String messagingServiceUri;

    private final JwtPropagationFilter jwtPropagationFilter;
    private final RateLimitFilter rateLimitFilter;

    public RouteConfig(JwtPropagationFilter jwtPropagationFilter, RateLimitFilter rateLimitFilter) {
        this.jwtPropagationFilter = jwtPropagationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return route("auth-service")
                .path("/api/auth/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(authServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> customerServiceRoute() {
        return route("customer-service")
                .path("/api/customer/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(customerServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> catalogServiceRoute() {
        return route("catalog-service")
                .path("/api/catalog/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(catalogServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute() {
        return route("order-service")
                .path("/api/orders/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(orderServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> dispatchingServiceRoute() {
        return route("dispatching-service")
                .path("/api/dispatch/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(dispatchingServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> routingServiceRoute() {
        return route("routing-service")
                .path("/api/routing/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(routingServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> driverOpsServiceRoute() {
        return route("driver-ops-service")
                .path("/api/driver-ops/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(driverOpsServiceUri))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> messagingServiceRoute() {
        return route("messaging-service")
                .path("/api/messaging/**", builder -> builder
                        .filter(jwtPropagationFilter)
                        .filter(rateLimitFilter)
                        .route(RequestPredicates.all(), http())
                )
                .before(uri(messagingServiceUri))
                .build();
    }
}
