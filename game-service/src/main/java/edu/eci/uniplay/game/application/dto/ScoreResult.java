package edu.eci.uniplay.game.application.dto;

import java.util.UUID;

public record ScoreResult(UUID playerId, int score) {
}
