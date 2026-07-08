package edu.eci.uniplay.voice.application.port.in;

import edu.eci.uniplay.voice.application.dto.ChangeSpeakingStateCommand;
import edu.eci.uniplay.voice.application.dto.SpeakingStateResult;

public interface ChangeSpeakingStateUseCase {

    SpeakingStateResult changeSpeakingState(ChangeSpeakingStateCommand command);
}
