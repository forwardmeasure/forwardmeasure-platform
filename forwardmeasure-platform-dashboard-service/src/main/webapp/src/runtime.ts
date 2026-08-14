import Keycloak, { type KeycloakTokenParsed } from "keycloak-js";
import { Configuration, PlatformApi } from "../generated-api/src/index";

export type RuntimeConfiguration = {
  tenantContextUrl: string;
  apiUrl: string;
  environment: string;
  applications: PlatformApplication[];
};

export type PlatformApplication = {
  id: string;
  name: string;
  description: string;
  href: string;
};

declare global {
  interface Window {
    __FORWARDMEASURE_PLATFORM_CONFIG__?: RuntimeConfiguration;
  }
}

export type PlatformIdentity = {
  keycloak: Keycloak;
  api: PlatformApi;
  displayName: string;
  tenantName: string;
  environment: string;
  applications: PlatformApplication[];
};

type Claims = KeycloakTokenParsed & {
  name?: string;
  preferred_username?: string;
  tenant_name?: string;
  tenant_did?: string;
};

type TenantContext = {
  tenant_did: string;
  tenant_name: string;
  display_name?: string;
  oidc_url: string;
  oidc_realm: string;
  oidc_client_id: string;
};

export async function initialize(): Promise<PlatformIdentity> {
  const value = window.__FORWARDMEASURE_PLATFORM_CONFIG__;
  if (!value?.tenantContextUrl || !value.apiUrl || !value.environment
      || !Array.isArray(value.applications)) {
    throw new Error("Platform Dashboard runtime configuration is incomplete");
  }
  const tenantResponse = await fetch(value.tenantContextUrl, {
    headers: { Accept: "application/json" },
    credentials: "same-origin",
  });
  if (!tenantResponse.ok) {
    throw new Error(
      `Unable to resolve tenant from this hostname (HTTP ${tenantResponse.status})`,
    );
  }
  const tenant = await tenantResponse.json() as Partial<TenantContext>;
  if (!tenant.tenant_did || !tenant.tenant_name || !tenant.oidc_url
      || !tenant.oidc_realm || !tenant.oidc_client_id) {
    throw new Error("Tenant bootstrap returned incomplete OIDC configuration");
  }
  const keycloak = new Keycloak({
    url: tenant.oidc_url,
    realm: tenant.oidc_realm,
    clientId: tenant.oidc_client_id,
  });
  const authenticated = await keycloak.init({
    onLoad: "login-required",
    checkLoginIframe: false,
    pkceMethod: "S256",
  });
  if (!authenticated || !keycloak.token) {
    throw new Error("Platform authentication did not produce an access token");
  }
  const claims = (keycloak.tokenParsed ?? {}) as Claims;
  if (claims.tenant_did !== tenant.tenant_did) {
    throw new Error("The authenticated account does not belong to this tenant");
  }
  const api = new PlatformApi(new Configuration({
    basePath: value.apiUrl.replace(/\/$/, ""),
    accessToken: async () => {
      await keycloak.updateToken(30);
      if (!keycloak.token) throw new Error("The platform session has expired");
      return keycloak.token;
    },
  }));
  return {
    keycloak,
    api,
    displayName: claims.name ?? claims.preferred_username ?? "Platform Operator",
    tenantName: claims.tenant_name
      ?? tenant.display_name
      ?? tenant.tenant_name,
    environment: value.environment,
    applications: value.applications,
  };
}
