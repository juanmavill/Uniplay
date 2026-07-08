package edu.eci.uniplay.game.application.port.in;

import edu.eci.uniplay.game.application.dto.CastVoteCommand;
import edu.eci.uniplay.game.application.dto.CastVoteResult;

public interface CastVoteUseCase {

    CastVoteResult castVote(CastVoteCommand command);
}
