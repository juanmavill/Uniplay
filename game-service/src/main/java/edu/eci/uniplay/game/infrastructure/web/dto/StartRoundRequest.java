package edu.eci.uniplay.game.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record StartRoundRequest(String mode, String deck, UUID drawerId, List<String> customWords) {
}
