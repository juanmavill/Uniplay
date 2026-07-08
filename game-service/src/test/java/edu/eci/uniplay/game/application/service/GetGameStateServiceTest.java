package edu.eci.uniplay.game.application.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.SecretWord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetGameStateServiceTest {

    @Test
    void returnsEmptyStateWhenSessionDoesNotExist() {
        GetGameStateService service = new GetGameStateService(repositoryReturning(null));

        GameStateResult result = service.getState("ABC123");

        assertThat(result.roomCode()).isEqualTo("ABC123");
        assertThat(result.round()).isNull();
        assertThat(result.scores()).isEmpty();
    }

    @Test
    void returnsPersistedRoundAndScores() {
        PlayerId playerId = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        GameSession session = GameSession.restore(
                new RoomCode("ABC123"),
                Round.start(
                        new RoundId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                        new SecretWord("Campus"),
                        Instant.parse("2026-07-07T12:00:00Z")
                ),
                Map.of(playerId, 100)
        );
        GetGameStateService service = new GetGameStateService(repositoryReturning(session));

        GameStateResult result = service.getState("ABC123");

        assertThat(result.round().word()).isEqualTo("Campus");
        assertThat(result.scores()).singleElement().satisfies(score -> {
            assertThat(score.playerId()).isEqualTo(playerId.value());
            assertThat(score.score()).isEqualTo(100);
        });
    }

    private static GameSessionRepository repositoryReturning(GameSession session) {
        return new GameSessionRepository() {
            @Override
            public Optional<GameSession> findByRoomCode(RoomCode roomCode) {
                return Optional.ofNullable(session);
            }

            @Override
            public void save(GameSession session) {
            }
        };
    }
}
