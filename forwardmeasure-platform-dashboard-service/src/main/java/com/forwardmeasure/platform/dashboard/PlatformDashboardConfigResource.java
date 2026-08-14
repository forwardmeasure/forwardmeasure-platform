package com.forwardmeasure.platform.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;

/** Publishes browser-safe deployment configuration without rebuilding the SPA. */
@Path("/config.js")
public final class PlatformDashboardConfigResource {

    private final PlatformDashboardConfiguration configuration;

    private final ObjectMapper json;

    @Inject
    public PlatformDashboardConfigResource(
            PlatformDashboardConfiguration configuration, ObjectMapper json) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.json = Objects.requireNonNull(json, "json");
    }

    @GET
    @Produces("application/javascript")
    public Response configuration() {
        try {
            String body = "window.__FORWARDMEASURE_PLATFORM_CONFIG__="
                    + json.writeValueAsString(Map.of(
                            "tenantContextUrl", "/platform/tenant-context",
                            "apiUrl", "/platform/api/v1",
                            "environment", configuration.environment(),
                            "applications", configuration.applications()
                                    .stream()
                                    .map(PlatformDashboardConfigResource
                                            ::browserApplication)
                                    .toList()))
                    + ";";
            CacheControl cache = new CacheControl();
            cache.setNoStore(true);
            return Response.ok(body).cacheControl(cache)
                    .header("X-Content-Type-Options", "nosniff")
                    .build();
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "Platform Dashboard configuration cannot be serialized", failure);
        }
    }

    private static BrowserApplication browserApplication(
            PlatformDashboardConfiguration.Application application) {
        PlatformApplicationPath href = PlatformApplicationPath.parse(
                application.href());
        return new BrowserApplication(
                application.id(),
                application.name(),
                application.description(),
                href.value());
    }

    record BrowserApplication(
            String id, String name, String description, String href) {
    }
}
