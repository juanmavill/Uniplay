package edu.eci.uniplay.room.infrastructure.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.room.application.port.in.CreateRoomUseCase;
import edu.eci.uniplay.room.application.port.in.JoinRoomUseCase;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.room.application.port.out.RoomCodeGenerator;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.application.service.CreateRoomService;
import edu.eci.uniplay.room.application.service.JoinRoomService;
import edu.eci.uniplay.room.application.service.RoomCreationPolicy;
import edu.eci.uniplay.room.infrastructure.code.SecureRandomRoomCodeGenerator;
import edu.eci.uniplay.room.infrastructure.redis.RedisDomainEventPublisher;
import edu.eci.uniplay.room.infrastructure.redis.RedisRoomRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RoomProperties.class)
public class RoomServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RoomCreationPolicy roomCreationPolicy(RoomProperties roomProperties) {
        return new RoomCreationPolicy(roomProperties.maxPlayers(), roomProperties.codeGenerationMaxAttempts());
    }

    @Bean
    RoomCodeGenerator roomCodeGenerator() {
        return new SecureRandomRoomCodeGenerator();
    }

    @Bean
    RoomRepository roomRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RoomProperties roomProperties
    ) {
        return new RedisRoomRepository(redisTemplate, objectMapper, roomProperties.ttl());
    }

    @Bean
    DomainEventPublisher domainEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisDomainEventPublisher(redisTemplate, objectMapper);
    }

    @Bean
    CreateRoomUseCase createRoomUseCase(
            RoomRepository roomRepository,
            RoomCodeGenerator roomCodeGenerator,
            DomainEventPublisher domainEventPublisher,
            RoomCreationPolicy roomCreationPolicy,
            Clock clock
    ) {
        return new CreateRoomService(
                roomRepository,
                roomCodeGenerator,
                domainEventPublisher,
                roomCreationPolicy,
                clock
        );
    }

    @Bean
    JoinRoomUseCase joinRoomUseCase(
            RoomRepository roomRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        return new JoinRoomService(roomRepository, domainEventPublisher, clock);
    }
}
