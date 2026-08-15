package com.orbit.controlplane.agents.api;

import com.orbit.controlplane.agents.domain.Environment;
import java.util.UUID;

public record DeployRequest(UUID versionId, Environment environment, String approvalReference) { }
