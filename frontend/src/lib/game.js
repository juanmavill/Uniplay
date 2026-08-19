// Reglas y vocabulario del juego, sin dependencias de React ni del DOM.
export const DECKS = ["GENERAL", "MATEMATICAS", "SISTEMAS", "FISICA", "CUSTOM"];
export const MODES = ["CLASSIC", "ALL_DRAW"];
export const DEFAULT_ROUND_LIMIT = 3;

export function clampRoundLimit(value) {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    return DEFAULT_ROUND_LIMIT;
  }
  return Math.min(Math.max(parsed, 1), 20);
}

export function maskWord(value) {
  return String(value)
    .split("")
    .map((character) => (character === " " ? " " : "_"))
    .join(" ");
}

export function indexScores(scores) {
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

export function deckLabel(value) {
  return {
    GENERAL: "General",
    MATEMATICAS: "Matematicas",
    SISTEMAS: "Sistemas",
    FISICA: "Fisica",
    CUSTOM: "Personalizado"
  }[value];
}

export function parseCustomWords(value) {
  const words = String(value)
    .split(/[\n,;]+/)
    .map((word) => word.trim())
    .filter(Boolean);
  return [...new Map(words.map((word) => [word.toLocaleLowerCase(), word])).values()];
}
