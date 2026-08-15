# Project memory

## Product direction

Build an internal agent platform: builders define agents declaratively, while the platform owns execution, identity, secrets, networking, observability, and CPU/GPU allocation. Nebius is an infrastructure and compute layer, not a builder-facing product. The experience should feel like **Heroku/Vercel for internal AI agents**, not a UI for Kubernetes.

Builder workflow: create an agent, choose a model, grant approved tools, test, and deploy. The platform abstracts Kubernetes, Nebius, service accounts, secrets, scheduling, autoscaling, observability, costs, and rollbacks.

The user-visible platform behavior is specified in [Functional requirements](docs/functional-requirements.md).

The agreed implementation choices and evolution triggers are specified in [Target architecture and technology stack](docs/target-stack.md).

## Core architecture

- Keep a strict separation between the **control plane** and **data plane**.
  - The control plane owns agent CRUD, versioning, deployments, RBAC, tool registration, secrets, service accounts, resource profiles and quotas, audit logs, deployment history, and Kubernetes/Nebius integration.
  - The data plane runs generic agent-runtime workloads on Kubernetes. A deployment should produce the required Deployment, Service, HPA, policies, and related resources.
- Start pragmatically with a Java/Micronaut control plane, Postgres, Redis only when needed, Kubernetes API, and Nebius API.
- Treat agent configuration—not bespoke application code—as the main product object. An agent includes an id, owner, system prompt, model, tools, environment, resource profile, security policy, and immutable versions.
- The runtime must be generic and small: prompt, conversation state, LLM client, tool client, retries/timeouts, observability, and policy enforcement. Creating an agent must not require building a Docker image.

## Security and identity

- Builders must never receive Kubernetes or Nebius credentials.
- Give each deployment its own service identity and least-privilege permissions; avoid shared all-agents credentials.
- Enforce authorization independently across builder → agent, builder → deployment, agent → tool, agent → secret, and agent → network. Make policy explicit, not scattered `isAdmin` checks.
- Use a Tool Gateway for every tool invocation. Agents do not call Stripe, databases, Slack, AWS, or other systems directly. The gateway enforces authentication, authorization, rate limits, validation, output filtering, network policy, per-agent permissions, and audit logs.
- Distinguish credentials:
  1. Control-plane credentials for Nebius/Kubernetes, automatically rotated.
  2. Short-lived agent credentials (for example, JWTs) for platform and Tool Gateway access.
  3. External secrets stored in a dedicated secrets system—not Postgres or Kubernetes ConfigMaps—and mounted only for workloads that need them.

## Deployment and environments

- Deployments are immutable. Model `AgentVersion`, `Deployment`, and `DeploymentRevision` separately; promotion and rollback select a version rather than mutate production in place.
- Every agent has DEV, STAGING, and PRODUCTION, each with separate credentials, databases, tools, resource limits, and network policies. Production promotion should be approval-gated.

## Resources and Nebius

- Expose stable product-level resource profiles such as SMALL, MEDIUM, LARGE, and XL; do not require builders to choose GPU models. The platform maps profiles to node selectors and Nebius capacity so hardware can change without changing agent definitions.
- Begin with Kubernetes-native CPU/GPU scheduling, node pools, autoscaling, quotas, and GPU components. Do not build a custom GPU scheduler initially.
- If GPU utilization becomes a material problem, evaluate Run:ai for sharing and dynamic GPU allocation.

## Observability and costs

For each execution capture agent, version, user, request ID, model, input/output tokens, latency, tool calls, GPU/CPU time, and errors. Product dashboards must make per-agent cost and utilization explainable.

## Delivery roadmap

1. MVP: agent creation, system prompts, model choice, approved tools, deploy/stop, logs, and basic RBAC.
2. Add versions, staging/production, secrets, per-agent service accounts, audit logs, quotas, and autoscaling.
3. Add dynamic GPU allocation, cost accounting, Tool Gateway, short-lived credentials, advanced scheduling, and GPU sharing.
4. Add evaluations, regression tests, canaries, A/B tests, performance analytics, model routing, and resource optimization.

## Intended service boundaries

Start with these seven logical components, without prematurely turning each into a separate microservice:

1. Agent Control Plane
2. Agent Runtime
3. Tool Gateway
4. Identity and Secrets
5. Resource Manager
6. Observability
7. Builder UI/API
