package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.SecretWord;

public class StartRoundService implements StartRoundUseCase {

    private final GameSessionRepository gameSessionRepository;
    private final WordDeckProvider wordDeckProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public StartRoundService(
            GameSessionRepository gameSessionRepository,
            WordDeckProvider wordDeckProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.wordDeckProvider = wordDeckProvider;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Override
    public StartRoundResult startRound(StartRoundCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        Instant startedAt = Instant.now(clock);
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        SecretWord secretWord = wordDeckProvider.nextWord(roomCode);
        RoundId roundId = new RoundId(UUID.randomUUID());

        GameSession updatedSession = session.startRound(roundId, secretWord, startedAt);
        gameSessionRepository.save(updatedSession);
        domainEventPublisher.publishRoundStarted(new RoundStartedEvent(
                roomCode.value(),
                roundId.value(),
                secretWord.value(),
                startedAt
        ));

        Round round = updatedSession.round().orElseThrow();
        return new StartRoundResult(
                roomCode.value(),
                round.id().value(),
                round.secretWord().value(),
                round.status().name(),
                round.startedAt()
        );
    }
}
