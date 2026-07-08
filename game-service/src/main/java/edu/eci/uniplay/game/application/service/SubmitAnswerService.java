package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;

import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.AnswerEvaluation;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundStatus;

public class SubmitAnswerService implements SubmitAnswerUseCase {

    private final GameSessionRepository gameSessionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final int pointsPerCorrectAnswer;
    private final Clock clock;

    public SubmitAnswerService(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            int pointsPerCorrectAnswer,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.pointsPerCorrectAnswer = pointsPerCorrectAnswer;
        this.clock = clock;
    }

    @Override
    public SubmitAnswerResult submitAnswer(SubmitAnswerCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        PlayerId playerId = new PlayerId(command.playerId());
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        Instant answeredAt = Instant.now(clock);
        AnswerEvaluation evaluation = session.submitAnswer(
                playerId,
                command.answer(),
                pointsPerCorrectAnswer,
                answeredAt
        );

        if (evaluation.correct()) {
            gameSessionRepository.save(evaluation.session());
            domainEventPublisher.publishRoundGuessed(new RoundGuessedEvent(
                    roomCode.value(),
                    evaluation.roundId().value(),
                    playerId.value(),
                    evaluation.score(),
                    answeredAt
            ));
        }

        return new SubmitAnswerResult(
                roomCode.value(),
                evaluation.roundId().value(),
                playerId.value(),
                evaluation.correct(),
                evaluation.score(),
                evaluation.correct() ? RoundStatus.FINISHED.name() : RoundStatus.ACTIVE.name(),
                answeredAt
        );
    }
}
