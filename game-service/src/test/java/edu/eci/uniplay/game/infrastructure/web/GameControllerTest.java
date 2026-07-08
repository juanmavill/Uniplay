package edu.eci.uniplay.game.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.eci.uniplay.game.application.dto.ExpireRoundCommand;
import edu.eci.uniplay.game.application.dto.ExpireRoundResult;
import edu.eci.uniplay.game.application.dto.GameStateResult;
import edu.eci.uniplay.game.application.dto.RoundResult;
import edu.eci.uniplay.game.application.dto.ScoreResult;
import edu.eci.uniplay.game.application.dto.StartRoundCommand;
import edu.eci.uniplay.game.application.dto.StartRoundResult;
import edu.eci.uniplay.game.application.dto.SubmitAnswerCommand;
import edu.eci.uniplay.game.application.dto.SubmitAnswerResult;
import edu.eci.uniplay.game.application.port.in.ExpireRoundUseCase;
import edu.eci.uniplay.game.application.port.in.GetGameStateUseCase;
import edu.eci.uniplay.game.application.port.in.StartRoundUseCase;
import edu.eci.uniplay.game.application.port.in.SubmitAnswerUseCase;
import edu.eci.uniplay.game.domain.model.RoundNotActiveException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final UUID ROUND_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-07T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartRoundUseCase startRoundUseCase;

    @MockBean
    private SubmitAnswerUseCase submitAnswerUseCase;

    @MockBean
    private GetGameStateUseCase getGameStateUseCase;

    @MockBean
    private ExpireRoundUseCase expireRoundUseCase;

    @Test
    void startsRound() throws Exception {
        when(startRoundUseCase.startRound(any())).thenReturn(new StartRoundResult(
                "ABC123",
                ROUND_ID,
                "Campus",
                "ACTIVE",
                NOW,
                NOW.plusSeconds(60)
        ));

        mockMvc.perform(post("/games/abc123/rounds"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/games/ABC123"))
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.word").value("Campus"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endsAt").value("2026-07-07T12:01:00Z"));

        ArgumentCaptor<StartRoundCommand> captor = ArgumentCaptor.forClass(StartRoundCommand.class);
        verify(startRoundUseCase).startRound(captor.capture());
        assertThat(captor.getValue().roomCode()).isEqualTo("abc123");
    }

    @Test
    void submitsAnswer() throws Exception {
        when(submitAnswerUseCase.submitAnswer(any())).thenReturn(new SubmitAnswerResult(
                "ABC123",
                ROUND_ID,
                PLAYER_ID,
                true,
                100,
                "FINISHED",
                NOW
        ));

        mockMvc.perform(post("/games/ABC123/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "22222222-2222-2222-2222-222222222222",
                                  "answer": "campus"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.roundStatus").value("FINISHED"));

        ArgumentCaptor<SubmitAnswerCommand> captor = ArgumentCaptor.forClass(SubmitAnswerCommand.class);
        verify(submitAnswerUseCase).submitAnswer(captor.capture());
        assertThat(captor.getValue().playerId()).isEqualTo(PLAYER_ID);
        assertThat(captor.getValue().answer()).isEqualTo("campus");
    }

    @Test
    void returnsGameState() throws Exception {
        when(getGameStateUseCase.getState("ABC123")).thenReturn(new GameStateResult(
                "ABC123",
                new RoundResult(ROUND_ID, "ACTIVE", "Campus", null, NOW, NOW.plusSeconds(60), null),
                List.of(new ScoreResult(PLAYER_ID, 100))
        ));

        mockMvc.perform(get("/games/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round.word").value("Campus"))
                .andExpect(jsonPath("$.round.endsAt").value("2026-07-07T12:01:00Z"))
                .andExpect(jsonPath("$.scores[0].playerId").value(PLAYER_ID.toString()))
                .andExpect(jsonPath("$.scores[0].score").value(100));
    }

    @Test
    void expiresRoundByTimer() throws Exception {
        when(expireRoundUseCase.expireRound(any())).thenReturn(new ExpireRoundResult(
                "ABC123",
                ROUND_ID,
                "EXPIRED",
                "TIMEOUT",
                NOW.plusSeconds(60)
        ));

        mockMvc.perform(post("/games/ABC123/rounds/11111111-1111-1111-1111-111111111111/timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.reason").value("TIMEOUT"))
                .andExpect(jsonPath("$.finishedAt").value("2026-07-07T12:01:00Z"));

        ArgumentCaptor<ExpireRoundCommand> captor = ArgumentCaptor.forClass(ExpireRoundCommand.class);
        verify(expireRoundUseCase).expireRound(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(ROUND_ID);
    }

    @Test
    void rejectsInvalidAnswerRequest() throws Exception {
        mockMvc.perform(post("/games/ABC123/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answer": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid game request"));
    }

    @Test
    void mapsRoundConflict() throws Exception {
        when(submitAnswerUseCase.submitAnswer(any()))
                .thenThrow(new RoundNotActiveException("room ABC123 does not have an active round"));

        mockMvc.perform(post("/games/ABC123/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "22222222-2222-2222-2222-222222222222",
                                  "answer": "campus"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Round conflict"));
    }
}
