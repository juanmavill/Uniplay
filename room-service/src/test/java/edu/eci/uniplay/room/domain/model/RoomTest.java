package edu.eci.uniplay.room.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

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
    }

    @Test
    void rejectsRoomsWithLessThanTwoPlayers() {
        assertThatThrownBy(() -> Room.create(RoomId.newId(), new RoomCode("ABC123"), 1, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPlayers must be at least 2");
    }
}
