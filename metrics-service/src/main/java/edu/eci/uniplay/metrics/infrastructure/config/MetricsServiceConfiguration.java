package edu.eci.uniplay.metrics.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.metrics.application.port.in.GetBusinessKpisUseCase;
import edu.eci.uniplay.metrics.application.port.in.RecordDomainEventUseCase;
import edu.eci.uniplay.metrics.application.service.BusinessKpiProjectionService;
import edu.eci.uniplay.metrics.infrastructure.redis.RedisDomainEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class MetricsServiceConfiguration {

    @Bean
    BusinessKpiProjectionService businessKpiProjectionService() {
        return new BusinessKpiProjectionService();
    }

    @Bean
    GetBusinessKpisUseCase getBusinessKpisUseCase(BusinessKpiProjectionService businessKpiProjectionService) {
        return businessKpiProjectionService;
    }

    @Bean
    RecordDomainEventUseCase recordDomainEventUseCase(BusinessKpiProjectionService businessKpiProjectionService) {
        return businessKpiProjectionService;
    }

    @Bean
    RedisDomainEventSubscriber redisDomainEventSubscriber(
            ObjectMapper objectMapper,
            RecordDomainEventUseCase recordDomainEventUseCase
    ) {
        return new RedisDomainEventSubscriber(objectMapper, recordDomainEventUseCase);
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisDomainEventSubscriber redisDomainEventSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(redisDomainEventSubscriber, new ChannelTopic(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL));
        container.addMessageListener(redisDomainEventSubscriber, new ChannelTopic(RedisDomainEventSubscriber.PLAYER_CONNECTED_CHANNEL));
        container.addMessageListener(redisDomainEventSubscriber, new ChannelTopic(RedisDomainEventSubscriber.ROUND_STARTED_CHANNEL));
        container.addMessageListener(redisDomainEventSubscriber, new ChannelTopic(RedisDomainEventSubscriber.WORD_GUESSED_CHANNEL));
        return container;
    }
}
