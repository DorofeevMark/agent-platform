package com.orbit.controlplane.agents.domain;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import java.time.Instant;
import java.util.UUID;

@MappedEntity("agents")
public record Agent(@Id UUID id, String name, String owner, Instant createdAt) { }
