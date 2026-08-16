# Platform contracts

This directory owns versioned contracts shared between deployables. It will contain:

- OpenAPI specifications for control-plane and Tool Gateway APIs.
- The framework-neutral agent runtime configuration schema.
- Event schemas emitted through the transactional outbox.

Do not place generated clients or service implementation code here. Those belong to the
deployable that uses them.

## Current contracts

- [Control-plane API v1](openapi/control-plane.v1.yaml) is the builder-facing HTTP API.
- [Agent runtime configuration v1](runtime/agent-runtime-config.v1.schema.json) defines
  the intended compiled, deployment-specific runtime document.

The runtime configuration schema is a forward contract for the configuration-compilation
integration. The current runtime still receives its version and Tool Gateway endpoint
through environment variables, so it does not yet consume this document.

Contracts use a major version in their filename. Additive compatible changes may be made
within a major version; breaking changes require a new versioned contract.
