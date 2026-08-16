package com.orbit.controlplane.agents.infrastructure;

import com.orbit.controlplane.agents.domain.Agent;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface AgentRepository extends CrudRepository<Agent, UUID> {
    List<Agent> findAllOrderByCreatedAt();
}
