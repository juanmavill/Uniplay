// Player routes: /sala/{code}/jugador/{id}
export function parsePlayerRoute(pathname) {
  const match = pathname.match(/^\/sala\/([^/]+)\/jugador\/([^/]+)\/?$/);
  if (!match) {
    return null;
  }
  return {
    roomCode: decodeURIComponent(match[1]).toUpperCase(),
    playerId: decodeURIComponent(match[2])
  };
}

export function playerPath(roomCode, playerId) {
  return `/sala/${encodeURIComponent(roomCode)}/jugador/${encodeURIComponent(playerId)}`;
}
