import { describe, expect, it } from "vitest";

import {
  clampRoundLimit,
  deckLabel,
  indexScores,
  maskWord,
  parseCustomWords
} from "./game.js";

describe("clampRoundLimit", () => {
  it("mantiene un valor dentro del rango permitido", () => {
    expect(clampRoundLimit("5")).toBe(5);
  });

  it("cae al valor por defecto cuando el texto no es un numero", () => {
    expect(clampRoundLimit("tres")).toBe(3);
  });

  it("acota los extremos para que una partida no quede sin rondas ni sea interminable", () => {
    expect(clampRoundLimit("0")).toBe(1);
    expect(clampRoundLimit("-4")).toBe(1);
    expect(clampRoundLimit("999")).toBe(20);
  });
});

describe("maskWord", () => {
  it("oculta cada letra manteniendo la longitud visible", () => {
    expect(maskWord("gato")).toBe("_ _ _ _");
  });

  /** El numero de palabras es una pista legitima; las letras no. */
  it("conserva los espacios para no ocultar cuantas palabras son", () => {
    expect(maskWord("gato negro")).toBe("_ _ _ _   _ _ _ _ _");
  });
});

describe("parseCustomWords", () => {
  it("acepta comas, puntos y coma y saltos de linea como separadores", () => {
    expect(parseCustomWords("uno, dos; tres\ncuatro")).toEqual(["uno", "dos", "tres", "cuatro"]);
  });

  it("descarta entradas vacias y espacios sobrantes", () => {
    expect(parseCustomWords("  uno ,, ; \n  dos  ")).toEqual(["uno", "dos"]);
  });

  /**
   * Una palabra repetida con distinta capitalizacion saldria dos veces en el
   * mazo y podria tocar dos rondas seguidas. Al deduplicar prevalece la ultima
   * forma escrita, que es la que el jugador acaba de teclear.
   */
  it("elimina duplicados sin distinguir mayusculas", () => {
    expect(parseCustomWords("Arbol, arbol, ARBOL")).toEqual(["ARBOL"]);
    expect(parseCustomWords("uno, UNO, dos")).toEqual(["UNO", "dos"]);
  });

  it("devuelve una lista vacia cuando no hay nada util", () => {
    expect(parseCustomWords("   ,;  ")).toEqual([]);
  });
});

describe("indexScores", () => {
  it("indexa la forma de lista que devuelve el servicio de juego", () => {
    const scores = indexScores([
      { playerId: "p1", score: 30 },
      { playerId: "p2", points: 10 }
    ]);

    expect(scores.get("p1")).toBe(30);
    expect(scores.get("p2")).toBe(10);
  });

  it("indexa tambien la forma de objeto", () => {
    expect(indexScores({ p1: "25" }).get("p1")).toBe(25);
  });

  it("no falla cuando todavia no hay puntajes", () => {
    expect(indexScores(undefined).size).toBe(0);
  });
});

describe("deckLabel", () => {
  it("traduce los mazos conocidos", () => {
    expect(deckLabel("SISTEMAS")).toBe("Sistemas");
    expect(deckLabel("CUSTOM")).toBe("Personalizado");
  });
});
