package edu.eci.uniplay.voice.application.port.in;

import edu.eci.uniplay.voice.application.dto.ChangeMuteStateCommand;
import edu.eci.uniplay.voice.application.dto.MuteStateResult;

public interface ChangeMuteStateUseCase {

    MuteStateResult changeMuteState(ChangeMuteStateCommand command);
}
