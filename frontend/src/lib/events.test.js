import { describe, expect, it } from "vitest";

import { eventType, initialVoiceState, safeJson, voiceStatusLabel } from "./events.js";

describe("eventType", () => {
  it("acepta los distintos nombres de campo con que llega el tipo", () => {
    expect(eventType({ type: "WORD_GUESSED" })).toBe("WORD_GUESSED");
    expect(eventType({ eventType: "ROUND_STARTED" })).toBe("ROUND_STARTED");
    expect(eventType({ name: "VOICE_MUTED" })).toBe("VOICE_MUTED");
  });

  it("no falla ante un mensaje vacio", () => {
    expect(eventType(null)).toBeUndefined();
  });
});

describe("safeJson", () => {
  it("interpreta el cuerpo cuando es JSON", () => {
    expect(safeJson('{"a":1}')).toEqual({ a: 1 });
  });

  /** Un mensaje que no sea JSON no debe tumbar la suscripcion STOMP. */
  it("envuelve el texto cuando no lo es", () => {
    expect(safeJson("hola")).toEqual({ message: "hola" });
  });
});

describe("voiceStatusLabel", () => {
  it("describe cada estado del canal de voz", () => {
    expect(voiceStatusLabel({ status: "connecting" })).toBe("Conectando");
    expect(voiceStatusLabel({ status: "reconnecting" })).toBe("Reconectando");
    expect(voiceStatusLabel({ connected: false })).toBe("Sin voz");
    expect(voiceStatusLabel({ connected: true, muted: true })).toBe("Escuchando");
    expect(voiceStatusLabel({ connected: true, muted: false })).toBe("Microfono activo");
  });
});

describe("initialVoiceState", () => {
  /** Entrar con el microfono abierto sin pedirlo seria un problema de privacidad. */
  it("arranca desconectado y silenciado", () => {
    const state = initialVoiceState();

    expect(state.connected).toBe(false);
    expect(state.muted).toBe(true);
  });
});
