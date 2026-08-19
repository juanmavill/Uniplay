import { createClientId } from "./ids.js";

// Maps the domain events arriving over STOMP onto UI state.
export function handleRoundEvent(payload, setChatMessages, currentPlayerId) {
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

export function eventType(payload) {
  return payload?.type || payload?.eventType || payload?.name;
}

export function handleVoiceEvent(payload, setVoice, currentPlayerId) {
  if (payload?.participantIdentity === currentPlayerId && typeof payload?.muted === "boolean") {
    setVoice((previous) => ({ ...previous, muted: payload.muted }));
  }
}

export function initialVoiceState() {
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

export function voiceStatusLabel(voice) {
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

export function removeVoiceAudioElements() {
  document.querySelectorAll("[data-uniplay-voice-audio='remote']").forEach((element) => element.remove());
}

export function safeJson(value) {
  try {
    return JSON.parse(value);
  } catch {
    return { message: value };
  }
}
