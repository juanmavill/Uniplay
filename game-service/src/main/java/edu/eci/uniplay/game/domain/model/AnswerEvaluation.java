package edu.eci.uniplay.game.domain.model;

public record AnswerEvaluation(
        GameSession session,
        boolean correct,
        boolean newlyGuessed,
        int score,
        RoundId roundId,
        boolean roundFinished
) {
}
