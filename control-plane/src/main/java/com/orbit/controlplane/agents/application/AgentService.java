package com.orbit.controlplane.agents.application;

import com.orbit.controlplane.agents.domain.AgentModels.Agent;
import com.orbit.controlplane.agents.domain.AgentModels.AgentVersion;
import com.orbit.controlplane.agents.domain.AgentModels.CreateAgentRequest;
import com.orbit.controlplane.agents.domain.AgentModels.CreateVersionRequest;
import com.orbit.controlplane.agents.domain.AgentModels.Deployment;
import com.orbit.controlplane.agents.domain.AgentModels.DeploymentStatus;
import com.orbit.controlplane.agents.domain.AgentModels.DeployRequest;
import com.orbit.controlplane.agents.domain.AgentModels.Environment;
import com.orbit.controlplane.agents.domain.AgentModels.ResourceProfile;
import com.orbit.controlplane.reconciler.KubernetesManifestCompiler;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class AgentService {
    private final Map<UUID, Agent> agents = new ConcurrentHashMap<>();
    private final Map<UUID, AgentVersion> versions = new ConcurrentHashMap<>();
    private final Map<UUID, Deployment> deployments = new ConcurrentHashMap<>();

    public Agent create(CreateAgentRequest request) {
        Agent agent = new Agent(UUID.randomUUID(), request.name(), request.owner(), Instant.now());
        agents.put(agent.id(), agent);
        return agent;
    }

    public List<Agent> listAgents() { return agents.values().stream().sorted(Comparator.comparing(Agent::createdAt)).toList(); }

    public AgentVersion createVersion(UUID agentId, CreateVersionRequest request) {
        requireAgent(agentId);
        int number = (int) versions.values().stream().filter(v -> v.agentId().equals(agentId)).count() + 1;
        ResourceProfile profile = request.resourceProfile() == null ? ResourceProfile.STANDARD : request.resourceProfile();
        String digest = sha256(agentId + "|" + number + "|" + request.systemPrompt() + "|" + request.model() + "|" + request.tools() + "|" + profile);
        AgentVersion version = new AgentVersion(UUID.randomUUID(), agentId, number, request.systemPrompt(), request.model(), List.copyOf(request.tools()), profile, digest, Instant.now());
        versions.put(version.id(), version);
        return version;
    }

    public List<AgentVersion> listVersions(UUID agentId) {
        requireAgent(agentId);
        return versions.values().stream().filter(v -> v.agentId().equals(agentId)).sorted(Comparator.comparing(AgentVersion::number)).toList();
    }

    public Deployment deploy(UUID agentId, DeployRequest request) {
        AgentVersion version = requireVersion(request.versionId(), agentId);
        if (request.environment() == null) throw badRequest("environment is required");
        if (request.environment() == Environment.PRODUCTION && (request.approvalReference() == null || request.approvalReference().isBlank())) {
            throw badRequest("production deployment requires an approvalReference");
        }
        ensurePromotionPath(agentId, version.id(), request.environment());
        String manifest = KubernetesManifestCompiler.compile(requireAgent(agentId), version, request.environment());
        Deployment deployment = new Deployment(UUID.randomUUID(), agentId, version.id(), request.environment(), DeploymentStatus.PENDING,
                request.approvalReference(), manifest, Instant.now());
        deployments.put(deployment.id(), deployment);
        return deployment;
    }

    public List<Deployment> listDeployments(UUID agentId) {
        requireAgent(agentId);
        return deployments.values().stream().filter(d -> d.agentId().equals(agentId)).sorted(Comparator.comparing(Deployment::createdAt).reversed()).toList();
    }

    private void ensurePromotionPath(UUID agentId, UUID versionId, Environment target) {
        if (target == Environment.DEV) return;
        Environment prerequisite = target == Environment.STAGING ? Environment.DEV : Environment.STAGING;
        boolean promoted = deployments.values().stream().anyMatch(d -> d.agentId().equals(agentId) && d.versionId().equals(versionId) && d.environment() == prerequisite);
        if (!promoted) throw badRequest("version must be deployed to " + prerequisite + " before " + target);
    }
    private Agent requireAgent(UUID id) { if (!agents.containsKey(id)) throw new HttpStatusException(HttpStatus.NOT_FOUND, "agent not found"); return agents.get(id); }
    private AgentVersion requireVersion(UUID versionId, UUID agentId) { AgentVersion v = versions.get(versionId); if (v == null || !v.agentId().equals(agentId)) throw new HttpStatusException(HttpStatus.NOT_FOUND, "agent version not found"); return v; }
    private HttpStatusException badRequest(String message) { return new HttpStatusException(HttpStatus.BAD_REQUEST, message); }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
