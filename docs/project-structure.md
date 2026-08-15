# Proposed repository structure

Orbit is a monorepo containing two operational deployables from the outset: the Java
control plane and the Python agent runtime. The Tool Gateway is a third logical
component with its own contract and source boundary; it becomes a separate deployment
only when scale or isolation warrants it.

```text
agent-platform/
├── control-plane/                 # Java 21 + Micronaut deployable
│   └── src/main/java/com/orbit/controlplane/
│       ├── agents/                # definitions and immutable versions
│       │   ├── api/               # HTTP endpoints and request/response types
│       │   ├── application/       # use cases and module entry points
│       │   ├── domain/            # domain objects and invariants
│       │   └── persistence/       # Postgres adapter (next increment)
│       ├── deployments/           # environments, promotion, approvals, revisions
│       ├── catalog/               # approved models, tools, resource profiles
│       ├── policy/                # RBAC and authorization decisions
│       ├── reconciler/            # desired Kubernetes state and observed status
│       ├── audit/                 # append-only security and operational events
│       ├── metering/              # execution metrics and cost attribution
│       └── platform/              # auth, database, outbox, and shared primitives
├── agent-runtime/                 # Python FastAPI deployable
│   └── app/
│       ├── executor/              # generic execution loop
│       ├── model_clients/         # provider adapters
│       ├── tool_client/           # sole client of Tool Gateway
│       ├── state/
│       ├── identity/
│       └── telemetry/
├── tool-gateway/                  # policy-enforced tool-invocation boundary
├── contracts/                     # OpenAPI, runtime config, and event schemas
├── ui/                            # React builder console (when introduced)
├── infra/                         # Compose, Kubernetes, and cloud infrastructure
└── docs/                          # product and architecture documentation
```

## Current vertical slice

The current repository intentionally implements only the first path through this
structure:

```text
catalog selection (request data)
  → agents (definition and immutable version)
  → deployment decision and promotion validation
  → reconciler (Kubernetes manifest compilation)
  → agent runtime
  → Tool Gateway HTTP boundary
```

The implemented Java classes are organized accordingly:

| Location | Responsibility |
| --- | --- |
| `agents/api` | HTTP API for agents, versions, and deployments. |
| `agents/application` | Creation, versioning, deployment, and promotion use cases. |
| `agents/domain` | Agent, AgentVersion, Deployment, and value types. |
| `reconciler` | Kubernetes workload manifest compilation. |

The storage adapter remains in memory for local development. It belongs behind the
`agents/persistence` and `deployments/persistence` boundaries when PostgreSQL/Flyway is
introduced; no other module should depend on its tables directly.

## Dependency rules

- A module exposes application interfaces, HTTP contracts, or domain events—not its
  repositories or persistence models.
- `api` depends on `application`; `application` may depend on its own `domain` and on
  explicit interfaces from another module. `domain` never depends on HTTP, database,
  Kubernetes, or framework classes.
- Only `reconciler` contains Kubernetes or Nebius integration details.
- The runtime receives a framework-neutral configuration and uses `ToolClient`; it
  never calls a tool connector directly.
- `contracts` contains definitions only. Generated clients and implementation code stay
  in their respective deployables.
- Shared code is limited to stable platform primitives such as identifiers, error
  formats, authentication claims, and telemetry conventions. Business rules remain in
  their owning module.

## How the structure evolves

Add a directory only when a working product capability needs it. The next additions are
`catalog`, `deployments`, and their persistence adapters, followed by `policy` and
`audit`. Introduce `ui` with the first builder-facing workflow. Extract Tool Gateway,
the reconciler, or an event broker only at the evolution triggers in
[the target stack](target-stack.md).
