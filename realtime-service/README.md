# realtime-service

Distribuye eventos en tiempo real hacia los navegadores usando STOMP sobre
SockJS. Para HU-04, transporta deltas de canvas; no calcula reglas de juego ni
puntajes.

## WebSocket

Endpoint SockJS:

```text
/ws
```

Destino para publicar deltas desde el cliente:

```text
/app/rooms/{roomCode}/draw
```

Topic para recibir deltas de la sala:

```text
/topic/rooms/{roomCode}/draw
```

Topic para recibir eventos de ronda y temporizador:

```text
/topic/rooms/{roomCode}/rounds
```

Eventos reenviados desde Redis:

| Canal Redis | Tipo STOMP |
|---|---|
| `ronda.iniciada` | `ROUND_STARTED` |
| `ronda.terminada` | `ROUND_FINISHED` |
| `palabra.adivinada` | `WORD_GUESSED` |
| `voto.emitido` | `VOTE_CAST` |

Para sincronizar temporizadores, los clientes deben usar `startedAt` y `endsAt`
del evento `ROUND_STARTED` como fuente de verdad y calcular el contador local.
Los votos del modo `ALL_DRAW` llegan al mismo topic con `voterId`,
`candidateId` y `tallies`.

Payload:

```json
{
  "playerId": "11111111-1111-1111-1111-111111111111",
  "fromX": 0.1,
  "fromY": 0.2,
  "toX": 0.3,
  "toY": 0.4,
  "color": "#00FFAA",
  "width": 4
}
```

Las coordenadas son normalizadas entre `0` y `1`. Esto permite que clientes con
distintos tamanos de canvas reconstruyan el trazo localmente sin enviar el
canvas completo.

## Responsabilidad

`realtime-service` solo valida y distribuye deltas. Las reglas de juego,
palabras, puntajes y temporizador pertenecen a `game-service`.

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `REALTIME_SERVICE_PORT` | `8083` |
| `REALTIME_WEBSOCKET_ENDPOINT` | `/ws` |
| `REALTIME_ALLOWED_ORIGINS` | `*` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
