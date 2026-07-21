# ADR 0002: Despliegue AWS agrupado con recuperacion automatica

## Estado

Aceptada y desplegada el 21 de julio de 2026 en `us-east-1`.

## Contexto

Desplegar un EC2 por microservicio aumenta costo y operacion sin aportar una
ventaja proporcional para la carga academica de UniPlay. Realtime Service no
puede ejecutarse en paralelo sin coordinar las sesiones STOMP entre replicas.
AWS Academy tampoco permite crear distribuciones CloudFront.

## Decision

- Agrupar API Gateway y Realtime Service en un nodo Edge.
- Agrupar Room, Game, Metrics y Voice Service en un nodo Core.
- Mantener LiveKit y Caddy en un nodo Media con puertos WebRTC dedicados.
- Mantener Prometheus y Grafana en un nodo Observability.
- Ejecutar Edge, Core y Media en ASG de una instancia para autorrecuperacion.
- Usar ElastiCache Redis con principal y replica Multi-AZ.
- Publicar HTTPS con Caddy, una EIP, NLB y nombres temporales `nip.io`.
- Conservar imagenes independientes por microservicio en ECR.

## Consecuencias

- Una falla EC2 activa el reemplazo automatico, pero existe una interrupcion
  mientras inicia la nueva instancia.
- El estado persistido en Redis sobrevive al reemplazo de Edge/Core/Media.
- El estado en memoria de WebSocket y trazos no confirmados puede perderse.
- No se requieren dos instancias Realtime ni sincronizacion STOMP distribuida.
- El frontend y las APIs usan HTTPS valido sin comprar un dominio.
- `nip.io` es apropiado para demostracion, no para un entorno productivo.

## Evolucion recomendada

Con un dominio y permisos AWS completos, reemplazar Caddy/nip.io por Route 53,
ACM y CloudFront o un ALB HTTPS. Cuando la carga lo justifique, externalizar el
estado de sesiones Realtime antes de aumentar su capacidad por encima de uno.
