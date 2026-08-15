package com.orbit.controlplane.agents.domain;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.time.Instant;
import java.util.UUID;

@MappedEntity("deployment_revisions")
public record DeploymentRevision(@Id UUID id, UUID deploymentId, UUID versionId, int number,
                                 DeploymentStatus status, String approvalReference, String manifest,
                                 Instant createdAt) { }
