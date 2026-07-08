package edu.eci.uniplay.game.application.port.in;

import java.util.UUID;

import edu.eci.uniplay.game.application.dto.GameStateResult;

public interface GetGameStateUseCase {

    GameStateResult getState(String roomCode, UUID viewerPlayerId);
}
