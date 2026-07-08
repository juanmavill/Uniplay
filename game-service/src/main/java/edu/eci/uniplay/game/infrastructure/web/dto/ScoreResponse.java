package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.UUID;

import edu.eci.uniplay.game.application.dto.ScoreResult;

public record ScoreResponse(UUID playerId, int score) {

    static ScoreResponse from(ScoreResult result) {
        return new ScoreResponse(result.playerId(), result.score());
    }
}
