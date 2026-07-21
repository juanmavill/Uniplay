# Despliegue AWS de UniPlay

## Arquitectura desplegada

```mermaid
flowchart TB
    player["Jugadores"] --> dns["nip.io + HTTPS"]
    dns --> nlb["NLB + EIP"]

    subgraph vpc["VPC 10.42.0.0/16 - us-east-1"]
        nlb --> media["Media ASG 1/1/1\nCaddy + LiveKit\nt3.small"]
        media --> alb["Application Load Balancer"]
        alb --> edge["Edge ASG 1/1/1\nAPI Gateway + Realtime\nt3.medium"]
        edge --> core["Core ASG 1/1/1\nRoom + Game + Metrics + Voice\nt3.medium"]

        edge --> redis[("ElastiCache Redis\nprincipal + replica Multi-AZ")]
        core --> redis
        media --> redis

        obs["Observability EC2\nPrometheus + Grafana\nt3.small"] --> edge
        obs --> core
        alb --> obs
    end

    s3["S3 privado\nfrontend + bootstrap"] -. "sincroniza al iniciar" .-> media
    ecr["ECR\n6 imagenes independientes"] -. "pull" .-> edge
    ecr -. "pull" .-> core
```

Edge y Core pueden ubicarse en cualquiera de las dos subredes privadas. Cada
ASG mantiene una sola instancia: ofrece recuperacion automatica, no dos
replicas activas. Redis es el componente con replica simultanea Multi-AZ.

## Endpoints actuales

| Componente | URL |
|---|---|
| UniPlay | `https://uniplay.3.82.79.9.nip.io` |
| Grafana | `https://uniplay.3.82.79.9.nip.io/grafana/` |
| LiveKit | `wss://livekit.3.82.79.9.nip.io` |

Las credenciales de Grafana se generan en cada despliegue y se guardan fuera
de Git. El usuario es `admin`.

## Evidencia de validacion

- Stacks `uniplay-foundation` y `uniplay-application` operativas; ambas
  quedaron en `UPDATE_COMPLETE` despues de los ajustes validados.
- Siete target groups saludables: Caddy HTTP/HTTPS, API Gateway, Grafana y
  LiveKit signaling/TCP/UDP.
- Prometheus: 7 de 7 targets en estado `up`.
- Flujo probado: crear sala, unir jugadores, iniciar ronda y ocultar la palabra
  al jugador que adivina.
- Voz probada desde navegador: token emitido, conexion LiveKit establecida y
  participante visible como `Escuchando`.

## Operacion

Desplegar desde AWS CloudShell:

```bash
AWS_REGION=us-east-1 ./infra/aws/deploy-resilient.sh
```

Destruir todos los recursos administrados por las dos pilas:

```bash
AWS_REGION=us-east-1 ./infra/aws/destroy-resilient.sh
```

La topologia usa dos NAT Gateway para mantener salida en ambas zonas. Son uno
de los componentes de mayor costo y deben eliminarse con el script cuando la
demostracion termine.

## Limitacion del laboratorio

El rol de AWS Academy bloquea `cloudfront:CreateDistribution`. Por eso el
despliegue predeterminado usa Caddy y certificados Let's Encrypt sobre la EIP.
El template conserva CloudFront detras de `EnableCloudFront=true` para una
cuenta con permisos completos.
