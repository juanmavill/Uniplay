# ADR 0003: Service boundaries, and which ones are hard to defend

## Status

Accepted, with reservations recorded below.

## Context

UniPlay is split into six services. The split was decided at the start of the
project, before the domain was well understood, and it was motivated as much by
wanting to practise distributed systems as by the problem itself.

This ADR records an honest assessment made after the system was working: which
boundaries earn their cost, which do not, and what I would change.

A service boundary is worth its cost when at least one of these is true:

1. The parts change for different reasons and at different times.
2. They have different runtime characteristics: scaling, protocol, failure mode.
3. They need to fail independently.

A boundary that satisfies none of them is a network call where a method call
would do.

## Decision

The six services are kept as they are, and the weak boundaries are documented
rather than hidden.

### Boundaries that hold up

**realtime-service** is the strongest case. It holds long-lived WebSocket
connections while every other service handles short request/response cycles. Its
memory profile, its failure mode and its scaling constraints are all different.
Mixing it into a REST service would tie the lifecycle of thousands of open
sockets to deployments of unrelated code.

**game-service** owns the rules: rounds, timers, words, scoring, voting. It is
the largest and most volatile part of the domain, and the one that changed most
often during development. Isolating it meant game rule changes never risked room
management.

**room-service** owns a genuinely separate lifecycle. A room exists before a game
starts and outlives individual rounds. It has its own invariants (capacity,
unique codes, no duplicate players) that have nothing to do with gameplay.

**api-gateway** is not a domain boundary, and is not claimed as one. It exists so
that the client has a single origin, and so CORS and rate limiting live in one
place instead of six.

### Boundaries that are hard to defend

**voice-service** is the weakest. It issues LiveKit access tokens and tracks mute
and speaking state: three endpoints and no real domain logic. Every reason to
call it a service is about the technology it wraps, not about the problem. It
changes when room membership changes, which is exactly the argument for it being
part of room-service.

**metrics-service** is a read model built from domain events. That is a legitimate
reason to keep it separate, since it can be rebuilt, lag behind, or go down
without affecting gameplay. But its projection is held in memory, so it does not
actually survive the restarts that independence is supposed to buy. As built, the
separation costs a network hop and delivers little.

### What I would do differently

Starting over for this problem, I would build a modular monolith with the same
internal boundaries, and extract realtime-service alone, because its runtime
characteristics genuinely differ. That would keep the hexagonal structure and the
domain separation while removing four deployment units, five Dockerfiles and the
inter-service failure modes.

The current split was still the right call for its actual purpose, which was
learning: independent deployment, per-service observability, event-driven
communication between processes and cloud deployment of a distributed system are
hard to practise in a monolith.

## Consequences

- Every cross-service interaction is a network call that can fail, time out or
  arrive out of order, and that has to be handled.
- A local development environment needs six JVMs plus Redis, which is why
  `infra/docker-compose.yml` exists.
- No distributed transactions: consistency across services is eventual, carried
  by Redis Pub/Sub events.
- Merging voice-service into room-service is a contained change and the first one
  I would make if this project continued.
- The weak boundaries are written down here rather than justified after the fact,
  so the trade-off can be discussed directly.
