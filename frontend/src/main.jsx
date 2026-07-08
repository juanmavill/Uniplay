import React from "react";
import { createRoot } from "react-dom/client";
import { Activity, BarChart3, Brush, Check, Copy, Mic, MicOff, Play, RefreshCw, Send, Users, Vote } from "lucide-react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Room, RoomEvent, Track } from "livekit-client";
import "./styles.css";

const DECKS = ["GENERAL", "MATEMATICAS", "SISTEMAS", "FISICA"];
const MODES = ["CLASSIC", "ALL_DRAW"];
const DEFAULT_KPIS = {
  activeRooms: 0,
  connectedPlayers: 0,
  guessRate: 0,
  averagePlayersPerRoom: 0
};

function App() {
  const [gatewayUrl, setGatewayUrl] = React.useState(() => localStorage.getItem("uniplay.gatewayUrl") || "");
  const [room, setRoom] = React.useState(null);
  const [roomCodeInput, setRoomCodeInput] = React.useState("");
  const [playerName, setPlayerName] = React.useState(() => localStorage.getItem("uniplay.playerName") || "Jugador");
  const [player, setPlayer] = React.useState(null);
  const [players, setPlayers] = React.useState([]);
  const [round, setRound] = React.useState(null);
  const [gameState, setGameState] = React.useState(null);
  const [answer, setAnswer] = React.useState("");
  const [deck, setDeck] = React.useState("SISTEMAS");
  const [mode, setMode] = React.useState("CLASSIC");
  const [candidateId, setCandidateId] = React.useState("");
  const [voice, setVoice] = React.useState({ connected: false, muted: true, speaking: false, roomName: "" });
  const [kpis, setKpis] = React.useState(DEFAULT_KPIS);
  const [events, setEvents] = React.useState([]);
  const [stompState, setStompState] = React.useState("offline");
  const [isBusy, setIsBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const livekitRoomRef = React.useRef(null);

  const api = React.useMemo(() => createApiClient(gatewayUrl), [gatewayUrl]);
  const currentRoomCode = room?.code || roomCodeInput.trim().toUpperCase();

  React.useEffect(() => {
    localStorage.setItem("uniplay.gatewayUrl", gatewayUrl);
  }, [gatewayUrl]);

  React.useEffect(() => {
    localStorage.setItem("uniplay.playerName", playerName);
  }, [playerName]);

  React.useEffect(() => {
    let cancelled = false;
    const loadKpis = async () => {
      try {
        const nextKpis = await api.getKpis();
        if (!cancelled) {
          setKpis(nextKpis);
        }
      } catch {
        if (!cancelled) {
          setKpis(DEFAULT_KPIS);
        }
      }
    };

    loadKpis();
    const intervalId = window.setInterval(loadKpis, 5000);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [api]);

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
          appendEvent(setEvents, "Ronda", safeJson(message.body));
        });
        client.subscribe(`/topic/rooms/${currentRoomCode}/voice`, (message) => {
          appendEvent(setEvents, "Voz", safeJson(message.body));
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
  }, [api, currentRoomCode]);

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

  async function createRoom() {
    await runAction(async () => {
      const createdRoom = await api.createRoom(21);
      setRoom(createdRoom);
      setRoomCodeInput(createdRoom.code);
      setPlayers([]);
      setRound(null);
      setGameState(null);
      appendEvent(setEvents, "Sala", { message: `Sala ${createdRoom.code} creada` });
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
      appendEvent(setEvents, "Jugador", { message: `${joined.playerName} entro a ${joined.code}` });
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
      const nextRound = await api.startRound(currentRoomCode, mode, deck);
      setRound(nextRound);
      setGameState(null);
      appendEvent(setEvents, "Juego", { message: `Ronda ${nextRound.mode} con ${nextRound.deck}` });
    });
  }

  async function submitAnswer() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de responder");
      }
      const result = await api.submitAnswer(currentRoomCode, player.playerId, answer);
      setAnswer("");
      appendEvent(setEvents, "Respuesta", result);
      await refreshGameState();
    });
  }

  async function refreshGameState() {
    if (!currentRoomCode) {
      return;
    }
    const state = await api.getGameState(currentRoomCode);
    setGameState(state);
  }

  async function castVote() {
    await runAction(async () => {
      if (!round?.roundId || !player?.playerId) {
        throw new Error("Necesitas una ronda activa y jugador");
      }
      const result = await api.castVote(currentRoomCode, round.roundId, player.playerId, candidateId);
      appendEvent(setEvents, "Voto", result);
      setCandidateId("");
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
      appendEvent(setEvents, "Voz", { message: `Conectado a ${token.voiceRoomName}` });
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
      appendEvent(setEvents, "Microfono", result);
    });
  }

  async function toggleSpeaking() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de reportar voz");
      }
      const result = await api.setSpeaking(currentRoomCode, player.playerId, !voice.speaking);
      setVoice((previous) => ({ ...previous, speaking: result.speaking }));
      appendEvent(setEvents, "Hablando", result);
    });
  }

  const timer = useRoundTimer(round || gameState?.round);

  return (
    <main className="app-shell">
      <section className="topbar">
        <div>
          <p className="eyebrow">UniPlay</p>
          <h1>Juego colaborativo</h1>
        </div>
        <div className="gateway-field">
          <label htmlFor="gatewayUrl">Gateway</label>
          <input
            id="gatewayUrl"
            value={gatewayUrl}
            placeholder="/"
            onChange={(event) => setGatewayUrl(event.target.value)}
          />
        </div>
      </section>

      {error && <div className="alert" role="alert">{error}</div>}

      <section className="workspace">
        <aside className="side-panel">
          <section className="panel">
            <PanelTitle icon={<Users size={18} />} title="Sala" />
            <div className="form-grid">
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
            </div>
            <div className="button-row">
              <button className="primary" onClick={createRoom} disabled={isBusy}>
                <Play size={16} />
                Crear
              </button>
              <button onClick={joinRoom} disabled={isBusy}>
                <Check size={16} />
                Unirse
              </button>
              <button className="icon-button" aria-label="Copiar codigo" title="Copiar codigo" onClick={() => navigator.clipboard?.writeText(currentRoomCode)}>
                <Copy size={16} />
              </button>
            </div>
            <StatusLine label="Sala" value={currentRoomCode || "Sin sala"} />
            <StatusLine label="STOMP" value={stompState} tone={stompState === "online" ? "good" : "muted"} />
          </section>

          <section className="panel">
            <PanelTitle icon={<Users size={18} />} title="Jugadores" action={<button className="icon-button" onClick={refreshPlayers} title="Actualizar jugadores"><RefreshCw size={16} /></button>} />
            <div className="player-list">
              {players.length === 0 && <p className="empty">Sin jugadores</p>}
              {players.map((item) => (
                <button key={item.playerId} className="player-item" onClick={() => setCandidateId(item.playerId)}>
                  <span>{item.playerName}</span>
                  <small>{shortId(item.playerId)}</small>
                </button>
              ))}
            </div>
          </section>

          <section className="panel">
            <PanelTitle icon={<BarChart3 size={18} />} title="KPIs" />
            <div className="kpi-grid">
              <Metric label="Salas" value={kpis.activeRooms} />
              <Metric label="Jugadores" value={kpis.connectedPlayers} />
              <Metric label="Aciertos" value={`${Math.round((kpis.guessRate || 0) * 100)}%`} />
              <Metric label="Promedio" value={Number(kpis.averagePlayersPerRoom || 0).toFixed(2)} />
            </div>
          </section>
        </aside>

        <section className="play-area">
          <section className="game-toolbar">
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

          <section className="round-strip">
            <div>
              <span className="label">Palabra</span>
              <strong>{round?.word || gameState?.round?.word || "Inicia una ronda"}</strong>
            </div>
            <div>
              <span className="label">Tiempo</span>
              <strong>{timer}</strong>
            </div>
            <div>
              <span className="label">Estado</span>
              <strong>{round?.status || gameState?.round?.status || "LISTO"}</strong>
            </div>
          </section>

          <DrawingBoard roomCode={currentRoomCode} playerId={player?.playerId} gatewayBase={api.baseUrl} />

          <section className="answer-bar">
            <input
              value={answer}
              placeholder="Escribe tu respuesta"
              onChange={(event) => setAnswer(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  submitAnswer();
                }
              }}
            />
            <button className="primary" onClick={submitAnswer} disabled={isBusy || !answer.trim()}>
              <Send size={16} />
              Enviar
            </button>
          </section>
        </section>

        <aside className="side-panel">
          <section className="panel">
            <PanelTitle icon={<Mic size={18} />} title="Voz" />
            <div className="voice-status">
              <span className={voice.connected ? "pulse on" : "pulse"} />
              <div>
                <strong>{voice.connected ? "Conectado" : "Sin voz"}</strong>
                <small>{voice.roomName || "Canal on-demand"}</small>
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
              Indicador hablando
            </label>
          </section>

          <section className="panel">
            <PanelTitle icon={<Vote size={18} />} title="Votacion" />
            <input value={candidateId} placeholder="ID candidato" onChange={(event) => setCandidateId(event.target.value)} />
            <button onClick={castVote} disabled={isBusy || !candidateId.trim()}>
              <Vote size={16} />
              Votar
            </button>
          </section>

          <section className="panel events-panel">
            <PanelTitle icon={<Activity size={18} />} title="Eventos" />
            <div className="event-list">
              {events.length === 0 && <p className="empty">Esperando actividad</p>}
              {events.map((event) => (
                <article key={event.id} className="event-item">
                  <strong>{event.kind}</strong>
                  <span>{event.text}</span>
                </article>
              ))}
            </div>
          </section>
        </aside>
      </section>
    </main>
  );
}

