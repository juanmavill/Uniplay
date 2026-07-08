package edu.eci.uniplay.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import edu.eci.uniplay.room.application.dto.JoinRoomCommand;
import edu.eci.uniplay.room.application.dto.JoinRoomResult;
import edu.eci.uniplay.room.application.event.PlayerJoinedEvent;
import edu.eci.uniplay.room.application.exception.RoomNotFoundException;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.DuplicatePlayerException;
import edu.eci.uniplay.room.domain.model.Player;
import edu.eci.uniplay.room.domain.model.PlayerId;
import edu.eci.uniplay.room.domain.model.PlayerName;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import edu.eci.uniplay.room.domain.model.RoomFullException;
import edu.eci.uniplay.room.domain.model.RoomId;
import edu.eci.uniplay.room.domain.model.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JoinRoomServiceTest {

    private static final Instant JOINED_AT = Instant.parse("2026-07-07T12:30:00Z");

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private JoinRoomService joinRoomService;

    @BeforeEach
    void setUp() {
        joinRoomService = new JoinRoomService(
                roomRepository,
                domainEventPublisher,
                Clock.fixed(JOINED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void joinsPlayerToExistingRoom() {
        Room room = Room.create(RoomId.newId(), new RoomCode("ABC123"), 2, Instant.parse("2026-07-07T12:00:00Z"));
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.of(room));

        JoinRoomResult result = joinRoomService.joinRoom(new JoinRoomCommand("ABC123", "Ana"));

        assertThat(result.code()).isEqualTo("ABC123");
        assertThat(result.playerName()).isEqualTo("Ana");
        assertThat(result.playerId()).isNotNull();
        assertThat(result.players()).hasSize(1);
        assertThat(result.joinedAt()).isEqualTo(JOINED_AT);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().players()).hasSize(1);

        ArgumentCaptor<PlayerJoinedEvent> eventCaptor = ArgumentCaptor.forClass(PlayerJoinedEvent.class);
        verify(domainEventPublisher).publishPlayerJoined(eventCaptor.capture());
        assertThat(eventCaptor.getValue().code()).isEqualTo("ABC123");
        assertThat(eventCaptor.getValue().playerName()).isEqualTo("Ana");
        assertThat(eventCaptor.getValue().occurredAt()).isEqualTo(JOINED_AT);
    }

    @Test
    void failsWhenRoomDoesNotExist() {
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinRoomService.joinRoom(new JoinRoomCommand("ABC123", "Ana")))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("room not found: ABC123");

        verify(roomRepository, never()).save(any(Room.class));
        verify(domainEventPublisher, never()).publishPlayerJoined(any(PlayerJoinedEvent.class));
    }

    @Test
    void failsWhenPlayerNameIsDuplicated() {
        Room room = Room.restore(
                RoomId.newId(),
                new RoomCode("ABC123"),
                RoomStatus.WAITING_FOR_PLAYERS,
                2,
                Instant.parse("2026-07-07T12:00:00Z"),
                List.of(new Player(PlayerId.newId(), new PlayerName("Ana")))
        );
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> joinRoomService.joinRoom(new JoinRoomCommand("ABC123", "ana")))
                .isInstanceOf(DuplicatePlayerException.class);

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void failsWhenRoomIsFull() {
        Room room = Room.restore(
                RoomId.newId(),
                new RoomCode("ABC123"),
                RoomStatus.WAITING_FOR_PLAYERS,
                2,
                Instant.parse("2026-07-07T12:00:00Z"),
                List.of(
                        new Player(PlayerId.newId(), new PlayerName("Ana")),
                        new Player(PlayerId.newId(), new PlayerName("Luis"))
                )
        );
        when(roomRepository.findByCode(new RoomCode("ABC123"))).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> joinRoomService.joinRoom(new JoinRoomCommand("ABC123", "Mia")))
                .isInstanceOf(RoomFullException.class)
                .hasMessage("room is full: ABC123");

        verify(roomRepository, never()).save(any(Room.class));
    }
}
