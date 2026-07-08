package edu.eci.uniplay.voice.application.port.in;

import edu.eci.uniplay.voice.application.dto.GenerateVoiceTokenCommand;
import edu.eci.uniplay.voice.application.dto.VoiceTokenResult;

public interface GenerateVoiceTokenUseCase {

    VoiceTokenResult generateToken(GenerateVoiceTokenCommand command);
}