function DrawingBoard({ roomCode, playerId, gatewayBase }) {
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
    drawingRef.current = true;
    previousPointRef.current = pointerPoint(event);
  }

  function stopDrawing() {
    drawingRef.current = false;
    previousPointRef.current = null;
  }

  function moveDrawing(event) {
    if (!drawingRef.current || !previousPointRef.current || !roomCode) {
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
        <PanelTitle icon={<Brush size={18} />} title="Canvas" />
        <div className="tool-group">
          <input type="color" value={color} onChange={(event) => setColor(event.target.value)} aria-label="Color" />
          <input type="range" min="2" max="12" value={width} onChange={(event) => setWidth(Number(event.target.value))} aria-label="Grosor" />
          <button onClick={() => clearCanvas(canvasRef.current)}>Limpiar</button>
        </div>
      </div>
      <canvas
        ref={canvasRef}
        className="drawing-canvas"
        onPointerDown={startDrawing}
        onPointerMove={moveDrawing}
        onPointerUp={stopDrawing}
        onPointerLeave={stopDrawing}
      />
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

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
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
    startRound: (code, mode, deck) => request(normalizedBaseUrl, `/games/${code}/rounds`, { method: "POST", body: { mode, deck } }),
    submitAnswer: (code, playerId, answer) => request(normalizedBaseUrl, `/games/${code}/answers`, { method: "POST", body: { playerId, answer } }),
    getGameState: (code) => request(normalizedBaseUrl, `/games/${code}`),
    castVote: (code, roundId, voterId, candidateId) => request(normalizedBaseUrl, `/games/${code}/rounds/${roundId}/votes`, { method: "POST", body: { voterId, candidateId } }),
    createVoiceToken: (roomCode, playerId, playerName) => request(normalizedBaseUrl, "/voice/token", { method: "POST", body: { roomCode, playerId, playerName } }),
    setMuted: (roomCode, playerId, muted) => request(normalizedBaseUrl, "/voice/mute", { method: "POST", body: { roomCode, playerId, muted } }),
    setSpeaking: (roomCode, playerId, speaking) => request(normalizedBaseUrl, "/voice/speaking", { method: "POST", body: { roomCode, playerId, speaking } }),
    getKpis: () => request(normalizedBaseUrl, "/metrics/kpis")
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

function appendEvent(setEvents, kind, payload) {
  const text = payload?.message || payload?.type || payload?.correct?.toString() || JSON.stringify(payload);
  setEvents((current) => [{ id: crypto.randomUUID(), kind, text }, ...current].slice(0, 12));
}

function safeJson(value) {
  try {
    return JSON.parse(value);
  } catch {
    return { message: value };
  }
}

function shortId(value) {
  if (!value) {
    return "";
  }
  return value.slice(0, 8);
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
