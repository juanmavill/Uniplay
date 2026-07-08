package edu.eci.uniplay.metrics.infrastructure.redis;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.metrics.application.port.in.RecordDomainEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisDomainEventSubscriberTest {

    private final RecordDomainEventUseCase useCase = mock(RecordDomainEventUseCase.class);
    private final RedisDomainEventSubscriber subscriber = new RedisDomainEventSubscriber(new ObjectMapper(), useCase);

    @Test
    void recordsRoomCreatedEvent() {
        subscriber.onMessage(message(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, """
                {
                  "code": "ABC123"
                }
                """), null);

        verify(useCase).recordRoomCreated("ABC123");
    }

    @Test
    void recordsPlayerConnectedEvent() {
        subscriber.onMessage(message(RedisDomainEventSubscriber.PLAYER_CONNECTED_CHANNEL, """
                {
                  "code": "ABC123",
                  "playerId": "22222222-2222-2222-2222-222222222222"
                }
                """), null);

        verify(useCase).recordPlayerConnected("ABC123", "22222222-2222-2222-2222-222222222222");
    }

    @Test
    void recordsRoundStartedEvent() {
        subscriber.onMessage(message(RedisDomainEventSubscriber.ROUND_STARTED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "roundId": "11111111-1111-1111-1111-111111111111"
                }
                """), null);

        verify(useCase).recordRoundStarted("ABC123", "11111111-1111-1111-1111-111111111111");
    }

    @Test
    void recordsWordGuessedEvent() {
        subscriber.onMessage(message(RedisDomainEventSubscriber.WORD_GUESSED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "roundId": "11111111-1111-1111-1111-111111111111"
                }
                """), null);

        verify(useCase).recordWordGuessed("ABC123", "11111111-1111-1111-1111-111111111111");
    }

    private DefaultMessage message(String channel, String body) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }
}
