package com.orbit.controlplane.catalog.application;

import com.orbit.controlplane.catalog.domain.ResourceProfile;
import java.util.List;

public interface ApprovedCatalog {
    boolean hasModel(String model);
    boolean hasTool(String tool);
    boolean hasResourceProfile(ResourceProfile profile);
    List<String> models();
    List<String> tools();
    List<ResourceProfile> resourceProfiles();
    String workloadResources(ResourceProfile profile);
}
