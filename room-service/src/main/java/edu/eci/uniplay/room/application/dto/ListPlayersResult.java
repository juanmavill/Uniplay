package edu.eci.uniplay.room.application.dto;

import java.util.List;
import java.util.UUID;

public record ListPlayersResult(UUID roomId, String code, List<PlayerResult> players) {
}
