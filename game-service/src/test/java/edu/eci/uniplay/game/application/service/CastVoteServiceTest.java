package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.CastVoteCommand;
import edu.eci.uniplay.game.application.dto.CastVoteResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.event.VoteCastEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.RoundMode;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CastVoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-07T12:00:05Z");
    private static final UUID ROUND_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VOTER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CANDIDATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void storesVoteAndPublishesEvent() {
        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository(activeAllDrawSession());
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        CastVoteService service = new CastVoteService(repository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));

        CastVoteResult result = service.castVote(new CastVoteCommand("ABC123", ROUND_ID, VOTER_ID, CANDIDATE_ID));

        assertThat(result.tallies()).singleElement().satisfies(tally -> {
            assertThat(tally.candidateId()).isEqualTo(CANDIDATE_ID);
            assertThat(tally.votes()).isEqualTo(1);
        });
        assertThat(repository.savedSession.round()).get()
                .satisfies(round -> assertThat(round.votes()).hasSize(1));
        assertThat(eventPublisher.voteCastEvents).singleElement().satisfies(event -> {
            assertThat(event.roomCode()).isEqualTo("ABC123");
            assertThat(event.voterId()).isEqualTo(VOTER_ID);
            assertThat(event.candidateId()).isEqualTo(CANDIDATE_ID);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    private static GameSession activeAllDrawSession() {
        return GameSession.newFor(new RoomCode("ABC123"))
                .startRound(
                        new RoundId(ROUND_ID),
                        new SecretWord("Campus"),
                        RoundMode.ALL_DRAW,
                        Instant.parse("2026-07-07T12:00:00Z"),
                        Instant.parse("2026-07-07T12:01:00Z")
                );
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

        private final List<VoteCastEvent> voteCastEvents = new ArrayList<>();

        @Override
        public void publishRoundStarted(RoundStartedEvent event) {
        }

        @Override
        public void publishRoundGuessed(RoundGuessedEvent event) {
        }

        @Override
        public void publishRoundFinished(RoundFinishedEvent event) {
        }

        @Override
        public void publishVoteCast(VoteCastEvent event) {
            voteCastEvents.add(event);
        }
    }
}
