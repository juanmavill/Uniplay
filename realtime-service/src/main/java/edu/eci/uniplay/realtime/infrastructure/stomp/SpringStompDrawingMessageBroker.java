package edu.eci.uniplay.realtime.infrastructure.stomp;

import edu.eci.uniplay.realtime.application.dto.DrawingDeltaMessage;
import edu.eci.uniplay.realtime.application.port.out.DrawingMessageBroker;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpringStompDrawingMessageBroker implements DrawingMessageBroker {

    private static final String ROOM_DRAW_TOPIC_TEMPLATE = "/topic/rooms/%s/draw";

    private final SimpMessagingTemplate messagingTemplate;

    public SpringStompDrawingMessageBroker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendDrawingDelta(DrawingDeltaMessage message) {
        messagingTemplate.convertAndSend(ROOM_DRAW_TOPIC_TEMPLATE.formatted(message.roomCode()), message);
    }
}
