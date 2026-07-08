package edu.eci.uniplay.game.application.port.out;

import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundFinishedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;

public interface DomainEventPublisher {

    void publishRoundStarted(RoundStartedEvent event);

    void publishRoundGuessed(RoundGuessedEvent event);

    void publishRoundFinished(RoundFinishedEvent event);
}
