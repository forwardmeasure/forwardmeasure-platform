# Tenant branding (white-label): design, 2026-10-12

**For:** engineers and coding agents building white-label support across the platform, both Studios
and the Platform Dashboard. The decisions below were made by the user on 2026-10-12. The facts
about today's code were checked in the repositories on that date, with file references.

## 1. Decisions

| # | Decision |
|---|---|
| D1 | A brand belongs to a **tenant**. Each tenant uses exactly one brand. Many tenants may share a brand, and `kriyagentic` is the default. |
| D2 | **One platform-branded sign-in page** serves every tenant. The tenant's brand applies from sign-in onwards. There is no per-tenant Keycloak theme and no realm per tenant. |
| D3 | **Option A first:** tenants and brands are declared in git (the Helm tenant list plus brand folders) and applied by redeploying. **Option B later:** the tenant administration API and the registry become the source of truth, with brand files in object storage. |
| D4 | Brand folders live in the **platform deploy config** (`deploy/helmfile/shared/brands/`), not in `forwardmeasure-presentation-system`. That repo is Apache-licensed and may be public, and customer logos must not sit in it. |
| D5 | **One shared tenant-context service, owned by `forwardmeasure-platform-operations`**, answers "which tenant is this hostname, and what is its brand?" for every app. It grows from the Platform Dashboard's existing `/tenant-context`. No app resolves tenants itself. |
| D6 | The platform name stays lower-case "kriyagentic" for now; revisit later. |

The design keeps the move from A to B cheap through one rule: **apps only ever call the
tenant-context contract (§4)**. Under A it reads a mounted ConfigMap. Under B it reads the
registry and object storage. The apps don't change.

## 2. Today (checked 2026-10-12)

- **Tenant declaration:** a Helm list in the cluster file:
  - `forwardmeasure-platform/deploy/helmfile/shared/clusters/gcp-openworkflow-prod.yaml.gotmpl:40`
    holds `(dict "alias" "lux" "displayName" "Lux Tenant")`;
  - lines 59–70 derive `host = <alias>.kriyagentic.com` and `did = did:web:<host>`;
  - `environments/shared-aliases.yaml.gotmpl:66-77` re-exposes the list as `tenants[{host, did, code, name}]`.
- **Provisioning: two paths, neither complete.**
  - **Helm hook Jobs:**
    - `openworkflow-migrations` creates the database and runtime role (`OpenWorkflowMigrationsMain`);
    - `openworkflow-tenant-reconciliation` (`TenantProvisioningMain`, `TenantOrganizationReconciler`) creates the Keycloak Organization, with attribute `forwardmeasure.tenant-did`;
    - the FEI and decision-engine migration Jobs add their schemas.
  - **REST API:** `POST /v1/tenants` (`openworkflow-tenant-administration`, `DefaultTenantProvisioning`) does Organization, database, registry row, capability packs and workflow bundles.
  - **Helm-only parts:** redirect URIs, certificate SANs, per-tenant secrets and the dashboard's tenant list exist only in the Helm list. They change only on redeploy.
- **Registry:** `tenant_registry` (Liquibase `forwardmeasure-jpa/forwardmeasure-jpa-liquibase/.../platform-registry.xml:31-61`) has tenant_id, tenant_did, alias, database_name, status, cell and timestamps. It has **no display name and no brand**. `listTenants` returns a null display name (`DefaultTenantProvisioning.java:132-145`).
- **Identity:** one realm, `forwardmeasure`; each tenant is an Organization inside it. Each tenant has its own host (`lux.kriyagentic.com`). Studio routes attach to the wildcard `tenant-https` listener, so every tenant host reaches the same backends.
- **Runtime config:**
  - The Studios' `config.js` is per deployment: platformDisplayName, OIDC settings, and for FEI the organization client ID.
  - OpenWorkflow's `StudioHost.configScript` reads the Host header for `apiBasePath`. FEI's `StudioResource` doesn't read the Host at all.
  - FEI's credentials release hard-codes `tenantAlias: lux` (`forwardmeasure-entity-intelligence/deploy/helmfile/releases/entity-intelligence-credentials/values.yaml:54`).
- **Hostname lookup before sign-in already exists, in the dashboard only:**
  - `GET /tenant-context` (`PlatformTenantContextResource`, `PlatformTenantResolver`) maps the Host header to the tenant DID, names and OIDC settings.
  - Its tenant list comes from Helm: `releases/platform-dashboard/base.yaml.gotmpl:37-43` → the chart's ConfigMap → `application-deployment.yaml` → `PlatformDashboardSettings.Tenant`.
  - Pods restart when it changes (a `checksum/config` annotation).
  - After sign-in the dashboard checks that the token's tenant matches.
