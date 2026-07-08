package edu.eci.uniplay.game.application.port.in;

import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;

public interface StartRoundUseCase {

    StartRoundResult startRound(StartRoundCommand command);
}
