package com.orbit.controlplane.agents.domain;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@MappedEntity("agent_versions")
public record AgentVersion(@Id UUID id, UUID agentId, int number, String systemPrompt, String model,
                           @MappedProperty("tools_json") @TypeDef(type = DataType.JSON) List<String> tools,
                           ResourceProfile resourceProfile, String configDigest,
                           Instant createdAt) { }
