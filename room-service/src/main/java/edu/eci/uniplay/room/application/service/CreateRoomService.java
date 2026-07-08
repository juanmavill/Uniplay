package edu.eci.uniplay.room.application.service;

import java.time.Clock;
import java.util.Map;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;
import edu.eci.uniplay.room.application.event.RoomCreatedEvent;
import edu.eci.uniplay.room.application.exception.RoomCodeGenerationException;
import edu.eci.uniplay.room.application.port.in.CreateRoomUseCase;
import edu.eci.uniplay.room.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.room.application.port.out.RoomCodeGenerator;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;
import edu.eci.uniplay.room.domain.model.RoomId;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRoomService implements CreateRoomUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateRoomService.class);

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;
    private final DomainEventPublisher domainEventPublisher;
    private final RoomCreationPolicy roomCreationPolicy;
    private final Clock clock;

    public CreateRoomService(
            RoomRepository roomRepository,
            RoomCodeGenerator roomCodeGenerator,
            DomainEventPublisher domainEventPublisher,
            RoomCreationPolicy roomCreationPolicy,
            Clock clock
    ) {
        this.roomRepository = roomRepository;
        this.roomCodeGenerator = roomCodeGenerator;
        this.domainEventPublisher = domainEventPublisher;
        this.roomCreationPolicy = roomCreationPolicy;
        this.clock = clock;
    }

    @Override
    public RoomCreatedResult createRoom(CreateRoomCommand command) {
        int maxPlayers = roomCreationPolicy.resolveMaxPlayers(command.maxPlayers());
        Room room = createRoomWithUniqueCode(maxPlayers);

        domainEventPublisher.publishRoomCreated(new RoomCreatedEvent(
                room.id().value(),
                room.code().value(),
                room.maxPlayers(),
                room.createdAt()
        ));
        LOGGER.info(Markers.appendEntries(Map.of(
                "salaId", room.id().value().toString(),
                "evento", "sala.creada",
                "timestamp", room.createdAt().toString()
        )), "room created");

        return new RoomCreatedResult(
                room.id().value(),
                room.code().value(),
                room.status().name(),
                room.maxPlayers(),
                room.createdAt()
        );
    }

    private Room createRoomWithUniqueCode(int maxPlayers) {
        int maxAttempts = roomCreationPolicy.maxCodeGenerationAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            RoomCode candidate = roomCodeGenerator.generate();
            Room room = Room.create(RoomId.newId(), candidate, maxPlayers, clock.instant());
            if (roomRepository.saveIfCodeAvailable(room)) {
                return room;
            }
        }

        throw new RoomCodeGenerationException(maxAttempts);
    }
}
