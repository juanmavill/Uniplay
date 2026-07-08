package edu.eci.uniplay.room.application.service;

import edu.eci.uniplay.room.application.dto.ListPlayersCommand;
import edu.eci.uniplay.room.application.dto.ListPlayersResult;
import edu.eci.uniplay.room.application.dto.PlayerResult;
import edu.eci.uniplay.room.application.exception.RoomNotFoundException;
import edu.eci.uniplay.room.application.port.in.ListPlayersUseCase;
import edu.eci.uniplay.room.application.port.out.RoomRepository;
import edu.eci.uniplay.room.domain.model.Room;
import edu.eci.uniplay.room.domain.model.RoomCode;

public class ListPlayersService implements ListPlayersUseCase {

    private final RoomRepository roomRepository;

    public ListPlayersService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public ListPlayersResult listPlayers(ListPlayersCommand command) {
        RoomCode code = new RoomCode(command.roomCode());
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RoomNotFoundException(code.value()));

        return new ListPlayersResult(
                room.id().value(),
                room.code().value(),
                room.players().stream().map(PlayerResult::from).toList()
        );
    }
}
