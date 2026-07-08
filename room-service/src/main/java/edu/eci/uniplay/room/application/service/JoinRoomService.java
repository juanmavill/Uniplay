package edu.eci.uniplay.room.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import edu.eci.uniplay.room.application.dto.JoinRoomCommand;
import edu.eci.uniplay.room.application.dto.JoinRoomResult;
import edu.eci.uniplay.room.application.dto.PlayerResult;
import edu.eci.uniplay.room.application.event.PlayerJoinedEvent;
import edu.eci.uniplay.room.application.exception.RoomNotFoundException;
import edu.eci.uniplay.room.application.port.in.JoinRoomUseCase;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Player;
import edu.eci.uniplay.room.domain.model.PlayerId;
import edu.eci.uniplay.room.domain.model.PlayerName;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinRoomService implements JoinRoomUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(JoinRoomService.class);

    private final RoomRepository roomRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public JoinRoomService(RoomRepository roomRepository, DomainEventPublisher domainEventPublisher, Clock clock) {
        this.roomRepository = roomRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Override
    public JoinRoomResult joinRoom(JoinRoomCommand command) {
        RoomCode code = new RoomCode(command.roomCode());
        Player player = new Player(PlayerId.newId(), new PlayerName(command.playerName()));
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RoomNotFoundException(code.value()));
        Room updatedRoom = room.join(player);
        Instant joinedAt = clock.instant();

        roomRepository.save(updatedRoom);
        domainEventPublisher.publishPlayerJoined(new PlayerJoinedEvent(
                updatedRoom.id().value(),
                updatedRoom.code().value(),
                player.id().value(),
                player.name().value(),
                joinedAt
        ));
        LOGGER.info(Markers.appendEntries(Map.of(
                "salaId", updatedRoom.id().value().toString(),
                "evento", "jugador.conectado",
                "timestamp", joinedAt.toString()
        )), "player joined room");

        return new JoinRoomResult(
                updatedRoom.id().value(),
                updatedRoom.code().value(),
                player.id().value(),
                player.name().value(),
                updatedRoom.players().stream().map(PlayerResult::from).toList(),
                joinedAt
        );
    }
}
