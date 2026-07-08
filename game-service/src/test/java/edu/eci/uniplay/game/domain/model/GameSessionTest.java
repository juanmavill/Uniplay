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
    private static final PlayerId OTHER_PLAYER_ID = new PlayerId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final Instant STARTED_AT = Instant.parse("2026-07-07T12:00:00Z");
    private static final Instant ANSWERED_AT = Instant.parse("2026-07-07T12:00:05Z");
    private static final Instant ENDS_AT = Instant.parse("2026-07-07T12:01:00Z");

    @Test
    void startsRoundWhenNoRoundIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE);

        GameSession updatedSession = session.startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        assertThat(updatedSession.round()).get().satisfies(round -> {
            assertThat(round.id()).isEqualTo(ROUND_ID);
            assertThat(round.mode()).isEqualTo(RoundMode.CLASSIC);
            assertThat(round.status()).isEqualTo(RoundStatus.ACTIVE);
            assertThat(round.startedAt()).isEqualTo(STARTED_AT);
            assertThat(round.endsAt()).isEqualTo(ENDS_AT);
        });
    }

    @Test
    void rejectsNewRoundWhenAnotherRoundIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.startRound(new RoundId(UUID.randomUUID()), new SecretWord("Campus"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT))
                .isInstanceOf(RoundAlreadyActiveException.class)
                .hasMessageContaining("already has an active round");
    }

    @Test
    void correctAnswerAddsScoreAndFinishesRound() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

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
    void rejectsAnswerFromDrawer() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, PLAYER_ID, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.submitAnswer(PLAYER_ID, "biblioteca", 100, ANSWERED_AT))
                .isInstanceOf(DrawingPlayerCannotGuessException.class)
                .hasMessageContaining("cannot guess");
    }

    @Test
    void wrongAnswerKeepsCurrentScoreAndRoundActive() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

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

    @Test
    void rejectsCorrectAnswerWhenRoundAlreadyExpired() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.submitAnswer(PLAYER_ID, "biblioteca", 100, ENDS_AT))
                .isInstanceOf(RoundExpiredException.class)
                .hasMessageContaining("expired at");
    }

    @Test
    void expiresRoundWhenDeadlineIsReached() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        GameSession updatedSession = session.expireRound(ROUND_ID, ENDS_AT);

        assertThat(updatedSession.round()).get().satisfies(round -> {
            assertThat(round.status()).isEqualTo(RoundStatus.EXPIRED);
            assertThat(round.finishedAt()).isEqualTo(ENDS_AT);
            assertThat(round.guessedBy()).isNull();
        });
    }

    @Test
    void rejectsExpirationBeforeDeadline() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.expireRound(ROUND_ID, ENDS_AT.minusSeconds(1)))
                .isInstanceOf(RoundNotExpiredException.class)
                .hasMessageContaining("has not expired yet");
    }

    @Test
    void acceptsVoteWhenAllDrawModeIsActive() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.ALL_DRAW, STARTED_AT, ENDS_AT);

        VoteEvaluation evaluation = session.castVote(ROUND_ID, PLAYER_ID, OTHER_PLAYER_ID);

        assertThat(evaluation.tallies()).containsEntry(OTHER_PLAYER_ID, 1);
        assertThat(evaluation.session().round()).get().satisfies(round -> {
            assertThat(round.mode()).isEqualTo(RoundMode.ALL_DRAW);
            assertThat(round.votes()).containsEntry(PLAYER_ID, OTHER_PLAYER_ID);
        });
    }

    @Test
    void rejectsVoteInClassicMode() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.CLASSIC, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.castVote(ROUND_ID, PLAYER_ID, OTHER_PLAYER_ID))
                .isInstanceOf(VotingNotEnabledException.class)
                .hasMessageContaining("does not accept votes");
    }

    @Test
    void rejectsDuplicateVote() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.ALL_DRAW, STARTED_AT, ENDS_AT);
        GameSession votedSession = session.castVote(ROUND_ID, PLAYER_ID, OTHER_PLAYER_ID).session();

        assertThatThrownBy(() -> votedSession.castVote(ROUND_ID, PLAYER_ID, new PlayerId(UUID.randomUUID())))
                .isInstanceOf(DuplicateVoteException.class)
                .hasMessageContaining("already voted");
    }

    @Test
    void rejectsSelfVote() {
        GameSession session = GameSession.newFor(ROOM_CODE)
                .startRound(ROUND_ID, new SecretWord("Biblioteca"), RoundMode.ALL_DRAW, STARTED_AT, ENDS_AT);

        assertThatThrownBy(() -> session.castVote(ROUND_ID, PLAYER_ID, PLAYER_ID))
                .isInstanceOf(SelfVoteException.class)
                .hasMessageContaining("cannot vote for themselves");
    }
}
