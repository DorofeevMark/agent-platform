package com.orbit.controlplane.agents.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AgentModels {
    private AgentModels() { }

    public enum Environment { DEV, STAGING, PRODUCTION }
    public enum ResourceProfile { STANDARD, HEAVY, GPU_INFERENCE, BATCH_GPU }
    public enum DeploymentStatus { PENDING, RUNNING, FAILED, ROLLED_BACK }

    public record CreateAgentRequest(@NotBlank @Pattern(regexp = "[a-z0-9-]{3,63}") String name,
                                     @NotBlank String owner) { }
    public record CreateVersionRequest(@NotBlank String systemPrompt,
                                       @NotBlank String model,
                                       @NotEmpty List<@NotBlank String> tools,
                                       ResourceProfile resourceProfile) { }
    public record DeployRequest(UUID versionId, Environment environment, String approvalReference) { }
    public record Agent(UUID id, String name, String owner, Instant createdAt) { }
    public record AgentVersion(UUID id, UUID agentId, int number, String systemPrompt, String model,
                               List<String> tools, ResourceProfile resourceProfile, String configDigest,
                               Instant createdAt) { }
    public record Deployment(UUID id, UUID agentId, UUID versionId, Environment environment,
                             DeploymentStatus status, String approvalReference, String manifest,
                             Instant createdAt) { }
}
