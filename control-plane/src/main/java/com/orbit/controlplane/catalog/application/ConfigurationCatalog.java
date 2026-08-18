package com.orbit.controlplane.catalog.application;

import com.orbit.controlplane.catalog.domain.ResourceProfile;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ConfigurationCatalog implements ApprovedCatalog {
    private final CatalogConfiguration configuration;

    private static final Map<ResourceProfile, String> RESOURCES = Map.of(
            ResourceProfile.SMALL,
            "requests: {cpu: 500m, memory: 1Gi}\n          limits: {cpu: '2', memory: 4Gi}",
            ResourceProfile.MEDIUM,
            "requests: {cpu: '2', memory: 8Gi}\n          limits: {cpu: '8', memory: 32Gi}",
            ResourceProfile.LARGE,
            "requests: {cpu: '4', memory: 16Gi, nvidia.com/gpu: '1'}\n          limits: {cpu: '8', memory: 32Gi, nvidia.com/gpu: '1'}",
            ResourceProfile.XL,
            "requests: {cpu: '8', memory: 32Gi, nvidia.com/gpu: '1'}\n          limits: {cpu: '16', memory: 64Gi, nvidia.com/gpu: '1'}");

    public ConfigurationCatalog(CatalogConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean hasModel(String model) {
        return configuration.getModels().contains(model);
    }

    public boolean hasTool(String tool) {
        return configuration.getTools().contains(tool);
    }

    public boolean hasResourceProfile(ResourceProfile profile) {
        return configuration.getResourceProfiles().contains(profile);
    }

    public List<String> models() {
        return configuration.getModels();
    }

    public List<String> tools() {
        return configuration.getTools();
    }

    public List<ResourceProfile> resourceProfiles() {
        return configuration.getResourceProfiles();
    }

    public String workloadResources(ResourceProfile profile) {
        return RESOURCES.get(profile);
    }
}
