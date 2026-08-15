# Orbit agent platform

A runnable vertical slice of the internal agent platform architecture. It intentionally ships with a small, in-memory control-plane adapter so it can be started locally before PostgreSQL, OIDC, Vault, Temporal, and Nebius credentials are available.

## What works now

- Creates agents and immutable agent versions.
- Deploys a specific version to `DEV`, `STAGING`, or `PRODUCTION`.
- Enforces a promotion path (`DEV → STAGING → PRODUCTION`).
- Enforces the rule that production deployments need an approval reference.
- Produces a Kubernetes `Deployment`, `ServiceAccount`, `NetworkPolicy`, and `HPA` manifest as a dry-run compilation result.
- Includes a small Python agent runtime that loads a compiled configuration and only calls the configured Tool Gateway.

## Local start

Prerequisites: Java 21, Gradle, and Python 3.12.

```bash
./gradlew :control-plane:run
```

The Micronaut API is available at `http://localhost:8080`.

```bash
curl -X POST http://localhost:8080/v1/agents \
  -H 'Content-Type: application/json' \
  -d '{"name":"customer-support","owner":"maya@acme.test"}'
```

To run the runtime separately:

```bash
cd agent-runtime
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --port 8081 --reload
```

## Repository layout

```text
control-plane/    Modular Micronaut control plane, organized by product domain
agent-runtime/    Config-driven Python execution runtime
tool-gateway/     Gateway contract and future independently deployable boundary
contracts/        Versioned contracts shared across platform deployables
infra/kubernetes/ Kubernetes templates and network policy baseline
docs/             Product requirements, architecture, and repository structure
docker-compose.yml Local Postgres and Redis dependencies for the next adapter
```

See [the proposed repository structure](docs/project-structure.md) for module ownership,
dependency rules, and the relationship between the current vertical slice and the target
architecture.

## Deliberate next integrations

The in-memory store is a local-development adapter. The next implementation increment replaces it with Postgres/Flyway, adds corporate OIDC and RBAC, changes the manifest dry-run to a Kubernetes reconciler, and binds runtime identities to Vault and the Tool Gateway.
