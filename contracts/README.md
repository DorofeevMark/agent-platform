# Platform contracts

This directory owns versioned contracts shared between deployables. It will contain:

- OpenAPI specifications for control-plane and Tool Gateway APIs.
- The framework-neutral agent runtime configuration schema.
- Event schemas emitted through the transactional outbox.

Do not place generated clients or service implementation code here. Those belong to the
deployable that uses them.
