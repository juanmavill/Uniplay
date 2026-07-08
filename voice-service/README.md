# voice-service

Genera tokens de acceso para el canal de voz de UniPlay y encapsula la
integracion con LiveKit. El frontend nunca consume credenciales ni la API
administrativa de LiveKit directamente.

## Endpoints

### Unirse a voz

```http
POST /voice/token
Content-Type: application/json

{
  "roomCode": "ABC123",
  "playerId": "22222222-2222-2222-2222-222222222222",
  "playerName": "Juan"
}
```

Respuesta exitosa:

```http
201 Created
```

```json
{
  "roomCode": "ABC123",
  "voiceRoomName": "uniplay-ABC123",
  "participantIdentity": "22222222-2222-2222-2222-222222222222",
  "participantName": "Juan",
  "livekitUrl": "ws://localhost:7880",
  "token": "jwt",
  "expiresAt": "2026-07-07T12:30:00Z"
}
```

El canal de voz se crea on-demand cuando el primer cliente usa el token para
conectarse a LiveKit.

### Cambiar estado de microfono

```http
POST /voice/mute
Content-Type: application/json

{
  "roomCode": "ABC123",
  "playerId": "22222222-2222-2222-2222-222222222222",
  "muted": true
}
```

### Reportar indicador de habla

El SDK de LiveKit del cliente detecta participantes activos. El frontend envia
ese cambio a `voice-service` para que el resto del sistema lo consuma por
Redis/STOMP sin conocer detalles de LiveKit.

```http
POST /voice/speaking
Content-Type: application/json

{
  "roomCode": "ABC123",
  "playerId": "22222222-2222-2222-2222-222222222222",
  "speaking": true
}
```

Respuesta exitosa:

```json
{
  "roomCode": "ABC123",
  "voiceRoomName": "uniplay-ABC123",
  "participantIdentity": "22222222-2222-2222-2222-222222222222",
  "speaking": true,
  "changedAt": "2026-07-07T12:00:00Z"
}
```

Respuesta exitosa:

```json
{
  "roomCode": "ABC123",
  "voiceRoomName": "uniplay-ABC123",
  "participantIdentity": "22222222-2222-2222-2222-222222222222",
  "muted": true,
  "changedAt": "2026-07-07T12:00:00Z"
}
```

## Eventos publicados

| Canal Redis | Momento |
|---|---|
| `voz.microfono_actualizado` | Despues de cambiar el estado de mute de un participante |
| `voz.jugador_hablando` | Despues de cambiar el estado de habla de un participante |

## LiveKit local

El `docker-compose.yml` levanta `livekit/livekit-server` en modo desarrollo:

```text
API key: devkey
API secret: secret
```

Estos valores son solo para desarrollo local. En AWS o Azure deben definirse
con variables de entorno y secretos del entorno de despliegue.

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `VOICE_SERVICE_PORT` | `8085` |
| `LIVEKIT_URL` | `ws://localhost:7880` |
| `LIVEKIT_PUBLIC_URL` | `ws://localhost:7880` |
| `LIVEKIT_API_KEY` | `devkey` |
| `LIVEKIT_API_SECRET` | `secret` |
| `VOICE_TOKEN_TTL` | `PT30M` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
