import React from "react";
import { createRoot } from "react-dom/client";
import { Brush, Check, Copy, Eraser, Mic, MicOff, Play, RefreshCw, Send, Trophy, Users, Volume2, Vote } from "lucide-react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { Room, RoomEvent, Track } from "livekit-client";
import "./styles.css";

const DECKS = ["GENERAL", "MATEMATICAS", "SISTEMAS", "FISICA", "CUSTOM"];
const MODES = ["CLASSIC", "ALL_DRAW"];
const DEFAULT_ROUND_LIMIT = 3;
const INITIAL_PARAMS = new URLSearchParams(window.location.search);
const INITIAL_ROUTE = parsePlayerRoute(window.location.pathname);

function createClientId() {
  if (typeof crypto?.randomUUID === "function") {
    return crypto.randomUUID();
  }
  if (typeof crypto?.getRandomValues === "function") {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    return [...bytes].map((byte, index) => {
      const value = byte.toString(16).padStart(2, "0");
      return [4, 6, 8, 10].includes(index) ? `-${value}` : value;
    }).join("");
  }
  return `client-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function parsePlayerRoute(pathname) {
  const match = pathname.match(/^\/sala\/([^/]+)\/jugador\/([^/]+)\/?$/);
  if (!match) {
    return null;
  }
  return {
    roomCode: decodeURIComponent(match[1]).toUpperCase(),
    playerId: decodeURIComponent(match[2])
  };
}

function playerPath(roomCode, playerId) {
  return `/sala/${encodeURIComponent(roomCode)}/jugador/${encodeURIComponent(playerId)}`;
}

function App() {
  const [gatewayUrl] = React.useState(() => localStorage.getItem("uniplay.gatewayUrl") || "");
  const [route, setRoute] = React.useState(INITIAL_ROUTE);
  const [room, setRoom] = React.useState(null);
  const [roomCodeInput, setRoomCodeInput] = React.useState(() => INITIAL_ROUTE?.roomCode || INITIAL_PARAMS.get("room")?.toUpperCase() || "");
  const [playerName, setPlayerName] = React.useState(() => (
    INITIAL_PARAMS.get("name")
    || sessionStorage.getItem("uniplay.playerName")
    || localStorage.getItem("uniplay.playerName")
    || "Jugador"
  ));
  const [player, setPlayer] = React.useState(null);
  const [players, setPlayers] = React.useState([]);
  const [round, setRound] = React.useState(null);
  const [gameState, setGameState] = React.useState(null);
  const [answer, setAnswer] = React.useState("");
  const [chatMessages, setChatMessages] = React.useState([]);
  const [deck, setDeck] = React.useState("SISTEMAS");
  const [customWordsText, setCustomWordsText] = React.useState(() => localStorage.getItem("uniplay.customWords") || "");
  const [mode, setMode] = React.useState("CLASSIC");
  const [roundLimit, setRoundLimit] = React.useState(() => clampRoundLimit(localStorage.getItem("uniplay.roundLimit") || DEFAULT_ROUND_LIMIT));
  const [roundsStarted, setRoundsStarted] = React.useState(0);
  const [candidateId, setCandidateId] = React.useState("");
  const [voice, setVoice] = React.useState(initialVoiceState);
  const [stompState, setStompState] = React.useState("offline");
  const [turnNumber, setTurnNumber] = React.useState(0);
  const [isBusy, setIsBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const livekitRoomRef = React.useRef(null);
  const localSpeakingRef = React.useRef(false);
  const countedRoundIdsRef = React.useRef(new Set());

  const api = React.useMemo(() => createApiClient(gatewayUrl), [gatewayUrl]);
  const playerRoute = route;
  const routeRoomCode = playerRoute?.roomCode;
  const routePlayerId = playerRoute?.playerId;
  const currentRoomCode = routeRoomCode || room?.code || roomCodeInput.trim().toUpperCase();
  const activeRound = gameState?.round || round;
  const roundIsActive = activeRound?.status === "ACTIVE";
  const timer = useRoundTimer(activeRound);
  const scores = React.useMemo(() => indexScores(gameState?.scores), [gameState]);
  const customWords = React.useMemo(() => parseCustomWords(customWordsText), [customWordsText]);
  const customDeckIsValid = deck !== "CUSTOM"
    || (customWords.length >= 3 && customWords.length <= 100 && customWords.every((word) => word.length <= 40));
  const hostPlayer = players[0] || null;
  const isHost = Boolean(player?.playerId && hostPlayer?.playerId === player.playerId);
  const matchComplete = roundsStarted >= roundLimit && !roundIsActive;
  const activeMode = activeRound?.mode || mode;
  const isAllDrawMode = activeMode === "ALL_DRAW";
  const turnDrawer = players.length > 0 ? players[turnNumber % players.length] : null;
  const currentDrawer = activeRound?.drawerId
    ? players.find((item) => item.playerId === activeRound.drawerId) || turnDrawer
    : turnDrawer;
  const isDrawer = Boolean(player?.playerId && roundIsActive && (isAllDrawMode || currentDrawer?.playerId === player.playerId));
  const canGuess = Boolean(player?.playerId && roundIsActive && !isDrawer);
  const visibleWord = activeRound?.word
    ? isDrawer
      ? activeRound.word
      : maskWord(activeRound.word)
    : "------";

  React.useEffect(() => {
    sessionStorage.setItem("uniplay.playerName", playerName);
    localStorage.setItem("uniplay.playerName", playerName);
  }, [playerName]);

  React.useEffect(() => {
    localStorage.setItem("uniplay.roundLimit", String(roundLimit));
  }, [roundLimit]);

  React.useEffect(() => {
    localStorage.setItem("uniplay.customWords", customWordsText);
  }, [customWordsText]);

  React.useEffect(() => {
    const handlePopState = () => setRoute(parsePlayerRoute(window.location.pathname));
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  React.useEffect(() => {
    if (!routeRoomCode || !routePlayerId) {
      return undefined;
    }

    let cancelled = false;
    const loadPlayerRoute = async () => {
      setError("");
      try {
        const result = await api.listPlayers(routeRoomCode);
        if (cancelled) {
          return;
        }
        const routePlayer = (result.players || []).find((item) => item.playerId === routePlayerId);
        if (!routePlayer) {
          throw new Error("No se encontro este jugador en la sala");
        }
        setRoom({ code: routeRoomCode });
        setRoomCodeInput(routeRoomCode);
        setPlayer({ playerId: routePlayer.playerId, playerName: routePlayer.playerName });
        setPlayerName(routePlayer.playerName);
        setPlayers(result.players || []);

        try {
          const state = await api.getGameState(routeRoomCode, routePlayer.playerId);
          if (!cancelled) {
            setGameState(state);
            setRound(state.round);
          }
        } catch {
          if (!cancelled) {
            setGameState(null);
            setRound(null);
          }
        }
      } catch (loadError) {
        if (!cancelled) {
          setPlayer(null);
          setPlayers([]);
          setGameState(null);
          setRound(null);
          setError(loadError.message || "No se pudo cargar este jugador");
        }
      }
    };

    loadPlayerRoute();
    return () => {
      cancelled = true;
    };
  }, [api, routeRoomCode, routePlayerId]);

  React.useEffect(() => {
    if (players.length > 0 && turnNumber >= players.length) {
      setTurnNumber(0);
    }
  }, [players.length, turnNumber]);

  React.useEffect(() => {
    if (!activeRound?.roundId || activeRound.status !== "ACTIVE") {
      return;
    }
    if (countedRoundIdsRef.current.has(activeRound.roundId)) {
      return;
    }
    countedRoundIdsRef.current.add(activeRound.roundId);
    setRoundsStarted((current) => Math.min(current + 1, roundLimit));
  }, [activeRound?.roundId, activeRound?.status, roundLimit]);

  React.useEffect(() => {
    if (!playerRoute || !currentRoomCode) {
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
  }, [api, currentRoomCode, playerRoute]);

  React.useEffect(() => {
    if (!playerRoute || !currentRoomCode) {
      setStompState("offline");
      return undefined;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${api.baseUrl}/ws`),
      reconnectDelay: 3000,
      onConnect: () => {
        setStompState("online");
        client.subscribe(`/topic/rooms/${currentRoomCode}/rounds`, (message) => {
          const payload = safeJson(message.body);
          handleRoundEvent(payload, setChatMessages, player?.playerId);
          if (eventType(payload) === "ROUND_STARTED") {
            window.dispatchEvent(new CustomEvent("uniplay:clear-canvas"));
          }
          if (["ROUND_STARTED", "WORD_GUESSED", "ROUND_FINISHED"].includes(eventType(payload))) {
            window.setTimeout(() => refreshGameState(), 150);
          }
        });
        client.subscribe(`/topic/rooms/${currentRoomCode}/voice`, (message) => {
          handleVoiceEvent(safeJson(message.body), setVoice, player?.playerId);
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
  }, [api, currentRoomCode, player?.playerId, playerRoute]);

  React.useEffect(() => {
    if (!roundIsActive || !activeRound?.endsAt || !activeRound?.roundId || !currentRoomCode) {
      return undefined;
    }
    const delay = Math.max(0, new Date(activeRound.endsAt).getTime() - Date.now()) + 400;
    const timeoutId = window.setTimeout(async () => {
      try {
        await api.expireRound(currentRoomCode, activeRound.roundId);
        addSystemMessage("Tiempo agotado");
        await refreshGameState();
      } catch {
        await refreshGameState();
      }
    }, delay);
    return () => window.clearTimeout(timeoutId);
  }, [api, currentRoomCode, activeRound?.roundId, activeRound?.endsAt, roundIsActive]);

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
      { id: createClientId(), type: "system", tone, text },
      ...current
    ].slice(0, 18));
  }

  function navigateToPlayer(code, playerId) {
    const normalizedCode = code.trim().toUpperCase();
    const path = playerPath(normalizedCode, playerId);
    window.history.pushState({}, "", path);
    setRoute({ roomCode: normalizedCode, playerId });
  }

  function goToLobby() {
    disconnectVoice();
    window.history.pushState({}, "", "/");
    setRoute(null);
    setRoom(null);
    setPlayer(null);
    setPlayers([]);
    setRound(null);
    setGameState(null);
    setChatMessages([]);
    setRoundsStarted(0);
    countedRoundIdsRef.current = new Set();
    setStompState("offline");
  }

  async function createRoom() {
    await runAction(async () => {
      const createdRoom = await api.createRoom(21);
      const joined = await api.joinRoom(createdRoom.code, playerName.trim() || "Jugador");
      setTurnNumber(0);
      setRoundsStarted(0);
      countedRoundIdsRef.current = new Set();
      setChatMessages([{ id: createClientId(), type: "system", tone: "system", text: `Sala ${joined.code} creada` }]);
      navigateToPlayer(joined.code, joined.playerId);
    });
  }

  async function joinRoom() {
    await runAction(async () => {
      const code = roomCodeInput.trim().toUpperCase();
      if (!code) {
        throw new Error("Ingresa o crea un codigo de sala");
      }
      const joined = await api.joinRoom(code, playerName.trim() || "Jugador");
      addSystemMessage(`${joined.playerName} entro a la sala`);
      navigateToPlayer(joined.code, joined.playerId);
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
      if (roundIsActive) {
        throw new Error("La ronda actual sigue activa");
      }
      if (!isHost) {
        throw new Error("Solo el host puede iniciar rondas");
      }
      if (matchComplete) {
        throw new Error("La partida ya alcanzo el numero de rondas configurado");
      }
      if (!customDeckIsValid) {
        throw new Error("El mazo personalizado requiere entre 3 y 100 palabras unicas de maximo 40 caracteres");
      }
      const nextTurnNumber = activeRound && players.length > 0 ? (turnNumber + 1) % players.length : turnNumber;
      const nextDrawer = players.length > 0 ? players[nextTurnNumber] : null;
      const nextRound = await api.startRound(
        currentRoomCode,
        mode,
        deck,
        mode === "ALL_DRAW" ? null : nextDrawer?.playerId,
        deck === "CUSTOM" ? customWords : null
      );
      const nextState = await api.getGameState(currentRoomCode, player?.playerId);
      const displayRound = Math.min(roundsStarted + 1, roundLimit);
      setTurnNumber(nextTurnNumber);
      setRound(nextState.round || nextRound);
      setGameState(nextState);
      window.dispatchEvent(new CustomEvent("uniplay:clear-canvas"));
      const drawerName = isAllDrawMode ? "todos" : nextDrawer?.playerName || "turno asignado";
      addSystemMessage(`Ronda ${displayRound} de ${roundLimit}. Dibuja ${drawerName}.`);
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
              id: createClientId(),
              type: "system",
              tone: "success",
              text: `${player.playerName} adivino la palabra`
            }
          : {
              id: createClientId(),
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
    setRound(state.round);
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
      livekitRoomRef.current?.disconnect();
      removeVoiceAudioElements();
      const lkRoom = new Room({ adaptiveStream: true, dynacast: true });
      setVoice((previous) => ({ ...previous, status: "connecting", permissionDenied: false }));
      void lkRoom.startAudio();
      lkRoom.on(RoomEvent.TrackSubscribed, (track) => {
        if (track.kind === Track.Kind.Audio) {
          const element = track.attach();
          element.autoplay = true;
          element.playsInline = true;
          element.dataset.uniplayVoiceAudio = "remote";
          document.body.appendChild(element);
        }
      });
      lkRoom.on(RoomEvent.TrackUnsubscribed, (track) => {
        track.detach().forEach((element) => element.remove());
      });
      lkRoom.on(RoomEvent.Disconnected, () => {
        removeVoiceAudioElements();
        livekitRoomRef.current = null;
        localSpeakingRef.current = false;
        setVoice(initialVoiceState());
      });
      lkRoom.on(RoomEvent.ConnectionStateChanged, (status) => {
        setVoice((previous) => ({
          ...previous,
          connected: status === "connected" || status === "reconnecting" || status === "signalReconnecting",
          status
        }));
      });
      lkRoom.on(RoomEvent.AudioPlaybackStatusChanged, (playing) => {
        setVoice((previous) => ({ ...previous, audioBlocked: !playing }));
      });
      const updateParticipantCount = () => {
        setVoice((previous) => ({ ...previous, participantCount: lkRoom.remoteParticipants.size + 1 }));
      };
      lkRoom.on(RoomEvent.ParticipantConnected, updateParticipantCount);
      lkRoom.on(RoomEvent.ParticipantDisconnected, updateParticipantCount);
      lkRoom.on(RoomEvent.ActiveSpeakersChanged, (participants) => {
        const speakingNames = participants.map((participant) => participant.name || "Jugador");
        const localSpeaking = participants.some((participant) => participant.identity === player.playerId);
        setVoice((previous) => ({ ...previous, speakingNames }));
        if (localSpeaking !== localSpeakingRef.current) {
          localSpeakingRef.current = localSpeaking;
          api.setSpeaking(currentRoomCode, player.playerId, localSpeaking).catch(() => {});
        }
      });
      lkRoom.on(RoomEvent.MediaDevicesError, () => {
        setVoice((previous) => ({ ...previous, muted: true, permissionDenied: true }));
      });

      try {
        const token = await api.createVoiceToken(currentRoomCode, player.playerId, player.playerName);
        await lkRoom.connect(token.livekitUrl, token.token);
        livekitRoomRef.current = lkRoom;
        setVoice((previous) => ({
          ...previous,
          connected: true,
          muted: true,
          roomName: token.voiceRoomName,
          status: "connected",
          participantCount: lkRoom.remoteParticipants.size + 1
        }));
        await lkRoom.startAudio();
        try {
          await lkRoom.localParticipant.setMicrophoneEnabled(true);
          setVoice((previous) => ({ ...previous, muted: false, permissionDenied: false }));
          await api.setMuted(currentRoomCode, player.playerId, false).catch(() => {});
        } catch (microphoneError) {
          setVoice((previous) => ({ ...previous, muted: true, permissionDenied: true }));
          throw new Error("Conectado para escuchar. El navegador no permitio usar el microfono");
        }
      } catch (connectionError) {
        if (!livekitRoomRef.current) {
          lkRoom.disconnect();
          setVoice(initialVoiceState());
        }
        throw connectionError;
      }
    });
  }

  function disconnectVoice() {
    livekitRoomRef.current?.disconnect();
    livekitRoomRef.current = null;
    localSpeakingRef.current = false;
    removeVoiceAudioElements();
    setVoice(initialVoiceState());
  }

  async function toggleMute() {
    await runAction(async () => {
      if (!player?.playerId || !currentRoomCode) {
        throw new Error("Unete a una sala antes de cambiar microfono");
      }
      if (!voice.connected || !livekitRoomRef.current) {
        throw new Error("Entra al canal de voz antes de activar el microfono");
      }
      const nextMuted = !voice.muted;
      await livekitRoomRef.current.localParticipant.setMicrophoneEnabled(!nextMuted);
      setVoice((previous) => ({ ...previous, muted: nextMuted, permissionDenied: false }));
      await api.setMuted(currentRoomCode, player.playerId, nextMuted).catch(() => {});
    });
  }

  async function resumeVoiceAudio() {
    await runAction(async () => {
      if (!livekitRoomRef.current) {
        throw new Error("Entra al canal de voz antes de activar el audio");
      }
      await livekitRoomRef.current.startAudio();
      setVoice((previous) => ({ ...previous, audioBlocked: false }));
    });
  }

  if (!playerRoute) {
    return (
      <main className="app-shell lobby-shell">
        <section className="lobby-panel">
          <div>
            <p className="eyebrow">UniPlay</p>
            <h1>Entrar a partida</h1>
          </div>
          {error && <div className="alert" role="alert">{error}</div>}
          <div className="lobby-form">
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
                placeholder="ABC123"
              />
            </label>
            <label>
              Rondas
              <input
                type="number"
                min="1"
                max="20"
                value={roundLimit}
                onChange={(event) => setRoundLimit(clampRoundLimit(event.target.value))}
              />
            </label>
          </div>
          <div className="button-row">
            <button className="primary" onClick={createRoom} disabled={isBusy}>
              <Play size={16} />
              Crear sala
            </button>
            <button onClick={joinRoom} disabled={isBusy || !roomCodeInput.trim()}>
              <Check size={16} />
              Unirse
            </button>
          </div>
        </section>
      </main>
    );
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
          <section className="panel room-panel session-panel">
            <PanelTitle icon={<Users size={18} />} title="Sala" />
            <StatusLine label="Codigo" value={currentRoomCode || "Sin sala"} />
            <StatusLine label="Tu jugador" value={player?.playerName || "Cargando"} tone={player?.playerId ? "good" : "muted"} />
            <StatusLine label="Host" value={hostPlayer?.playerName || "Pendiente"} tone={isHost ? "good" : "muted"} />
            <StatusLine label="Tiempo real" value={stompState} tone={stompState === "online" ? "good" : "muted"} />
            <div className="button-row">
              <button onClick={goToLobby}>
                Lobby
              </button>
            </div>
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
            <div>
              <span>Ronda</span>
              <strong>{isHost ? `${roundsStarted}/${roundLimit}` : roundsStarted || "--"}</strong>
            </div>
          </section>

          {isHost ? (
            <section className="match-controls">
              <div className="segmented">
                {MODES.map((item) => (
                  <button key={item} className={mode === item ? "selected" : ""} onClick={() => setMode(item)} disabled={roundIsActive || roundsStarted > 0}>
                    {item === "CLASSIC" ? "Clasico" : "Todos dibujan"}
                  </button>
                ))}
              </div>
              <select value={deck} onChange={(event) => setDeck(event.target.value)} aria-label="Mazo" disabled={roundIsActive || roundsStarted > 0}>
                {DECKS.map((item) => <option key={item} value={item}>{deckLabel(item)}</option>)}
              </select>
              {deck === "CUSTOM" && (
                <label className="custom-deck-control">
                  Palabras personalizadas
                  <textarea
                    rows="3"
                    value={customWordsText}
                    onChange={(event) => setCustomWordsText(event.target.value)}
                    disabled={roundIsActive || roundsStarted > 0}
                    placeholder="cohete, castillo, dragon"
                  />
                  <small className={customDeckIsValid ? "good" : "invalid"}>{customWords.length}/100</small>
                </label>
              )}
              <label className="round-count-control">
                Rondas
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={roundLimit}
                  onChange={(event) => setRoundLimit(clampRoundLimit(event.target.value))}
                  disabled={roundIsActive || roundsStarted > 0}
                />
              </label>
              <button className="primary" onClick={startRound} disabled={isBusy || !currentRoomCode || roundIsActive || players.length === 0 || matchComplete || !customDeckIsValid}>
                <Play size={16} />
                {matchComplete ? "Finalizada" : activeRound ? "Siguiente" : "Ronda"}
              </button>
            </section>
          ) : (
            <section className="match-controls viewer-controls">
              <StatusLine label="Modo" value={activeMode === "CLASSIC" ? "Clasico" : "Todos dibujan"} />
              <StatusLine label="Mazo" value={deckLabel(deck)} />
              <StatusLine label="Rondas" value={roundsStarted || "Esperando host"} />
            </section>
          )}

          <DrawingBoard
            roomCode={currentRoomCode}
            playerId={player?.playerId}
            gatewayBase={api.baseUrl}
            canDraw={Boolean(roundIsActive && (isDrawer || isAllDrawMode))}
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
                <strong>{voiceStatusLabel(voice)}</strong>
                <small>{voice.connected ? `${voice.participantCount} en voz` : voice.roomName || "Canal de sala"}</small>
              </div>
            </div>
            {voice.speakingNames.length > 0 && (
              <p className="voice-speaking">Hablando: {voice.speakingNames.join(", ")}</p>
            )}
            {voice.permissionDenied && <p className="voice-warning">Permiso de microfono bloqueado</p>}
            <div className="button-row">
              {voice.connected ? (
                <button onClick={disconnectVoice} disabled={isBusy}>
                  Salir de voz
                </button>
              ) : (
                <button className="primary" onClick={connectVoice} disabled={isBusy || !player?.playerId}>
                  <Mic size={16} />
                  Entrar a voz
                </button>
              )}
              <button onClick={toggleMute} disabled={isBusy || !voice.connected}>
                {voice.muted ? <MicOff size={16} /> : <Mic size={16} />}
                {voice.muted ? "Activar microfono" : "Silenciar"}
              </button>
              {voice.audioBlocked && (
                <button onClick={resumeVoiceAudio} disabled={isBusy}>
                  <Volume2 size={16} />
                  Activar audio
                </button>
              )}
            </div>
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
    const handler = () => {
      drawingRef.current = false;
      previousPointRef.current = null;
      clearCanvas(canvasRef.current);
    };
    window.addEventListener("uniplay:clear-canvas", handler);
    return () => window.removeEventListener("uniplay:clear-canvas", handler);
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

function clampRoundLimit(value) {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    return DEFAULT_ROUND_LIMIT;
  }
  return Math.min(Math.max(parsed, 1), 20);
}

