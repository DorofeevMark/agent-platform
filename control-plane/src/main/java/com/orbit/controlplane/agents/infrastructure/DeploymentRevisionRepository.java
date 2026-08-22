package com.orbit.controlplane.agents.infrastructure;

import com.orbit.controlplane.agents.domain.DeploymentRevision;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface DeploymentRevisionRepository extends CrudRepository<DeploymentRevision, UUID> {
    List<DeploymentRevision> findByDeploymentIdOrderByNumberDesc(UUID deploymentId);

    @Query("SELECT COALESCE(MAX(number), 0) FROM deployment_revisions WHERE deployment_id = :deploymentId")
    int highestNumber(UUID deploymentId);

    @Query("SELECT COUNT(*) > 0 FROM deployment_revisions r JOIN deployments d ON d.id = r.deployment_id WHERE d.agent_id = :agentId AND r.version_id = :versionId AND d.environment = :environment")
    boolean hasDeploymentInEnvironment(UUID agentId, UUID versionId, String environment);
}
