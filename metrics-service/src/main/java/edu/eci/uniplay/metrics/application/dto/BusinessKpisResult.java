package edu.eci.uniplay.metrics.application.dto;

public record BusinessKpisResult(
        int activeRooms,
        int connectedPlayers,
        double guessRate,
        double averagePlayersPerRoom
) {
}
