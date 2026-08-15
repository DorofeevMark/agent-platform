package com.orbit.controlplane.agents.infrastructure;

import com.orbit.controlplane.agents.domain.Deployment;
import com.orbit.controlplane.agents.domain.Environment;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository extends CrudRepository<Deployment, UUID> {
    List<Deployment> findByAgentIdOrderByCreatedAtDesc(UUID agentId);
    Optional<Deployment> findByAgentIdAndEnvironment(UUID agentId, Environment environment);
}