- **Branding:**
  - Per deployment: `platform.branding.displayName` (`shared/deployment.yaml:21`).
  - Per tenant: only `displayName`.
  - The login theme is stock `keycloak.v2`. `forwardmeasure-platform-operations/operations-keycloak-theme` exists but has never been built or deployed.
- **Front end:** all three apps use `@forwardmeasure/presentation-system`. Its tokens, components and shared TopBar already follow the white-label rules:
  - role tokens only;
  - names passed in;
  - fonts through `--font-display`, `--font-sans` and `--font-mono`;
  - an empty logo slot.
  - The Studios' content security policy allows images and styles from `'self'` and `data:` only (`StudioHost.java:53-57`).

## 3. What a brand is

A brand is a folder:

```
deploy/helmfile/shared/brands/<brand>/
  brand.json     names and colour overrides
  logo.svg       optional; the TopBar's logo slot stays empty without it
  favicon.svg    optional; falls back to the default brand's
```

```json
{
  "key": "acme",
  "platformName": "Acme Intelligence",
  "colours": {
    "accent":       { "light": "#0b5cad", "dark": "#7fb2ea" },
    "accent-hover": { "light": "#094b8d", "dark": "#a3c8f0" },
    "on-accent":    { "light": "#ffffff", "dark": "#0b1622" },
    "accent-text":  { "light": "#0b5cad", "dark": "#7fb2ea" },
    "accent-soft":  { "light": "#e3eef9", "dark": "#14263a" }
  },
  "fonts": null
}
```

- **Brandable:**
  - the `accent` family (`accent`, `accent-hover`, `on-accent`, `accent-text`, `accent-soft`);
  - optionally `secondary` and `on-secondary`;
  - the platform name;
  - the logo and favicon.
  - Product names (Entity Intelligence Studio, OpenWorkflow Studio) stay the product's own.
- **Locked; a brand may never set them:**
  - surfaces, ink and lines;
  - the operational status colours (`danger`, `success`, `warning`, `info`, `on-status`);
  - every evidence band and its `-soft` tint;
  - sensitivity, quarantine and redaction;
  - `focus`, `scrim`, and the `chart-*`, `seq-*`, `div-*` and `conf-*` scales.
  - These carry meaning and have been contrast-checked; a customer's red must never read as "Failed".
- **Fonts:** `fonts` stays `null` until a customer needs one. Custom fonts mean bundling the files
  and checking their licences, so they are a later extension.
- **The brand check:** a new command in `forwardmeasure-presentation-system-web`,
  `node scripts/brand.mjs check|compile <folder>`. It rejects:
  - any locked or unknown token;
  - a missing light or dark value;
  - text pairs below 4.5:1 (`on-accent` on `accent` and on `accent-hover`; `accent-text` on
    `surface`, `surface-raised` and `accent-soft`);
  - control and focus pairs below 3:1;
  - SVGs with scripts, event handlers or external references.
- **Compiling:** the command compiles the brand to `brand.css`: overrides under
  `:root[data-theme="light"]` and `:root[data-theme="dark"]`, loaded after `tokens.css`.
- **The default brand:** `kriyagentic` is a brand folder with no colour overrides, so every tenant
  goes through the same path.

## 4. The tenant-context contract

A tenant's host also serves the platform's tenant-context endpoints. Every app's front end calls
them before sign-in. They are same-origin, so the Studios' content security policy needs no change.

```
GET /platform/tenant-context        (no authentication)
{
  "tenant_did": "did:web:lux.kriyagentic.com",
  "tenant_name": "lux",
  "display_name": "Lux Tenant",
  "oidc_url": "...", "oidc_realm": "forwardmeasure", "oidc_client_id": "...",
  "brand": {
    "key": "kriyagentic",
    "platform_name": "kriyagentic",
    "css": "/platform/brand/kriyagentic/brand.css",
    "logo": null,
    "favicon": "/platform/brand/kriyagentic/favicon.svg",
    "version": "sha256:…"
  }
}
GET /platform/brand/{key}/{file}     (no authentication; brand.css, logo.svg, favicon.svg only)
```

- **Display only, never authority.** The hostname lookup decides what the page looks like before
  sign-in, nothing else. After sign-in each app checks that the token's
  `forwardmeasure.tenant-did` equals `tenant_did` and refuses to continue if it doesn't, as the
  dashboard does today. APIs keep taking the tenant only from the verified token (the audit's
  Identity and Tenancy invariants).
- **Unknown hosts** return 404, with no brand and no tenant.
- **Caching:** brand files are served with long-lived caching, and their URLs carry `version` as a
  query string so a change takes effect at once. `tenant-context` is `no-store`.
