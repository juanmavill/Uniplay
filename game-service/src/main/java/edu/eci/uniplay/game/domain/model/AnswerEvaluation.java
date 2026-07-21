package edu.eci.uniplay.game.domain.model;

public record AnswerEvaluation(
        GameSession session,
        boolean correct,
        boolean newlyGuessed,
        int score,
        int pointsAwarded,
        RoundId roundId,
        boolean roundFinished,
        boolean drawerBonusAwarded
) {
}
