package edu.eci.uniplay.game.application.port.out;

import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.event.VoteCastEvent;

public interface DomainEventPublisher {

    void publishRoundStarted(RoundStartedEvent event);

    void publishRoundGuessed(RoundGuessedEvent event);

    void publishRoundFinished(RoundFinishedEvent event);

    void publishVoteCast(VoteCastEvent event);
}
