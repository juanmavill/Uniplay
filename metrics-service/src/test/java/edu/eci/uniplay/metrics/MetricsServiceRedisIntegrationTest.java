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
 * Verifica el camino completo del servicio contra un Redis real: los eventos de
 * dominio publicados por los demas microservicios llegan por pub/sub, alimentan la
 * proyeccion de KPIs y quedan expuestos por HTTP.
 *
 * <p>Se usa Testcontainers en lugar de un doble porque el valor de este servicio
 * esta en la suscripcion misma. Un test que simule la conexion Redis no comprobaria
 * el registro de los canales ni la deserializacion de los eventos.
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
    void proyectaKpisDeNegocioDesdeEventosPublicadosEnRedis() {
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
            // Se inician dos rondas y se adivina una: 1/2.
            assertThat(kpis.guessRate()).isEqualTo(0.5);
            assertThat(kpis.averagePlayersPerRoom()).isEqualTo(2.0);
        });

        BusinessKpisResponse expuestos = restTemplate.getForObject("/metrics/kpis", BusinessKpisResponse.class);
        assertThat(expuestos).isNotNull();
        assertThat(expuestos.activeRooms()).isEqualTo(1);
        assertThat(expuestos.connectedPlayers()).isEqualTo(2);
    }

    @Test
    void mantieneLaSuscripcionActivaTrasUnEventoInvalido() {
        publish(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, "no-es-json");
        publish(RedisDomainEventSubscriber.ROOM_CREATED_CHANNEL, "{\"code\":\"SALA02\"}");

        await().atMost(EVENT_TIMEOUT)
                .untilAsserted(() -> assertThat(getBusinessKpisUseCase.currentKpis().activeRooms()).isEqualTo(1));
    }

    private void publish(String channel, String payload) {
        redisTemplate.convertAndSend(channel, payload);
    }
}
