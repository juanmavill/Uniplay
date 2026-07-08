package edu.eci.uniplay.gateway.infrastructure.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator uniplayRoutes(RouteLocatorBuilder routes, GatewayServiceProperties properties) {
        return routes.routes()
                .route("room-service", route -> route
                        .path("/salas/**")
                        .uri(properties.getRoomServiceUri().toString()))
                .route("game-service", route -> route
                        .path("/games/**")
                        .uri(properties.getGameServiceUri().toString()))
                .route("realtime-service-websocket", route -> route
                        .path("/ws/**")
                        .and()
                        .header("Upgrade", "websocket")
                        .uri(properties.getRealtimeWebsocketUri().toString()))
                .route("realtime-service", route -> route
                        .path("/ws", "/ws/**")
                        .uri(properties.getRealtimeServiceUri().toString()))
                .route("metrics-service", route -> route
                        .path("/metrics/**")
                        .uri(properties.getMetricsServiceUri().toString()))
                .route("voice-service", route -> route
                        .path("/voice/**")
                        .uri(properties.getVoiceServiceUri().toString()))
                .build();
    }
}
