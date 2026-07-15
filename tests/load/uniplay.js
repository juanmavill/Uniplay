import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080';
const profile = __ENV.K6_PROFILE || 'smoke';

const stagesByProfile = {
  smoke: [
    { duration: '5s', target: 1 },
    { duration: '10s', target: 3 },
    { duration: '5s', target: 0 },
  ],
  load: [
    { duration: '15s', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '1m', target: 100 },
    { duration: '15s', target: 0 },
  ],
  stress: [
    { duration: '30s', target: 100 },
    { duration: '1m', target: 250 },
    { duration: '1m', target: 500 },
    { duration: '2m', target: 500 },
    { duration: '30s', target: 0 },
  ],
};

if (!stagesByProfile[profile]) {
  throw new Error(`Perfil desconocido: ${profile}. Usa smoke, load o stress.`);
}

export const options = {
  scenarios: {
    players: {
      executor: 'ramping-vus',
      gracefulRampDown: '15s',
      stages: stagesByProfile[profile],
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    uniplay_flow_success: ['rate>0.99'],
  },
};

const flowSuccess = new Rate('uniplay_flow_success');
let roomCode;

function headers() {
  const vuIndex = __VU - 1;
  return {
    'Content-Type': 'application/json',
    'X-Forwarded-For': `10.200.${Math.floor(vuIndex / 254)}.${(vuIndex % 254) + 1}`,
  };
}

function readJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function enterRoom() {
  const createResponse = http.post(
    `${baseUrl}/salas`,
    JSON.stringify({ maxPlayers: 8 }),
    { headers: headers(), tags: { operation: 'create_room' } },
  );
  const room = readJson(createResponse);
  const created = check(createResponse, {
    'sala creada': (response) => response.status === 201,
    'codigo de sala recibido': () => Boolean(room && room.code),
  });

  if (!created) {
    flowSuccess.add(false);
    return false;
  }

  roomCode = room.code;
  const joinResponse = http.post(
    `${baseUrl}/salas/${roomCode}/jugadores`,
    JSON.stringify({ playerName: `Carga-${__VU}` }),
    { headers: headers(), tags: { operation: 'join_room' } },
  );
  const joined = check(joinResponse, {
    'jugador unido': (response) => response.status === 200,
    'id de jugador recibido': (response) => Boolean(readJson(response)?.playerId),
  });
  flowSuccess.add(joined);
  return joined;
}

export default function () {
  if (!roomCode && !enterRoom()) {
    sleep(2);
    return;
  }

  const playersResponse = http.get(`${baseUrl}/salas/${roomCode}/jugadores`, {
    headers: headers(),
    tags: { operation: 'list_players' },
  });
  const listed = check(playersResponse, {
    'jugadores consultados': (response) => response.status === 200,
    'lista de jugadores presente': (response) => Array.isArray(readJson(response)?.players),
  });
  flowSuccess.add(listed);

  sleep(1 + Math.random() * 2);
}
