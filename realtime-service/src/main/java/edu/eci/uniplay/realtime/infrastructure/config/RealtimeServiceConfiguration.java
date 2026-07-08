package edu.eci.uniplay.realtime.infrastructure.config;

import java.time.Clock;

import edu.eci.uniplay.realtime.application.port.in.BroadcastDrawingUseCase;
import edu.eci.uniplay.realtime.application.port.out.DrawingMessageBroker;
import edu.eci.uniplay.realtime.application.service.BroadcastDrawingService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RealtimeProperties.class)
public class RealtimeServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    BroadcastDrawingUseCase broadcastDrawingUseCase(DrawingMessageBroker drawingMessageBroker, Clock clock) {
        return new BroadcastDrawingService(drawingMessageBroker, clock);
    }
}
