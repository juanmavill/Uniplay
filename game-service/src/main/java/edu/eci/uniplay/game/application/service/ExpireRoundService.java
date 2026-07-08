package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;

import edu.eci.uniplay.game.application.dto.ExpireRoundCommand;
import edu.eci.uniplay.game.application.dto.ExpireRoundResult;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.port.in.ExpireRoundUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;

public class ExpireRoundService implements ExpireRoundUseCase {

    private static final String TIMEOUT_REASON = "TIMEOUT";

    private final GameSessionRepository gameSessionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ExpireRoundService(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Override
    public ExpireRoundResult expireRound(ExpireRoundCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        RoundId roundId = new RoundId(command.roundId());
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        Instant expiredAt = Instant.now(clock);

        GameSession updatedSession = session.expireRound(roundId, expiredAt);
        Round round = updatedSession.round().orElseThrow();
        gameSessionRepository.save(updatedSession);
        domainEventPublisher.publishRoundFinished(new RoundFinishedEvent(
                roomCode.value(),
                round.id().value(),
                round.status().name(),
                TIMEOUT_REASON,
                round.finishedAt(),
                expiredAt
        ));

        return new ExpireRoundResult(
                roomCode.value(),
                round.id().value(),
                round.status().name(),
                TIMEOUT_REASON,
                round.finishedAt()
        );
    }
}
