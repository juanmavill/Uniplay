package edu.eci.uniplay.game.infrastructure.memory;

import edu.eci.uniplay.game.application.event.RoundGuessedEvent;
import edu.eci.uniplay.game.application.event.RoundStartedEvent;
import edu.eci.uniplay.game.application.port.out.DomainEventPublisher;

public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publishRoundStarted(RoundStartedEvent event) {
    }

    @Override
    public void publishRoundGuessed(RoundGuessedEvent event) {
    }
}
