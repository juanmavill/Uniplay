package edu.eci.uniplay.metrics.infrastructure.web.dto;

import edu.eci.uniplay.metrics.application.dto.BusinessKpisResult;

public record BusinessKpisResponse(
        int activeRooms,
        int connectedPlayers,
        double guessRate,
        double averagePlayersPerRoom
) {
    public static BusinessKpisResponse from(BusinessKpisResult result) {
        return new BusinessKpisResponse(
                result.activeRooms(),
                result.connectedPlayers(),
                result.guessRate(),
                result.averagePlayersPerRoom()
        );
    }
}
