package edu.eci.uniplay.room.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank
        @Size(min = 2, max = 30)
        String playerName
) {
}
