package edu.eci.uniplay.room.infrastructure.web;

import java.net.URI;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.JoinRoomCommand;
import edu.eci.uniplay.room.application.dto.JoinRoomResult;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;
import edu.eci.uniplay.room.application.port.in.CreateRoomUseCase;
import edu.eci.uniplay.room.application.port.in.JoinRoomUseCase;
import edu.eci.uniplay.room.infrastructure.web.dto.CreateRoomRequest;
import edu.eci.uniplay.room.infrastructure.web.dto.JoinRoomRequest;
import edu.eci.uniplay.room.infrastructure.web.dto.JoinRoomResponse;
import edu.eci.uniplay.room.infrastructure.web.dto.RoomResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/salas")
public class RoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final JoinRoomUseCase joinRoomUseCase;

    public RoomController(CreateRoomUseCase createRoomUseCase, JoinRoomUseCase joinRoomUseCase) {
        this.createRoomUseCase = createRoomUseCase;
        this.joinRoomUseCase = joinRoomUseCase;
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

    @PostMapping("/{code}/jugadores")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String code,
            @Valid @RequestBody JoinRoomRequest request
    ) {
        JoinRoomResult result = joinRoomUseCase.joinRoom(new JoinRoomCommand(code, request.playerName()));
        return ResponseEntity.ok(JoinRoomResponse.from(result));
    }
}
