package com.orbit.controlplane.agents.infrastructure;

import com.orbit.controlplane.agents.domain.AgentVersion;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.UUID;

public interface AgentVersionRepository extends CrudRepository<AgentVersion, UUID> {
    List<AgentVersion> findByAgentIdOrderByNumber(UUID agentId);
    @Query("SELECT COALESCE(MAX(number), 0) FROM agent_versions WHERE agent_id = :agentId")
    int highestNumber(UUID agentId);
}
