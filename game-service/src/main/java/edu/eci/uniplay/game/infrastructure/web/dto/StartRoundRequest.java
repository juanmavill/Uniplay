package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.UUID;

public record StartRoundRequest(String mode, String deck, UUID drawerId) {
}
