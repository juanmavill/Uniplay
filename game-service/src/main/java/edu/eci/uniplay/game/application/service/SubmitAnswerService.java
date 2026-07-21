package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;

import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
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

    private static final String ALL_GUESSED_REASON = "ALL_GUESSED";

    private final GameSessionRepository gameSessionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final int pointsPerCorrectAnswer;
    private final int drawerMajorityBonus;
    private final Clock clock;

    public SubmitAnswerService(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            int pointsPerCorrectAnswer,
            int drawerMajorityBonus,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.pointsPerCorrectAnswer = pointsPerCorrectAnswer;
        this.drawerMajorityBonus = drawerMajorityBonus;
        this.clock = clock;
    }

    @Override
    public SubmitAnswerResult submitAnswer(SubmitAnswerCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        PlayerId playerId = new PlayerId(command.playerId());
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        Instant answeredAt = Instant.now(clock);
        int points = pointsFor(session, answeredAt);
        AnswerEvaluation evaluation = session.submitAnswer(
                playerId,
                command.answer(),
                points,
                drawerMajorityBonus,
                answeredAt
        );

        if (evaluation.newlyGuessed()) {
            gameSessionRepository.save(evaluation.session());
            domainEventPublisher.publishRoundGuessed(new RoundGuessedEvent(
                    roomCode.value(),
                    evaluation.roundId().value(),
                    playerId.value(),
                    evaluation.score(),
                    answeredAt
            ));
            if (evaluation.roundFinished()) {
                domainEventPublisher.publishRoundFinished(new RoundFinishedEvent(
                        roomCode.value(),
                        evaluation.roundId().value(),
                        RoundStatus.FINISHED.name(),
                        ALL_GUESSED_REASON,
                        answeredAt,
                        answeredAt
                ));
            }
        }

        String roundStatus = evaluation.session().round().orElseThrow().status().name();

        return new SubmitAnswerResult(
                roomCode.value(),
                evaluation.roundId().value(),
                playerId.value(),
                evaluation.correct(),
                evaluation.newlyGuessed(),
                evaluation.score(),
                evaluation.pointsAwarded(),
                evaluation.drawerBonusAwarded(),
                roundStatus,
                answeredAt
        );
    }

    private int pointsFor(GameSession session, Instant answeredAt) {
        var round = session.round().orElse(null);
        if (round == null) {
            return pointsPerCorrectAnswer;
        }
        long totalMillis = Duration.between(round.startedAt(), round.endsAt()).toMillis();
        long remainingMillis = Math.max(0, Duration.between(answeredAt, round.endsAt()).toMillis());
        double proportionalPoints = (double) pointsPerCorrectAnswer * remainingMillis / totalMillis;
        return Math.max(1, Math.min(pointsPerCorrectAnswer, (int) Math.ceil(proportionalPoints)));
    }
}
