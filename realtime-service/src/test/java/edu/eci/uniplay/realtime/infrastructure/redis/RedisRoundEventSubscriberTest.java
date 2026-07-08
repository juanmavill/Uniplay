package edu.eci.uniplay.realtime.infrastructure.redis;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.realtime.application.dto.RoundEventMessage;
import edu.eci.uniplay.realtime.application.port.out.RoundEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisRoundEventSubscriberTest {

    @Mock
    private RoundEventBroker roundEventBroker;

    @Test
    void forwardsRoundStartedEvent() {
        RedisRoundEventSubscriber subscriber = new RedisRoundEventSubscriber(new ObjectMapper(), roundEventBroker);

        subscriber.onMessage(message(RedisRoundEventSubscriber.ROUND_STARTED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "roundId": "11111111-1111-1111-1111-111111111111",
                  "word": "Campus",
                  "startedAt": "2026-07-07T12:00:00Z",
                  "endsAt": "2026-07-07T12:01:00Z",
                  "occurredAt": "2026-07-07T12:00:00Z"
                }
                """), null);

        ArgumentCaptor<RoundEventMessage> captor = ArgumentCaptor.forClass(RoundEventMessage.class);
        verify(roundEventBroker).sendRoundEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ROUND_STARTED");
        assertThat(captor.getValue().roomCode()).isEqualTo("ABC123");
        assertThat(captor.getValue().endsAt()).isEqualTo(Instant.parse("2026-07-07T12:01:00Z"));
    }

    @Test
    void forwardsRoundFinishedEvent() {
        RedisRoundEventSubscriber subscriber = new RedisRoundEventSubscriber(new ObjectMapper(), roundEventBroker);

        subscriber.onMessage(message(RedisRoundEventSubscriber.ROUND_FINISHED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "roundId": "11111111-1111-1111-1111-111111111111",
                  "status": "EXPIRED",
                  "reason": "TIMEOUT",
                  "finishedAt": "2026-07-07T12:01:00Z",
                  "occurredAt": "2026-07-07T12:01:00Z"
                }
                """), null);

        ArgumentCaptor<RoundEventMessage> captor = ArgumentCaptor.forClass(RoundEventMessage.class);
        verify(roundEventBroker).sendRoundEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ROUND_FINISHED");
        assertThat(captor.getValue().reason()).isEqualTo("TIMEOUT");
    }

    @Test
    void forwardsWordGuessedEvent() {
        RedisRoundEventSubscriber subscriber = new RedisRoundEventSubscriber(new ObjectMapper(), roundEventBroker);

        subscriber.onMessage(message(RedisRoundEventSubscriber.ROUND_GUESSED_CHANNEL, """
                {
                  "roomCode": "ABC123",
                  "roundId": "11111111-1111-1111-1111-111111111111",
                  "playerId": "22222222-2222-2222-2222-222222222222",
                  "score": 100,
                  "occurredAt": "2026-07-07T12:00:05Z"
                }
                """), null);

        ArgumentCaptor<RoundEventMessage> captor = ArgumentCaptor.forClass(RoundEventMessage.class);
        verify(roundEventBroker).sendRoundEvent(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("WORD_GUESSED");
        assertThat(captor.getValue().score()).isEqualTo(100);
    }

    private DefaultMessage message(String channel, String body) {
        return new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }
}
