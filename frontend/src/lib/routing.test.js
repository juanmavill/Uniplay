import { describe, expect, it } from "vitest";

import { parsePlayerRoute, playerPath } from "./routing.js";

describe("parsePlayerRoute", () => {
  it("extracts room and player from a player route", () => {
    expect(parsePlayerRoute("/sala/ABC123/jugador/p-1")).toEqual({
      roomCode: "ABC123",
      playerId: "p-1"
    });
  });

  it("normalises the room code to upper case", () => {
    expect(parsePlayerRoute("/sala/abc123/jugador/p-1").roomCode).toBe("ABC123");
  });

  it("tolerates a trailing slash", () => {
    expect(parsePlayerRoute("/sala/ABC123/jugador/p-1/")).not.toBeNull();
  });

  it("returns null for the lobby and for incomplete routes", () => {
    expect(parsePlayerRoute("/")).toBeNull();
    expect(parsePlayerRoute("/sala/ABC123")).toBeNull();
    expect(parsePlayerRoute("/otra/cosa")).toBeNull();
  });
});

describe("playerPath", () => {
  /**
   * Each tab opens its own player route, so an identifier containing reserved
   * characters would break the link if it were not encoded.
   */
  it("produces a route that parsePlayerRoute reads back", () => {
    const path = playerPath("ABC123", "jugador/raro?x=1");

    expect(parsePlayerRoute(path)).toEqual({
      roomCode: "ABC123",
      playerId: "jugador/raro?x=1"
    });
  });
});
