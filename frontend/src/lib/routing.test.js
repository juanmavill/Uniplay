import { describe, expect, it } from "vitest";

import { parsePlayerRoute, playerPath } from "./routing.js";

describe("parsePlayerRoute", () => {
  it("extrae sala y jugador de una ruta de jugador", () => {
    expect(parsePlayerRoute("/sala/ABC123/jugador/p-1")).toEqual({
      roomCode: "ABC123",
      playerId: "p-1"
    });
  });

  it("normaliza el codigo de sala a mayusculas", () => {
    expect(parsePlayerRoute("/sala/abc123/jugador/p-1").roomCode).toBe("ABC123");
  });

  it("tolera la barra final", () => {
    expect(parsePlayerRoute("/sala/ABC123/jugador/p-1/")).not.toBeNull();
  });

  it("devuelve null en el lobby y en rutas incompletas", () => {
    expect(parsePlayerRoute("/")).toBeNull();
    expect(parsePlayerRoute("/sala/ABC123")).toBeNull();
    expect(parsePlayerRoute("/otra/cosa")).toBeNull();
  });
});

describe("playerPath", () => {
  /**
   * Cada pestana abre su propia ruta de jugador, de modo que un identificador con
   * caracteres reservados romperia el enlace si no se codificara.
   */
  it("produce una ruta que parsePlayerRoute vuelve a entender", () => {
    const path = playerPath("ABC123", "jugador/raro?x=1");

    expect(parsePlayerRoute(path)).toEqual({
      roomCode: "ABC123",
      playerId: "jugador/raro?x=1"
    });
  });
});
