package edu.eci.uniplay.room.application.service;

public record RoomCreationPolicy(int defaultMaxPlayers, int maxCodeGenerationAttempts) {

    public RoomCreationPolicy {
        if (defaultMaxPlayers < 2) {
            throw new IllegalArgumentException("defaultMaxPlayers must be at least 2");
        }

        if (maxCodeGenerationAttempts < 1) {
            throw new IllegalArgumentException("maxCodeGenerationAttempts must be at least 1");
        }
    }

    public int resolveMaxPlayers(Integer requestedMaxPlayers) {
        if (requestedMaxPlayers == null) {
            return defaultMaxPlayers;
        }

        if (requestedMaxPlayers < 2) {
            throw new IllegalArgumentException("maxPlayers must be at least 2");
        }

        return requestedMaxPlayers;
    }
}
