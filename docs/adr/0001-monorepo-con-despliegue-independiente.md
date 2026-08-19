# ADR 0001: Monorepo with independent deployment per service

## Status

Accepted.

The original one-instance-per-service layout was replaced by the node grouping
described in ADR 0002. The decision to keep independent builds and images per
service still stands.

## Context

UniPlay is made up of six microservices, a frontend, Redis as an event bus and
LiveKit as the SFU for voice. Each service needs its own deployable artifact and
communicates with the others only over the network.

The project is built by one person, however. Maintaining several repositories
would add coordination overhead without improving any of the qualities the design
is meant to demonstrate.

## Decision

Use a monorepo with one independent folder per service.

Every service keeps its own build, configuration, tests, Dockerfile, health check
and deployment contract. The monorepo only groups the source code and the history
that shows how it was built.

## Positive consequences

- Less friction for a single developer.
- One history in which decisions, user stories, commits and pull requests can be
  traced.
- Simpler local integration testing through Docker Compose.
- Centralised CI configuration with per-service jobs.

## Risks

- Accidental coupling between services because the code sits side by side.
- Slow pipelines if every change rebuilds every service.
- Temptation to extract shared libraries that carry domain logic.

## Controls

- Never share data repositories or domain models between services.
- Publish events over Redis using `{context}.{event}` channel names.
- Build independent Docker images per service.
- Run CI per affected folder wherever there is executable code.
- Record new architectural decisions as ADRs.
