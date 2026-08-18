package com.orbit.controlplane.catalog.application;

import com.orbit.controlplane.catalog.domain.ResourceProfile;
import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("orbit.catalog")
public class CatalogConfiguration {
    private List<String> models = List.of("gpt-5");
    private List<String> tools = List.of("search");
    private List<ResourceProfile> resourceProfiles = List.of(
            ResourceProfile.SMALL,
            ResourceProfile.MEDIUM,
            ResourceProfile.LARGE,
            ResourceProfile.XL);

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = List.copyOf(models);
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = List.copyOf(tools);
    }

    public List<ResourceProfile> getResourceProfiles() {
        return resourceProfiles;
    }

    public void setResourceProfiles(List<ResourceProfile> resourceProfiles) {
        this.resourceProfiles = List.copyOf(resourceProfiles);
    }
}
