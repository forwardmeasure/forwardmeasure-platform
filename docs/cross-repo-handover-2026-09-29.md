# Cross-repository deployment and database changes — handover, 2026-09-29

**For:** the agents working in forwardmeasure-openworkflow (fowf), forwardmeasure-entity-intelligence
(fei), forwardmeasure-data-streaming (fds) and forwardmeasure-decision-engine (fde). fde also has its
own handover (`forwardmeasure-decision-engine/docs/handover-2026-09-29-deployment-readiness.md`) and
is covered here only in §0 and where it touches your repository.

**How to use this:** read §0 first (the greenfield reset - it gates the next install), then §1–§2
(the shared model, which applies to every repository), then your own repository's section in §3.
Each section lists what changed, how to verify it, and what to update or drop in your memory. §4
lists what is still open.

**Status.** Nothing forwardmeasure-* is deployed anywhere (prod or dev), so everything below was
changed cleanly - renames and drops with no compatibility fallbacks. Some of it is committed and
pushed, some is still uncommitted in the working trees (marked per item). Everything was verified by
rendering and compiling, not by running on a cluster; the tests listed in §3 have not been run yet.
The prod cluster and its Cloud SQL instance have since been emptied for a from-scratch install (§0).

---

## 0. Read first: greenfield reset (later on 2026-09-29)

The next install into `gcp-openworkflow-prod` is a **complete, from-scratch install**. It waits
until fei, fds and fde have each worked through this document and reported ready.

### 0.1 State of the prod cluster and database (verified live)
- **Every Helm release is uninstalled** from the `openworkflow-prod` GKE cluster (platform and fowf;
  fei, fds and fde had nothing installed). **All data volumes are deleted** (Kafka, OpenSearch,
  Valkey). Namespaces were kept - the uninstall scripts keep them by design.
- **Cloud SQL `openworkflow-prod-postgres` holds only the `postgres` database and user**, which Cloud
  SQL creates itself. Every other database (including `forwardmeasure_lux`) and every role is gone.
  The instance itself is kept.
- **Let's Encrypt certificates are preserved.** Their seven TLS Secrets (six in `istio-system`, one in
  `keycloak`) are not Helm-owned and survive uninstall, so cert-manager reuses them on reinstall.
  **Do not delete the `istio-system` or `keycloak` namespaces:** that forces seven reissues against
  Let's Encrypt's weekly limits (5 per exact hostname set - `auth.kriyagentic.com` has two
  Certificates - and 50 per registered domain).

### 0.2 The admin role is renamed: `openworkflow_migration` → `forwardmeasure_admin`
It is the one admin every product's migration Job (and fowf's tenant-administration) connects as,
and it owns every tenant database, so the old product-specific name was misleading.
- Changed in: both Terraform roots (openworkflow-k8s-setup `database_administrator_username`, now
  also set explicitly in prod's `terraform.tfvars`; forwardmeasure-platform's `cloudsql_users` and its
  test), fowf's `database.administratorUsername` (`gcp-platform-cluster.yaml.gotmpl`,
  `production-multi-engine.yaml.gotmpl`), and platform's unused `identity.openworkflowAdministratorUsername`
  (renamed `identity.databaseAdministratorUsername`).
- Products receive it through fowf's migrations helmfile. Rendered for `gcp-openworkflow-prod`:
  `OPENWORKFLOW_`, `ENTITY_INTELLIGENCE_` and `DECISION_ENGINE_ADMIN_DATABASE_USERNAME` are all
  `forwardmeasure_admin`.
- fei, fds and fde contain no reference to the old name (grep-verified, docs included), so there is
  **no code change for you**. Update any memory or notes that name `openworkflow_migration`.
  (`openworkflow_migrations`, plural, is an unrelated fowf Cassandra keyspace / test database name -
  unchanged.)

### 0.3 Who creates what on the fresh install
| What | Created by | Password set by |
|---|---|---|
| `postgres` database and user | Cloud SQL | - (unused) |
| `forwardmeasure_admin` | Terraform | Terraform (generated; Secret Manager `<name>-database-administrator`) |
| `forwardmeasure_control_plane`, `keycloak`, `superset`, `nominatim`, `kriyagentic` databases | Terraform | - |
| Component users `keycloak`, `superset`, `nominatim`, `kriyagentic` | Terraform | Terraform, from `TF_VAR_cloudsql_user_passwords` |
| Tenant databases `forwardmeasure_<alias>` | migration Jobs, as `forwardmeasure_admin` | - |
| Product runtime roles (`openworkflow`, `entityintelligence`, `decision_engine`) | migration Jobs | migration Jobs |

Migration Jobs never touch component users' passwords or permissions (§4 item 2). `nominatim` and
`kriyagentic` are new: they have no consumer yet. The prod import mechanism for pre-existing roles
(`cloudsql_existing_users`) is no longer needed - nothing pre-exists - and is unset in prod.

### 0.4 New or changed tooling
- `openworkflow-k8s-setup/scripts/reset-databases.sh` empties the instance down to `postgres`. It
  drops through SQL from a throwaway in-cluster psql pod as the admin: the instance is private-IP
  only, and the Cloud SQL API refuses to delete a database not owned by `cloudsqlsuperuser` (which
  failed on `keycloak`, reassigned by the old migration Job).
