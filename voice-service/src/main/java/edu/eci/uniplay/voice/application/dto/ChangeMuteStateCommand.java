package edu.eci.uniplay.voice.application.dto;

public record ChangeMuteStateCommand(String roomCode, String playerId, boolean muted) {
}
