package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmitAnswerServiceTest {

    private static final Instant STARTED_AT = Instant.parse("2026-07-07T12:00:00Z");
    private static final Instant ANSWERED_AT = Instant.parse("2026-07-07T12:00:07Z");
    private static final Instant ENDS_AT = Instant.parse("2026-07-07T12:01:00Z");
    private static final UUID PLAYER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void correctAnswerSavesScoreAndPublishesEvent() {
        GameSession session = activeSession();
        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository(session);
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        SubmitAnswerService service = new SubmitAnswerService(
                repository,
                eventPublisher,
                100,
                Clock.fixed(ANSWERED_AT, ZoneOffset.UTC)
        );

        SubmitAnswerResult result = service.submitAnswer(new SubmitAnswerCommand("ABC123", PLAYER_ID, "campus"));

        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.roundStatus()).isEqualTo("FINISHED");
        assertThat(repository.savedSession.scoreOf(new PlayerId(PLAYER_ID))).isEqualTo(100);
        assertThat(eventPublisher.roundGuessedEvents).singleElement().satisfies(event -> {
            assertThat(event.playerId()).isEqualTo(PLAYER_ID);
            assertThat(event.score()).isEqualTo(100);
            assertThat(event.occurredAt()).isEqualTo(ANSWERED_AT);
        });
        assertThat(eventPublisher.roundFinishedEvents).singleElement().satisfies(event -> {
            assertThat(event.reason()).isEqualTo("GUESSED");
            assertThat(event.status()).isEqualTo("FINISHED");
            assertThat(event.finishedAt()).isEqualTo(ANSWERED_AT);
        });
    }

    @Test
    void wrongAnswerDoesNotPersistOrPublishEvent() {
        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository(activeSession());
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        SubmitAnswerService service = new SubmitAnswerService(
                repository,
                eventPublisher,
                100,
                Clock.fixed(ANSWERED_AT, ZoneOffset.UTC)
        );

        SubmitAnswerResult result = service.submitAnswer(new SubmitAnswerCommand("ABC123", PLAYER_ID, "biblioteca"));

        assertThat(result.correct()).isFalse();
        assertThat(result.score()).isZero();
        assertThat(result.roundStatus()).isEqualTo("ACTIVE");
        assertThat(repository.saveCount).isZero();
        assertThat(eventPublisher.roundGuessedEvents).isEmpty();
        assertThat(eventPublisher.roundFinishedEvents).isEmpty();
    }

    private static GameSession activeSession() {
        return GameSession.newFor(new RoomCode("ABC123"))
                .startRound(
                        new RoundId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                        new SecretWord("Campus"),
                        STARTED_AT,
                        ENDS_AT
                );
    }

    private static final class InMemoryGameSessionRepository implements GameSessionRepository {

        private GameSession savedSession;
        private int saveCount;

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
            saveCount++;
        }
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {

        private final List<RoundGuessedEvent> roundGuessedEvents = new ArrayList<>();
        private final List<RoundFinishedEvent> roundFinishedEvents = new ArrayList<>();

        @Override
        public void publishRoundStarted(RoundStartedEvent event) {
        }

        @Override
        public void publishRoundGuessed(RoundGuessedEvent event) {
            roundGuessedEvents.add(event);
        }

        @Override
        public void publishRoundFinished(RoundFinishedEvent event) {
            roundFinishedEvents.add(event);
        }
    }
}
