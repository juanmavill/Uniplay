package edu.eci.uniplay.metrics.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessKpiProjectionServiceTest {

    @Test
    void calculatesBusinessKpisFromDomainEvents() {
        BusinessKpiProjectionService service = new BusinessKpiProjectionService();

        service.recordRoomCreated("ABC123");
        service.recordPlayerConnected("ABC123", "22222222-2222-2222-2222-222222222222");
        service.recordPlayerConnected("ABC123", "33333333-3333-3333-3333-333333333333");
        service.recordRoundStarted("ABC123", "11111111-1111-1111-1111-111111111111");
        service.recordRoundStarted("ABC123", "44444444-4444-4444-4444-444444444444");
        service.recordWordGuessed("ABC123", "11111111-1111-1111-1111-111111111111");

        var result = service.currentKpis();

        assertThat(result.activeRooms()).isEqualTo(1);
        assertThat(result.connectedPlayers()).isEqualTo(2);
        assertThat(result.guessRate()).isEqualTo(0.5);
        assertThat(result.averagePlayersPerRoom()).isEqualTo(2.0);
    }

    @Test
    void avoidsDivisionByZeroWhenNoEventsWereRecorded() {
        BusinessKpiProjectionService service = new BusinessKpiProjectionService();

        var result = service.currentKpis();

        assertThat(result.guessRate()).isZero();
        assertThat(result.averagePlayersPerRoom()).isZero();
    }
}
