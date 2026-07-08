package edu.eci.uniplay.game.application.port.out;

import java.util.Optional;

import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;

public interface GameSessionRepository {

    Optional<GameSession> findByRoomCode(RoomCode roomCode);

    void save(GameSession session);
}
