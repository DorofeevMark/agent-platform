package com.orbit.controlplane.agents.application;

import com.orbit.controlplane.agents.api.CreateVersionRequest;
import com.orbit.controlplane.agents.api.DeployRequest;
import com.orbit.controlplane.agents.domain.Agent;
import com.orbit.controlplane.agents.domain.AgentVersion;
import com.orbit.controlplane.agents.domain.Deployment;
import com.orbit.controlplane.agents.domain.DeploymentRevision;
import com.orbit.controlplane.agents.domain.Environment;
import com.orbit.controlplane.catalog.domain.ResourceProfile;
import com.orbit.controlplane.catalog.application.ApprovedCatalog;
import com.orbit.controlplane.agents.infrastructure.AgentRepository;
import com.orbit.controlplane.agents.infrastructure.AgentVersionRepository;
import com.orbit.controlplane.agents.infrastructure.DeploymentRepository;
import com.orbit.controlplane.agents.infrastructure.DeploymentRevisionRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {
    @Mock AgentRepository agentRepository;
    @Mock AgentVersionRepository agentVersionRepository;
    @Mock DeploymentRepository deploymentRepository;
    @Mock DeploymentRevisionRepository deploymentRevisionRepository;
    @Mock ApprovedCatalog catalog;
    @InjectMocks AgentService agentService;

    @Test
    void rejectsProductionWithoutApprovalBeforeAccessingPersistence() {
        HttpStatusException exception = assertThrows(HttpStatusException.class,
                () -> agentService.deploy(UUID.randomUUID(), new DeployRequest(UUID.randomUUID(), Environment.PRODUCTION, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(agentRepository, agentVersionRepository, deploymentRepository, deploymentRevisionRepository);
    }

    @Test
    void requiresDevDeploymentBeforeProduction() {
        UUID agentId = UUID.randomUUID();
        AgentVersion version = version(agentId);
        when(agentVersionRepository.findById(version.id())).thenReturn(Optional.of(version));
        when(deploymentRevisionRepository.hasDeploymentInEnvironment(agentId, version.id(), Environment.DEV.name())).thenReturn(false);

        HttpStatusException exception = assertThrows(HttpStatusException.class,
                () -> agentService.deploy(agentId, new DeployRequest(version.id(), Environment.PRODUCTION, "approval-1")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(deploymentRepository, never()).save(any());
    }

    @Test
    void createsDevRevisionAndUpdatesTheStableSlot() {
        UUID agentId = UUID.randomUUID();
        Agent agent = new Agent(agentId, "support-triage", "maya@acme.test", Instant.now());
        AgentVersion version = version(agentId);
        Deployment slot = new Deployment(UUID.randomUUID(), agentId, Environment.DEV, null, Instant.now());
        when(agentVersionRepository.findById(version.id())).thenReturn(Optional.of(version));
        when(deploymentRepository.findByAgentIdAndEnvironment(agentId, Environment.DEV)).thenReturn(Optional.empty());
        when(deploymentRepository.save(any())).thenReturn(slot);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(deploymentRevisionRepository.highestNumber(slot.id())).thenReturn(0);
        when(deploymentRevisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentRevision revision = agentService.deploy(agentId, new DeployRequest(version.id(), Environment.DEV, null));

        assertEquals(slot.id(), revision.deploymentId());
        assertEquals(1, revision.number());
        ArgumentCaptor<Deployment> updatedSlot = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentRepository).update(updatedSlot.capture());
        assertEquals(revision.id(), updatedSlot.getValue().activeRevisionId());
    }

    @Test
    void createsTheFirstVersionWithTheDefaultResourceProfile() {
        UUID agentId = UUID.randomUUID();
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(new Agent(agentId, "support-triage", "maya@acme.test", Instant.now())));
        when(agentVersionRepository.highestNumber(agentId)).thenReturn(0);
        when(agentVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalog.hasModel("gpt-5")).thenReturn(true);
        when(catalog.hasTool("search")).thenReturn(true);
        when(catalog.hasResourceProfile(ResourceProfile.SMALL)).thenReturn(true);

        AgentVersion version = agentService.createVersion(agentId,
                new CreateVersionRequest("Be helpful", "gpt-5", List.of("search"), null));

        assertEquals(1, version.number());
        assertEquals(ResourceProfile.SMALL, version.resourceProfile());
        assertEquals(List.of("search"), version.tools());
    }

    private AgentVersion version(UUID agentId) {
        return new AgentVersion(UUID.randomUUID(), agentId, 1, "Be helpful", "gpt-5", List.of("search"),
                ResourceProfile.MEDIUM, "a".repeat(64), Instant.now());
    }
}
