package edu.eci.uniplay.game.domain.model;

public record AnswerEvaluation(
        GameSession session,
        boolean correct,
        int score,
        RoundId roundId
) {
}
