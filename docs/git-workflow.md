# Flujo Git de UniPlay

## Ramas

UniPlay usa un flujo simple:

```text
main <- develop <- feature/{servicio}-{descripcion-corta}
```

- `main`: siempre debe representar una version desplegable.
- `develop`: integra trabajo validado antes de promoverlo a `main`.
- `feature/*`: contiene cambios pequenos asociados a una HU o tarea tecnica.

## Commits progresivos

El desarrollo debe avanzar mediante commits atomicos. Cada commit representa un
cambio unico, completo y verificable. No se debe mezclar dominio, endpoints,
infraestructura y pruebas en un mismo commit si pueden validarse por separado.

Ejemplo esperado para una HU:

```text
feat(room-service): agregar entidad Sala y repositorio
test(room-service): cubrir generacion de codigo unico
feat(room-service): exponer endpoint POST /salas
test(room-service): cubrir integracion de creacion de sala
```

Ejemplo a evitar:

```text
feat(room-service): implementar HU-01 completa
```

## Convencion de mensajes

Los commits siguen Conventional Commits:

```text
tipo(scope): descripcion corta
```

Tipos principales:

- `feat`: funcionalidad nueva.
- `fix`: correccion de bug.
- `test`: pruebas nuevas o ajustadas.
- `docs`: documentacion.
- `chore`: mantenimiento sin impacto funcional.
- `ci`: cambios de integracion continua.
- `build`: configuracion de build o dependencias.
- `refactor`: cambio interno sin alterar comportamiento.

Cuando aplique, la descripcion del PR debe referenciar la HU cubierta y listar
los criterios de aceptacion verificados.

## Reglas de calidad por commit

- El proyecto debe compilar si el commit toca codigo ejecutable.
- Las pruebas relevantes deben pasar antes de crear el commit.
- Los cambios grandes deben dividirse antes de commitear.
- Cada servicio conserva independencia de build, Dockerfile y configuracion.
