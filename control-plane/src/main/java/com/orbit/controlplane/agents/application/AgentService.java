package com.orbit.controlplane.agents.application;

import com.orbit.controlplane.agents.api.CreateAgentRequest;
import com.orbit.controlplane.agents.api.CreateVersionRequest;
import com.orbit.controlplane.agents.api.DeployRequest;
import com.orbit.controlplane.agents.api.RollbackRequest;
import com.orbit.controlplane.agents.domain.Agent;
import com.orbit.controlplane.agents.domain.AgentVersion;
import com.orbit.controlplane.agents.domain.Deployment;
import com.orbit.controlplane.agents.domain.DeploymentRevision;
import com.orbit.controlplane.agents.domain.DeploymentStatus;
import com.orbit.controlplane.agents.domain.Environment;
import com.orbit.controlplane.catalog.application.ApprovedCatalog;
import com.orbit.controlplane.catalog.domain.ResourceProfile;
import com.orbit.controlplane.agents.infrastructure.AgentRepository;
import com.orbit.controlplane.agents.infrastructure.AgentVersionRepository;
import com.orbit.controlplane.agents.infrastructure.DeploymentRepository;
import com.orbit.controlplane.agents.infrastructure.DeploymentRevisionRepository;
import com.orbit.controlplane.reconciler.KubernetesManifestCompiler;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Singleton
public class AgentService {
    private final AgentRepository agentRepository;
    private final AgentVersionRepository agentVersionRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentRevisionRepository deploymentRevisionRepository;
    private final ApprovedCatalog catalog;

    public AgentService(AgentRepository agentRepository, AgentVersionRepository agentVersionRepository,
                        DeploymentRepository deploymentRepository, DeploymentRevisionRepository deploymentRevisionRepository, ApprovedCatalog catalog) {
        this.agentRepository = agentRepository;
        this.agentVersionRepository = agentVersionRepository;
        this.deploymentRepository = deploymentRepository;
        this.deploymentRevisionRepository = deploymentRevisionRepository;
        this.catalog = catalog;
    }

    @Transactional
    public Agent create(CreateAgentRequest request) {
        return agentRepository.save(new Agent(UUID.randomUUID(), request.name(), request.owner(), Instant.now()));
    }

    public List<Agent> listAgents() { return agentRepository.findAllOrderByCreatedAt(); }

    @Transactional
    public AgentVersion createVersion(UUID agentId, CreateVersionRequest request) {
        requireAgent(agentId);
        validateCatalog(request);
        int number = agentVersionRepository.highestNumber(agentId) + 1;
        ResourceProfile profile = request.resourceProfile() == null ? ResourceProfile.SMALL : request.resourceProfile();
        String digest = sha256(agentId + "|" + number + "|" + request.systemPrompt() + "|" + request.model() + "|" + request.tools() + "|" + profile);
        AgentVersion version = new AgentVersion(UUID.randomUUID(), agentId, number, request.systemPrompt(), request.model(),
                List.copyOf(request.tools()), profile, digest, Instant.now());
        return agentVersionRepository.save(version);
    }

    public List<AgentVersion> listVersions(UUID agentId) {
        requireAgent(agentId);
        return agentVersionRepository.findByAgentIdOrderByNumber(agentId);
    }

    @Transactional
    public DeploymentRevision deploy(UUID agentId, DeployRequest request) {
        if (request.environment() == null) throw badRequest("environment is required");
        requireApproval(request.environment(), request.approvalReference());
        AgentVersion version = requireVersion(request.versionId(), agentId);
        ensurePromotionPath(agentId, version.id(), request.environment());
        Deployment deployment = deploymentRepository.findByAgentIdAndEnvironment(agentId, request.environment())
                .orElseGet(() -> deploymentRepository.save(new Deployment(UUID.randomUUID(), agentId, request.environment(), null, Instant.now())));
        return createRevision(deployment, version, request.approvalReference());
    }

    @Transactional
    public DeploymentRevision rollback(UUID agentId, UUID deploymentId, RollbackRequest request) {
        Deployment deployment = requireDeployment(deploymentId, agentId);
        requireApproval(deployment.environment(), request.approvalReference());
        AgentVersion version = requireVersion(request.versionId(), agentId);
        return createRevision(deployment, version, request.approvalReference());
    }

    public List<Deployment> listDeployments(UUID agentId) {
        requireAgent(agentId);
        return deploymentRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    public List<DeploymentRevision> listRevisions(UUID agentId, UUID deploymentId) {
        requireDeployment(deploymentId, agentId);
        return deploymentRevisionRepository.findByDeploymentIdOrderByNumberDesc(deploymentId);
    }

    private DeploymentRevision createRevision(Deployment deployment, AgentVersion version, String approvalReference) {
        Agent agent = requireAgent(deployment.agentId());
        int number = deploymentRevisionRepository.highestNumber(deployment.id()) + 1;
        String manifest = KubernetesManifestCompiler.compile(agent, version, deployment.environment());
        DeploymentRevision revision = deploymentRevisionRepository.save(new DeploymentRevision(UUID.randomUUID(), deployment.id(), version.id(), number,
                DeploymentStatus.PENDING, approvalReference, manifest, Instant.now()));
        deploymentRepository.update(new Deployment(deployment.id(), deployment.agentId(), deployment.environment(), revision.id(), deployment.createdAt()));
        return revision;
    }

    private void ensurePromotionPath(UUID agentId, UUID versionId, Environment target) {
        if (target == Environment.DEV) return;
        if (!deploymentRevisionRepository.hasDeploymentInEnvironment(agentId, versionId, Environment.DEV.name())) {
            throw badRequest("version must be deployed to DEV before " + target);
        }
    }

    private void requireApproval(Environment environment, String approvalReference) {
        if (environment == Environment.PRODUCTION && (approvalReference == null || approvalReference.isBlank())) {
            throw badRequest("production deployment requires an approvalReference");
        }
    }

    private Agent requireAgent(UUID id) { return agentRepository.findById(id).orElseThrow(() -> notFound("agent not found")); }
    private void validateCatalog(CreateVersionRequest request) {
        ResourceProfile profile = request.resourceProfile() == null ? ResourceProfile.SMALL : request.resourceProfile();
        if (!catalog.hasModel(request.model())) throw badRequest("model is not approved: " + request.model());
        request.tools().forEach(tool -> { if (!catalog.hasTool(tool)) throw badRequest("tool is not approved: " + tool); });
        if (!catalog.hasResourceProfile(profile)) throw badRequest("resource profile is not approved: " + profile);
    }
    private Deployment requireDeployment(UUID id, UUID agentId) {
        Deployment deployment = deploymentRepository.findById(id).orElseThrow(() -> notFound("deployment not found"));
        if (!deployment.agentId().equals(agentId)) throw notFound("deployment not found");
        return deployment;
    }
    private AgentVersion requireVersion(UUID versionId, UUID agentId) {
        AgentVersion version = agentVersionRepository.findById(versionId).orElseThrow(() -> notFound("agent version not found"));
        if (!version.agentId().equals(agentId)) throw notFound("agent version not found");
        return version;
    }
    private HttpStatusException badRequest(String message) { return new HttpStatusException(HttpStatus.BAD_REQUEST, message); }
    private HttpStatusException notFound(String message) { return new HttpStatusException(HttpStatus.NOT_FOUND, message); }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
