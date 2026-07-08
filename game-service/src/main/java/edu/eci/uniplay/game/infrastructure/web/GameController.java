package edu.eci.uniplay.game.infrastructure.web;

import java.net.URI;

import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.infrastructure.web.dto.GameStateResponse;
import edu.eci.uniplay.game.infrastructure.web.dto.StartRoundResponse;
import edu.eci.uniplay.game.infrastructure.web.dto.SubmitAnswerRequest;
import edu.eci.uniplay.game.infrastructure.web.dto.SubmitAnswerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{roomCode}")
public class GameController {

    private final StartRoundUseCase startRoundUseCase;
    private final SubmitAnswerUseCase submitAnswerUseCase;
    private final GetGameStateUseCase getGameStateUseCase;

    public GameController(
            StartRoundUseCase startRoundUseCase,
            SubmitAnswerUseCase submitAnswerUseCase,
            GetGameStateUseCase getGameStateUseCase
    ) {
        this.startRoundUseCase = startRoundUseCase;
        this.submitAnswerUseCase = submitAnswerUseCase;
        this.getGameStateUseCase = getGameStateUseCase;
    }

    @PostMapping("/rounds")
    ResponseEntity<StartRoundResponse> startRound(@PathVariable String roomCode) {
        StartRoundResult result = startRoundUseCase.startRound(new StartRoundCommand(roomCode));
        return ResponseEntity
                .created(URI.create("/games/" + result.roomCode()))
                .body(StartRoundResponse.from(result));
    }

    @PostMapping("/answers")
    SubmitAnswerResponse submitAnswer(
            @PathVariable String roomCode,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        SubmitAnswerResult result = submitAnswerUseCase.submitAnswer(new SubmitAnswerCommand(
                roomCode,
                request.playerId(),
                request.answer()
        ));
        return SubmitAnswerResponse.from(result);
    }

    @GetMapping
    GameStateResponse getState(@PathVariable String roomCode) {
        GameStateResult result = getGameStateUseCase.getState(roomCode);
        return GameStateResponse.from(result);
    }
}
