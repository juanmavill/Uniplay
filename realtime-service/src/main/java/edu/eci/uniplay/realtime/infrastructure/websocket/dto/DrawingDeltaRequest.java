package edu.eci.uniplay.realtime.infrastructure.websocket.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DrawingDeltaRequest(
        @NotNull UUID playerId,
        @Min(0) @Max(1) double fromX,
        @Min(0) @Max(1) double fromY,
        @Min(0) @Max(1) double toX,
        @Min(0) @Max(1) double toY,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
        @Min(1) @Max(40) double width
) {
}
