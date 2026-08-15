package com.orbit.controlplane.agents.api;

import java.util.UUID;

public record RollbackRequest(UUID versionId, String approvalReference) { }
