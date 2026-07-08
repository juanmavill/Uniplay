package edu.eci.uniplay.metrics.application.port.in;

public interface RecordDomainEventUseCase {

    void recordRoomCreated(String roomCode);

    void recordPlayerConnected(String roomCode, String playerId);

    void recordRoundStarted(String roomCode, String roundId);

    void recordWordGuessed(String roomCode, String roundId);
}
