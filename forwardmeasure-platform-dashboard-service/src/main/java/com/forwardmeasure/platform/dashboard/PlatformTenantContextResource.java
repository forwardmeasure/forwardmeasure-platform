package com.forwardmeasure.platform.dashboard;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.IDN;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the tenant bootstrap from the request hostname allowlist. */
@Path("/tenant-context")
public final class PlatformTenantContextResource {

    private final HttpHeaders headers;

    private final PlatformDashboardConfiguration configuration;

    private final Map<String, PlatformDashboardConfiguration.Tenant> tenants;

    @Inject
    public PlatformTenantContextResource(
            HttpHeaders headers,
            PlatformDashboardConfiguration configuration) {
        this.headers = Objects.requireNonNull(headers, "headers");
        this.configuration = Objects.requireNonNull(
                configuration, "configuration");
        this.tenants = configuration.tenants().stream().collect(
                Collectors.toUnmodifiableMap(
                        tenant -> canonicalHost(tenant.host()),
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalArgumentException(
                                    "Duplicate Platform Dashboard tenant host: "
                                            + duplicate.host());
                        }));
        if (tenants.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one Platform Dashboard tenant host must be configured");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response context() {
        final String host;
        try {
            host = canonicalHost(headers.getHeaderString(HttpHeaders.HOST));
        } catch (IllegalArgumentException invalidHost) {
            return problem(Response.Status.BAD_REQUEST, "invalid-host",
                    "Invalid Host", "The HTTP Host header is invalid");
        }
        PlatformDashboardConfiguration.Tenant tenant = tenants.get(host);
        if (tenant == null) {
            return problem(Response.Status.NOT_FOUND, "unknown-host",
                    "Tenant Not Found",
                    "This hostname is not assigned to a platform tenant");
        }

        var value = new LinkedHashMap<String, String>();
        value.put("tenant_did", tenant.did());
        value.put("tenant_name", tenant.name());
        value.put("display_name", tenant.displayName());
        value.put("oidc_url", configuration.oidcUrl());
        value.put("oidc_realm", configuration.oidcRealm());
        value.put("oidc_client_id", configuration.oidcClientId());

        var cache = new CacheControl();
        cache.setNoStore(true);
        return Response.ok(value)
                .cacheControl(cache)
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    static String canonicalHost(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("HTTP Host is required");
        }
        String value = raw.strip();
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon) {
            value = value.substring(0, colon);
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES)
                .toLowerCase(Locale.ROOT);
    }

    private static Response problem(
            Response.Status status,
            String type,
            String title,
            String detail) {
        return Response.status(status)
                .type("application/problem+json")
                .entity(Map.of(
                        "type", "https://problems.forwardmeasure.com/tenant/"
                                + type,
                        "title", title,
                        "status", status.getStatusCode(),
                        "detail", detail))
                .header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}
