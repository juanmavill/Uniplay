package edu.eci.uniplay.game.application.service;

import java.util.UUID;

import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;

public class GetGameStateService implements GetGameStateUseCase {

    private final GameSessionRepository gameSessionRepository;

    public GetGameStateService(GameSessionRepository gameSessionRepository) {
        this.gameSessionRepository = gameSessionRepository;
    }

    @Override
    public GameStateResult getState(String roomCode, UUID viewerPlayerId) {
        RoomCode code = new RoomCode(roomCode);
        GameSession session = gameSessionRepository.findByRoomCode(code)
                .orElseGet(() -> GameSession.newFor(code));

        return GameResultMapper.toStateResult(session, viewerPlayerId);
    }
}
