package edu.eci.uniplay.game.infrastructure.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.game.application.port.in.CastVoteUseCase;
import edu.eci.uniplay.game.application.port.in.ExpireRoundUseCase;
import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.application.service.CastVoteService;
import edu.eci.uniplay.game.application.service.ExpireRoundService;
import edu.eci.uniplay.game.application.service.GetGameStateService;
import edu.eci.uniplay.game.application.service.StartRoundService;
import edu.eci.uniplay.game.application.service.SubmitAnswerService;
import edu.eci.uniplay.game.infrastructure.redis.RedisDomainEventPublisher;
import edu.eci.uniplay.game.infrastructure.redis.RedisGameSessionRepository;
import edu.eci.uniplay.game.infrastructure.word.DefaultWordDeckProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(GameProperties.class)
public class GameServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    WordDeckProvider wordDeckProvider() {
        return new DefaultWordDeckProvider();
    }

    @Bean
    GameSessionRepository gameSessionRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            GameProperties gameProperties
    ) {
        return new RedisGameSessionRepository(redisTemplate, objectMapper, gameProperties.sessionTtl());
    }

    @Bean
    DomainEventPublisher domainEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisDomainEventPublisher(redisTemplate, objectMapper);
    }

    @Bean
    StartRoundUseCase startRoundUseCase(
            GameSessionRepository gameSessionRepository,
            WordDeckProvider wordDeckProvider,
            DomainEventPublisher domainEventPublisher,
            GameProperties gameProperties,
            Clock clock
    ) {
        return new StartRoundService(
                gameSessionRepository,
                wordDeckProvider,
                domainEventPublisher,
                gameProperties.roundDuration(),
                clock
        );
    }

    @Bean
    SubmitAnswerUseCase submitAnswerUseCase(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            GameProperties gameProperties,
            Clock clock
    ) {
        return new SubmitAnswerService(
                gameSessionRepository,
                domainEventPublisher,
                gameProperties.pointsPerCorrectAnswer(),
                gameProperties.drawerMajorityBonus(),
                clock
        );
    }

    @Bean
    ExpireRoundUseCase expireRoundUseCase(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        return new ExpireRoundService(gameSessionRepository, domainEventPublisher, clock);
    }

    @Bean
    CastVoteUseCase castVoteUseCase(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        return new CastVoteService(gameSessionRepository, domainEventPublisher, clock);
    }

    @Bean
    GetGameStateUseCase getGameStateUseCase(GameSessionRepository gameSessionRepository) {
        return new GetGameStateService(gameSessionRepository);
    }
}
