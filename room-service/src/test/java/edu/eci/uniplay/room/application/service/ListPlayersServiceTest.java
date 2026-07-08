package edu.eci.uniplay.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import edu.eci.uniplay.room.application.dto.ListPlayersCommand;
import edu.eci.uniplay.room.application.dto.ListPlayersResult;
import edu.eci.uniplay.room.application.exception.RoomNotFoundException;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Player;
import edu.eci.uniplay.room.domain.model.PlayerId;
import edu.eci.uniplay.room.domain.model.PlayerName;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import edu.eci.uniplay.room.domain.model.RoomId;
import edu.eci.uniplay.room.domain.model.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPlayersServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private ListPlayersService listPlayersService;

    @BeforeEach
    void setUp() {
        listPlayersService = new ListPlayersService(roomRepository);
    }

    @Test
    void listsPlayersInRoom() {
        Room room = Room.restore(
                RoomId.newId(),
                new RoomCode("ABC123"),
                RoomStatus.WAITING_FOR_PLAYERS,
                21,
                Instant.parse("2026-07-07T12:00:00Z"),
                List.of(new Player(PlayerId.newId(), new PlayerName("Ana")))
        );
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.of(room));

        ListPlayersResult result = listPlayersService.listPlayers(new ListPlayersCommand("ABC123"));

        assertThat(result.roomId()).isEqualTo(room.id().value());
        assertThat(result.code()).isEqualTo("ABC123");
        assertThat(result.players()).hasSize(1);
        assertThat(result.players().getFirst().playerName()).isEqualTo("Ana");
    }

    @Test
    void failsWhenRoomDoesNotExist() {
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listPlayersService.listPlayers(new ListPlayersCommand("ABC123")))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("room not found: ABC123");
    }
}
