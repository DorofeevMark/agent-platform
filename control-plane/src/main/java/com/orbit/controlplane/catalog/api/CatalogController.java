package com.orbit.controlplane.catalog.api;

import com.orbit.controlplane.catalog.application.ApprovedCatalog;
import com.orbit.controlplane.catalog.domain.ResourceProfile;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller("/v1/catalog")
public class CatalogController {
    private final ApprovedCatalog catalog;

    public CatalogController(ApprovedCatalog catalog) {
        this.catalog = catalog;
    }

    @Get("/models")
    public List<String> models() {
        return catalog.models();
    }

    @Get("/tools")
    public List<String> tools() {
        return catalog.tools();
    }

    @Get("/resource-profiles")
    public List<ResourceProfile> resourceProfiles() {
        return catalog.resourceProfiles();
    }
}
