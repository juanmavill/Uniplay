# ADR 0001: Monorepo con despliegue independiente por servicio

## Estado

Aceptada.

## Contexto

UniPlay esta compuesto por seis microservicios, un frontend, Redis como bus de
eventos y LiveKit como SFU para voz. La arquitectura de ejecucion exige que
cada servicio viva en una instancia independiente y se comunique solo por red.

El proyecto, sin embargo, sera desarrollado por una sola persona en un contexto
academico. Mantener multiples repositorios agregaria sobrecarga operacional sin
mejorar por si solo los atributos de calidad evaluados.

## Decision

Usar un monorepo con una carpeta independiente por servicio.

Cada servicio debe mantener su propio build, configuracion, pruebas,
Dockerfile, health check y contrato de despliegue. El monorepo solo agrupa el
codigo fuente y la evidencia de trazabilidad.

## Consecuencias positivas

- Menor friccion para desarrollo individual.
- Un solo historial para evidenciar decisiones, HU, commits y PRs.
- Pruebas locales de integracion mas simples con Docker Compose.
- Configuracion de CI/CD centralizada con jobs segmentados por servicio.

## Riesgos

- Acoplamiento accidental entre servicios por cercania fisica del codigo.
- Pipelines lentos si cada cambio reconstruye todos los servicios.
- Tentacion de extraer librerias compartidas con logica de dominio.

## Medidas de control

- No compartir repositorios de datos ni modelos de dominio entre servicios.
- Publicar eventos por Redis usando nombres `{contexto}.{evento}`.
- Construir imagenes Docker independientes por servicio.
- Configurar CI por carpeta afectada cuando exista codigo ejecutable.
- Documentar nuevas decisiones arquitectonicas como ADRs.
