import React from "react";
import { createRoot } from "react-dom/client";
import { Brush, Check, Copy, Eraser, Mic, MicOff, Play, RefreshCw, Send, Trophy, Users, Vote } from "lucide-react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Room, RoomEvent, Track } from "livekit-client";
import "./styles.css";

const DECKS = ["GENERAL", "MATEMATICAS", "SISTEMAS", "FISICA"];
const MODES = ["CLASSIC", "ALL_DRAW"];

function App() {
  const [gatewayUrl] = React.useState(() => localStorage.getItem("uniplay.gatewayUrl") || "");
  const [room, setRoom] = React.useState(null);
  const [roomCodeInput, setRoomCodeInput] = React.useState("");
  const [playerName, setPlayerName] = React.useState(() => localStorage.getItem("uniplay.playerName") || "Jugador");
  const [player, setPlayer] = React.useState(null);
  const [players, setPlayers] = React.useState([]);
  const [round, setRound] = React.useState(null);
  const [gameState, setGameState] = React.useState(null);
  const [answer, setAnswer] = React.useState("");
  const [chatMessages, setChatMessages] = React.useState([]);
  const [deck, setDeck] = React.useState("SISTEMAS");
  const [mode, setMode] = React.useState("CLASSIC");
  const [candidateId, setCandidateId] = React.useState("");
  const [voice, setVoice] = React.useState({ connected: false, muted: true, speaking: false, roomName: "" });
  const [stompState, setStompState] = React.useState("offline");
  const [turnNumber, setTurnNumber] = React.useState(0);
  const [isBusy, setIsBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const livekitRoomRef = React.useRef(null);

  const api = React.useMemo(() => createApiClient(gatewayUrl), [gatewayUrl]);
  const currentRoomCode = room?.code || roomCodeInput.trim().toUpperCase();
  const activeRound = round || gameState?.round;
  const timer = useRoundTimer(activeRound);
  const scores = React.useMemo(() => indexScores(gameState?.scores), [gameState]);
  const isAllDrawMode = mode === "ALL_DRAW" || activeRound?.mode === "ALL_DRAW";
  const turnDrawer = players.length > 0 ? players[turnNumber % players.length] : null;
  const currentDrawer = activeRound?.drawerId
    ? players.find((item) => item.playerId === activeRound.drawerId) || turnDrawer
    : turnDrawer;
  const isDrawer = Boolean(player?.playerId && activeRound && (isAllDrawMode || currentDrawer?.playerId === player.playerId));
  const canGuess = Boolean(player?.playerId && activeRound && !isDrawer);
  const visibleWord = activeRound?.word
    ? isDrawer
      ? activeRound.word
      : maskWord(activeRound.word)
    : "------";

  React.useEffect(() => {
    localStorage.setItem("uniplay.playerName", playerName);
  }, [playerName]);

  React.useEffect(() => {
    if (players.length > 0 && turnNumber >= players.length) {
      setTurnNumber(0);
    }
  }, [players.length, turnNumber]);

  React.useEffect(() => {
    if (!currentRoomCode) {
      return undefined;
    }

    let cancelled = false;
    const refresh = async () => {
      try {
        const result = await api.listPlayers(currentRoomCode);
        if (!cancelled) {
          setPlayers(result.players || []);
        }
      } catch {
        // The explicit actions still surface connection errors to the player.
      }
    };

    refresh();
    const intervalId = window.setInterval(refresh, 3500);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [api, currentRoomCode]);

  React.useEffect(() => {
    if (!currentRoomCode) {
      setStompState("offline");
      return undefined;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${api.baseUrl}/ws`),
      reconnectDelay: 3000,
      onConnect: () => {
        setStompState("online");
        client.subscribe(`/topic/rooms/${currentRoomCode}/rounds`, (message) => {
          handleRoundEvent(safeJson(message.body), setChatMessages, player?.playerId);
        });
        client.subscribe(`/topic/rooms/${currentRoomCode}/voice`, (message) => {
          handleVoiceEvent(safeJson(message.body), setVoice);
        });
        client.subscribe(`/topic/rooms/${currentRoomCode}/draw`, (message) => {
          window.dispatchEvent(new CustomEvent("uniplay:draw", { detail: safeJson(message.body) }));
        });
      },
      onDisconnect: () => setStompState("offline"),
      onStompError: () => setStompState("error"),
      onWebSocketClose: () => setStompState("offline")
    });

    client.activate();
    return () => client.deactivate();
  }, [api, currentRoomCode, player?.playerId]);

  async function runAction(action) {
    setIsBusy(true);
    setError("");
    try {
      await action();
    } catch (actionError) {
      setError(actionError.message || "No se pudo completar la accion");
    } finally {
      setIsBusy(false);
    }
  }

  function addSystemMessage(text, tone = "system") {
    setChatMessages((current) => [
      { id: crypto.randomUUID(), type: "system", tone, text },
      ...current
    ].slice(0, 18));
  }

  async function createRoom() {
    await runAction(async () => {
      const createdRoom = await api.createRoom(21);
      setRoom(createdRoom);
      setRoomCodeInput(createdRoom.code);
      setPlayers([]);
      setRound(null);
      setGameState(null);
      setTurnNumber(0);
      setChatMessages([{ id: crypto.randomUUID(), type: "system", tone: "system", text: `Sala ${createdRoom.code} creada` }]);
    });
  }

  async function joinRoom() {
    await runAction(async () => {
      const code = currentRoomCode;
      if (!code) {
        throw new Error("Ingresa o crea un codigo de sala");
      }
      const joined = await api.joinRoom(code, playerName.trim() || "Jugador");
      setRoom({ code: joined.code, roomId: joined.roomId });
      setRoomCodeInput(joined.code);
      setPlayer({ playerId: joined.playerId, playerName: joined.playerName });
      setPlayers(joined.players || []);
      try {
        const state = await api.getGameState(joined.code, joined.playerId);
        setGameState(state);
        setRound(state.round);
      } catch {
        setGameState(null);
      }
      addSystemMessage(`${joined.playerName} entro a la sala`);
    });
  }

  async function refreshPlayers() {
    await runAction(async () => {
      if (!currentRoomCode) {
        throw new Error("No hay sala activa");
      }
      const result = await api.listPlayers(currentRoomCode);
      setPlayers(result.players || []);
    });
  }

  async function startRound() {
    await runAction(async () => {
      if (!currentRoomCode) {
        throw new Error("No hay sala activa");
      }
      const nextTurnNumber = activeRound && players.length > 0 ? (turnNumber + 1) % players.length : turnNumber;
      const nextDrawer = players.length > 0 ? players[nextTurnNumber] : null;
      const nextRound = await api.startRound(
        currentRoomCode,
        mode,
        deck,
        mode === "ALL_DRAW" ? null : nextDrawer?.playerId
      );
      setTurnNumber(nextTurnNumber);
      setRound(nextRound);
      setGameState(null);
      clearCanvasById("main-drawing-canvas");
      const drawerName = isAllDrawMode ? "todos" : nextDrawer?.playerName || "turno asignado";
      addSystemMessage(`Nueva ronda. Dibuja ${drawerName}.`);
    });
  }

  async function submitAnswer() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de responder");
      }
      if (!canGuess) {
        throw new Error("En este turno no puedes adivinar");
      }
      const trimmedAnswer = answer.trim();
      if (!trimmedAnswer) {
        return;
      }
      const result = await api.submitAnswer(currentRoomCode, player.playerId, trimmedAnswer);
      setChatMessages((current) => [
        result.correct
          ? {
              id: crypto.randomUUID(),
              type: "system",
              tone: "success",
              text: `${player.playerName} adivino la palabra`
            }
          : {
              id: crypto.randomUUID(),
              type: "guess",
              author: player.playerName,
              text: trimmedAnswer
            },
        ...current
      ].slice(0, 18));
      setAnswer("");
      await refreshGameState();
    });
  }

  async function refreshGameState() {
    if (!currentRoomCode) {
      return;
    }
    const state = await api.getGameState(currentRoomCode, player?.playerId);
    setGameState(state);
  }

  async function castVote() {
    await runAction(async () => {
      if (!activeRound?.roundId || !player?.playerId) {
        throw new Error("Necesitas una ronda activa y jugador");
      }
      const targetId = candidateId.trim();
      if (!targetId) {
        throw new Error("Selecciona un jugador");
      }
      await api.castVote(currentRoomCode, activeRound.roundId, player.playerId, targetId);
      setCandidateId("");
      addSystemMessage("Voto registrado");
    });
  }

  async function connectVoice() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de activar voz");
      }
      const token = await api.createVoiceToken(currentRoomCode, player.playerId, player.playerName);
      const lkRoom = new Room({ adaptiveStream: true, dynacast: true });
      lkRoom.on(RoomEvent.TrackSubscribed, (track) => {
        if (track.kind === Track.Kind.Audio) {
          const element = track.attach();
          element.autoplay = true;
          document.body.appendChild(element);
        }
      });
      lkRoom.on(RoomEvent.Disconnected, () => {
        setVoice((previous) => ({ ...previous, connected: false, muted: true, speaking: false }));
      });
      await lkRoom.connect(token.livekitUrl, token.token);
      livekitRoomRef.current = lkRoom;
      setVoice({ connected: true, muted: true, speaking: false, roomName: token.voiceRoomName });
    });
  }

  async function toggleMute() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de cambiar microfono");
      }
      const nextMuted = !voice.muted;
      if (livekitRoomRef.current) {
        await livekitRoomRef.current.localParticipant.setMicrophoneEnabled(!nextMuted);
      }
      const result = await api.setMuted(currentRoomCode, player.playerId, nextMuted);
      setVoice((previous) => ({ ...previous, muted: result.muted }));
    });
  }

  async function toggleSpeaking() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de reportar voz");
      }
      const result = await api.setSpeaking(currentRoomCode, player.playerId, !voice.speaking);
      setVoice((previous) => ({ ...previous, speaking: result.speaking }));
    });
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">UniPlay</p>
          <h1>Partida de dibujo</h1>
        </div>
        <div className="room-pill">
          <span>{currentRoomCode || "Sin sala"}</span>
          <button className="icon-button" aria-label="Copiar codigo" title="Copiar codigo" onClick={() => navigator.clipboard?.writeText(currentRoomCode)}>
            <Copy size={16} />
          </button>
        </div>
      </header>

      {error && <div className="alert" role="alert">{error}</div>}

      <section className="game-shell">
        <aside className="left-rail">
          <section className="panel room-panel">
            <PanelTitle icon={<Users size={18} />} title="Sala" />
            <label>
              Nombre
              <input value={playerName} onChange={(event) => setPlayerName(event.target.value)} />
            </label>
            <label>
              Codigo
              <input
                value={roomCodeInput}
                maxLength={6}
                onChange={(event) => setRoomCodeInput(event.target.value.toUpperCase())}
              />
            </label>
            <div className="button-row">
              <button className="primary" onClick={createRoom} disabled={isBusy}>
                <Play size={16} />
                Crear
              </button>
              <button onClick={joinRoom} disabled={isBusy}>
                <Check size={16} />
                Unirse
              </button>
            </div>
            <StatusLine label="Tiempo real" value={stompState} tone={stompState === "online" ? "good" : "muted"} />
          </section>

          <section className="panel players-panel">
            <PanelTitle
              icon={<Trophy size={18} />}
              title="Jugadores"
              action={<button className="icon-button" onClick={refreshPlayers} title="Actualizar jugadores"><RefreshCw size={16} /></button>}
            />
            <div className="player-list">
              {players.length === 0 && <p className="empty">Sin jugadores</p>}
              {players.map((item) => {
                const drawing = Boolean(activeRound && (isAllDrawMode || currentDrawer?.playerId === item.playerId));
                return (
                  <button
                    key={item.playerId}
                    className={drawing ? "player-item drawing" : "player-item"}
                    onClick={() => setCandidateId(item.playerId)}
                  >
                    <span>{item.playerName}</span>
                    <strong>{scores.get(item.playerId) || 0}</strong>
                  </button>
                );
              })}
            </div>
          </section>
        </aside>

        <section className="stage">
          <section className="round-banner">
            <div>
              <span>Palabra</span>
              <strong>{visibleWord}</strong>
            </div>
            <div>
              <span>Turno</span>
              <strong>{isAllDrawMode ? "Todos" : currentDrawer?.playerName || "Pendiente"}</strong>
            </div>
            <div>
              <span>Tiempo</span>
              <strong>{timer}</strong>
            </div>
          </section>

          <section className="match-controls">
            <div className="segmented">
              {MODES.map((item) => (
                <button key={item} className={mode === item ? "selected" : ""} onClick={() => setMode(item)}>
                  {item === "CLASSIC" ? "Clasico" : "Todos dibujan"}
                </button>
              ))}
            </div>
            <select value={deck} onChange={(event) => setDeck(event.target.value)} aria-label="Mazo">
              {DECKS.map((item) => <option key={item} value={item}>{deckLabel(item)}</option>)}
            </select>
            <button className="primary" onClick={startRound} disabled={isBusy || !currentRoomCode}>
              <Play size={16} />
              Ronda
            </button>
          </section>

          <DrawingBoard
            roomCode={currentRoomCode}
            playerId={player?.playerId}
            gatewayBase={api.baseUrl}
            canDraw={Boolean(activeRound && (isDrawer || isAllDrawMode))}
          />
        </section>

        <aside className="right-rail">
          <section className="panel chat-panel">
            <PanelTitle icon={<Send size={18} />} title="Chat" />
            <div className="chat-messages" aria-live="polite">
              {chatMessages.length === 0 && <p className="empty">Sin mensajes</p>}
              {chatMessages.map((message) => (
                <article key={message.id} className={message.type === "system" ? `chat-message system ${message.tone}` : "chat-message"}>
                  {message.type === "system" ? (
                    <p>{message.text}</p>
                  ) : (
                    <>
                      <strong>{message.author}</strong>
                      <p>{message.text}</p>
                    </>
                  )}
                </article>
              ))}
            </div>
            <div className="answer-bar">
              <input
                value={answer}
                placeholder={canGuess ? "Tu respuesta" : "Espera tu turno"}
                onChange={(event) => setAnswer(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && canGuess) {
                    submitAnswer();
                  }
                }}
                disabled={!canGuess}
              />
              <button className="primary icon-button" onClick={submitAnswer} disabled={isBusy || !answer.trim() || !canGuess} title="Enviar respuesta">
                <Send size={17} />
              </button>
            </div>
          </section>

          <section className="panel voice-panel">
            <PanelTitle icon={<Mic size={18} />} title="Voz" />
            <div className="voice-status">
              <span className={voice.connected ? "pulse on" : "pulse"} />
              <div>
                <strong>{voice.connected ? "Conectado" : "Sin voz"}</strong>
                <small>{voice.roomName || "Canal de sala"}</small>
              </div>
            </div>
            <div className="button-row">
              <button onClick={connectVoice} disabled={isBusy || voice.connected}>
                <Mic size={16} />
                Entrar
              </button>
              <button onClick={toggleMute} disabled={isBusy || !player}>
                {voice.muted ? <MicOff size={16} /> : <Mic size={16} />}
                {voice.muted ? "Activar" : "Mute"}
              </button>
            </div>
            <label className="toggle-row">
              <input type="checkbox" checked={voice.speaking} onChange={toggleSpeaking} />
              Hablando
            </label>
          </section>

          {isAllDrawMode && (
            <section className="panel vote-panel">
              <PanelTitle icon={<Vote size={18} />} title="Votacion" />
              <div className="vote-list">
                {players.map((item) => (
                  <button key={item.playerId} className={candidateId === item.playerId ? "selected" : ""} onClick={() => setCandidateId(item.playerId)}>
                    {item.playerName}
                  </button>
                ))}
              </div>
              <button onClick={castVote} disabled={isBusy || !candidateId.trim()}>
                <Vote size={16} />
                Votar
              </button>
            </section>
          )}
        </aside>
      </section>
    </main>
  );
}

function DrawingBoard({ roomCode, playerId, gatewayBase, canDraw }) {
  const canvasRef = React.useRef(null);
  const stompRef = React.useRef(null);
  const drawingRef = React.useRef(false);
  const previousPointRef = React.useRef(null);
  const [color, setColor] = React.useState("#0f8b8d");
  const [width, setWidth] = React.useState(5);

  React.useEffect(() => {
    if (!roomCode) {
      return undefined;
    }
    const client = new Client({
      webSocketFactory: () => new SockJS(`${gatewayBase}/ws`),
      reconnectDelay: 3000
    });
    client.activate();
    stompRef.current = client;
    return () => client.deactivate();
  }, [gatewayBase, roomCode]);

  React.useEffect(() => {
    const handler = (event) => drawDelta(canvasRef.current, event.detail);
    window.addEventListener("uniplay:draw", handler);
    return () => window.removeEventListener("uniplay:draw", handler);
  }, []);

  React.useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      canvas.width = Math.floor(rect.width * window.devicePixelRatio);
      canvas.height = Math.floor(rect.height * window.devicePixelRatio);
    };
    resize();
    window.addEventListener("resize", resize);
    return () => window.removeEventListener("resize", resize);
  }, []);

  function pointerPoint(event) {
    const rect = canvasRef.current.getBoundingClientRect();
    return {
      x: (event.clientX - rect.left) / rect.width,
      y: (event.clientY - rect.top) / rect.height
    };
  }

  function startDrawing(event) {
    if (!canDraw) {
      return;
    }
    drawingRef.current = true;
    previousPointRef.current = pointerPoint(event);
  }

  function stopDrawing() {
    drawingRef.current = false;
    previousPointRef.current = null;
  }

  function moveDrawing(event) {
    if (!canDraw || !drawingRef.current || !previousPointRef.current || !roomCode) {
      return;
    }
    const nextPoint = pointerPoint(event);
    const delta = {
      playerId: playerId || "anonymous",
      fromX: previousPointRef.current.x,
      fromY: previousPointRef.current.y,
      toX: nextPoint.x,
      toY: nextPoint.y,
      color,
      width
    };
    drawDelta(canvasRef.current, delta);
    if (stompRef.current?.connected) {
      stompRef.current.publish({
        destination: `/app/rooms/${roomCode}/draw`,
        body: JSON.stringify(delta)
      });
    }
    previousPointRef.current = nextPoint;
  }

  return (
    <section className="canvas-shell">
      <div className="canvas-tools">
        <PanelTitle icon={<Brush size={18} />} title="Lienzo" />
        <div className="tool-group">
          <input type="color" value={color} onChange={(event) => setColor(event.target.value)} aria-label="Color" disabled={!canDraw} />
          <input type="range" min="2" max="12" value={width} onChange={(event) => setWidth(Number(event.target.value))} aria-label="Grosor" disabled={!canDraw} />
          <button onClick={() => clearCanvas(canvasRef.current)} disabled={!canDraw} title="Limpiar">
            <Eraser size={16} />
          </button>
        </div>
      </div>
      <div className="canvas-frame">
        <canvas
          id="main-drawing-canvas"
          ref={canvasRef}
          className="drawing-canvas"
          onPointerDown={startDrawing}
          onPointerMove={moveDrawing}
          onPointerUp={stopDrawing}
          onPointerLeave={stopDrawing}
        />
        {!canDraw && <div className="canvas-lock">Esperando turno</div>}
      </div>
    </section>
  );
}

function PanelTitle({ icon, title, action }) {
  return (
    <div className="panel-title">
      <div>{icon}<h2>{title}</h2></div>
      {action}
    </div>
  );
}

function StatusLine({ label, value, tone = "muted" }) {
  return (
    <div className="status-line">
      <span>{label}</span>
      <strong className={tone}>{value}</strong>
    </div>
  );
}

function useRoundTimer(round) {
  const [now, setNow] = React.useState(Date.now());
  React.useEffect(() => {
    const intervalId = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);
  if (!round?.endsAt) {
    return "--:--";
  }
  const remainingSeconds = Math.max(0, Math.ceil((new Date(round.endsAt).getTime() - now) / 1000));
  return `${String(Math.floor(remainingSeconds / 60)).padStart(2, "0")}:${String(remainingSeconds % 60).padStart(2, "0")}`;
}

function createApiClient(baseUrl) {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");
  return {
    baseUrl: normalizedBaseUrl,
    createRoom: (maxPlayers) => request(normalizedBaseUrl, "/salas", { method: "POST", body: { maxPlayers } }),
    joinRoom: (code, playerName) => request(normalizedBaseUrl, `/salas/${code}/jugadores`, { method: "POST", body: { playerName } }),
    listPlayers: (code) => request(normalizedBaseUrl, `/salas/${code}/jugadores`),
    startRound: (code, mode, deck, drawerId) => request(normalizedBaseUrl, `/games/${code}/rounds`, { method: "POST", body: { mode, deck, drawerId } }),
    submitAnswer: (code, playerId, answer) => request(normalizedBaseUrl, `/games/${code}/answers`, { method: "POST", body: { playerId, answer } }),
    getGameState: (code, viewerPlayerId) => request(normalizedBaseUrl, `/games/${code}${viewerPlayerId ? `?viewerPlayerId=${encodeURIComponent(viewerPlayerId)}` : ""}`),
    castVote: (code, roundId, voterId, candidateId) => request(normalizedBaseUrl, `/games/${code}/rounds/${roundId}/votes`, { method: "POST", body: { voterId, candidateId } }),
    createVoiceToken: (roomCode, playerId, playerName) => request(normalizedBaseUrl, "/voice/token", { method: "POST", body: { roomCode, playerId, playerName } }),
    setMuted: (roomCode, playerId, muted) => request(normalizedBaseUrl, "/voice/mute", { method: "POST", body: { roomCode, playerId, muted } }),
    setSpeaking: (roomCode, playerId, speaking) => request(normalizedBaseUrl, "/voice/speaking", { method: "POST", body: { roomCode, playerId, speaking } })
  };
}

async function request(baseUrl, path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method || "GET",
    headers: options.body ? { "Content-Type": "application/json" } : undefined,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  if (!response.ok) {
    let detail = `HTTP ${response.status}`;
    try {
      const problem = await response.json();
      detail = problem.detail || problem.title || detail;
    } catch {
      detail = response.statusText || detail;
    }
    throw new Error(detail);
  }

  if (response.status === 204) {
    return {};
  }
  return response.json();
}

function drawDelta(canvas, delta) {
  if (!canvas || !delta) {
    return;
  }
  const context = canvas.getContext("2d");
  const ratio = window.devicePixelRatio || 1;
  context.save();
  context.scale(ratio, ratio);
  context.strokeStyle = delta.color || "#0f8b8d";
  context.lineWidth = delta.width || 4;
  context.lineCap = "round";
  context.lineJoin = "round";
  const rect = canvas.getBoundingClientRect();
  context.beginPath();
  context.moveTo(delta.fromX * rect.width, delta.fromY * rect.height);
  context.lineTo(delta.toX * rect.width, delta.toY * rect.height);
  context.stroke();
  context.restore();
}

function clearCanvas(canvas) {
  if (!canvas) {
    return;
  }
  canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
}

function clearCanvasById(id) {
  clearCanvas(document.getElementById(id));
}

function handleRoundEvent(payload, setChatMessages, currentPlayerId) {
  const type = payload?.type || payload?.eventType || payload?.name;
  if (type === "WORD_GUESSED") {
    if (payload?.playerId === currentPlayerId) {
      return;
    }
    setChatMessages((current) => [
      { id: crypto.randomUUID(), type: "system", tone: "success", text: "Un jugador adivino la palabra" },
      ...current
    ].slice(0, 18));
  }
}

function handleVoiceEvent(payload, setVoice) {
  if (typeof payload?.muted === "boolean") {
    setVoice((previous) => ({ ...previous, muted: payload.muted }));
  }
  if (typeof payload?.speaking === "boolean") {
    setVoice((previous) => ({ ...previous, speaking: payload.speaking }));
  }
}

function safeJson(value) {
  try {
    return JSON.parse(value);
  } catch {
    return { message: value };
  }
}

function maskWord(value) {
  return String(value)
    .split("")
    .map((character) => (character === " " ? " " : "_"))
    .join(" ");
}

function indexScores(scores) {
  const indexed = new Map();
  if (Array.isArray(scores)) {
    scores.forEach((score) => {
      indexed.set(score.playerId, score.score || score.points || 0);
    });
  } else if (scores && typeof scores === "object") {
    Object.entries(scores).forEach(([playerId, score]) => indexed.set(playerId, Number(score) || 0));
  }
  return indexed;
}

function deckLabel(value) {
  return {
    GENERAL: "General",
    MATEMATICAS: "Matematicas",
    SISTEMAS: "Sistemas",
    FISICA: "Fisica"
  }[value];
}

createRoot(document.getElementById("root")).render(<App />);
