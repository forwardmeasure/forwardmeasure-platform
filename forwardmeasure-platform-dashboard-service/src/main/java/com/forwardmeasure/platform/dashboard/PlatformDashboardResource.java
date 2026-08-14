package com.forwardmeasure.platform.dashboard;

import com.forwardmeasure.platform.dashboard.api.ComponentsApi;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/** Authenticated, read-only shared-platform operational summary. */
@Path("/api/v1")
@RolesAllowed({"platform-viewer", "platform-admin"})
@Blocking
@ApplicationScoped
public final class PlatformDashboardResource implements ComponentsApi {

    private final PlatformHealthService health;

    @Inject
    public PlatformDashboardResource(PlatformHealthService health) {
        this.health = Objects.requireNonNull(health, "health");
    }

    @Override
    public Response listPlatformComponents() {
        return Response.ok(health.observe()).build();
    }
}
