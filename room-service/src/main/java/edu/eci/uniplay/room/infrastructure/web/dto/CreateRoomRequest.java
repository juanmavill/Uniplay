package edu.eci.uniplay.room.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record CreateRoomRequest(@Min(2) Integer maxPlayers) {
}
