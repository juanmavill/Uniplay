package edu.eci.uniplay.metrics;

import java.time.Duration;

import edu.eci.uniplay.metrics.application.dto.BusinessKpisResult;
import edu.eci.uniplay.metrics.application.port.in.GetBusinessKpisUseCase;
import edu.eci.uniplay.metrics.infrastructure.redis.RedisDomainEventSubscriber;
import edu.eci.uniplay.metrics.infrastructure.web.dto.BusinessKpisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the full path against a real Redis: domain events published by the
 * other microservices arrive over Pub/Sub, feed the KPI projection, and are
 * exposed over HTTP.
 *
 * <p>Testcontainers is used instead of a stub because the value of this service
 * is the subscription itself. A test that mocked the Redis connection would
 * verify neither the channel registration nor the event deserialization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetricsServiceRedisIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(5);

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GetBusinessKpisUseCase getBusinessKpisUseCase;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void projectsBusinessKpisFromEventsPublishedToRedis() {
        publish(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, "{\"code\":\"SALA01\"}");
        publish(RedisDomainEventSubscriber.PLAYER_CONNECTED_CHANNEL, "{\"code\":\"SALA01\",\"playerId\":\"p1\"}");
        publish(RedisDomainEventSubscriber.PLAYER_CONNECTED_CHANNEL, "{\"code\":\"SALA01\",\"playerId\":\"p2\"}");
        publish(RedisDomainEventSubscriber.ROUND_STARTED_CHANNEL, "{\"roomCode\":\"SALA01\",\"roundId\":\"r1\"}");
        publish(RedisDomainEventSubscriber.ROUND_STARTED_CHANNEL, "{\"roomCode\":\"SALA01\",\"roundId\":\"r2\"}");
        publish(RedisDomainEventSubscriber.WORD_GUESSED_CHANNEL, "{\"roomCode\":\"SALA01\",\"roundId\":\"r1\"}");

        await().atMost(EVENT_TIMEOUT).untilAsserted(() -> {
            BusinessKpisResult kpis = getBusinessKpisUseCase.currentKpis();
            assertThat(kpis.activeRooms()).isEqualTo(1);
            assertThat(kpis.connectedPlayers()).isEqualTo(2);
            // Two rounds started, one guessed: 1/2.
            assertThat(kpis.guessRate()).isEqualTo(0.5);
            assertThat(kpis.averagePlayersPerRoom()).isEqualTo(2.0);
        });

        BusinessKpisResponse exposed = restTemplate.getForObject("/metrics/kpis", BusinessKpisResponse.class);
        assertThat(exposed).isNotNull();
        assertThat(exposed.activeRooms()).isEqualTo(1);
        assertThat(exposed.connectedPlayers()).isEqualTo(2);
    }

    @Test
    void keepsTheSubscriptionAliveAfterAnInvalidEvent() {
        publish(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, "not-json");
        publish(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, "{\"code\":\"SALA02\"}");

        await().atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> assertThat(getBusinessKpisUseCase.currentKpis().activeRooms()).isEqualTo(1));
    }

    private void publish(String channel, String payload) {
        redisTemplate.convertAndSend(channel, payload);
    }
}
