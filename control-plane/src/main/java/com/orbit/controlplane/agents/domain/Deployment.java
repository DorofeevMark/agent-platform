package com.orbit.controlplane.agents.domain;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.time.Instant;
import java.util.UUID;

@MappedEntity("deployments")
public record Deployment(@Id UUID id, UUID agentId, Environment environment, UUID activeRevisionId,
                         Instant createdAt) { }
