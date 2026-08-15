package com.orbit.controlplane.reconciler;

import com.orbit.controlplane.agents.domain.Agent;
import com.orbit.controlplane.agents.domain.AgentVersion;
import com.orbit.controlplane.agents.domain.Environment;

public final class KubernetesManifestCompiler {
    private KubernetesManifestCompiler() { }
    public static String compile(Agent agent, AgentVersion version, Environment environment) {
        String namespace = "agents-" + environment.name().toLowerCase();
        String app = agent.name() + "-v" + version.number();
        String resources = switch (version.resourceProfile()) {
            case STANDARD -> "requests: {cpu: 500m, memory: 1Gi}\n          limits: {cpu: '2', memory: 4Gi}";
            case HEAVY -> "requests: {cpu: '2', memory: 8Gi}\n          limits: {cpu: '8', memory: 32Gi}";
            case GPU_INFERENCE -> "requests: {cpu: '4', memory: 16Gi, nvidia.com/gpu: '1'}\n          limits: {cpu: '8', memory: 32Gi, nvidia.com/gpu: '1'}";
            case BATCH_GPU -> "requests: {cpu: '8', memory: 32Gi, nvidia.com/gpu: '1'}\n          limits: {cpu: '16', memory: 64Gi, nvidia.com/gpu: '1'}";
        };
        return """
            apiVersion: v1
            kind: ServiceAccount
            metadata: {name: %s, namespace: %s}
            ---
            apiVersion: apps/v1
            kind: Deployment
            metadata: {name: %s, namespace: %s, labels: {orbit.io/config-digest: '%s'}}
            spec:
              replicas: 1
              selector: {matchLabels: {app: %s}}
              template:
                metadata: {labels: {app: %s}}
                spec:
                  serviceAccountName: %s
                  containers:
                    - name: agent-runtime
                      image: registry.internal/orbit/agent-runtime@sha256:PINNED_BY_CI
                      env:
                        - {name: ORBIT_AGENT_VERSION_ID, value: '%s'}
                        - {name: ORBIT_TOOL_GATEWAY_URL, value: 'http://tool-gateway.tool-gateway.svc.cluster.local'}
                      resources:
                        %s
            ---
            apiVersion: networking.k8s.io/v1
            kind: NetworkPolicy
            metadata: {name: %s-egress, namespace: %s}
            spec:
              podSelector: {matchLabels: {app: %s}}
              policyTypes: [Egress]
              egress: [] # Cilium policy fills explicit model, gateway, vault and telemetry destinations.
            """.formatted(app, namespace, app, namespace, version.configDigest(), app, app, app, version.id(), resources, app, namespace, app);
    }
}
