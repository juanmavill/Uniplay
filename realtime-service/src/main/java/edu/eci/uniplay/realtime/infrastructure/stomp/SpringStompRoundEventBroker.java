package edu.eci.uniplay.realtime.infrastructure.stomp;

import edu.eci.uniplay.realtime.application.dto.RoundEventMessage;
import edu.eci.uniplay.realtime.application.port.out.RoundEventBroker;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpringStompRoundEventBroker implements RoundEventBroker {

    private static final String ROOM_ROUNDS_TOPIC_TEMPLATE = "/topic/rooms/%s/rounds";

    private final SimpMessagingTemplate messagingTemplate;

    public SpringStompRoundEventBroker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendRoundEvent(RoundEventMessage message) {
        messagingTemplate.convertAndSend(ROOM_ROUNDS_TOPIC_TEMPLATE.formatted(message.roomCode()), message);
    }
}
