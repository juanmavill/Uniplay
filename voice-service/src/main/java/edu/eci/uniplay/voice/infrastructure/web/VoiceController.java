package edu.eci.uniplay.voice.infrastructure.web;

import edu.eci.uniplay.voice.application.dto.GenerateVoiceTokenCommand;
import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
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

    public VoiceController(GenerateVoiceTokenUseCase generateVoiceTokenUseCase) {
        this.generateVoiceTokenUseCase = generateVoiceTokenUseCase;
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
}
