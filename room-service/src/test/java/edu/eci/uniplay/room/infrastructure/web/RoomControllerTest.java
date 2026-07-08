package edu.eci.uniplay.room.infrastructure.web;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import edu.eci.uniplay.room.application.dto.CreateRoomCommand;
import edu.eci.uniplay.room.application.dto.RoomCreatedResult;
import edu.eci.uniplay.room.application.exception.RoomCodeGenerationException;
import edu.eci.uniplay.room.application.port.in.CreateRoomUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateRoomUseCase createRoomUseCase;

    @Test
    void createsRoom() throws Exception {
        when(createRoomUseCase.createRoom(any(CreateRoomCommand.class))).thenReturn(new RoomCreatedResult(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ABC123",
                "WAITING_FOR_PLAYERS",
                21,
                Instant.parse("2026-07-07T12:00:00Z")
        ));

        mockMvc.perform(post("/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/salas/ABC123")))
                .andExpect(jsonPath("$.roomId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.code").value("ABC123"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
                .andExpect(jsonPath("$.maxPlayers").value(21))
                .andExpect(jsonPath("$.createdAt").value("2026-07-07T12:00:00Z"));
    }

    @Test
    void rejectsInvalidMaxPlayers() throws Exception {
        mockMvc.perform(post("/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxPlayers\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid room request"));
    }

    @Test
    void returnsServiceUnavailableWhenUniqueCodeCannotBeGenerated() throws Exception {
        when(createRoomUseCase.createRoom(any(CreateRoomCommand.class))).thenThrow(new RoomCodeGenerationException(10));

        mockMvc.perform(post("/salas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Room code unavailable"));
    }
}
