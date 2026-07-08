package edu.eci.uniplay.room.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Room {

    private final RoomId id;
    private final RoomCode code;
    private final RoomStatus status;
    private final int maxPlayers;
    private final Instant createdAt;
    private final List<Player> players;

    private Room(RoomId id, RoomCode code, RoomStatus status, int maxPlayers, Instant createdAt, List<Player> players) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = Objects.requireNonNull(code, "code is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.players = List.copyOf(Objects.requireNonNull(players, "players is required"));

        if (maxPlayers < 2) {
            throw new IllegalArgumentException("maxPlayers must be at least 2");
        }

        if (players.size() > maxPlayers) {
            throw new IllegalArgumentException("players cannot exceed maxPlayers");
        }

        this.maxPlayers = maxPlayers;
    }

    public static Room create(RoomId id, RoomCode code, int maxPlayers, Instant createdAt) {
        return new Room(id, code, RoomStatus.WAITING_FOR_PLAYERS, maxPlayers, createdAt, List.of());
    }

    public static Room restore(
            RoomId id,
            RoomCode code,
            RoomStatus status,
            int maxPlayers,
            Instant createdAt,
            List<Player> players
    ) {
        return new Room(id, code, status, maxPlayers, createdAt, players);
    }

    public Room join(Player player) {
        Objects.requireNonNull(player, "player is required");

        if (players.size() >= maxPlayers) {
            throw new RoomFullException(code);
        }

        if (hasPlayerNamed(player.name())) {
            throw new DuplicatePlayerException(player.name());
        }

        List<Player> updatedPlayers = new ArrayList<>(players);
        updatedPlayers.add(player);
        return new Room(id, code, status, maxPlayers, createdAt, updatedPlayers);
    }

    private boolean hasPlayerNamed(PlayerName playerName) {
        return players.stream().anyMatch(player -> player.hasName(playerName));
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

    public List<Player> players() {
        return players;
    }
}
