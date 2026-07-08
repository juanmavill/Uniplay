package edu.eci.uniplay.room.infrastructure.web;

import java.net.URI;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;
import edu.eci.uniplay.room.application.port.in.CreateRoomUseCase;
import edu.eci.uniplay.room.infrastructure.web.dto.CreateRoomRequest;
import edu.eci.uniplay.room.infrastructure.web.dto.RoomResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/salas")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;

    public RoomController(CreateRoomUseCase createRoomUseCase) {
        this.createRoomUseCase = createRoomUseCase;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomCreatedResult result = createRoomUseCase.createRoom(new CreateRoomCommand(request.maxPlayers()));
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(result.code())
                .toUri();

        return ResponseEntity.created(location).body(RoomResponse.from(result));
    }
}
