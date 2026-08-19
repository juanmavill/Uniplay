import { describe, expect, it } from "vitest";

import {
  clampRoundLimit,
  deckLabel,
  indexScores,
  maskWord,
  parseCustomWords
} from "./game.js";

describe("clampRoundLimit", () => {
  it("keeps a value inside the allowed range", () => {
    expect(clampRoundLimit("5")).toBe(5);
  });

  it("falls back to the default when the text is not a number", () => {
    expect(clampRoundLimit("tres")).toBe(3);
  });

  it("clamps both ends so a match is neither empty nor endless", () => {
    expect(clampRoundLimit("0")).toBe(1);
    expect(clampRoundLimit("-4")).toBe(1);
    expect(clampRoundLimit("999")).toBe(20);
  });
});

describe("maskWord", () => {
  it("hides every letter while keeping the length visible", () => {
    expect(maskWord("gato")).toBe("_ _ _ _");
  });

  /** The number of words is a fair hint; the letters are not. */
  it("keeps spaces so the word count stays visible", () => {
    expect(maskWord("gato negro")).toBe("_ _ _ _   _ _ _ _ _");
  });
});

describe("parseCustomWords", () => {
  it("accepts commas, semicolons and newlines as separators", () => {
    expect(parseCustomWords("uno, dos; tres\ncuatro")).toEqual(["uno", "dos", "tres", "cuatro"]);
  });

  it("drops empty entries and surrounding whitespace", () => {
    expect(parseCustomWords("  uno ,, ; \n  dos  ")).toEqual(["uno", "dos"]);
  });

  /**
   * A word repeated with different capitalisation would sit twice in the deck and
   * could come up in two consecutive rounds. When deduplicating, the last form
   * wins, which is the one the player just typed.
   */
  it("removes duplicates regardless of case", () => {
    expect(parseCustomWords("Arbol, arbol, ARBOL")).toEqual(["ARBOL"]);
    expect(parseCustomWords("uno, UNO, dos")).toEqual(["UNO", "dos"]);
  });

  it("returns an empty list when there is nothing usable", () => {
    expect(parseCustomWords("   ,;  ")).toEqual([]);
  });
});

describe("indexScores", () => {
  it("indexes the list shape returned by the game service", () => {
    const scores = indexScores([
      { playerId: "p1", score: 30 },
      { playerId: "p2", points: 10 }
    ]);

    expect(scores.get("p1")).toBe(30);
    expect(scores.get("p2")).toBe(10);
  });

  it("indexes the object shape as well", () => {
    expect(indexScores({ p1: "25" }).get("p1")).toBe(25);
  });

  it("does not fail before any score exists", () => {
    expect(indexScores(undefined).size).toBe(0);
  });
});

describe("deckLabel", () => {
  it("labels the known decks", () => {
    expect(deckLabel("SISTEMAS")).toBe("Sistemas");
    expect(deckLabel("CUSTOM")).toBe("Personalizado");
  });
});
