package edu.eci.uniplay.game.infrastructure.web;

import java.net.URI;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.CastVoteCommand;
import edu.eci.uniplay.game.application.dto.CastVoteResult;
import edu.eci.uniplay.game.application.dto.ExpireRoundCommand;
import edu.eci.uniplay.game.application.dto.ExpireRoundResult;
import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.port.in.CastVoteUseCase;
import edu.eci.uniplay.game.application.port.in.ExpireRoundUseCase;
import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.infrastructure.web.dto.CastVoteRequest;
import edu.eci.uniplay.game.infrastructure.web.dto.CastVoteResponse;
import edu.eci.uniplay.game.infrastructure.web.dto.GameStateResponse;
import edu.eci.uniplay.game.infrastructure.web.dto.ExpireRoundResponse;
import edu.eci.uniplay.game.infrastructure.web.dto.StartRoundRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{roomCode}")
public class GameController {

    private final StartRoundUseCase startRoundUseCase;
    private final SubmitAnswerUseCase submitAnswerUseCase;
    private final GetGameStateUseCase getGameStateUseCase;
    private final ExpireRoundUseCase expireRoundUseCase;
    private final CastVoteUseCase castVoteUseCase;

    public GameController(
            StartRoundUseCase startRoundUseCase,
            SubmitAnswerUseCase submitAnswerUseCase,
            GetGameStateUseCase getGameStateUseCase,
            ExpireRoundUseCase expireRoundUseCase,
            CastVoteUseCase castVoteUseCase
    ) {
        this.startRoundUseCase = startRoundUseCase;
        this.submitAnswerUseCase = submitAnswerUseCase;
        this.getGameStateUseCase = getGameStateUseCase;
        this.expireRoundUseCase = expireRoundUseCase;
        this.castVoteUseCase = castVoteUseCase;
    }

    @PostMapping("/rounds")
    ResponseEntity<StartRoundResponse> startRound(
            @PathVariable String roomCode,
            @RequestBody(required = false) StartRoundRequest request
    ) {
        StartRoundResult result = startRoundUseCase.startRound(new StartRoundCommand(
                roomCode,
                request == null ? null : request.mode(),
                request == null ? null : request.deck(),
                request == null ? null : request.drawerId(),
                request == null ? null : request.customWords()
        ));
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

    @PostMapping("/rounds/{roundId}/timeout")
    ExpireRoundResponse expireRound(@PathVariable String roomCode, @PathVariable UUID roundId) {
        ExpireRoundResult result = expireRoundUseCase.expireRound(new ExpireRoundCommand(roomCode, roundId));
        return ExpireRoundResponse.from(result);
    }

    @PostMapping("/rounds/{roundId}/votes")
    CastVoteResponse castVote(
            @PathVariable String roomCode,
            @PathVariable UUID roundId,
            @Valid @RequestBody CastVoteRequest request
    ) {
        CastVoteResult result = castVoteUseCase.castVote(new CastVoteCommand(
                roomCode,
                roundId,
                request.voterId(),
                request.candidateId()
        ));
        return CastVoteResponse.from(result);
    }

    @GetMapping
    GameStateResponse getState(
            @PathVariable String roomCode,
            @RequestParam(required = false) UUID viewerPlayerId
    ) {
        GameStateResult result = getGameStateUseCase.getState(roomCode, viewerPlayerId);
        return GameStateResponse.from(result);
    }
}
