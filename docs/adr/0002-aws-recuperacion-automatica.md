# ADR 0002: Grouped AWS deployment with automatic recovery

## Status

Accepted and deployed on 21 July 2026 in `us-east-1`.

## Context

Running one EC2 instance per microservice multiplies cost and operational effort
without a proportional benefit for UniPlay's workload. realtime-service cannot run
as multiple replicas without coordinating STOMP sessions between them. The
academic AWS account also does not allow creating CloudFront distributions.

## Decision

- Group api-gateway and realtime-service on an Edge node.
- Group room, game, metrics and voice services on a Core node.
- Keep LiveKit and Caddy on a Media node with dedicated WebRTC ports.
- Keep Prometheus and Grafana on an Observability node.
- Run Edge, Core and Media in single-instance Auto Scaling Groups so a failed
  instance is replaced automatically.
- Use ElastiCache Redis with a primary and a Multi-AZ replica.
- Serve HTTPS through Caddy with one Elastic IP, an NLB and temporary `nip.io`
  hostnames.
- Keep independent per-service images in ECR.

## Consequences

- An EC2 failure triggers automatic replacement, but there is an outage while the
  new instance boots.
- State persisted in Redis survives the replacement of an Edge, Core or Media
  node.
- In-memory WebSocket state and unconfirmed drawing strokes are lost.
- No second realtime instance is needed, and no distributed STOMP session
  synchronisation.
- The frontend and the APIs get valid HTTPS without buying a domain.
- `nip.io` is appropriate for a demonstration, not for a production environment.

## Recommended evolution

With a real domain and full AWS permissions, replace Caddy and `nip.io` with
Route 53, ACM and either CloudFront or an HTTPS ALB. When load justifies it, move
realtime session state out of the process before scaling it beyond one instance.
