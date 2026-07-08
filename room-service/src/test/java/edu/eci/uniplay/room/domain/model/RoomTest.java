package edu.eci.uniplay.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void createsRoomWaitingForPlayers() {
        RoomId id = RoomId.newId();
        RoomCode code = new RoomCode("ABC123");
        Instant createdAt = Instant.parse("2026-07-07T12:00:00Z");

        Room room = Room.create(id, code, 21, createdAt);

        assertThat(room.id()).isEqualTo(id);
        assertThat(room.code()).isEqualTo(code);
        assertThat(room.status()).isEqualTo(RoomStatus.WAITING_FOR_PLAYERS);
        assertThat(room.maxPlayers()).isEqualTo(21);
        assertThat(room.createdAt()).isEqualTo(createdAt);
        assertThat(room.players()).isEmpty();
    }

    @Test
    void rejectsRoomsWithLessThanTwoPlayers() {
        assertThatThrownBy(() -> Room.create(RoomId.newId(), new RoomCode("ABC123"), 1, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPlayers must be at least 2");
    }

    @Test
    void joinsPlayerToWaitingRoom() {
        Room room = Room.create(RoomId.newId(), new RoomCode("ABC123"), 2, Instant.now());
        Player player = new Player(PlayerId.newId(), new PlayerName("Ana"));

        Room updatedRoom = room.join(player);

        assertThat(updatedRoom.players()).containsExactly(player);
        assertThat(room.players()).isEmpty();
    }

    @Test
    void rejectsDuplicatePlayerNameIgnoringCase() {
        Player firstPlayer = new Player(PlayerId.newId(), new PlayerName("Ana"));
        Room room = Room.restore(
                RoomId.newId(),
                new RoomCode("ABC123"),
                RoomStatus.WAITING_FOR_PLAYERS,
                2,
                Instant.now(),
                List.of(firstPlayer)
        );

        assertThatThrownBy(() -> room.join(new Player(PlayerId.newId(), new PlayerName("ana"))))
                .isInstanceOf(DuplicatePlayerException.class)
                .hasMessage("player already joined room: ana");
    }

    @Test
    void rejectsJoinWhenRoomIsFull() {
        Room room = Room.restore(
                RoomId.newId(),
                new RoomCode("ABC123"),
                RoomStatus.WAITING_FOR_PLAYERS,
                2,
                Instant.now(),
                List.of(
                        new Player(PlayerId.newId(), new PlayerName("Ana")),
                        new Player(PlayerId.newId(), new PlayerName("Luis"))
                )
        );

        assertThatThrownBy(() -> room.join(new Player(PlayerId.newId(), new PlayerName("Mia"))))
                .isInstanceOf(RoomFullException.class)
                .hasMessage("room is full: ABC123");
    }
}
