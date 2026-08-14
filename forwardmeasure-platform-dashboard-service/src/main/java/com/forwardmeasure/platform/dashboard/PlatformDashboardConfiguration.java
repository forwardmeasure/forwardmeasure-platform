package com.forwardmeasure.platform.dashboard;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Server-owned dashboard targets and browser-safe runtime configuration. */
@ConfigMapping(prefix = "forwardmeasure.platform.dashboard")
public interface PlatformDashboardConfiguration {

    String oidcUrl();

    String oidcRealm();

    String oidcClientId();

    List<Tenant> tenants();

    List<Application> applications();

    @WithDefault("Production")
    String environment();

    @WithDefault("PT2S")
    Duration probeTimeout();

    @WithDefault("PT10S")
    Duration observationTtl();

    List<Component> components();

    interface Tenant {

        String host();

        String did();

        String name();

        String displayName();
    }

    interface Component {

        String id();

        String name();

        String category();

        @WithDefault("HTTP")
        String probe();

        URI healthUri();

        Optional<URI> consoleUri();
    }

    interface Application {

        String id();

        String name();

        String description();

        String href();
    }
}
