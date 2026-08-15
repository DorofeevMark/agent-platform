package com.orbit.controlplane.agents.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAgentRequest(@NotBlank @Pattern(regexp = "[a-z0-9-]{3,63}") String name,
                                 @NotBlank String owner) { }