- Both `uninstall.sh` scripts (forwardmeasure-platform, fowf) now pass `--deleteWait` to
  `helmfile destroy`. Before this, an operator was uninstalled while its custom resources were still
  terminating, so their finalizers never ran and the workloads the operator created were orphaned
  (it happened to Knative Serving/Eventing on this teardown).

### 0.4a Our images are deployed by digest only (user decision, 2026-10-01)
- Every image of ours (`[docker.io/]forwardmeasure/...`) is pinned by digest on cloud installs;
  third-party images may be pinned or float by tag. kind/local are exempt (they load local images
  by tag).
- `forwardmeasure-platform/deploy/scripts/sync-image-digests.sh` (run by `install-platform.sh`)
  now resolves **every** entry of ours, including empty `digest: ""`, and fails the install,
  listing all of them, if any can't be resolved (not pushed). `--check` reports without writing.
  fowf's `resolve-image-digests.sh` follows the same rule.
- `check-image-digests.sh` checks every product's **render**; `validate-platform.sh` runs it and
  `install-platform.sh` now validates before installing. It catches a template that drops a digest.
- **For your repository:** keep a `digest:` field on every image entry of yours in your
  `environments/base.yaml(.gotmpl)` (the install fills it), and make sure every template that
  renders one of your images uses the digest when it is set. The first install writes the
  resolved digests into your file; commit them.

### 0.4b Tenant roles, workflow publishing and lookup by name (fowf, 2026-10-01)
Full design and progress: `forwardmeasure-openworkflow/docs/workflow-lookup-and-publishing-handoff-2026-10-01.md`.
In fowf source; not yet built or deployed.

- **Tenant role groups now carry each role on two clients.** fowf's tenant reconciler maps a role
  group's role on `openworkflow` (what the AuthZEN `organization-role` policy checks) and on the
  organization client `forwardmeasure-public` (what services read from the token). Before, it did
  only the first, so every member got 401. A member of a reconciled group now passes both. If your
  bootstrap maps a fowf role onto a tenant's group, the group already has both mappings.
- **One publisher for every product's workflows.** A product ships its workflow YAMLs as a release
  of helm-charts' `openworkflow-workflow-bundle` chart, with:
  - chart version: `chartVersions.openworkflowWorkflowBundle` (shared layer; first publish 0.1.1);
  - image: `imageVersions.openworkflowWorkflowPublisher`, by digest;
  - `domain`: your moniker;
  - `tenants`: the platform tenant aliases;
  - fowf's definition-management and token URLs.

  Install it in fowf's namespace, where the publisher identity Secret `openworkflow-bootstrap`
  lives. Workflows are named `<domain>-<document.name>`, in every listed tenant.
- **Find workflows by name, not by configured ID:** `GET /v1/workflows?name=<domain>-<name>`
  (generated Java client: `listWorkflows(name, offset, limit)`), then the published revision as
  today. Workflow IDs differ per tenant, so a configured ID can't work.
