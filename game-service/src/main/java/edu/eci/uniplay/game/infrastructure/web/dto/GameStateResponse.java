package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.List;

import edu.eci.uniplay.game.application.dto.GameStateResult;

public record GameStateResponse(
        String roomCode,
        RoundResponse round,
        List<ScoreResponse> scores
) {

    public static GameStateResponse from(GameStateResult result) {
        return new GameStateResponse(
                result.roomCode(),
                result.round() == null ? null : RoundResponse.from(result.round()),
                result.scores().stream().map(ScoreResponse::from).toList()
        );
    }
}
