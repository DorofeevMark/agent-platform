package com.orbit.controlplane.agents.api;

import com.orbit.controlplane.catalog.domain.ResourceProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateVersionRequest(@NotBlank String systemPrompt, @NotBlank String model,
                                   @NotEmpty List<@NotBlank String> tools,
                                   ResourceProfile resourceProfile) { }
