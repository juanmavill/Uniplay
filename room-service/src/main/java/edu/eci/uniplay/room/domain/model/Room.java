package edu.eci.uniplay.room.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Room {

    private final RoomId id;
    private final RoomCode code;
    private final RoomStatus status;
    private final int maxPlayers;
    private final Instant createdAt;

    private Room(RoomId id, RoomCode code, RoomStatus status, int maxPlayers, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = Objects.requireNonNull(code, "code is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");

        if (maxPlayers < 2) {
            throw new IllegalArgumentException("maxPlayers must be at least 2");
        }

        this.maxPlayers = maxPlayers;
    }

    public static Room create(RoomId id, RoomCode code, int maxPlayers, Instant createdAt) {
        return new Room(id, code, RoomStatus.WAITING_FOR_PLAYERS, maxPlayers, createdAt);
    }

    public RoomId id() {
        return id;
    }

    public RoomCode code() {
        return code;
    }

    public RoomStatus status() {
        return status;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
