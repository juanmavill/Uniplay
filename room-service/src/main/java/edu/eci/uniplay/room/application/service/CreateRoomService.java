package edu.eci.uniplay.room.application.service;

import java.time.Clock;
import java.time.Instant;

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

public class CreateRoomService implements CreateRoomUseCase {

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
