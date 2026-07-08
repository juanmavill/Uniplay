package edu.eci.uniplay.game.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameSessionTest {

    private static final RoomCode ROOM_CODE = new RoomCode("ABC123");
    private static final RoundId ROUND_ID = new RoundId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final PlayerId PLAYER_ID = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final Instant STARTED_AT = Instant.parse("2026-07-07T12:00:00Z");
    private static final Instant ANSWERED_AT = Instant.parse("2026-07-07T12:00:05Z");

    @Test
    void startsRoundWhenNoRoundIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE);

        GameSession updatedSession = session.startRound(ROUND_ID, new SecretWord("Biblioteca"), STARTED_AT);

        assertThat(updatedSession.round()).get().satisfies(round -> {
            assertThat(round.id()).isEqualTo(ROUND_ID);
            assertThat(round.status()).isEqualTo(RoundStatus.ACTIVE);
            assertThat(round.startedAt()).isEqualTo(STARTED_AT);
        });
    }

    @Test
    void rejectsNewRoundWhenAnotherRoundIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), STARTED_AT);

        assertThatThrownBy(() -> session.startRound(new RoundId(UUID.randomUUID()), new SecretWord("Campus"), STARTED_AT))
                .isInstanceOf(RoundAlreadyActiveException.class)
                .hasMessageContaining("already has an active round");
    }

    @Test
    void correctAnswerAddsScoreAndFinishesRound() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), STARTED_AT);

        AnswerEvaluation evaluation = session.submitAnswer(PLAYER_ID, " biblioteca ", 100, ANSWERED_AT);

        assertThat(evaluation.correct()).isTrue();
        assertThat(evaluation.score()).isEqualTo(100);
        assertThat(evaluation.session().round()).get().satisfies(round -> {
            assertThat(round.status()).isEqualTo(RoundStatus.FINISHED);
            assertThat(round.guessedBy()).isEqualTo(PLAYER_ID);
            assertThat(round.finishedAt()).isEqualTo(ANSWERED_AT);
        });
    }

    @Test
    void wrongAnswerKeepsCurrentScoreAndRoundActive() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), STARTED_AT);

        AnswerEvaluation evaluation = session.submitAnswer(PLAYER_ID, "cafeteria", 100, ANSWERED_AT);

        assertThat(evaluation.correct()).isFalse();
        assertThat(evaluation.score()).isZero();
        assertThat(evaluation.session()).isSameAs(session);
        assertThat(session.round()).get().extracting(Round::status).isEqualTo(RoundStatus.ACTIVE);
    }

    @Test
    void rejectsAnswerWhenNoRoundIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE);

        assertThatThrownBy(() -> session.submitAnswer(PLAYER_ID, "biblioteca", 100, ANSWERED_AT))
                .isInstanceOf(RoundNotActiveException.class)
                .hasMessageContaining("does not have an active round");
    }
}
