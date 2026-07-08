# metrics-service

Consume eventos de dominio publicados en Redis y mantiene una proyeccion en
memoria con KPIs de negocio para el dashboard.

## Endpoints

### Consultar KPIs

```http
GET /metrics/kpis
```

```json
{
  "activeRooms": 1,
  "connectedPlayers": 2,
  "guessRate": 0.5,
  "averagePlayersPerRoom": 2.0
}
```

## Eventos consumidos

| Canal Redis | Uso |
|---|---|
| `sala.creada` | Incrementa salas activas |
| `jugador.conectado` | Incrementa jugadores conectados |
| `ronda.iniciada` | Incrementa rondas iniciadas |
| `palabra.adivinada` | Incrementa rondas adivinadas |

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `METRICS_SERVICE_PORT` | `8084` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
