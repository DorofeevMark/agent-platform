# Tool Gateway

The Tool Gateway is the only permitted path from an agent runtime to an internal or
external tool. Its stable caller-facing contract is:

```text
Agent runtime → Tool Gateway → approved tool connector
```

The runtime sends `POST /v1/tool-invocations` with a workload token and the agent
version identity. The gateway authenticates the workload; authorizes the version's
tool, secret, and network access; validates input; applies rate limits; filters output;
and writes an audit event before returning the result.

The current runtime already uses this boundary. The gateway implementation is deferred
until the platform has an approved tool connector to enforce. Initially it may share a
deployment with the control plane. The HTTP contract and this source boundary allow it
to become an independently scaled and isolated service without changing the runtime.