- **FEI, before the install:**
  1. Ship your five workflows as a bundle with domain `fei`, and switch the five `*_WORKFLOW_ID`
     settings to name lookup (`fei-<name>`). Then delete `WorkflowDefinitionPublisherMain` and its
     image (handoff §6). Until then, fowf's `entity-intelligence-workflows` release keeps running
     your publisher, now with fowf's pair under the same Secret keys.
  2. **Done for you (fowf session, 2026-10-01):** `entity-intelligence-credentials` used a fixed
     `openworkflow` client UUID (`00000000-0000-0000-0000-000000000001`, which exists only in fowf's
     acceptance realm), so on the platform its hook Job would fail the install. It now looks the
     client up by `fowfClientId`; `fowfClientUuid` is gone. FDS's `data-streaming-credentials` had
     the same bug and got the same fix (`clientUuid` removed from its values and
     `helmfiles/launcher.yaml.gotmpl`). Both render, and `sh -n` passes on both scripts.
  3. **Publish your API documents to the registry (added 2026-10-02).** Your workflows reference
     `https://specs.forwardmeasure.com/...`, which doesn't exist, so every workflow fails
     validation (your K3s test shows it). fowf's side is done: definition-management allows the
     platform registry automatically, and the bundle chart uploads documents before publishing.
     - **Bundle release values:** `registry.apiUrl: {{ .Values.platform.endpoints.registry.apiUrl }}`
       and `apiDocuments:` with `ingestion-worker-kubernetes-job.yaml`
       (`api-specifications/src/main/resources/asyncapi/`) and `entity-intelligence-api.yaml`
       (`.../META-INF/openapi/`), read with `readFile` as you do the definitions. They become
       artifacts `fei/ingestion-worker-kubernetes-job` and `fei/entity-intelligence-api`.
     - **The five definitions:** every `document.endpoint` becomes
       ```yaml
       endpoint:
         uri: http://platform-registry-app-service.apicurio-registry.svc.cluster.local:8080/apis/registry/v3/groups/fei/artifacts/<artifact>/versions/branch=latest/content
         authentication:
           oidc:
             use: apicurio-registry-reader
       ```
       (`apicurio-registry-reader` is the client-credentials secret fowf's platform-clients
       provisions for definition-management.)
     - **K3s test:** deploy a real Apicurio (`quay.io/apicurio/apicurio-registry:3.3.1`, no auth)
       as Service `platform-registry-app-service` in namespace `apicurio-registry`, upload with
       `RegistryResourceUploader.withoutAuthentication(...)`, and give definition-management
       `OPENWORKFLOW_DEFINITION_RESOURCE_ALLOWED_HOSTS` (that host and Keycloak's) and
       `OPENWORKFLOW_DEFINITION_RESOURCE_AUTH_SECRETS` with an `apicurio-registry-reader` entry
       (a real client in the test realm; the loader fetches a token whenever a policy is declared).
       Drop the `fei-fixtures` AsyncAPI copy.
     - Your compiler tests that resolve these documents by URL need the new URLs.
  4. **`PublishedWorkflowRevisionResolver` picks the oldest published revision (found
     2026-10-02).** A `status=PUBLISHED` listing is ordered by ascending revision number, and the
     resolver takes the first entry with `limit=1`. fowf's publisher now deprecates superseded
     revisions, so bundle-published workflows have exactly one. But a revision published by hand
     (Studio) leaves several, and FEI would then launch the oldest. Request `limit=100` and take
     the latest `publishedAt`.
  5. When the launcher adopts `subjectActor`, set `launcherRole: workflow-operator`
     (`forwardmeasure-openworkflow/docs/subject-actor-fei-adoption-handoff-2026-09-25.md`).

### 0.5 What each repository must do before the install
- **fei:** everything in §3.2, including the **§3.2.1 e2e harness fix**, and the §4 item 3
  readiness items. Run §3.2's verify commands.
- **fds:** run §3.3's verify commands; nothing new from the reset.
- **fde:** work through your own handover; §1.3–§1.6 and §0 apply to you.
- **Everyone:** reply to the user with "ready for the full install" plus what you ran and what
  passed, or what is blocking you.

After all three report ready: `tofu apply` in openworkflow-k8s-setup (§2), then
`install-platform.sh gcp-openworkflow-prod`, then live verification. That verification checks that
the certificates were reused, that Keycloak boots against its fresh database, and that the migration
Jobs create the control-plane tables and tenant databases.

---

## 1. The shared model (applies to every repository)

### 1.1 Shared values layer — committed
`forwardmeasure-platform/deploy/helmfile/shared/` is the single source of truth for every value more
than one repository deploys with. Every repository's helmfile loads it directly, **last**, by relative
path from the sibling checkout (`../../../forwardmeasure-platform/deploy/helmfile/shared/...`).
Nothing is copied any more: `platform-endpoints.yaml.gotmpl` copies, `sync-platform-endpoints.sh` and
`sync-chart-versions.sh` are gone.

| File | Holds |
|---|---|
| `common.yaml.gotmpl` | shared namespaces, in-cluster endpoints (Keycloak, Kafka, OpenSearch, Valkey, fowf APIs), identity realm and org client, Gateway API version, platform-delivered Secret names (`platform.secrets.*`), `platform.databases.*` |
| `chart-versions.yaml` / `image-versions.yaml` | pins used by more than one repository (java-microservice chart, cloud-sql-proxy, fds's kafka-streams executor) |
| `clusters/<env>.yaml.gotmpl` | facts about one cluster: domain, gateway IP and hostnames, **tenants** (alias + displayName; host/DID derived), GCP project/region/cluster, Cloud SQL instance, per-product workload-identity GSAs, fowf's overflow bucket |

fds/fei/fde read `.Values.platform.*` directly. fowf maps the shared values onto its own keys in
`environments/platform-derived.yaml.gotmpl` (loaded after its other environment files).

Verified helmfile 1.3.2 behaviour worth knowing: an environment values file cannot see values loaded
before it (use `readFile`+`tpl`); `readFile` resolves relative to that file's own directory; a
sub-helmfile gets no values from its parent unless passed explicitly; `needs` does not cross
sub-helmfiles.

### 1.2 Environments — committed
Every repository declares `gcp-openworkflow-prod` and `gcp-greenfield-example`. Each product's old
`environments/gcp-openworkflow-prod.yaml.gotmpl` was renamed (git mv) to a cluster-neutral profile,
`environments/gcp-platform-cluster.yaml.gotmpl`, loaded by both; the two environments differ only in
which shared cluster file they load. `install-platform.sh <env>` and `validate-platform.sh <env>` run
for either. Platform CI (`.github/workflows/platform-verify.yml`, job `helmfile-validate`) validates
forwardmeasure-platform for `base`, `gcp-openworkflow-prod` and `gcp-greenfield-example`, and renders
fowf, fei, fds and fde for both GCP environments.

### 1.3 Databases — uncommitted
- **No product has a database of its own.** Tenant data lives in one database per tenant,
  `forwardmeasure_<alias>` (e.g. `forwardmeasure_lux`), one schema per product (`openworkflow`,
  `entity_intelligence`, `decision_intelligence`). fds has no database at all.
- **One control-plane database, `forwardmeasure_control_plane`** (shared
  `platform.databases.controlPlane`): `tenant_registry` (tenant → tenant-database mapping) and Pekko's
  cluster-wide coordinator store. It replaces both the old shared `openworkflow` database (a stopgap
  from the database-per-tenant redesign, commit 343e082) and the never-used `platform` database; both
  are removed from Terraform. forwardmeasure-jpa's `TenantDatabase` now rejects the alias
  `control_plane`, so no tenant can ever be provisioned into it.
- **Two connections per server.** A fixed control-plane URL (tenant registry lookups, Hibernate
  bootstrap), and per-tenant connections built at request time from a host/port prefix plus the
  database name `TenantDatabaseResolver` finds in `tenant_registry`. Tenant database names never
  appear in configuration.
- **Tenant identity** is alias-only; the tenant UUID is never stored - always `TenantId.forDid(did)`
  (`did:web:<alias>.<domain>`). platform's old hardcoded `tenants[].id` is gone.

### 1.4 Database environment variables, renamed by role — uncommitted
`<P>` = `OPENWORKFLOW`, `ENTITY_INTELLIGENCE`, `DECISION_ENGINE`. No fallback to the old names.

| New name | Meaning | Replaces |
|---|---|---|
| `<P>_CONTROL_PLANE_DATABASE_URL` | control-plane JDBC URL | `<P>_DATABASE_URL`, `OPENWORKFLOW_TENANT_REGISTRY_URL` |
| `<P>_TENANT_DATABASE_HOST` / `_PORT` | where tenant databases live | `<P>_DATABASE_HOST` / `_PORT` |
| `<P>_RUNTIME_DATABASE_USERNAME` / `_PASSWORD` | runtime role (servers, both connections; migration Jobs create it) | servers' `<P>_DATABASE_USERNAME/PASSWORD`, `OPENWORKFLOW_TENANT_REGISTRY_USERNAME/PASSWORD` |
| `<P>_ADMIN_DATABASE_USERNAME` / `_PASSWORD` | admin role (migration Jobs, fowf tenant-administration) | migration Jobs' `<P>_DATABASE_USERNAME/PASSWORD` |

Also: Secret key `OPENWORKFLOW_DATABASE_PASSWORD` → `OPENWORKFLOW_RUNTIME_DATABASE_PASSWORD`;
`application.yaml` fallback URLs → `forwardmeasure_control_plane`; runtime role names are shared
contracts (`platform.databases.runtimeRoles`: `decision_engine`, `entityintelligence`).

### 1.5 Runtime roles, grants and passwords — uncommitted
- New `OpenWorkflowTenantMigrator.grantTenantRegistryRead()` (fowf migrations module): creates
  `tenant_registry` if absent, then grants the runtime role `SELECT` on it. Called by fowf's, fei's and
  fde's migration mains. Before this, nothing granted it - the live grant had been applied by hand.
- forwardmeasure-platform's platform-secrets now delivers `ENTITY_INTELLIGENCE_RUNTIME_DATABASE_PASSWORD`
  (was misspelled `ENTITYINTELLIGENCE_...`) and `DECISION_ENGINE_RUNTIME_DATABASE_PASSWORD` into fowf's
  credentials Secret, where the migration Jobs read them. Without them both Jobs' pods could not start.

### 1.6 Cluster prerequisites for product namespaces — uncommitted
- `forwardmeasure-platform/deploy/helmfile/manifests/namespaces.yaml` now creates `data-streaming`,
  `entity-intelligence` and `decision-engine` (platform-secrets writes into them; nothing created them).
- The image-pull Secret (`docker-io-credentials`) is now also delivered to `entity-intelligence` and
  `decision-engine`.
- Cloud SQL Auth Proxy: products take the instance connection name from their own database-credentials
  Secret (key `db-cloud-sql-instance`, composed by platform-secrets from the shared cluster file) via
  `cloudSqlProxy.existingSecretName` - no separate `cloudsql` Secret. Workload identity: each product's
  ServiceAccount is annotated with its GSA from the shared cluster file; prod GSAs are declared in
  openworkflow-k8s-setup's gitignored `terraform/gcp/terraform.tfvars`, new clusters' in
  forwardmeasure-platform's `k8s-infrastructure/opentofu/gcp/variables.tf`.

---

## 2. Where things live that git does not show

- Real prod secret values are in **gitignored** files on the machine that ran this work:
  `forwardmeasure-platform/deploy/helmfile/releases/platform-secrets/gcp/openworkflow-prod.credentials.yaml.gotmpl`
  (includes two generated runtime passwords) and `openworkflow-k8s-setup/terraform/gcp/terraform.tfvars`
  (control-plane database, fei/fde workload identities). Anyone else running installs needs them.
- Live order for the full install (§0): `export TF_VAR_cloudsql_user_passwords` with all four
  component users (`keycloak`, `superset`, `nominatim`, `kriyagentic`); `tofu apply` in
  openworkflow-k8s-setup (creates the databases, `forwardmeasure_admin` and the component users on
  the emptied instance, creates fei/fde GSAs); then `install-platform.sh gcp-openworkflow-prod`; then
  live verification.

---

## 3. Per repository

### 3.1 forwardmeasure-openworkflow

**Changed.**
- Committed: `openworkflow-foundation` → `openworkflow-bootstrap`; shared-layer loading via
  `platform-derived.yaml.gotmpl` (namespaces, Keycloak, fowf URLs, Gateway API version, identity Secret;
  on platform clusters also domain, auth host, Kafka, GCP project, Cloud SQL instance, tenants,
  workload-identity GSAs, overflow bucket); `gcp-platform-cluster.yaml.gotmpl` profile +
  `gcp-greenfield-example`; `validate.sh` treats greenfield as production.
- Uncommitted: `OpenWorkflowTenantMigrator.grantTenantRegistryRead()` + `TenantRegistryReadGrantTest`;
  every `helmfiles/*.yaml.gotmpl` defines `$controlPlaneDatabaseUrl` from `database.host`/`port` + the
  shared name (an explicit `database.url` still wins - production-multi-engine), used for the
  control-plane URL, Pekko's persistence endpoint and the fei/fde migrations URLs; GCP profile sets
  `database.host: localhost`; openworkflow-bootstrap's local Postgres initial database is
  `forwardmeasure_control_plane`; the §1.4 rename across helmfiles, all `application.yaml` files, k3s
  tests and scripts (the duplicate env entries left by folding `TENANT_REGISTRY_*` were removed);
  **tenant-administration is admin context** (`OPENWORKFLOW_ADMIN_DATABASE_*` for its own connection,
  `OPENWORKFLOW_RUNTIME_DATABASE_USERNAME` for the role it creates); fei/fde migrations take their
  runtime role names from the shared layer.
- Details: `docs/tenant-registry-grant-and-runtime-passwords-2026-09-29.md`.

**Verify.**
```
mvn -f openworkflow-deployments/migrations/pom.xml test | tee /tmp/mvn.out
OPENWORKFLOW_VERSION=ci-render deploy/helmfile/validate.sh gcp-openworkflow-prod
OPENWORKFLOW_VERSION=ci-render deploy/helmfile/validate.sh local
```
The k3s suites (`engine-pekko/k3s-verification`) need images rebuilt with the renamed variables before
they run.

**Memory: drop** `OPENWORKFLOW_DATABASE_URL`/`_USERNAME`/`_PASSWORD` and `OPENWORKFLOW_TENANT_REGISTRY_*`;
"the control plane / tenant registry is the `openworkflow` database"; `gcp-openworkflow-prod.yaml.gotmpl`
as a file name; synced `platform-endpoints.yaml.gotmpl`; `openworkflow-foundation`.
**Record** §1.3–§1.5, and that tenant-administration uses the admin role.

### 3.2 forwardmeasure-entity-intelligence

**Changed.**
- Committed: helmfile replaces `deploy/helm`; shared-layer loading (`.Values.platform.*`); credentials
  bootstrap's `tenantAlias` from the shared tenant list; `gcp-platform-cluster.yaml.gotmpl` +
  `gcp-greenfield-example`; dead `gcpProjectId`/`imagePullSecretName`/`gcp.yaml` removed.
- Uncommitted: the services previously pointed at `REPLACE-ME:5432/entityintelligence` (a database of
  their own that does not exist) and read `db-password` from `entity-intelligence-credentials`, which
  nothing writes - they could not have started. Now every service's control-plane URL is
  `jdbc:postgresql://localhost:5432/forwardmeasure_control_plane` through its own Cloud SQL Auth Proxy
  sidecar, as the `entityintelligence` role; credentials come from
  `entity-intelligence-database-credentials` (platform-secrets: `username`, `password`,
  `db-cloud-sql-instance`), kept separate from `entity-intelligence-credentials` (still written by this
  repo's Keycloak bootstrap Job); the release runs under a new `entity-intelligence` ServiceAccount
  annotated with its GSA (prod `openworkflow-prod-entity-intel@…`; new clusters `entity-intelligence@…`
  with `roles/cloudsql.client`). §1.4 rename in all nine service `application.yaml` files, the release,
  `EntityIntelligenceMigrationsMain` and the k3s test; fallback URLs → `forwardmeasure_control_plane`;
  `grantTenantRegistryRead()` called from the migrations main and `EntityIntelligenceTenantProvisioner`.
  The Studio e2e `environment.ts` key was renamed to `ENTITY_INTELLIGENCE_RUNTIME_DATABASE_PASSWORD`.
- The old "collision risk" comment is gone: admin vs runtime is now structural in the names.

**Verify.**
```
mvn -f forwardmeasure-entity-intelligence-migrations/pom.xml test | tee /tmp/mvn.out
helmfile --file deploy/helmfile/helmfile.yaml.gotmpl -e gcp-openworkflow-prod template
```
In the render: each service has a `cloud-sql-proxy` container reading
`entity-intelligence-database-credentials/db-cloud-sql-instance`, `serviceAccountName: entity-intelligence`,
and `ENTITY_INTELLIGENCE_CONTROL_PLANE_DATABASE_URL=jdbc:postgresql://localhost:5432/forwardmeasure_control_plane`.
The ingestion k3s test needs rebuilt images.

**Memory: drop** `ENTITY_INTELLIGENCE_DATABASE_*`, `entityintelligence` as a database name, the
`db-password` key in `entity-intelligence-credentials`, `ENTITYINTELLIGENCE_RUNTIME_DATABASE_PASSWORD`,
the old collision-risk warning. **Record** §1.3–§1.6 and the ServiceAccount/Secret names above.

#### 3.2.1 FEI must fix: the Studio Playwright e2e harness cannot boot

**Required, not optional.** Found 2026-09-29 by the fowf agent while verifying §3.2; checked against
the real `application.yaml` files and Java mains, not inferred.

**Where:** `forwardmeasure-entity-intelligence-studio/webapp/src/main/webapp/tests-e2e/support/`
- `environment.ts` (`bootEnvironment()`, everything from Layer 1 to Studio)
- `milestone3.ts` (`runWorkflowDefinitionPublisher`, `seedSimpleSourceIngestionPipeline`, and the
  ingestion-service restart block)
- `keycloakAdmin.ts` (`provisionTenant` writes the tenant id you pass it; no change needed beyond
  passing the right value)

**What is wrong.** The harness was last committed on 2026-09-18 and still encodes three generations
of models that no longer exist. Today's `ENTITY_INTELLIGENCE_RUNTIME_DATABASE_PASSWORD` edit renamed
one key; everything else is unchanged:
1. **Retired env-var spelling.** Every fei key uses `ENTITYINTELLIGENCE_*` (no underscore), retired
   2026-09-19. The services default almost every key to something plausible (`localhost:5432`,
   role `entityintelligence`), so they don't fail on config. They boot pointed at nothing and fail
   later, which is why this went unnoticed.
2. **Retired single fowf URL.** `…_FOWF_BASE_URL` was split into `…_FOWF_EXECUTION_BASE_URL` and
   `…_FOWF_DEFINITION_BASE_URL` on 2026-09-20; the publisher now *requires* the definition one.
3. **Retired database model.** `DB_NAME = "openworkflow"` (the shared database, now gone) and, in
   `milestone3.ts`, `SET search_path TO t_<uuid>` (the schema-per-tenant model before that).
4. **Random tenant id.** `tenantId = crypto.randomUUID()`. The tenant id must be
   `TenantId.forDid(did:web:<alias>.<domain>)` (see the DID-first rule in
   `forwardmeasure-openworkflow/docs/tenant-provisioning-did-first-fix-2026-09-18.md`).
5. **No tenant registration.** Services resolve every request's database via the org's
   `forwardmeasure.tenant-id` → `tenant_registry` → `forwardmeasure_<alias>`. Only fowf's
   `openworkflow-migrations` Job writes `tenant_registry` (`EntityIntelligenceMigrationsMain`
   deliberately doesn't), and the harness never runs it, so every tenant-scoped request would fail
   to resolve a database even with the names fixed.

**What to build.** Reproduce production's shape rather than renaming keys:
1. **Postgres:** `POSTGRES_DB: "forwardmeasure_control_plane"`. Recommended: create a non-superuser
   admin (`CREATE ROLE … LOGIN CREATEDB CREATEROLE NOSUPERUSER`, like Cloud SQL's
   `forwardmeasure_admin`) and run both migration containers as it. The container superuser skips
   ownership/membership checks and hides grant bugs (precedent: fowf's `TenantDatabaseOwnershipTest`).
2. **Tenant identity:** pick an alias (the org alias, e.g. `fei-e2e-org`; must match
   `[a-z][a-z0-9_-]{0,47}`) and a domain (e.g. `e2e.test`). Derive the id and pass it to
   `provisionTenant` as the `forwardmeasure.tenant-id` attribute:
   ```ts
   import { createHash } from "node:crypto";
   // TenantId.forDid: UUID.nameUUIDFromBytes(did UTF-8) - type-3 MD5 UUID.
   export function tenantIdForDid(did: string): string {
     const b = createHash("md5").update(did, "utf8").digest();
     b[6] = (b[6] & 0x0f) | 0x30;
     b[8] = (b[8] & 0x3f) | 0x80;
     const h = b.toString("hex");
     return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
   }
   // Test vector: tenantIdForDid("did:web:lux.kriyagentic.com") === "50b78292-e7b9-33ed-b6ad-8f9671278ff9"
   ```
3. **Two migration containers in Layer 2, both before any service:**
   - fowf `openworkflow-migrations` (new, same tag as the harness's other fowf images):
     `OPENWORKFLOW_CONTROL_PLANE_DATABASE_URL`, `OPENWORKFLOW_ADMIN_DATABASE_USERNAME/_PASSWORD`,
     `OPENWORKFLOW_RUNTIME_DATABASE_USERNAME/_PASSWORD`, `OPENWORKFLOW_TENANTS=<alias>:<display name>`,
     `OPENWORKFLOW_TENANT_DOMAIN=<domain>` (same domain as step 2). It creates the tenant database,
     the `openworkflow` schema and runtime role, and registers the tenant.
   - fei `entity-intelligence-migrations`: `ENTITY_INTELLIGENCE_CONTROL_PLANE_DATABASE_URL`,
     `ENTITY_INTELLIGENCE_ADMIN_DATABASE_USERNAME/_PASSWORD`,
     `ENTITY_INTELLIGENCE_RUNTIME_DATABASE_USERNAME/_PASSWORD`,
     `ENTITY_INTELLIGENCE_TENANTS=<alias>:<display name>` (replaces `ENTITYINTELLIGENCE_TENANT_IDS`,
     which took a UUID).

   They can run concurrently (both provision idempotently; Liquibase locks serialize).
4. **Separate runtime roles, as in production:** `openworkflow` for fowf, `entityintelligence` for
   fei, instead of one shared `app_runtime`. Since 2026-09-29 each migration Job grants database
   `CONNECT` only to its own runtime role, so a shared role would hide exactly the grant bugs this
   harness should catch.
5. **fowf services** (`commonDbEnv`): control-plane URL → `forwardmeasure_control_plane`, add
   `OPENWORKFLOW_TENANT_DATABASE_HOST=postgres` / `OPENWORKFLOW_TENANT_DATABASE_PORT=5432`;
   engine-pekko's `OPENWORKFLOW_PERSISTENCE_ENDPOINT` → the control-plane URL.
6. **fei services, renames** (apply in `environment.ts` and both `milestone3.ts` blocks):

   | Old | New |
   |---|---|
   | `ENTITYINTELLIGENCE_DATABASE_URL` | `ENTITY_INTELLIGENCE_CONTROL_PLANE_DATABASE_URL` (→ `forwardmeasure_control_plane`) |
   | `ENTITYINTELLIGENCE_DATABASE_USERNAME/_PASSWORD` (services) | `ENTITY_INTELLIGENCE_RUNTIME_DATABASE_USERNAME/_PASSWORD` |
   | *(none)* | `ENTITY_INTELLIGENCE_TENANT_DATABASE_HOST=postgres`, `_PORT=5432` (new, required for per-tenant routing) |
   | `ENTITYINTELLIGENCE_{INGESTION,SCREENING}_FOWF_BASE_URL` | `…_FOWF_EXECUTION_BASE_URL` + `…_FOWF_DEFINITION_BASE_URL` (point directly at execution-/definition-management) |
   | publisher's `ENTITYINTELLIGENCE_INGESTION_FOWF_BASE_URL` | `ENTITY_INTELLIGENCE_INGESTION_FOWF_DEFINITION_BASE_URL` (required) |
   | every other `ENTITYINTELLIGENCE_<X>` | `ENTITY_INTELLIGENCE_<X>` (including all `…_STUDIO_*`, `…_FOWF_KEYCLOAK_*`, worker images, namespaces) |

   Also set the ingestion keys added since 2026-09-18 that the harness has never set:
   `ENTITY_INTELLIGENCE_INGESTION_WORKER_API_BASE_URL`,
   `ENTITY_INTELLIGENCE_INGESTION_WORKER_KEYCLOAK_TOKEN_URL`,
   `ENTITY_INTELLIGENCE_INGESTION_RESOLUTION_DELIVERY_WORKER_IMAGE` (blank-default `String`s hit the
   same SRCFG00040 boot failure the harness's own comments already describe for their siblings).
7. **`milestone3.ts` direct SQL:** connect to database `forwardmeasure_<alias>` as the admin and
   `SET search_path TO entity_intelligence` (not `t_<uuid>` in `openworkflow`).
8. **Images:** the harness pins published `:1.1.0` tags, which were built before today's env-var
   rename and read the old names. Rebuild every fowf/fei image it uses from the current working tree
   before running.
9. **Guard against a repeat:** add an assertion in `bootEnvironment()` that no container env key
   matches `/^ENTITYINTELLIGENCE_/` or `/^OPENWORKFLOW_(DATABASE|TENANT_REGISTRY)_/`. Nothing compiles
   these string keys, which is how the harness drifted for ten days.

**Verify.** `npx tsc --noEmit` (types only); `grep -rn "ENTITYINTELLIGENCE_\|t_\${\|\"openworkflow\"" tests-e2e`
returns nothing relevant; then the real runs, `npm run test:e2e` and `npm run test:e2e:milestone3`.
**Done when** both pass against freshly built images: all containers healthy, Milestone 1 health
checks, Milestone 2 browser login, Milestone 3 trigger reaching a running execution.

### 3.3 forwardmeasure-data-streaming

**Changed.** (fds has no database, so §1.3–§1.5 do not apply.)
- Committed: launcher helmfile loads the shared layer (endpoints, namespace, org client, image-pull
  Secret); the kafka-streams executor pin moved to the shared `imageVersions.dataStreamingExecutorKafkaStreams`
  (registry's current `1.1.0` digest `8d60…`, replacing fds's stale `eadb…`); the credentials bootstrap's
  `tenantAlias` comes from the shared tenant list; `gcp-platform-cluster.yaml.gotmpl` +
  `gcp-greenfield-example`; dead `gcp.yaml`/`chart-versions.yaml` and the synced
  `platform-endpoints.yaml.gotmpl` removed.
- Uncommitted: the `data-streaming` namespace is now created by forwardmeasure-platform (§1.6); the
  launcher's `application.yml` files were renamed to `application.yaml`.

**Verify.**
```
helmfile --file deploy/helmfile/helmfile.yaml.gotmpl -e gcp-openworkflow-prod template
helmfile --file deploy/helmfile/helmfile.yaml.gotmpl -e gcp-greenfield-example template
```
~~Note: the Kafka Streams real-image test fixtures still pin digest `eadb…`.~~ **Resolved:** all
seven fixtures now pin `8d60…` (confirmed against the registry; `eadb…` still resolves, so the
tests were passing against a stale image rather than failing).

**Memory: drop** the synced `platform-endpoints.yaml.gotmpl`, fds's own `imageVersions.kafkaStreams`,
the `"lux"` tenant-alias fallback. **Record** §1.1–§1.2 and that platform creates the namespace.

### 3.4 forwardmeasure-decision-engine (for reference)
Owns its own release (platform's duplicate removed); chart 0.1.6 published with the Cloud SQL Auth
Proxy, **0.1.7 unpublished** (renamed container env vars); switched on for both GCP clusters; §1.3–§1.6
apply. See its own handover.

### 3.5 forwardmeasure-jpa
Uncommitted: `TenantDatabase.RESERVED_CONTROL_PLANE_ALIAS` (`control_plane`) + test;
`TenantRegistry` javadoc names `forwardmeasure_control_plane`; `forwardmeasure-jpa-quarkus` test config
converted to `application.yaml` (with a test-scoped `quarkus-config-yaml`).

---

## 4. Still open

Items 1, 2 and 6 were resolved later on 2026-09-29 by the fowf agent (uncommitted; details in
`forwardmeasure-openworkflow/docs/tenant-registry-grant-and-runtime-passwords-2026-09-29.md` §3).
Item 3 was partly stale.

1. ~~**Tenant database ownership.**~~ **Resolved (fowf's decision).** `provisionAndMigrate` now keeps
   the tenant database owned by the provisioning admin role (reclaiming it from a runtime role if an
   earlier run gave it away), revokes `PUBLIC`'s access, and grants each product's runtime role only
   `CONNECT, TEMPORARY`. New test `TenantDatabaseOwnershipTest` - compiled, not yet run.
2. ~~**Two owners for runtime roles.**~~ **Resolved.** One rule in both Terraform roots:
   Terraform creates the login role of every platform component that owns its own database
   (`keycloak`, `superset`, later e.g. Pinot) via the same `cloudsql_users` + write-only
   `cloudsql_user_passwords` variables; the migration Jobs own product runtime roles
   (`openworkflow`, `entityintelligence`, `decision_engine`) and nothing else.
   - forwardmeasure-platform's OpenTofu: dropped `openworkflow`/`entity_intelligence` (the latter was
     orphaned - fei's real role is `entityintelligence`), added the admin `forwardmeasure_admin`
     that new clusters were missing entirely.
   - openworkflow-k8s-setup (prod): new `cloudsql_users` (now `keycloak`, `superset`, `nominatim`,
     `kriyagentic`) and `google_sql_user.components`, plus an `import` for roles that already exist
     (`cloudsql_existing_users`). After the §0 reset nothing pre-exists, so prod leaves it unset.
     Requires OpenTofu >= 1.11.
   - fowf: `additionalRuntimeDatabases`, `provisionAdditionalRuntimeDatabasesIfConfigured` and
     `ensureDatabaseOwnership` removed; platform-secrets no longer delivers
     `OPENWORKFLOW_RUNTIME_DATABASE_{KEYCLOAK,SUPERSET}_PASSWORD` to fowf.
   - This also fixes a fresh cluster's ordering: Keycloak's role now exists before Keycloak installs.
   - **Accepted tradeoff (user decision):** Cloud SQL makes every API-created user a
     `cloudsqlsuperuser` member with `CREATEROLE`/`CREATEDB`, so component roles are
     admin-equivalent. Kept as a consistent, reusable pattern until it proves insufficient; recorded
     in openworkflow-k8s-setup's `docs/operations.md`. The stricter alternative is IAM database users
     plus a grant-only bootstrap Job.
   - **Before the next prod `tofu apply`:** export
     `TF_VAR_cloudsql_user_passwords='{"keycloak":"…","superset":"…","nominatim":"…","kriyagentic":"…"}'`.
     Keycloak's and Superset's must equal what their charts read (`platform-keycloak-database-password` /
     `platform-superset-database-password` in the gitignored prod credentials file), or they cannot
     log in. `nominatim` and `kriyagentic` are new values - store them wherever their future
     consumers will read them.
3. **fei readiness:** `REPLACE-ME` worker workflow IDs and evidence storage. (The health-endpoint
   part was stale: every fei REST leaf has its framework's health module; the release comment saying
   otherwise was corrected.) **New, must fix:** fei's Studio Playwright e2e harness cannot boot -
   see §3.2.1 for the full fix.
4. **forwardmeasure-agent-os** probably has the same ambiguous `AGENT_OS_DATABASE_*` naming; not changed.
5. **Scale:** per-tenant connection pools × pods × products against one Cloud SQL instance will need
   small pools plus PgBouncer/managed pooling as tenant count grows.
6. ~~Stale: openworkflow-k8s-setup's `terraform.example.tfvars` (`kafka_streams`/`actor_engine`
   keys).~~ **Resolved:** the example and `variables.tf`'s `databases` default now describe the
   control-plane database; the example also uses the real `forwardmeasure-openworkflow` namespace
   and adds the entity-intelligence identity.
