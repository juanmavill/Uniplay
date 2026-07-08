package edu.eci.uniplay.realtime.infrastructure.stomp;

import edu.eci.uniplay.realtime.application.dto.VoiceEventMessage;
import edu.eci.uniplay.realtime.application.port.out.VoiceEventBroker;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpringStompVoiceEventBroker implements VoiceEventBroker {

    private static final String ROOM_VOICE_TOPIC_TEMPLATE = "/topic/rooms/%s/voice";

    private final SimpMessagingTemplate messagingTemplate;

    public SpringStompVoiceEventBroker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendVoiceEvent(VoiceEventMessage message) {
        messagingTemplate.convertAndSend(ROOM_VOICE_TOPIC_TEMPLATE.formatted(message.roomCode()), message);
    }
}
