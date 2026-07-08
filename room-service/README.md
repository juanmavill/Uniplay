# room-service

Gestiona la creacion de salas y la reserva de codigos de ingreso de UniPlay.

## Endpoint HU-01

### Crear sala

```http
POST /salas
Content-Type: application/json

{
  "maxPlayers": 21
}
```

`maxPlayers` es opcional. Si no se envia, se usa `ROOM_MAX_PLAYERS`.

Respuesta exitosa:

```http
201 Created
Location: /salas/{codigo}
```

```json
{
  "roomId": "11111111-1111-1111-1111-111111111111",
  "code": "ABC123",
  "status": "WAITING_FOR_PLAYERS",
  "maxPlayers": 21,
  "createdAt": "2026-07-07T12:00:00Z"
}
```

## Eventos publicados

| Canal Redis | Momento |
|---|---|
| `sala.creada` | Despues de reservar el codigo y persistir la sala |

## Persistencia

La sala se guarda en Redis con TTL. El codigo se reserva usando una operacion
atomica `setIfAbsent`, evitando carreras cuando dos solicitudes generan el
mismo codigo.

## Configuracion

| Variable | Valor por defecto |
|---|---|
| `ROOM_SERVICE_PORT` | `8081` |
| `ROOM_MAX_PLAYERS` | `21` |
| `ROOM_CODE_GENERATION_MAX_ATTEMPTS` | `10` |
| `ROOM_TTL` | `PT2H` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |

## Verificacion

```bash
mvn verify
```

El build falla si la cobertura de lineas cae por debajo del 70%.
