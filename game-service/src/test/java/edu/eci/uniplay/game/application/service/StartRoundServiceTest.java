package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.event.VoteCastEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StartRoundServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-07T12:00:00Z");

    @Test
    void createsSessionRoundAndPublishesEvent() {
        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository();
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        StartRoundService service = new StartRoundService(
                repository,
                (roomCode, deck, customWords) -> new SecretWord("Campus"),
                eventPublisher,
                Duration.ofSeconds(45),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        StartRoundResult result = service.startRound(new StartRoundCommand(
                "abc123",
                "ALL_DRAW",
                "sistemas",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                null,
                List.of(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        UUID.fromString("33333333-3333-3333-3333-333333333333")
                )
        ));

        assertThat(result.roomCode()).isEqualTo("ABC123");
        assertThat(result.word()).isEqualTo("Campus");
        assertThat(result.drawerId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(result.mode()).isEqualTo("ALL_DRAW");
        assertThat(result.deck()).isEqualTo("SISTEMAS");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.startedAt()).isEqualTo(NOW);
        assertThat(result.endsAt()).isEqualTo(NOW.plusSeconds(45));
        assertThat(repository.savedSession).isNotNull();
        assertThat(repository.savedSession.round()).get().satisfies(round ->
                assertThat(round.eligibleGuessers())
                        .containsExactly(new edu.eci.uniplay.game.domain.model.PlayerId(
                                UUID.fromString("33333333-3333-3333-3333-333333333333")
                        ))
        );
        assertThat(eventPublisher.roundStartedEvents).singleElement().satisfies(event -> {
            assertThat(event.roomCode()).isEqualTo("ABC123");
            assertThat(event.roundId()).isEqualTo(result.roundId());
            assertThat(event.word()).isEqualTo("Campus");
            assertThat(event.mode()).isEqualTo("ALL_DRAW");
            assertThat(event.deck()).isEqualTo("SISTEMAS");
            assertThat(event.startedAt()).isEqualTo(NOW);
            assertThat(event.endsAt()).isEqualTo(NOW.plusSeconds(45));
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    private static final class InMemoryGameSessionRepository implements GameSessionRepository {

        private GameSession savedSession;

        @Override
        public Optional<GameSession> findByRoomCode(RoomCode roomCode) {
            return Optional.ofNullable(savedSession);
        }

        @Override
        public void save(GameSession session) {
            savedSession = session;
        }
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {

        private final List<RoundStartedEvent> roundStartedEvents = new ArrayList<>();

        @Override
        public void publishRoundStarted(RoundStartedEvent event) {
            roundStartedEvents.add(event);
        }

        @Override
        public void publishRoundGuessed(RoundGuessedEvent event) {
        }

        @Override
        public void publishRoundFinished(RoundFinishedEvent event) {
        }

        @Override
        public void publishVoteCast(VoteCastEvent event) {
        }
    }
}