- **Three frameworks:** the endpoints are built in the Quarkus, Spring and Micronaut bindings like
  the rest of platform-operations, through the existing three-way service pattern.

## 5. Option A: steps in build order

1. **Brand tooling** (`forwardmeasure-presentation-system`):
   - `scripts/brand.mjs` with `check` and `compile`, plus tests for every rejection rule in §3;
   - the package exports a small `applyBrand(context)` helper in `theme.js`, which loads `brand.css`,
     sets the favicon and returns the names for the TopBar.
2. **Brand folders and the tenant field** (`forwardmeasure-platform/deploy/helmfile`):
   - add `shared/brands/kriyagentic/`;
   - add `brand` to each tenant entry in the cluster files (`lux` → `kriyagentic`; the default when
     missing is `kriyagentic`);
   - carry `brand` through `shared-aliases.yaml.gotmpl` into `tenants[]`;
   - a Helmfile step runs `brand.mjs check` and fails the deploy on any error.
3. **Delivery** (`forwardmeasure-platform` releases):
   - Helmfile `readFile`s each brand folder that a tenant uses into a `platform-brands` ConfigMap,
     as `tenant-capability-packs.yaml.gotmpl` already does for capability packs;
   - the compiled `brand.css` is produced at render time by the check step.
   - The tenant-context service mounts the ConfigMap. A `checksum/config` annotation restarts its
     pods when a brand changes.
4. **Tenant-context service** (`forwardmeasure-platform-operations`):
   - move the dashboard's `PlatformTenantResolver` and `/tenant-context` into a shared module;
   - add the brand to `PlatformDashboardSettings.Tenant` and to the response;
   - add `/platform/brand/{key}/{file}`;
   - in all three framework bindings, route `/platform/*` on every tenant host;
   - tests through the REST API with real HTTP, including the unknown-host and wrong-file cases.
5. **Registry** (`forwardmeasure-jpa`): a new, forward-only Liquibase changeset adds
   `display_name` and `brand` to `tenant_registry`. Never edit the applied changeset. The tenant
   reconciliation Job and `DefaultTenantProvisioning` fill them, and `listTenants` returns them.
6. **Apps:**
   - **Both Studios:** the front end calls `/platform/tenant-context` before `keycloak.init`. It
     takes the OIDC settings from there instead of `config.js`, applies the brand through
     `applyBrand`, and checks the token's tenant after sign-in.
   - **FEI:** `StudioResource`'s `config.js` drops its now-unneeded per-deployment fields, and the
     credentials release stops hard-coding `tenantAlias: lux`.
   - **Dashboard:** moves to the shared endpoint.
7. **Checks:**
   - the presentation-system tests;
   - each app's unit tests;
   - a K3s test with two tenants on two brands, checking for each host that the page loads that
     tenant's colours, logo and names, and that signing in as the other tenant's user is refused;
   - the Helm render diff;
   - RAT on every changed repository.

**Onboarding a tenant under A:** add the tenant entry (and a brand folder if it's new), open a pull
request (the brand check runs in CI), merge, and redeploy. Everything else follows from the
Helm list.

## 6. Option B: the later retrofit

- **Source of truth:** `tenant_registry` and the tenant administration API.
  - `POST /v1/tenants` takes `brand`.
  - New endpoints, `PUT /v1/brands/{key}` and `PUT /v1/brands/{key}/files/{file}`, store brands.
  - The platform-operator role guards them.
- **Brand files:** a private `platform-brands` bucket written through `forwardmeasure-object-storage`
  (the Artifacts invariant), never a cloud SDK directly. The tenant-context service streams files
  out of it; the bucket stays private.
- **Brand check:** the Java API must apply the same rules as `brand.mjs`. To avoid two
  implementations drifting, define the rules (locked tokens, required pairs, ratios) as one JSON
  document that both the Node tool and the Java service read. Decide this when B starts.
- **The Helm-only parts become runtime work:**
  - Keycloak redirect URIs and web origins move to the reconciler;
  - per-tenant secrets move to the External Secrets flow;
  - certificate SANs move to a wildcard certificate (already `*.host` today) or to cert-manager per host.
- **What doesn't change:** the tenant-context contract (§4) and every app. The service swaps its
  ConfigMap reader for a registry-and-bucket reader behind the same interface.

## 7. Open questions

1. **Sign-in hint:** should the shared sign-in page receive the tenant as a hint
   (Keycloak's `organization` scope), so a person with access to several tenants lands in the right
   one? This doesn't affect branding.
2. **Where brands appear outside the apps:** emails sent by Keycloak or the platform, PDF and CSV
   exports, and screening reports. They are not in scope for the first pass; list them before B.
3. **Custom fonts for a brand:** later, once a customer asks.
