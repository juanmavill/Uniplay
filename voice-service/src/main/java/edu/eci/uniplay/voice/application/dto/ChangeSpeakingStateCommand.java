package edu.eci.uniplay.voice.application.dto;

public record ChangeSpeakingStateCommand(String roomCode, String playerId, boolean speaking) {
}
