package edu.eci.uniplay.metrics.application.service;

import java.util.HashSet;
import java.util.Set;

import edu.eci.uniplay.metrics.application.dto.BusinessKpisResult;
import edu.eci.uniplay.metrics.application.port.in.GetBusinessKpisUseCase;
import edu.eci.uniplay.metrics.application.port.in.RecordDomainEventUseCase;

public class BusinessKpiProjectionService implements GetBusinessKpisUseCase, RecordDomainEventUseCase {

    private final Set<String> activeRooms = new HashSet<>();
    private final Set<String> connectedPlayers = new HashSet<>();
    private final Set<String> startedRounds = new HashSet<>();
    private final Set<String> guessedRounds = new HashSet<>();

    @Override
    public synchronized void recordRoomCreated(String roomCode) {
        activeRooms.add(roomCode);
    }

    @Override
    public synchronized void recordPlayerConnected(String roomCode, String playerId) {
        activeRooms.add(roomCode);
        connectedPlayers.add(roomCode + ":" + playerId);
    }

    @Override
    public synchronized void recordRoundStarted(String roomCode, String roundId) {
        activeRooms.add(roomCode);
        startedRounds.add(roomCode + ":" + roundId);
    }

    @Override
    public synchronized void recordWordGuessed(String roomCode, String roundId) {
        guessedRounds.add(roomCode + ":" + roundId);
    }

    @Override
    public synchronized BusinessKpisResult currentKpis() {
        return new BusinessKpisResult(
                activeRooms.size(),
                connectedPlayers.size(),
                guessRate(),
                averagePlayersPerRoom()
        );
    }

    private double guessRate() {
        if (startedRounds.isEmpty()) {
            return 0.0;
        }
        return (double) guessedRounds.size() / startedRounds.size();
    }

    private double averagePlayersPerRoom() {
        if (activeRooms.isEmpty()) {
            return 0.0;
        }
        return (double) connectedPlayers.size() / activeRooms.size();
    }
}
