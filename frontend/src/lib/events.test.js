import { describe, expect, it } from "vitest";

import { eventType, initialVoiceState, safeJson, voiceStatusLabel } from "./events.js";

describe("eventType", () => {
  it("accepts the different field names the type arrives under", () => {
    expect(eventType({ type: "WORD_GUESSED" })).toBe("WORD_GUESSED");
    expect(eventType({ eventType: "ROUND_STARTED" })).toBe("ROUND_STARTED");
    expect(eventType({ name: "VOICE_MUTED" })).toBe("VOICE_MUTED");
  });

  it("does not fail on an empty message", () => {
    expect(eventType(null)).toBeUndefined();
  });
});

describe("safeJson", () => {
  it("parses the body when it is JSON", () => {
    expect(safeJson('{"a":1}')).toEqual({ a: 1 });
  });

  /** A non-JSON message must not tear down the STOMP subscription. */
  it("wraps the text when it is not", () => {
    expect(safeJson("hola")).toEqual({ message: "hola" });
  });
});

describe("voiceStatusLabel", () => {
  it("describes every state of the voice channel", () => {
    expect(voiceStatusLabel({ status: "connecting" })).toBe("Conectando");
    expect(voiceStatusLabel({ status: "reconnecting" })).toBe("Reconectando");
    expect(voiceStatusLabel({ connected: false })).toBe("Sin voz");
    expect(voiceStatusLabel({ connected: true, muted: true })).toBe("Escuchando");
    expect(voiceStatusLabel({ connected: true, muted: false })).toBe("Microfono activo");
  });
});

describe("initialVoiceState", () => {
  /** Joining with an open microphone unasked would be a privacy problem. */
  it("starts disconnected and muted", () => {
    const state = initialVoiceState();

    expect(state.connected).toBe(false);
    expect(state.muted).toBe(true);
  });
});
