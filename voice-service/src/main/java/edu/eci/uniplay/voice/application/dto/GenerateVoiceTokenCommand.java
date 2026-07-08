package edu.eci.uniplay.voice.application.dto;

public record GenerateVoiceTokenCommand(String roomCode, String playerId, String playerName) {
}
