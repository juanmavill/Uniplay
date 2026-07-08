# room-service

Gestiona la creacion de salas y la reserva de codigos de ingreso de UniPlay.

## Endpoints

### Crear sala

```http
POST /salas
Content-Type: application/json

{
  "maxPlayers": 21
}
```

### Unirse a sala

```http
POST /salas/{codigo}/jugadores
Content-Type: application/json

{
  "playerName": "Ana"
}
```

Respuesta exitosa:

```http
200 OK
```

```json
{
  "roomId": "11111111-1111-1111-1111-111111111111",
  "code": "ABC123",
  "playerId": "22222222-2222-2222-2222-222222222222",
  "playerName": "Ana",
  "players": [
    {
      "playerId": "22222222-2222-2222-2222-222222222222",
      "playerName": "Ana"
    }
  ],
  "joinedAt": "2026-07-07T12:30:00Z"
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
| `jugador.conectado` | Despues de agregar el jugador y persistir la sala |

## Persistencia

La sala se guarda en Redis con TTL. El codigo se reserva usando una operacion
atomica `setIfAbsent`, evitando carreras cuando dos solicitudes generan el
mismo codigo.

Los jugadores quedan dentro del documento de sala. La validacion de duplicados
y cupo maximo vive en el dominio `Room`, no en el adaptador Redis.

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
