package edu.eci.uniplay.game.application.port.in;

import edu.eci.uniplay.game.application.dto.ExpireRoundCommand;
import edu.eci.uniplay.game.application.dto.ExpireRoundResult;

public interface ExpireRoundUseCase {

    ExpireRoundResult expireRound(ExpireRoundCommand command);
}
