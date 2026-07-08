package edu.eci.uniplay.game.infrastructure.config;

import java.time.Clock;

import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.application.service.GetGameStateService;
import edu.eci.uniplay.game.application.service.StartRoundService;
import edu.eci.uniplay.game.application.service.SubmitAnswerService;
import edu.eci.uniplay.game.infrastructure.memory.InMemoryGameSessionRepository;
import edu.eci.uniplay.game.infrastructure.memory.NoOpDomainEventPublisher;
import edu.eci.uniplay.game.infrastructure.word.DefaultWordDeckProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    GameSessionRepository gameSessionRepository() {
        return new InMemoryGameSessionRepository();
    }

    @Bean
    DomainEventPublisher domainEventPublisher() {
        return new NoOpDomainEventPublisher();
    }

    @Bean
    StartRoundUseCase startRoundUseCase(
            GameSessionRepository gameSessionRepository,
            WordDeckProvider wordDeckProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        return new StartRoundService(gameSessionRepository, wordDeckProvider, domainEventPublisher, clock);
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
                clock
        );
    }

    @Bean
    GetGameStateUseCase getGameStateUseCase(GameSessionRepository gameSessionRepository) {
        return new GetGameStateService(gameSessionRepository);
    }
}
