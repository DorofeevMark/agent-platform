package com.orbit.controlplane.agents.api;

import com.orbit.controlplane.agents.application.AgentService;
import com.orbit.controlplane.agents.domain.Agent;
import com.orbit.controlplane.agents.domain.AgentVersion;
import com.orbit.controlplane.agents.domain.Deployment;
import com.orbit.controlplane.agents.domain.DeploymentRevision;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Validated
@Controller("/v1")
public class AgentController {
    private final AgentService service;
    public AgentController(AgentService service) {
        this.service = service;
    }

    @Post("/agents")
    @Status(HttpStatus.CREATED)
    public Agent create(@Valid @Body CreateAgentRequest request) {
        return service.create(request);
    }

    @Get("/agents")
    public List<Agent> list() {
        return service.listAgents();
    }

    @Post("/agents/{agentId}/versions")
    @Status(HttpStatus.CREATED)
    public AgentVersion version(@PathVariable UUID agentId, @Valid @Body CreateVersionRequest request) {
        return service.createVersion(agentId, request);
    }

    @Get("/agents/{agentId}/versions")
    public List<AgentVersion> versions(@PathVariable UUID agentId) {
        return service.listVersions(agentId);
    }

    @Post("/agents/{agentId}/deployments")
    @Status(HttpStatus.ACCEPTED)
    public DeploymentRevision deploy(@PathVariable UUID agentId, @Body DeployRequest request) {
        return service.deploy(agentId, request);
    }

    @Get("/agents/{agentId}/deployments")
    public List<Deployment> deployments(@PathVariable UUID agentId) {
        return service.listDeployments(agentId);
    }

    @Get("/agents/{agentId}/deployments/{deploymentId}/revisions")
    public List<DeploymentRevision> revisions(@PathVariable UUID agentId, @PathVariable UUID deploymentId) {
        return service.listRevisions(agentId, deploymentId);
    }

    @Post("/agents/{agentId}/deployments/{deploymentId}/rollback") @Status(HttpStatus.ACCEPTED)
    public DeploymentRevision rollback(@PathVariable UUID agentId, @PathVariable UUID deploymentId, @Body RollbackRequest request) {
        return service.rollback(agentId, deploymentId, request);
    }
}
