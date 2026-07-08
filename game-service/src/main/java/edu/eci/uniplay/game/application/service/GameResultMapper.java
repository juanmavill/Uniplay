package edu.eci.uniplay.game.application.service;

import java.util.Comparator;
import java.util.List;

import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.dto.RoundResult;
import edu.eci.uniplay.game.application.dto.ScoreResult;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.Round;

final class GameResultMapper {

    private GameResultMapper() {
    }

    static GameStateResult toStateResult(GameSession session) {
        List<ScoreResult> scores = session.scores().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .map(entry -> toScoreResult(entry.getKey(), entry.getValue()))
                .toList();

        return new GameStateResult(
                session.roomCode().value(),
                session.round().map(GameResultMapper::toRoundResult).orElse(null),
                scores
        );
    }

    static RoundResult toRoundResult(Round round) {
        return new RoundResult(
                round.id().value(),
                round.status().name(),
                round.secretWord().value(),
                round.guessedBy() == null ? null : round.guessedBy().value(),
                round.startedAt(),
                round.endsAt(),
                round.finishedAt()
        );
    }

    private static ScoreResult toScoreResult(PlayerId playerId, int score) {
        return new ScoreResult(playerId.value(), score);
    }
}
