package com.orbit.controlplane.agents.infrastructure;

import com.orbit.controlplane.agents.domain.Agent;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.UUID;

public interface AgentRepository extends CrudRepository<Agent, UUID> {
    List<Agent> findAllByOrderByCreatedAt();
}
