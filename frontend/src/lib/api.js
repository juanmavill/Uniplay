// HTTP client for the api-gateway.
export function createApiClient(baseUrl) {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");
  return {
    baseUrl: normalizedBaseUrl,
    createRoom: (maxPlayers) => request(normalizedBaseUrl, "/salas", { method: "POST", body: { maxPlayers } }),
    joinRoom: (code, playerName) => request(normalizedBaseUrl, `/salas/${code}/jugadores`, { method: "POST", body: { playerName } }),
    listPlayers: (code) => request(normalizedBaseUrl, `/salas/${code}/jugadores`),
    startRound: (code, mode, deck, drawerId, customWords, participantIds) => request(normalizedBaseUrl, `/games/${code}/rounds`, { method: "POST", body: { mode, deck, drawerId, customWords, participantIds } }),
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
