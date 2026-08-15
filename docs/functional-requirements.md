# Functional requirements

This document defines the functional behavior of the internal agent platform. Architectural constraints and implementation guidance remain in [AGENTS.md](../AGENTS.md).

## 1. Agent lifecycle

- Builders must be able to create agents declaratively without writing application code or building a container image.
- An agent definition must support: identifier, owner, system prompt, model, approved tools, environment, resource profile, security policy, and immutable versions.
- Builders must be able to view, update, and manage their agents according to RBAC permissions.
- The platform must support stopping and deploying agents.
- The platform must retain agent version history and deployment history.

## 2. Versions and deployments

- The platform must create immutable agent versions.
- It must represent `AgentVersion`, `Deployment`, and `DeploymentRevision` as separate concepts.
- A deployment must reference a specific immutable agent version.
- The platform must support promoting a selected version and rolling back a deployment to an earlier version without mutating the prior production version.
- Deploying an agent must result in the required runtime workload and supporting infrastructure resources being provisioned.

## 3. Environments and promotion

- Every agent must support DEV, STAGING, and PRODUCTION environments.
- Each environment must be independently configurable for credentials, databases, tools, resource limits, and network policies.
- The platform must allow builders to test an agent in DEV and deploy it to STAGING before production.
- Production promotion must support an approval gate.

## 4. Models, tools, and runtime

- Builders must be able to select an approved model for an agent.
- Builders must be able to grant an agent access only to approved tools.
- The generic agent runtime must execute the configured system prompt, conversation state, model client, and tool client.
- The runtime must apply retry and timeout behavior and emit observability data for executions.
- Agents must invoke external and internal tools through the Tool Gateway rather than directly.

## 5. Tool Gateway

- The Tool Gateway must authenticate and authorize each tool invocation.
- It must enforce per-agent tool permissions, rate limits, input validation, output filtering, and network access policy.
- It must record audit data for tool invocations.

## 6. Identity, access, and secrets

- The platform must enforce RBAC for builder access to agents and deployments.
- Each agent deployment must have an individual least-privilege service identity.
- The platform must authorize builder-to-agent, builder-to-deployment, agent-to-tool, agent-to-secret, and agent-to-network access independently.
- Agent workloads must use short-lived credentials to authenticate to platform services and the Tool Gateway.
- The platform must store external credentials in a dedicated secrets system and grant each deployment access only to its required secrets.
- Builders must not receive Kubernetes or Nebius credentials.
- The platform must audit access and security-relevant actions.

## 7. Resources and scaling

- Builders must select stable resource profiles rather than individual GPU models.
- The platform must map each resource profile to the underlying CPU/GPU capacity and scheduling configuration.
- The platform must enforce per-agent resource quotas.
- The platform must support CPU and GPU workloads, autoscaling, and workload scheduling across the available node pools.

## 8. Observability and cost visibility

- The platform must provide logs for deployed agents.
- For every execution, it must record the agent, version, user, request ID, model, input tokens, output tokens, latency, tool calls, GPU time, CPU time, and errors.
- The platform must provide dashboards that expose agent counts, deployment status, request volume, GPU utilization, and model/GPU costs.
- Users with appropriate permissions must be able to attribute operational and model costs to individual agents.

## 9. Initial MVP scope

The first release must provide agent creation, system prompts, model selection, approved tool selection, deploy/stop controls, logs, and basic RBAC. Versioning, multi-environment promotion, secrets, per-agent identities, audit logs, quotas, and autoscaling follow in the next phase.
