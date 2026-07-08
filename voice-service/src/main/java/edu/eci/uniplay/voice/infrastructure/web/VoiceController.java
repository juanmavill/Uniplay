package edu.eci.uniplay.voice.infrastructure.web;

import edu.eci.uniplay.voice.application.dto.GenerateVoiceTokenCommand;
import edu.eci.uniplay.voice.application.dto.ChangeMuteStateCommand;
import edu.eci.uniplay.voice.application.dto.ChangeSpeakingStateCommand;
import edu.eci.uniplay.voice.application.port.in.ChangeMuteStateUseCase;
import edu.eci.uniplay.voice.application.port.in.ChangeSpeakingStateUseCase;
import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
import edu.eci.uniplay.voice.infrastructure.web.dto.MuteStateRequest;
import edu.eci.uniplay.voice.infrastructure.web.dto.MuteStateResponse;
import edu.eci.uniplay.voice.infrastructure.web.dto.SpeakingStateRequest;
import edu.eci.uniplay.voice.infrastructure.web.dto.SpeakingStateResponse;
import edu.eci.uniplay.voice.infrastructure.web.dto.VoiceTokenRequest;
import edu.eci.uniplay.voice.infrastructure.web.dto.VoiceTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voice")
public class VoiceController {

    private final GenerateVoiceTokenUseCase generateVoiceTokenUseCase;
    private final ChangeMuteStateUseCase changeMuteStateUseCase;
    private final ChangeSpeakingStateUseCase changeSpeakingStateUseCase;

    public VoiceController(
            GenerateVoiceTokenUseCase generateVoiceTokenUseCase,
            ChangeMuteStateUseCase changeMuteStateUseCase,
            ChangeSpeakingStateUseCase changeSpeakingStateUseCase
    ) {
        this.generateVoiceTokenUseCase = generateVoiceTokenUseCase;
        this.changeMuteStateUseCase = changeMuteStateUseCase;
        this.changeSpeakingStateUseCase = changeSpeakingStateUseCase;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.CREATED)
    VoiceTokenResponse generateToken(@Valid @RequestBody VoiceTokenRequest request) {
        return VoiceTokenResponse.from(generateVoiceTokenUseCase.generateToken(new GenerateVoiceTokenCommand(
                request.roomCode(),
                request.playerId(),
                request.playerName()
        )));
    }

    @PostMapping("/mute")
    MuteStateResponse changeMuteState(@Valid @RequestBody MuteStateRequest request) {
        return MuteStateResponse.from(changeMuteStateUseCase.changeMuteState(new ChangeMuteStateCommand(
                request.roomCode(),
                request.playerId(),
                request.muted()
        )));
    }

    @PostMapping("/speaking")
    SpeakingStateResponse changeSpeakingState(@Valid @RequestBody SpeakingStateRequest request) {
        return SpeakingStateResponse.from(changeSpeakingStateUseCase.changeSpeakingState(new ChangeSpeakingStateCommand(
                request.roomCode(),
                request.playerId(),
                request.speaking()
        )));
    }
}
