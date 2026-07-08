package edu.eci.uniplay.gateway.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    private static final BackendStub BACKEND_STUB = BackendStub.start();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("uniplay.gateway.room-service-uri", BACKEND_STUB::uri);
        registry.add("uniplay.gateway.game-service-uri", BACKEND_STUB::uri);
        registry.add("uniplay.gateway.realtime-service-uri", BACKEND_STUB::uri);
        registry.add("uniplay.gateway.realtime-websocket-uri", () -> BACKEND_STUB.uri().replace("http://", "ws://"));
        registry.add("uniplay.gateway.metrics-service-uri", BACKEND_STUB::uri);
        registry.add("uniplay.gateway.voice-service-uri", BACKEND_STUB::uri);
        registry.add("uniplay.gateway.rate-limit.enabled", () -> "false");
        registry.add("uniplay.gateway.allowed-origins", () -> "http://localhost:5173");
    }

    @AfterAll
    static void stopBackendStub() {
        BACKEND_STUB.stop();
    }

    @Test
    void routesRoomRequestsToConfiguredService() {
        webTestClient.post()
                .uri("/salas")
                .bodyValue("{\"maxPlayers\":21}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("POST /salas"));
    }

    @Test
    void routesWebSocketHandshakePathToRealtimeService() {
        webTestClient.get()
                .uri("/ws/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("GET /ws/info"));
    }

    @Test
    void exposesCorsPolicyForBrowserClients() {
        webTestClient.options()
                .uri("/games/ABC123")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
                .expectHeader().value(
                        "Access-Control-Allow-Methods",
                        value -> assertThat(value).contains("POST")
                );
    }

    private static final class BackendStub {

        private final HttpServer server;

        private BackendStub(HttpServer server) {
            this.server = server;
        }

        private static BackendStub start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/", BackendStub::respond);
                server.start();
                return new BackendStub(server);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not start gateway backend stub", exception);
            }
        }

        private String uri() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void stop() {
            server.stop(0);
        }

        private static void respond(HttpExchange exchange) throws IOException {
            byte[] body = (exchange.getRequestMethod() + " " + exchange.getRequestURI())
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        }
    }
}
