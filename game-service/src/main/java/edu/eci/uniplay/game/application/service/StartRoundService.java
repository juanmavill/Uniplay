package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.application.port.out.WordDeckProvider;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundMode;
import edu.eci.uniplay.game.domain.model.Round;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.SecretWord;

public class StartRoundService implements StartRoundUseCase {

    private static final String DEFAULT_DECK = "GENERAL";

    private final GameSessionRepository gameSessionRepository;
    private final WordDeckProvider wordDeckProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Duration roundDuration;
    private final Clock clock;

    public StartRoundService(
            GameSessionRepository gameSessionRepository,
            WordDeckProvider wordDeckProvider,
            DomainEventPublisher domainEventPublisher,
            Duration roundDuration,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.wordDeckProvider = wordDeckProvider;
        this.domainEventPublisher = domainEventPublisher;
        this.roundDuration = roundDuration;
        this.clock = clock;
    }

    @Override
    public StartRoundResult startRound(StartRoundCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        RoundMode mode = command.mode() == null || command.mode().isBlank()
                ? RoundMode.CLASSIC
                : RoundMode.valueOf(command.mode().trim().toUpperCase());
        Instant startedAt = Instant.now(clock);
        Instant endsAt = startedAt.plus(roundDuration);
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        String deck = selectedDeck(command.deck());
        SecretWord secretWord = wordDeckProvider.nextWord(roomCode, deck, command.customWords());
        RoundId roundId = new RoundId(UUID.randomUUID());
        PlayerId drawerId = command.drawerId() == null ? null : new PlayerId(command.drawerId());

        GameSession updatedSession = session.startRound(roundId, secretWord, mode, drawerId, startedAt, endsAt);
        gameSessionRepository.save(updatedSession);
        domainEventPublisher.publishRoundStarted(new RoundStartedEvent(
                roomCode.value(),
                roundId.value(),
                secretWord.value(),
                mode.name(),
                deck,
                startedAt,
                endsAt,
                startedAt
        ));

        Round round = updatedSession.round().orElseThrow();
        return new StartRoundResult(
                roomCode.value(),
                round.id().value(),
                round.secretWord().value(),
                round.drawerId() == null ? null : round.drawerId().value(),
                round.mode().name(),
                deck,
                round.status().name(),
                round.startedAt(),
                round.endsAt()
        );
    }

    private String selectedDeck(String deck) {
        if (deck == null || deck.isBlank()) {
            return DEFAULT_DECK;
        }
        return deck.trim().toUpperCase(Locale.ROOT);
    }
}
