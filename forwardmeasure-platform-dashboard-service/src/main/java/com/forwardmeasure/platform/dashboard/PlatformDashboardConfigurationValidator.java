package com.forwardmeasure.platform.dashboard;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Fails startup when deployment-owned dashboard configuration is unsafe. */
@Startup
@ApplicationScoped
public final class PlatformDashboardConfigurationValidator {

    @Inject
    public PlatformDashboardConfigurationValidator(
            PlatformDashboardConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        validateOidcUrl(configuration.oidcUrl());
        requireText(configuration.oidcRealm(), "oidcRealm");
        requireText(configuration.oidcClientId(), "oidcClientId");
        requireText(configuration.environment(), "environment");
        requirePositive(configuration.probeTimeout(), "probeTimeout");
        requirePositive(configuration.observationTtl(), "observationTtl");

        if (configuration.tenants().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one Platform Dashboard tenant must be configured");
        }
        Set<String> tenantHosts = new HashSet<>();
        Set<String> tenantDids = new HashSet<>();
        Set<String> tenantNames = new HashSet<>();
        for (PlatformDashboardConfiguration.Tenant tenant
                : configuration.tenants()) {
            String host = PlatformTenantContextResource.canonicalHost(
                    tenant.host());
            if (!tenantHosts.add(host)) {
                throw new IllegalArgumentException(
                        "Duplicate Platform Dashboard tenant host: " + host);
            }
            requireDid(tenant.did());
            if (!tenantDids.add(tenant.did())) {
                throw new IllegalArgumentException(
                        "Duplicate Platform Dashboard tenant DID: "
                                + tenant.did());
            }
            requireText(tenant.name(), "tenant.name");
            if (!tenantNames.add(tenant.name())) {
                throw new IllegalArgumentException(
                        "Duplicate Platform Dashboard tenant name: "
                                + tenant.name());
            }
            requireText(tenant.displayName(), "tenant.displayName");
        }

        Set<String> applicationIds = new HashSet<>();
        for (PlatformDashboardConfiguration.Application application
                : configuration.applications()) {
            requireUniqueId(application.id(), applicationIds, "application");
            requireText(application.name(), "application.name");
            requireText(application.description(), "application.description");
            PlatformApplicationPath.parse(application.href());
        }

        Set<String> componentIds = new HashSet<>();
        for (PlatformDashboardConfiguration.Component component
                : configuration.components()) {
            requireUniqueId(component.id(), componentIds, "component");
            requireText(component.name(), "component.name");
            requireText(component.category(), "component.category");
            PlatformHealthService.validate(
                    component.probe(), component.healthUri());
            component.consoleUri().ifPresent(
                    PlatformDashboardConfigurationValidator::validateConsoleUri);
        }
    }

    private static void requirePositive(
            java.time.Duration value, String field) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireUniqueId(
            String id, Set<String> observed, String kind) {
        requireText(id, kind + ".id");
        if (!observed.add(id)) {
            throw new IllegalArgumentException(
                    "Duplicate Platform Dashboard " + kind + " id: " + id);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requireDid(String value) {
        requireText(value, "tenant.did");
        int methodSeparator = value.indexOf(':', 4);
        if (!value.startsWith("did:")
                || methodSeparator < 5
                || methodSeparator == value.length() - 1
                || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "Platform Dashboard tenant DID is invalid: " + value);
        }
    }

    private static void validateOidcUrl(String value) {
        requireText(value, "oidcUrl");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "Platform Dashboard OIDC URL is invalid", invalid);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Platform Dashboard OIDC URL must be an HTTP(S) base URL "
                            + "without user info, query or fragment");
        }
    }

    private static void validateConsoleUri(java.net.URI uri) {
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Platform console URI must be an HTTP(S) URI without user info or fragment");
        }
    }
}
