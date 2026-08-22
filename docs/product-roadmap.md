# Product roadmap

## Product outcome

Orbit turns a builder's declarative agent configuration into a governed, observable,
deployable runtime. It is not a Kubernetes interface or a collection of bespoke agent
applications.

The primary builder journey is:

```text
Define → Configure → Test → Deploy → Observe → Promote or roll back
```

Every roadmap item must make this journey safer, faster, or more understandable.

## Product capabilities

### 1. Agent definition and immutable versions

Builders create an agent, set its prompt, choose an approved model, select approved
tools, and choose a stable resource profile. Saving produces an immutable version that
can always be reproduced, tested, deployed, promoted, or rolled back.

### 2. Curated platform catalog

Builders select models, tools, and resource profiles from an approved catalog. They do
not select GPU types, Kubernetes settings, provider credentials, or deployment
manifests. The platform maps product-level choices to infrastructure.

### 3. Safe development execution

Builders invoke a version in DEV before deployment and can inspect its response, tool
calls, latency, token use, errors, and logs. This is the shortest feedback loop in the
product and is required before production promotion.

### 4. Deployment lifecycle

Builders deploy a selected version, stop it, inspect its health and logs, and promote
a known version through environments. Production promotion is approval-gated. Rollback
selects an earlier immutable version; it does not mutate the version currently running.

### 5. Generic runtime execution

The data plane runs the agent configuration without a builder-created image. The
runtime owns the prompt, conversation state, model client, tool client, retries,
timeouts, policy enforcement, and telemetry.

### 6. Explicit authorization and policy

The platform independently decides builder-to-agent, builder-to-deployment,
agent-to-tool, agent-to-secret, and agent-to-network access. Builders do not receive
Kubernetes or Nebius credentials.

### 7. Observability and operational control

Builders and operators can understand and control a deployment through status, logs,
execution history, errors, model and tool use, resource utilization, and cost.

### 8. Managed platform operations

The platform provides per-deployment identities, dedicated secrets delivery, network
policy, quotas, autoscaling, CPU/GPU scheduling, and Nebius capacity management. These
are platform capabilities, not builder configuration chores.

## Delivery sequence

| Phase | Builder outcome | Core scope |
| --- | --- | --- |
| MVP | I can create, test, deploy, stop, and inspect an agent. | Agent definitions, immutable versions, approved catalog entries, DEV execution, deploy/stop, logs, and basic RBAC. |
| Governed delivery | I can promote a tested version safely. | Environment promotion, production approval, deployment history, rollback, audit events, secrets, and per-deployment identities. |
| Efficient operation | I can understand reliability, cost, and capacity. | Tool Gateway, short-lived workload credentials, quotas, autoscaling, metering, dashboards, and GPU scheduling. |
| Optimization | I can improve agent quality and performance with evidence. | Evaluations, regression tests, canaries, A/B tests, analytics, model routing, and resource optimization. |

## First complete vertical slice

The first product-complete workflow is:

```text
Builder creates an agent
→ selects an approved model and tool
→ saves an immutable version
→ invokes it in DEV
→ deploys it
→ sees deployment status and logs
→ stops it
```

The system must implement this path end to end before expanding into independent
infrastructure projects. Subsequent work should extend this workflow with promotion,
security, and operational guarantees.

## Non-goals for early delivery

- A Kubernetes or Nebius console for builders.
- Builder-managed Docker images for standard agents.
- A custom GPU scheduler.
- Microservice extraction before an observed scale, isolation, or ownership need.
- A general-purpose agent framework as the platform's canonical contract.
