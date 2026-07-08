package edu.eci.uniplay.metrics.infrastructure.web;

import edu.eci.uniplay.metrics.application.dto.BusinessKpisResult;
import edu.eci.uniplay.metrics.application.port.in.GetBusinessKpisUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricsControllerTest {

    @Test
    void returnsBusinessKpis() throws Exception {
        GetBusinessKpisUseCase useCase = mock(GetBusinessKpisUseCase.class);
        when(useCase.currentKpis()).thenReturn(new BusinessKpisResult(1, 2, 0.5, 2.0));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MetricsController(useCase)).build();

        mockMvc.perform(get("/metrics/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRooms").value(1))
                .andExpect(jsonPath("$.connectedPlayers").value(2))
                .andExpect(jsonPath("$.guessRate").value(0.5))
                .andExpect(jsonPath("$.averagePlayersPerRoom").value(2.0));
    }
}
