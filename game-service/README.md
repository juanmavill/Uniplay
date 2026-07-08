# game-service

Gestiona rondas, palabras secretas, respuestas por chat y puntajes de UniPlay.

## Endpoints

### Iniciar ronda

```http
POST /games/{codigo}/rounds
```

Respuesta exitosa:

```http
201 Created
Location: /games/{codigo}
```

```json
{
  "roomCode": "ABC123",
  "roundId": "11111111-1111-1111-1111-111111111111",
  "word": "Campus",
  "status": "ACTIVE",
  "startedAt": "2026-07-07T12:00:00Z"
}
```

### Enviar respuesta

```http
POST /games/{codigo}/answers
Content-Type: application/json

{
  "playerId": "22222222-2222-2222-2222-222222222222",
  "answer": "campus"
}
```

Respuesta exitosa:

```json
{
  "roomCode": "ABC123",
  "roundId": "11111111-1111-1111-1111-111111111111",
  "playerId": "22222222-2222-2222-2222-222222222222",
  "correct": true,
  "score": 100,
  "roundStatus": "FINISHED",
  "answeredAt": "2026-07-07T12:00:05Z"
}
```

### Consultar estado

```http
GET /games/{codigo}
```

```json
{
  "roomCode": "ABC123",
  "round": {
    "roundId": "11111111-1111-1111-1111-111111111111",
    "status": "FINISHED",
    "word": "Campus",
    "guessedBy": "22222222-2222-2222-2222-222222222222",
    "startedAt": "2026-07-07T12:00:00Z",
    "finishedAt": "2026-07-07T12:00:05Z"
  },
  "scores": [
    {
      "playerId": "22222222-2222-2222-2222-222222222222",
      "score": 100
    }
  ]
}
```

## Eventos publicados

| Canal Redis | Momento |
|---|---|
| `ronda.iniciada` | Despues de persistir una nueva ronda activa |
| `palabra.adivinada` | Despues de sumar puntos y cerrar la ronda |

## Persistencia

Las sesiones de juego se guardan en Redis con TTL usando la clave
`game-session:{codigo}`. El estado incluye la ronda actual y el acumulado de
puntajes por jugador.

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `GAME_SERVICE_PORT` | `8082` |
| `GAME_POINTS_PER_CORRECT_ANSWER` | `100` |
| `GAME_SESSION_TTL` | `PT2H` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
