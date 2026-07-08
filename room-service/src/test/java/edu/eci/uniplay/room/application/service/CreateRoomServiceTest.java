package edu.eci.uniplay.room.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;
import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import edu.eci.uniplay.room.application.exception.RoomCodeGenerationException;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.room.application.port.out.RoomCodeGenerator;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRoomServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-07T12:00:00Z");

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomCodeGenerator roomCodeGenerator;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private CreateRoomService createRoomService;

    @BeforeEach
    void setUp() {
        RoomCreationPolicy policy = new RoomCreationPolicy(21, 3);
        Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
        createRoomService = new CreateRoomService(
                roomRepository,
                roomCodeGenerator,
                domainEventPublisher,
                policy,
                clock
        );
    }

    @Test
    void createsRoomWithGeneratedUniqueCode() {
        RoomCode code = new RoomCode("ABC123");
        when(roomCodeGenerator.generate()).thenReturn(code);
        when(roomRepository.saveIfCodeAvailable(any(Room.class))).thenReturn(true);

        RoomCreatedResult result = createRoomService.createRoom(new CreateRoomCommand(null));

        assertThat(result.code()).isEqualTo("ABC123");
        assertThat(result.status()).isEqualTo("WAITING_FOR_PLAYERS");
        assertThat(result.maxPlayers()).isEqualTo(21);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.roomId()).isNotNull();

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).saveIfCodeAvailable(roomCaptor.capture());
        assertThat(roomCaptor.getValue().code()).isEqualTo(code);

        ArgumentCaptor<RoomCreatedEvent> eventCaptor = ArgumentCaptor.forClass(RoomCreatedEvent.class);
        verify(domainEventPublisher).publishRoomCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(result.roomId());
        assertThat(eventCaptor.getValue().code()).isEqualTo("ABC123");
        assertThat(eventCaptor.getValue().occurredAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void retriesWhenGeneratedCodeAlreadyExists() {
        RoomCode duplicatedCode = new RoomCode("ABC123");
        RoomCode uniqueCode = new RoomCode("XYZ789");
        when(roomCodeGenerator.generate()).thenReturn(duplicatedCode, uniqueCode);
        when(roomRepository.saveIfCodeAvailable(any(Room.class))).thenReturn(false, true);

        RoomCreatedResult result = createRoomService.createRoom(new CreateRoomCommand(8));

        assertThat(result.code()).isEqualTo("XYZ789");
        assertThat(result.maxPlayers()).isEqualTo(8);
        verify(roomRepository, times(2)).saveIfCodeAvailable(any(Room.class));
    }

    @Test
    void failsWhenUniqueCodeCannotBeGenerated() {
        RoomCode duplicatedCode = new RoomCode("ABC123");
        when(roomCodeGenerator.generate()).thenReturn(duplicatedCode);
        when(roomRepository.saveIfCodeAvailable(any(Room.class))).thenReturn(false);

        assertThatThrownBy(() -> createRoomService.createRoom(new CreateRoomCommand(null)))
                .isInstanceOf(RoomCodeGenerationException.class)
                .hasMessage("could not generate a unique room code after 3 attempts");

        verify(domainEventPublisher, never()).publishRoomCreated(any(RoomCreatedEvent.class));
    }
}
