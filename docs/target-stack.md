# Target architecture and technology stack

## Decision

Build Orbit as a **modular control plane plus independently scalable agent runtime**. Begin with a modular monolith rather than a collection of microservices. Extract components only when a measured scale, isolation, or ownership need justifies it.

The control plane uses **Java 21 and Micronaut**. Micronaut is selected for a lean runtime, fast startup, compile-time dependency injection, and a direct Java migration path. The runtime remains Python because it is a better fit for model and agent integrations.

## System shape

```text
Builder UI / API client
        │
        ▼
Control plane (Java 21 + Micronaut)
        ├── PostgreSQL
        ├── deployment reconciler → Kubernetes / Nebius
        └── OpenTelemetry
                 │
                 ▼
Agent runtime pods (Python + FastAPI)
        ├── model providers
        ├── Tool Gateway
        └── OpenTelemetry
```

## Chosen technologies

| Area | Target | Purpose |
|---|---|---|
| Control plane | Java 21, Micronaut, Netty | API, domain logic, RBAC, policies, deployment orchestration, and reconciliation. |
| API contracts | HTTP/JSON, OpenAPI | Builder UI and automation integration. Generate typed clients from the API contract. |
| Primary database | PostgreSQL, Flyway | Source of truth for agents, immutable versions, deployments, approvals, policies, audits, and the transactional outbox. |
| Async processing | Transactional outbox and in-process worker | Durable deployment and reconciliation jobs without an initial broker dependency. |
| Kubernetes integration | Fabric8 Kubernetes client | Reconcile platform deployment revisions into standard Kubernetes resources. |
| Agent runtime | Python 3.12, FastAPI, Pydantic, `httpx` | Generic config-driven execution runtime. |
| Model integration | Direct provider adapters behind a platform-owned `ModelClient` interface | Keep model requests, streaming, retries, and telemetry explicit and portable. |
| Tool integration | Platform-owned Tool Gateway and `ToolClient` interface | Central policy enforcement, validation, audit, and rate limiting for every tool call. |
| Identity | Corporate OIDC, JWT validation | Builder authentication and authorization. |
| Workload identity | Short-lived workload tokens | Per-deployment authentication to the Tool Gateway and internal platform services. |
| Secrets | Vault in production | Least-privilege delivery of external secrets to workloads. |
| Observability | OpenTelemetry, Prometheus, Grafana, Loki, Tempo | Metrics, traces, logs, execution history, and cost attribution. |
| UI | React, TypeScript, Vite | Lightweight internal builder interface. |
| Local development | Docker Compose | Local PostgreSQL and only the services needed for the current increment. |

## Control-plane modules

Keep module APIs explicit and prevent direct access to another module’s internals.

1. **agents** — agent definitions, immutable versions, and validation.
2. **deployments** — environments, promotion paths, approvals, and deployment revisions.
3. **policy** — RBAC and agent-to-tool, secret, and network authorization decisions.
4. **catalog** — approved models, tools, and stable resource profiles.
5. **reconciler** — desired Kubernetes state and observed workload status.
6. **audit** — append-only security and operational events.
7. **metering** — execution metrics and cost attribution.

## Runtime contract

The platform owns the execution contract. Agent-framework types must not be persisted or exposed as the canonical API.

```text
AgentVersion configuration
  → AgentExecutor
    → ModelClient
    → ToolClient / Tool Gateway
    → StateStore
    → telemetry and audit events
```

An agent framework may later implement `AgentExecutor` for a specific workflow type, but the platform’s versioning, policy, audit, and deployment contracts remain framework-neutral.

## Deliberately deferred

- Microservice extraction.
- Kafka, NATS, or another event broker.
- Temporal or another workflow engine.
- A custom Kubernetes operator or CRDs.
- A separate Tool Gateway deployment.
- Redis, until there is a demonstrated caching, rate-limit, or coordination requirement.
- A general agent framework as the core runtime abstraction.
- ClickHouse or another analytics store; PostgreSQL is sufficient for the initial metering workload.

## Evolution triggers

| Trigger | Next step |
|---|---|
| Durable workflows span approvals, long waits, retries, or human intervention | Introduce Temporal. |
| Independent Tool Gateway scale or stronger isolation is required | Extract Tool Gateway into a dedicated service. |
| Cross-module event throughput or external consumers exceed the outbox worker | Add NATS JetStream or Kafka. |
| Cost and execution analytics outgrow partitioned PostgreSQL tables | Add ClickHouse. |
| Runtime needs complex, stateful multi-step workflows | Add a framework adapter, such as LangGraph, behind `AgentExecutor`. |
| Kubernetes reconciliation needs independent lifecycle or team ownership | Extract a controller; introduce CRDs only if native abstractions no longer fit. |
