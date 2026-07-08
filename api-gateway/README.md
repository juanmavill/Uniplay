# api-gateway

Punto unico de entrada HTTP para el frontend de UniPlay. Enruta las rutas
publicas hacia microservicios internos y centraliza CORS y rate limiting.

## Rutas

| Ruta publica | Servicio destino |
|---|---|
| `/salas/**` | `room-service` |
| `/games/**` | `game-service` |
| `/ws`, `/ws/**` | `realtime-service` |
| `/metrics/**` | `metrics-service` |
| `/voice/**` | `voice-service` |

El gateway conserva las rutas originales para que los contratos REST y STOMP de
cada servicio sigan siendo independientes.

## Rate limiting

El filtro `BasicRateLimitingFilter` aplica un limite simple por IP de cliente
usando `X-Forwarded-For` cuando existe. No aplica a `OPTIONS` ni a
`/actuator/**`, para no bloquear preflight CORS ni health checks.

Respuesta cuando se excede el limite:

```http
429 Too Many Requests
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 0
```

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `API_GATEWAY_PORT` | `8080` |
| `ROOM_SERVICE_URI` | `http://localhost:8081` |
| `GAME_SERVICE_URI` | `http://localhost:8082` |
| `REALTIME_SERVICE_URI` | `http://localhost:8083` |
| `REALTIME_WEBSOCKET_URI` | `ws://localhost:8083` |
| `METRICS_SERVICE_URI` | `http://localhost:8084` |
| `VOICE_SERVICE_URI` | `http://localhost:8085` |
| `GATEWAY_ALLOWED_ORIGINS` | `*` |
| `GATEWAY_RATE_LIMIT_ENABLED` | `true` |
| `GATEWAY_RATE_LIMIT_REQUESTS_PER_WINDOW` | `120` |
| `GATEWAY_RATE_LIMIT_WINDOW` | `PT1M` |

En perfil `docker`, las URIs apuntan a los nombres internos de servicios de
Docker Compose. En perfil `aws`, las URIs deben venir del entorno de despliegue.

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
