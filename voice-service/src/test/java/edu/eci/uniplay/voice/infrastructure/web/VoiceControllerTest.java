package edu.eci.uniplay.voice.infrastructure.web;

import java.time.Instant;

import edu.eci.uniplay.voice.application.dto.MuteStateResult;
import edu.eci.uniplay.voice.application.dto.SpeakingStateResult;
import edu.eci.uniplay.voice.application.dto.VoiceTokenResult;
import edu.eci.uniplay.voice.application.port.in.ChangeMuteStateUseCase;
import edu.eci.uniplay.voice.application.port.in.ChangeSpeakingStateUseCase;
import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
import edu.eci.uniplay.voice.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VoiceControllerTest {

    @Test
    void createsVoiceToken() throws Exception {
        GenerateVoiceTokenUseCase useCase = mock(GenerateVoiceTokenUseCase.class);
        when(useCase.generateToken(any())).thenReturn(new VoiceTokenResult(
                "ABC123",
                "uniplay-ABC123",
                "22222222-2222-2222-2222-222222222222",
                "Juan",
                "ws://localhost:7880",
                "jwt-token",
                Instant.parse("2026-07-07T12:30:00Z")
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VoiceController(
                        useCase,
                        mock(ChangeMuteStateUseCase.class),
                        mock(ChangeSpeakingStateUseCase.class)
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/voice/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "ABC123",
                                  "playerId": "22222222-2222-2222-2222-222222222222",
                                  "playerName": "Juan"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voiceRoomName").value("uniplay-ABC123"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void rejectsInvalidRequestBody() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VoiceController(
                        mock(GenerateVoiceTokenUseCase.class),
                        mock(ChangeMuteStateUseCase.class),
                        mock(ChangeSpeakingStateUseCase.class)
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/voice/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid voice request"));
    }

    @Test
    void changesMuteState() throws Exception {
        ChangeMuteStateUseCase useCase = mock(ChangeMuteStateUseCase.class);
        when(useCase.changeMuteState(any())).thenReturn(new MuteStateResult(
                "ABC123",
                "uniplay-ABC123",
                "22222222-2222-2222-2222-222222222222",
                true,
                Instant.parse("2026-07-07T12:00:00Z")
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VoiceController(
                        mock(GenerateVoiceTokenUseCase.class),
                        useCase,
                        mock(ChangeSpeakingStateUseCase.class)
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/voice/mute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "ABC123",
                                  "playerId": "22222222-2222-2222-2222-222222222222",
                                  "muted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muted").value(true));
    }

    @Test
    void changesSpeakingState() throws Exception {
        ChangeSpeakingStateUseCase useCase = mock(ChangeSpeakingStateUseCase.class);
        when(useCase.changeSpeakingState(any())).thenReturn(new SpeakingStateResult(
                "ABC123",
                "uniplay-ABC123",
                "22222222-2222-2222-2222-222222222222",
                true,
                Instant.parse("2026-07-07T12:00:00Z")
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VoiceController(
                        mock(GenerateVoiceTokenUseCase.class),
                        mock(ChangeMuteStateUseCase.class),
                        useCase
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/voice/speaking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "ABC123",
                                  "playerId": "22222222-2222-2222-2222-222222222222",
                                  "speaking": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speaking").value(true));
    }
}