function createApiClient(baseUrl) {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");
  return {
    baseUrl: normalizedBaseUrl,
    createRoom: (maxPlayers) => request(normalizedBaseUrl, "/salas", { method: "POST", body: { maxPlayers } }),
    joinRoom: (code, playerName) => request(normalizedBaseUrl, `/salas/${code}/jugadores`, { method: "POST", body: { playerName } }),
    listPlayers: (code) => request(normalizedBaseUrl, `/salas/${code}/jugadores`),
    startRound: (code, mode, deck, drawerId, customWords) => request(normalizedBaseUrl, `/games/${code}/rounds`, { method: "POST", body: { mode, deck, drawerId, customWords } }),
    submitAnswer: (code, playerId, answer) => request(normalizedBaseUrl, `/games/${code}/answers`, { method: "POST", body: { playerId, answer } }),
    getGameState: (code, viewerPlayerId) => request(normalizedBaseUrl, `/games/${code}${viewerPlayerId ? `?viewerPlayerId=${encodeURIComponent(viewerPlayerId)}` : ""}`),
    expireRound: (code, roundId) => request(normalizedBaseUrl, `/games/${code}/rounds/${roundId}/timeout`, { method: "POST" }),
    castVote: (code, roundId, voterId, candidateId) => request(normalizedBaseUrl, `/games/${code}/rounds/${roundId}/votes`, { method: "POST", body: { voterId, candidateId } }),
    createVoiceToken: (roomCode, playerId, playerName) => request(normalizedBaseUrl, "/voice/token", { method: "POST", body: { roomCode, playerId, playerName } }),
    setMuted: (roomCode, playerId, muted) => request(normalizedBaseUrl, "/voice/mute", { method: "POST", body: { roomCode, playerId, muted } }),
    setSpeaking: (roomCode, playerId, speaking) => request(normalizedBaseUrl, "/voice/speaking", { method: "POST", body: { roomCode, playerId, speaking } }),
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

function handleRoundEvent(payload, setChatMessages, currentPlayerId) {
  const type = eventType(payload);
  if (type === "WORD_GUESSED") {
    if (payload?.playerId === currentPlayerId) {
      return;
    }
    setChatMessages((current) => [
      { id: createClientId(), type: "system", tone: "success", text: "Un jugador adivino la palabra" },
      ...current
    ].slice(0, 18));
  }
}

function eventType(payload) {
  return payload?.type || payload?.eventType || payload?.name;
}

function handleVoiceEvent(payload, setVoice, currentPlayerId) {
  if (payload?.participantIdentity === currentPlayerId && typeof payload?.muted === "boolean") {
    setVoice((previous) => ({ ...previous, muted: payload.muted }));
  }
}

function initialVoiceState() {
  return {
    connected: false,
    muted: true,
    roomName: "",
    status: "disconnected",
    participantCount: 0,
    speakingNames: [],
    audioBlocked: false,
    permissionDenied: false
  };
}

function voiceStatusLabel(voice) {
  if (voice.status === "connecting") {
    return "Conectando";
  }
  if (voice.status === "reconnecting" || voice.status === "signalReconnecting") {
    return "Reconectando";
  }
  if (!voice.connected) {
    return "Sin voz";
  }
  return voice.muted ? "Escuchando" : "Microfono activo";
}

function removeVoiceAudioElements() {
  document.querySelectorAll("[data-uniplay-voice-audio='remote']").forEach((element) => element.remove());
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
    FISICA: "Fisica",
    CUSTOM: "Personalizado"
  }[value];
}

function parseCustomWords(value) {
  const words = String(value)
    .split(/[\n,;]+/)
    .map((word) => word.trim())
    .filter(Boolean);
  return [...new Map(words.map((word) => [word.toLocaleLowerCase(), word])).values()];
}

createRoot(document.getElementById("root")).render(<App />);
