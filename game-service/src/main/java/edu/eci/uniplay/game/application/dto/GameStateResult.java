package edu.eci.uniplay.game.application.dto;

import java.util.List;

public record GameStateResult(
        String roomCode,
        RoundResult round,
        List<ScoreResult> scores
) {
}
