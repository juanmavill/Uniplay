package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.ExpireRoundCommand;
import edu.eci.uniplay.game.application.dto.ExpireRoundResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.RoundMode;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpireRoundServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-07T12:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-07-07T12:01:00Z");
    private static final UUID ROUND_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void expiresRoundAndPublishesFinishedEvent() {
        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository(activeSession());
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        ExpireRoundService service = new ExpireRoundService(
                repository,
                eventPublisher,
                Clock.fixed(ENDS_AT, ZoneOffset.UTC)
        );

        ExpireRoundResult result = service.expireRound(new ExpireRoundCommand("ABC123", ROUND_ID));

        assertThat(result.status()).isEqualTo("EXPIRED");
        assertThat(result.reason()).isEqualTo("TIMEOUT");
        assertThat(result.finishedAt()).isEqualTo(ENDS_AT);
        assertThat(repository.savedSession.round()).get()
                .extracting(round -> round.status().name())
                .isEqualTo("EXPIRED");
        assertThat(eventPublisher.roundFinishedEvents).singleElement().satisfies(event -> {
            assertThat(event.roomCode()).isEqualTo("ABC123");
            assertThat(event.roundId()).isEqualTo(ROUND_ID);
            assertThat(event.reason()).isEqualTo("TIMEOUT");
            assertThat(event.finishedAt()).isEqualTo(ENDS_AT);
        });
    }

    private static GameSession activeSession() {
        return GameSession.newFor(new RoomCode("ABC123"))
                .startRound(new RoundId(ROUND_ID), new SecretWord("Campus"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);
    }

    private static final class InMemoryGameSessionRepository implements GameSessionRepository {

        private GameSession savedSession;

        private InMemoryGameSessionRepository(GameSession savedSession) {
            this.savedSession = savedSession;
        }

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

        private final List<RoundFinishedEvent> roundFinishedEvents = new ArrayList<>();

        @Override
        public void publishRoundStarted(RoundStartedEvent event) {
        }

        @Override
        public void publishRoundGuessed(RoundGuessedEvent event) {
        }

        @Override
        public void publishRoundFinished(RoundFinishedEvent event) {
            roundFinishedEvents.add(event);
        }
    }
}
