package edu.eci.uniplay.game.application.port.in;

import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;

public interface SubmitAnswerUseCase {

    SubmitAnswerResult submitAnswer(SubmitAnswerCommand command);
}
