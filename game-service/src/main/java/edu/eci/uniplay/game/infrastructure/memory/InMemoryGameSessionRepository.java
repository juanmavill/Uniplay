package edu.eci.uniplay.game.infrastructure.memory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;

public class InMemoryGameSessionRepository implements GameSessionRepository {

    private final ConcurrentMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<GameSession> findByRoomCode(RoomCode roomCode) {
        return Optional.ofNullable(sessions.get(roomCode.value()));
    }

    @Override
    public void save(GameSession session) {
        sessions.put(session.roomCode().value(), session);
    }
}
