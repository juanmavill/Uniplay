package edu.eci.uniplay.realtime.infrastructure.websocket;

import edu.eci.uniplay.realtime.infrastructure.config.RealtimeProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties realtimeProperties;

    public WebSocketConfig(RealtimeProperties realtimeProperties) {
        this.realtimeProperties = realtimeProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(realtimeProperties.websocketEndpoint())
                .setAllowedOriginPatterns(realtimeProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }
}
