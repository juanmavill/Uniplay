# frontend

Cliente React 18 de UniPlay. La primera pantalla es la experiencia de juego:
sala, jugadores, canvas, rondas, respuestas, votacion, voz y KPIs.

## Desarrollo local

```bash
npm install
npm run dev
```

La app queda en:

```text
http://localhost:5173
```

Vite proxifica las rutas del backend hacia `http://localhost:8080`, por lo que
el gateway debe estar corriendo.

## Variables y rutas

El campo `Gateway` de la UI puede quedar vacio cuando se usa Vite o Nginx,
porque la app consume rutas relativas:

| Ruta | Uso |
|---|---|
| `/salas/**` | crear sala, unirse, listar jugadores |
| `/games/**` | rondas, respuestas, estado y votos |
| `/voice/**` | token LiveKit, mute e indicador de habla |
| `/metrics/kpis` | KPIs cada 5 segundos |
| `/ws` | STOMP/SockJS para canvas y eventos |

## Produccion local con Docker

El Dockerfile construye los assets y los sirve con Nginx. Nginx tambien
proxifica las rutas anteriores hacia `api-gateway:8080` dentro de Compose.

```bash
docker compose -f infra/docker-compose.yml up -d --build frontend
```
