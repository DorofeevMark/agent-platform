package com.orbit.controlplane.agents;

import com.orbit.controlplane.agents.api.CreateAgentRequest;
import com.orbit.controlplane.agents.api.CreateVersionRequest;
import com.orbit.controlplane.agents.api.DeployRequest;
import com.orbit.controlplane.agents.api.RollbackRequest;
import com.orbit.controlplane.agents.domain.Agent;
import com.orbit.controlplane.agents.domain.AgentVersion;
import com.orbit.controlplane.agents.domain.Deployment;
import com.orbit.controlplane.agents.domain.DeploymentRevision;
import com.orbit.controlplane.agents.domain.Environment;
import com.orbit.controlplane.agents.domain.ResourceProfile;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.orbit.controlplane.support.PostgresIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurableAgentDomainIntegrationTest extends PostgresIntegrationTest {

    @Test
    void createsSequentialImmutableAgentVersions() throws SQLException {
        Agent agent = createAgent();
        AgentVersion first = createVersion(agent, "first");
        AgentVersion second = createVersion(agent, "second");

        assertEquals(List.of(first, second), agentService.listVersions(agent.id()));

        assertThrows(SQLException.class, () -> updateVersionModel(first.id()));
    }

    @Test
    void deploysRevisionsIntoOneStableEnvironmentSlot() {
        Agent agent = createAgent();
        AgentVersion first = createVersion(agent, "first");
        AgentVersion second = createVersion(agent, "second");

        DeploymentRevision firstRevision = agentService.deploy(agent.id(), new DeployRequest(first.id(), Environment.DEV, null));
        DeploymentRevision secondRevision = agentService.deploy(agent.id(), new DeployRequest(second.id(), Environment.DEV, null));
        List<Deployment> deployments = agentService.listDeployments(agent.id());

        assertEquals(1, deployments.size());
        assertEquals(firstRevision.deploymentId(), secondRevision.deploymentId());
        assertEquals(2, secondRevision.number());
        assertEquals(secondRevision.id(), deployments.getFirst().activeRevisionId());
        assertEquals(List.of(secondRevision, firstRevision), agentService.listRevisions(agent.id(), deployments.getFirst().id()));
    }

    @Test
    void requiresDevAndApprovalBeforeProduction() {
        Agent agent = createAgent();
        AgentVersion version = createVersion(agent, "production-ready");

        HttpStatusException missingDev = assertThrows(HttpStatusException.class,
                () -> agentService.deploy(agent.id(), new DeployRequest(version.id(), Environment.PRODUCTION, "approval-1")));
        assertEquals(HttpStatus.BAD_REQUEST, missingDev.getStatus());

        agentService.deploy(agent.id(), new DeployRequest(version.id(), Environment.DEV, null));

        HttpStatusException missingApproval = assertThrows(HttpStatusException.class,
                () -> agentService.deploy(agent.id(), new DeployRequest(version.id(), Environment.PRODUCTION, null)));
        assertEquals(HttpStatus.BAD_REQUEST, missingApproval.getStatus());

        DeploymentRevision productionRevision = agentService.deploy(agent.id(),
                new DeployRequest(version.id(), Environment.PRODUCTION, "approval-1"));
        assertEquals(version.id(), productionRevision.versionId());
    }

    @Test
    void rollbackAppendsARevisionWithoutChangingHistoricalRevisions() {
        Agent agent = createAgent();
        AgentVersion first = createVersion(agent, "first");
        AgentVersion second = createVersion(agent, "second");
        DeploymentRevision firstRevision = agentService.deploy(agent.id(), new DeployRequest(first.id(), Environment.DEV, null));
        DeploymentRevision secondRevision = agentService.deploy(agent.id(), new DeployRequest(second.id(), Environment.DEV, null));

        DeploymentRevision rollbackRevision = agentService.rollback(agent.id(), firstRevision.deploymentId(), new RollbackRequest(first.id(), null));

        assertEquals(3, rollbackRevision.number());
        assertEquals(first.id(), rollbackRevision.versionId());
        assertNotEquals(secondRevision.id(), rollbackRevision.id());
        assertEquals(List.of(rollbackRevision, secondRevision, firstRevision),
                agentService.listRevisions(agent.id(), firstRevision.deploymentId()));
    }

    private Agent createAgent() {
        return agentService.create(new CreateAgentRequest("support-triage", "maya@acme.test"));
    }

    private AgentVersion createVersion(Agent agent, String prompt) {
        return agentService.createVersion(agent.id(), new CreateVersionRequest(prompt, "gpt-5", List.of("search"), ResourceProfile.STANDARD));
    }

    private void updateVersionModel(java.util.UUID versionId) throws SQLException {
        try (var connection = connection();
             var statement = connection.prepareStatement("UPDATE agent_versions SET model = 'other' WHERE id = ?")) {
            statement.setObject(1, versionId);
            statement.executeUpdate();
        }
    }
}
