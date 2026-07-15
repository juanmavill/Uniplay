# UniPlay

UniPlay es una plataforma web de juegos colaborativos en tiempo real para
comunidades universitarias, construida como un sistema distribuido de
microservicios independientes.

## Decision de repositorio

El proyecto usa un monorepo para simplificar el desarrollo individual, la
trazabilidad academica y las pruebas locales de integracion. Esta decision no
convierte el sistema en un monolito: cada servicio se compila, prueba,
configura, empaqueta y despliega de forma independiente.

## Servicios

| Servicio | Puerto | Responsabilidad principal |
|---|---:|---|
| api-gateway | 8080 | Entrada publica, CORS, rutas y rate limiting |
| room-service | 8081 | Creacion de salas y ciclo de vida de jugadores |
| game-service | 8082 | Reglas de juego, rondas, palabras y puntajes |
| realtime-service | 8083 | WebSocket STOMP para canvas y chat |
| metrics-service | 8084 | KPIs de negocio y metricas tecnicas |
| voice-service | 8085 | Tokens de voz, integracion con LiveKit y eventos de voz |
| frontend | 5173 | Cliente React para sala, juego, voz y metricas |

## Entrada publica local

En desarrollo local, el frontend corre en:

```text
http://localhost:5173
```

El frontend consume el backend por el gateway:

```text
http://localhost:8080
```

Ejemplos:

| Operacion | URL publica |
|---|---|
| Crear sala | `POST http://localhost:8080/salas` |
| Iniciar ronda | `POST http://localhost:8080/games/{codigo}/rounds` |
| Solicitar token de voz | `POST http://localhost:8080/voice/token` |
| Consultar KPIs | `GET http://localhost:8080/metrics/kpis` |
| STOMP/SockJS | `http://localhost:8080/ws` |

## Regla de independencia

Los servicios se comunican exclusivamente por red mediante REST o Redis
Pub/Sub. No comparten memoria, base de datos ni logica de dominio.

## Observabilidad y carga

La infraestructura local incluye Prometheus en `http://localhost:9090` y
Grafana en `http://localhost:3000`. El dashboard, las alertas y los perfiles de
k6 de hasta 500 usuarios concurrentes estan descritos en
[`docs/observability.md`](docs/observability.md).

## Estructura esperada

```text
uniplay/
  api-gateway/
  room-service/
  game-service/
  realtime-service/
  voice-service/
  metrics-service/
  frontend/
  infra/
  docs/
```
