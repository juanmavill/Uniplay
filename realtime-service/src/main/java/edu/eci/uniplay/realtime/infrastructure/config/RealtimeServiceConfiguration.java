package edu.eci.uniplay.realtime.infrastructure.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.realtime.application.port.in.BroadcastDrawingUseCase;
import edu.eci.uniplay.realtime.application.port.out.DrawingMessageBroker;
import edu.eci.uniplay.realtime.application.port.out.RoundEventBroker;
import edu.eci.uniplay.realtime.application.port.out.VoiceEventBroker;
import edu.eci.uniplay.realtime.application.service.BroadcastDrawingService;
import edu.eci.uniplay.realtime.infrastructure.redis.RedisRoundEventSubscriber;
import edu.eci.uniplay.realtime.infrastructure.redis.RedisVoiceEventSubscriber;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

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

    @Bean
    RedisRoundEventSubscriber redisRoundEventSubscriber(ObjectMapper objectMapper, RoundEventBroker roundEventBroker) {
        return new RedisRoundEventSubscriber(objectMapper, roundEventBroker);
    }

    @Bean
    RedisVoiceEventSubscriber redisVoiceEventSubscriber(ObjectMapper objectMapper, VoiceEventBroker voiceEventBroker) {
        return new RedisVoiceEventSubscriber(objectMapper, voiceEventBroker);
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisRoundEventSubscriber redisRoundEventSubscriber,
            RedisVoiceEventSubscriber redisVoiceEventSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(redisRoundEventSubscriber, new ChannelTopic(RedisRoundEventSubscriber.ROUND_STARTED_CHANNEL));
        container.addMessageListener(redisRoundEventSubscriber, new ChannelTopic(RedisRoundEventSubscriber.ROUND_FINISHED_CHANNEL));
        container.addMessageListener(redisRoundEventSubscriber, new ChannelTopic(RedisRoundEventSubscriber.ROUND_GUESSED_CHANNEL));
        container.addMessageListener(redisRoundEventSubscriber, new ChannelTopic(RedisRoundEventSubscriber.VOTE_CAST_CHANNEL));
        container.addMessageListener(redisVoiceEventSubscriber, new ChannelTopic(RedisVoiceEventSubscriber.SPEAKING_STATE_CHANGED_CHANNEL));
        return container;
    }
}
