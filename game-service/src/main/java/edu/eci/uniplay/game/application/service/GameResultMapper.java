package edu.eci.uniplay.game.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
        return toStateResult(session, null);
    }

    static GameStateResult toStateResult(GameSession session, UUID viewerPlayerId) {
        List<ScoreResult> scores = session.scores().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .map(entry -> toScoreResult(entry.getKey(), entry.getValue()))
                .toList();

        return new GameStateResult(
                session.roomCode().value(),
                session.round().map(round -> toRoundResult(round, viewerPlayerId)).orElse(null),
                scores
        );
    }

    static RoundResult toRoundResult(Round round) {
        return toRoundResult(round, null);
    }

    static RoundResult toRoundResult(Round round, UUID viewerPlayerId) {
        return new RoundResult(
                round.id().value(),
                round.mode().name(),
                round.status().name(),
                canSeeWord(round, viewerPlayerId) ? round.secretWord().value() : null,
                round.drawerId() == null ? null : round.drawerId().value(),
                round.guessedBy() == null ? null : round.guessedBy().value(),
                round.guessedPlayers().stream().map(PlayerId::value).sorted().toList(),
                round.eligibleGuessers().size(),
                round.startedAt(),
                round.endsAt(),
                round.finishedAt()
        );
    }

    private static ScoreResult toScoreResult(PlayerId playerId, int score) {
        return new ScoreResult(playerId.value(), score);
    }

    private static boolean canSeeWord(Round round, UUID viewerPlayerId) {
        return round.mode().name().equals("ALL_DRAW")
                || round.drawerId() == null
                || (viewerPlayerId != null && round.drawerId().value().equals(viewerPlayerId));
    }
}
