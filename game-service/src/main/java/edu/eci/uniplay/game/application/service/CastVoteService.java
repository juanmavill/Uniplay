package edu.eci.uniplay.game.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.CastVoteCommand;
import edu.eci.uniplay.game.application.dto.CastVoteResult;
import edu.eci.uniplay.game.application.dto.VoteTallyResult;
import edu.eci.uniplay.game.application.event.VoteCastEvent;
import edu.eci.uniplay.game.application.port.in.CastVoteUseCase;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;
import edu.eci.uniplay.game.application.port.out.GameSessionRepository;
import edu.eci.uniplay.game.domain.model.GameSession;
import edu.eci.uniplay.game.domain.model.PlayerId;
import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.RoundId;
import edu.eci.uniplay.game.domain.model.VoteEvaluation;

public class CastVoteService implements CastVoteUseCase {

    private final GameSessionRepository gameSessionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public CastVoteService(
            GameSessionRepository gameSessionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.gameSessionRepository = gameSessionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Override
    public CastVoteResult castVote(CastVoteCommand command) {
        RoomCode roomCode = new RoomCode(command.roomCode());
        RoundId roundId = new RoundId(command.roundId());
        PlayerId voterId = new PlayerId(command.voterId());
        PlayerId candidateId = new PlayerId(command.candidateId());
        GameSession session = gameSessionRepository.findByRoomCode(roomCode)
                .orElseGet(() -> GameSession.newFor(roomCode));
        Instant votedAt = Instant.now(clock);

        VoteEvaluation evaluation = session.castVote(roundId, voterId, candidateId);
        gameSessionRepository.save(evaluation.session());

        var tallies = evaluation.tallies().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .map(entry -> new VoteTallyResult(entry.getKey().value(), entry.getValue()))
                .toList();

        domainEventPublisher.publishVoteCast(new VoteCastEvent(
                roomCode.value(),
                roundId.value(),
                voterId.value(),
                candidateId.value(),
                tallies.stream()
                        .map(tally -> new VoteCastEvent.VoteTallyPayload(tally.candidateId(), tally.votes()))
                        .toList(),
                votedAt
        ));

        return new CastVoteResult(
                roomCode.value(),
                roundId.value(),
                voterId.value(),
                candidateId.value(),
                tallies,
                votedAt
        );
    }
}
