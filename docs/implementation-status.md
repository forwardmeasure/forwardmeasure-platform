# ForwardMeasure Implementation Status

**As of 2026-10-13.** This document records what is implemented in the forwardmeasure-* repositories, what is partial, and the remaining functional TO-DOs. It replaces the status parts of the older documents listed under [Sources](#15-sources), which move to `docs/archive/`.

- **Audience:** engineers and coding agents working on the platform.
- **Related documents:**
  - Vision, rules and design: [technical-vision.md](technical-vision.md).
  - Test evidence per framework and engine: [multi-framework-testing-status.md](multi-framework-testing-status.md).
  - OpenWorkflow (FOWF) detail, backlog (F-xx) and test evidence (T-xx) live only in FOWF's own
    documents: [FOWF implementation status](../../forwardmeasure-openworkflow/docs/implementation-status.md),
    [FOWF vision](../../forwardmeasure-openworkflow/docs/architecture-design.md) and
    [FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md).
    Section 4 here is a dated summary that links there.
- **Scope rule:** only work that exists in the forwardmeasure-* repositories counts here. Work described in the sources as done on the retiring legacy system is listed as a port backlog, not as status.

## Overall state in brief

- **Deployed.** `gcp-openworkflow-prod` (tenant `lux`, `lux.kriyagentic.com`) runs the Quarkus selection with the Kafka Streams engine and the Decision Engine (FDE) enabled, using the legacy namespace layout.
  - On 2026-10-13 OpenWorkflow and the Entity Intelligence (FEI) services were redeployed with that day's changes. The FOWF working tree holds new Quarkus digests for six images (definition management, execution management, human-task management, tenant administration, the Kafka Streams engine and the Kafka Streams operation adapter); those digests are uncommitted, so the deployed state cannot be reproduced from committed source.
  - Spring and Micronaut images have not been pushed for most FEI services (their digests are empty in FEI's `base.yaml`).
- **Last complete runtime evidence: 2026-10-09.** It used local images in disposable K3s clusters:

  | Matrix | Result |
  |---|---|
  | OpenWorkflow basic | 9/9 cells |
  | OpenWorkflow overflow | 9/9 cells |
  | FEI complete public API | 9/9 cells |
  | Data Streaming (FDS) | 39/39 cells (57 tests) |
  | FDE | 15/15 cells |

  - These results certify the recorded images only.
  - Large changes landed between 2026-10-11 and 2026-10-13 and have not been through these matrices.
  - No acceptance run against the live cluster is recorded.
- **Built since 2026-10-09 but not yet run in integration suites:**
  - FEI: population target contracts, input definitions, monitored populations, membership, and screening by membership.
  - OpenWorkflow:
    - one data-placement rule for both engines;
    - shared CloudEvent publication;
    - uniform start revisions;
    - compile-once publication;
    - parked engine events.
- **Coverage:** no product has a recorded passing coverage gate. The policy is 85% lines and 85% branches for handwritten code.
- **Not started:**
  - tenant branding (Option A);
  - moving production to the standard namespaces;
  - AWS and Azure infrastructure;
  - the information-extraction pipeline;
  - Studio browser acceptance.
- **Undecided:** Agent OS's purpose. An analysis handover exists, but no report has been written.

## Contents

1. [How to read this document](#1-how-to-read-this-document)
2. [Deployment state (gcp-openworkflow-prod)](#2-deployment-state-gcp-openworkflow-prod)
3. [Platform (forwardmeasure-platform)](#3-platform-forwardmeasure-platform)
4. [OpenWorkflow (forwardmeasure-openworkflow)](#4-openworkflow-forwardmeasure-openworkflow)
5. [Entity Intelligence (forwardmeasure-entity-intelligence)](#5-entity-intelligence-forwardmeasure-entity-intelligence)
6. [Data Streaming (forwardmeasure-data-streaming)](#6-data-streaming-forwardmeasure-data-streaming)
7. [Decision Engine (forwardmeasure-decision-engine)](#7-decision-engine-forwardmeasure-decision-engine)
8. [Agent OS and the information-extraction agent studio](#8-agent-os-and-the-information-extraction-agent-studio)
9. [Platform operations (forwardmeasure-platform-operations)](#9-platform-operations-forwardmeasure-platform-operations)
10. [Shared libraries](#10-shared-libraries)
11. [Cross-cutting open items](#11-cross-cutting-open-items)
12. [Uncommitted work at the time of writing](#12-uncommitted-work-at-the-time-of-writing)
13. [Superseded statements](#13-superseded-statements)
14. [Consolidated TO-DO table](#14-consolidated-to-do-table)
15. [Sources](#15-sources)

---

## 1. How to read this document

**Status words:**

| Word | Meaning |
|---|---|
| **Done** | Implemented, with the evidence named next to it. |
| **Partial** | Some of the required behaviour exists; the gap is stated. |
| **Not started** | No implementation found. |
| **Open defect** | Known wrong behaviour. |

**Evidence labels:** written, compiled, executed, packaged-image verified, deployed verified. Compiling is not passing. A passing test on one framework or engine says nothing about the others. See [multi-framework-testing-status.md](multi-framework-testing-status.md).

**Dates and verification:**
- Every claim carries a date.
- "Verified 2026-10-13" means it was checked against source code or git history in the forwardmeasure-* repositories while this document was written. That check reads code and git; it does not run anything.
- Claims marked "(per *source*, *date*; not re-verified)" come from the named source and were not checked again.
- Source abbreviations are defined in [Sources](#15-sources).

**Priority column in the TO-DO table:**

| Value | Meaning |
|---|---|
| **Next** | The newest source names it as the next step, or it blocks a deployed path. |
| **Open** | Required, but the sources give no order. |
| **Later** | The sources explicitly defer it. |

---

## 2. Deployment state (gcp-openworkflow-prod)

### 2.1 What is deployed

| Item | State | Evidence |
|---|---|---|
| Cluster | GKE `openworkflow-prod`, Cloud SQL `openworkflow-prod-postgres`, tenant `lux`, domain `kriyagentic.com` | XR 2026-09-29 |
| All product Deployments | 12 product Deployments ready. FEI Studio returned HTTPS 200 at `https://lux.kriyagentic.com/ei/studio/`. Bootstrapped workflows were visible in OpenWorkflow Studio. | IDH, 2026-10-06; not re-verified |
| OpenWorkflow Studio path | `/owf/studio/` | working notes, 2026-10-06; not re-verified |
| Deployment selection | Quarkus, `kafka-streams`, `enableFde: true`, display name `kriyagentic`. Product namespaces always carry the `forwardmeasure-` prefix; the legacy layout was removed 2026-10-20 (greenfield reinstall). | `deploy/helmfile/shared/deployment.yaml`, `shared/common.yaml.gotmpl` |
| OpenWorkflow (Quarkus, Kafka Streams) | Redeployed 2026-10-13 with OpenWorkflow commits `92a7b1c2`, `2b042da3` and `97f32483` (see [4](#4-openworkflow-forwardmeasure-openworkflow)). Six FOWF Quarkus image digests changed in the uncommitted FOWF `deploy/helmfile/environments/image-versions.yaml`: definition management, execution management, human-task management, tenant administration, Kafka Streams engine and Kafka Streams operation adapter. No live acceptance is recorded. | 2026-10-13 change record; digests verified 2026-10-13 (`git diff`) |
| FEI services | Redeployed 2026-10-13. Includes step D (FEI commit `d9b161b`) and the `MonitoringScreeningHit` fix. That entity had two `@PrePersist` callbacks, which crashed the services at startup; they were merged into one. The fix is in source but uncommitted. | 2026-10-13 change record; uncommitted diff verified 2026-10-13 |
| Istio chart repository | Moved to `https://blob.istio.io/istio-release/charts`. The old storage.googleapis.com bucket returns 403. The platform helmfile change is uncommitted. | verified 2026-10-13 (`00-foundation.yaml.gotmpl` diff) |
| Image digests | FEI `base.yaml` holds Quarkus digests only; Spring and Micronaut digests are empty. Switching framework therefore needs those images built and pushed; there is no tag-only fallback. | verified 2026-10-13; DEP 2026-10-06 |

### 2.2 Known deployment caveats (2026-10-13)

- **Lifecycle events.** Executions started before the 2026-10-13 deploy publish no lifecycle CloudEvents.
- FOWF's own record of these caveats, with the code evidence, is [FOWF implementation status §6](../../forwardmeasure-openworkflow/docs/implementation-status.md#6-engine-separation-and-deployment-state) and backlog F-03.
- **Engine upgrade order.** The Start command wire format changed: it now carries the definition bundle.
  - Commit `92a7b1c2` accepted the new wire pin on the condition that the command topics are drained before an engine upgrade.
  - A Start written by the old engine that is still unprocessed in `openworkflow.commands` would fail deserialization in the new engine. Kafka Streams' default LogAndFail handler would then stop the stream thread.
  - Rule: drain `openworkflow.commands`, or pause admissions, before every engine upgrade (overnight findings, 2026-10-13).
- **Changelog topics.** The `openworkflow-topics` chart must declare the `oks-execution-definitions` and `oks-lifecycle-coordinates` changelog topics before the engine upgrades.
  - The missing definitions changelog made the engine upgrades at Helm revisions 24 and 26 fail restoration.
  - Fixed in `b9a2df5a` and `97f32483` (verified 2026-10-13, git log).
- **Pending operator actions from 2026-10-05 (per IDH; not re-verified whether done):**
  - full installer rerun for the AuthZEN request-ID fix, the label-selector fix and the human-task signing secret;
  - `infra-plan` and `infra-apply` for the human-task secret;
  - public HTTPS echo check of request IDs;
  - confirmation that authenticated bundle publication succeeds.
- **Background error seen 2026-10-06.** Execution-management's recovery thread reported a missing `async_task` table (IDH).
  - OpenWorkflow's master changelog now includes `forwardmeasure-jpa-async-task.xml` (verified 2026-10-13).
  - That the live error is gone has not been verified.

### 2.3 Rules that bind operators

These rules are restated in [technical-vision.md](technical-vision.md):
- Do not delete the `istio-system` or `keycloak` namespaces. Doing so forces 7 Let's Encrypt reissues.
- Use forward Liquibase changesets only; never edit an applied changeset.
- Our images are pinned by digest.
- Run one installer at a time.

---

## 3. Platform (forwardmeasure-platform)

### 3.1 Build and version management

| Item | Status | Evidence |
|---|---|---|
| Parent `com.forwardmeasure.platform:forwardmeasure-platform:1.1.0`, core BOM, Quarkus/Spring/Micronaut BOMs | Done | JBP; VC 2026-10-07 |
| `forwardmeasure-platform-policy-maven-plugin` | Done. Derives owned versions from the parent model and runs at `validate`. Rejects first-party version overrides; third-party overrides need a key and a reason. | VC 2026-10-07; not re-verified |
| Full compile | Done on 2026-10-07: 14 Maven repositories and 482 reactor modules compiled; policy validate passed on 14 reactors. | VC 2026-10-07; not re-verified |
| Coverage policy | Implemented in the parent: 85% lines, 85% branches and zero missed classes on aggregates. Generated code is exempt by provenance. Missing execution data fails via `require-coverage-data`. | JBP; VC 2026-10-07 |
| Maven build cache | Enabled (maven-build-cache-extension 1.3.0). OpenAPI generator and dependency goals always run on cache hits (2026-10-07). | MBC |
| `build.sh` | Fails when an image it built was never pushed. The reactor now also builds `forwardmeasure-presentation-system` and `forwardmeasure-platform-operations`. | verified 2026-10-13 (commit `5ac6dbe`, 2026-10-12) |
| Install tooling | Per-stage helmfile apply; ordered sync for undeployed releases; tool versions pinned in `deploy/tool-versions.env` (Helmfile 1.8.1). The installer refuses binaries built for another CPU. | verified 2026-10-13 (commit `5ac6dbe`) |

**Partial or open:**
- **Cache acceptance.** Not done: compare cached and uncached product artifacts, generated clients, JDK/profile changes, image build/push behaviour and the selected-framework package layout (MBC, 2026-10-07).
- **Product reactor rerun.** The FEI managed-services / OpenWorkflow reproduction rerun was pending on 2026-10-07 (MBC). Its result is unknown.
- **Deployment selection rendering.** The 18-combination regression (`deploy/tests/test_deployment_selection_rendering.py`; 3 frameworks × 3 engines × FDE on/off) was authored 2026-10-06 and not executed then (DEP). The file exists (verified 2026-10-13); whether it has been run is unknown.
- **RAT.** RAT is a separate gate from the bootstrap install (VC 2026-10-07). Run offline RAT on every changed repository.

### 3.2 Shared contracts

| Item | Status | Evidence |
|---|---|---|
| `forwardmeasure-platform-api-contracts` | Done. Generates RFC 9457 `Problem` and `Violation` once (Java package `com.forwardmeasure.openworkflow.common.model` kept). Products publish generated inline snapshots, synchronised with `scripts/sync-shared-api-components.py`. | SCO 2026-10-09; generated `Problem` verified 2026-10-13 |
| Shared API ownership verification | 140 tests passed on 2026-10-09: 68 schema/model/handler tests and 72 real-HTTP cases on all three frameworks. This is source-level evidence; the packaged matrices were not rerun. | SCO, HO; see the testing document |
| `forwardmeasure-platform-data-contracts` | Done. On 2026-10-13 the contracts became versioned schema documents (commit `c24c4c9`). | verified 2026-10-13 |
| `forwardmeasure-platform-server-jaxrs`, `-micronaut-validation`, `-spring-security`, `-quarkus-security` | Done. These are the shared request-contract and error modules. | working notes 2026-10-01; module directories verified 2026-10-13 |

Data-contract files present on 2026-10-13:
- `contracts/catalog.json`
- `resolved-entity-record-schema-v1.yaml`
- `screening-document-schema-v1.yaml`
- `customer-master-source-schema-v1.yaml`
- `customer-master-resolved-entity-mapping-v1.yaml`
- `worldcheck-source-schema-v1.yaml`
- `worldcheck-opensearch-mapping-v1.yaml`. Despite its name, this is an FDS TransformSpec, not an index body.
- `opensearch/reference-population-index-mapping.json`
- `opensearch/worldcheck-index-v1.json`

**Open:**
- **FDS legacy fixtures.** FDS's legacy flat-target fixture templates repeat historical mappings. A later fixture composition change should remove that repetition (SCO, 2026-10-08).
- **NLP error shape.** `forwardmeasure-nlp` still uses the older error-response lineage, not RFC 9457. Its migration was deliberately deferred (working notes, 2026-10-01; not re-verified).

### 3.3 Deployment configuration

| Item | Status | Evidence |
|---|---|---|
| Shared helmfile layer `deploy/helmfile/shared/` | Done. `common.yaml.gotmpl`, `chart-versions.yaml`, `image-versions.yaml`, `deployment.yaml` and `clusters/`; every product helmfile loads it last. | verified 2026-10-13 (directory listing); XR 2026-09-29 |
| Environments | `gcp-openworkflow-prod` and `gcp-greenfield-example` are declared in every product repository. Adding a cluster still needs a manual `<env>` entry in 5 helmfiles. | XR; working notes 2026-09-29; not re-verified |
| Digest-only images | Done. `sync-image-digests.sh`, `check-image-digests.sh` and validation run before install. | XR 2026-10-01 |
| Deployment selection resolver | Done (2026-10-06): framework, engine, FDE switch, display name and namespace layout. It records the selection in ConfigMap `forwardmeasure-deployment-selection`. Engine changes and silent FDE removal are blocked. | DEP; `deployment.yaml` verified 2026-10-13 |
| Namespace layout | The standard layout is the default. Production keeps `legacy`; the cutover has not been performed. | verified 2026-10-13 |
| Secrets | One central ClusterSecretStore, `fake-secret-store` (ESO fake provider), in every environment. Values are fetched at deploy time; ExternalSecrets go out, PushSecret comes in. | working notes 2026-10-10 |
| Database model | One database per tenant, `forwardmeasure_<alias>`, with a product schema each. One control-plane database, `forwardmeasure_control_plane`, holds `tenant_registry`. The admin role is `forwardmeasure_admin`. Migration Jobs own the runtime roles. | XR 2026-09-29; registry changelog verified 2026-10-13 |

**Open (from DEP 2026-10-06 unless noted):**
- **Namespace cutover runbook** for production, moving from legacy to standard namespaces:
  - IAM `additional_bindings`;
  - drain and retire the old releases;
  - remove the legacy override;
  - remove the selection ConfigMap as an explicit acknowledgement;
  - full install;
  - rollback window.
- **Engine-change migration design.** Changing engines while keeping tenant databases is not a migration.
- **Connection pooling.** Per-tenant pools × pods × products against one Cloud SQL instance will need small pools plus PgBouncer or managed pooling as tenants grow (XR 2026-09-29).
- **Cloud SQL role tradeoff (accepted).** API-created Cloud SQL users are admin-equivalent. The stricter alternative, IAM database users plus a grant-only bootstrap Job, is recorded but not built (XR).

### 3.4 Identity and tenancy

| Item | Status | Evidence |
|---|---|---|
| Tenant identity | Alias-only. `TenantId.forDid(did:web:<alias>.<domain>)` (type-3 UUID). The database name never leaves persistence code. | XR 2026-09-29; working notes 2026-10-04 |
| Service-client inventory | Implemented and compiled on 2026-10-05; integration tests not executed then. Covers: nine-client inventory in FWP; secrets in Tofu/GSM; ESO as the sole Secret writer; capability packs; `TenantClientCredentials` with exact-one-organization tokens. | IDN/IDH 2026-10-05; not re-verified |
| AuthZEN request-ID correlation through the gateway | Fixed in the chart (`preserveRequestId` on the auth HTTPS listener). Needed an installer rerun. | IDH 2026-10-05 |
| Human-task signing secret | Required on all three frameworks. Generated in Tofu as `<cluster>-platform-openworkflow-human-task-review-secret`. | IDH 2026-10-05 |
| Tenant role groups | The role is mapped on both the `openworkflow` client and `forwardmeasure-public` (fixes 401 for every member). | XR 2026-10-01 |
| FEI onboarding | Two steps. The OpenWorkflow tenant API's ACTIVE status does not mean the FEI schema is ready. Automatic FEI schema provisioning is not implemented and awaits an owner decision. | IDH, IDN 2026-10-05 |

**Open:**
- **Identity acceptance.** Execute identity acceptance cases ID-01 to ID-10. Run the identity fixtures written on 2026-10-05:
  - `CapabilityPackLoaderTest`
  - `TenantOrganizationReconcilerTest`
  - `HttpKeycloakOrganizationAdminTest`
  - `TenantProvisioningMainTest`
  - the real PostgreSQL/Keycloak tenant-administration test
  - `DataStreamingCapabilityPackTest`
  - the Keycloak chart and FWP Python suites
  - the ESO integration suite, which needs a disposable cluster

  The AuthZEN real-Keycloak tests ran on 2026-10-07/08; the rest are unknown (IDH, VC).
- **Gateway hop.** Add the gateway-hop case to the K3s matrix (IDH).
- **Revocation.** Revocation is not automatic. Removing a pack declaration does not revoke grants; explicit administrative revocation is needed (IDN O4).
- **FEI schema provisioning.** Decide whether FEI schema provisioning should run automatically on tenant creation (IDN).

### 3.5 Branding and UI delivery

| Item | Status | Evidence |
|---|---|---|
| Runtime display name in both Studios and the dashboard | Done (2026-10-06) | DEP |
| Shared presentation system | Done. `forwardmeasure-presentation-system` provides `@forwardmeasure/presentation-system` (tokens, `kg-` components, shell). OpenWorkflow Studio, FEI Studio and the Platform Dashboard were migrated and committed 2026-10-12. | verified 2026-10-13 (commits `9dee548e`, `d1d2d68`, `dfe2127`, `12d489e`) |
| Tenant branding design (Option A first, Option B later) | Designed 2026-10-12; implementation not started. There is no `shared/brands/` folder, no `brand.mjs`, and no `display_name` or `brand` column in `tenant_registry`. | BRAND; verified 2026-10-13 |
| Tenant context | The dashboard's `/tenant-context` still serves only the dashboard. There is no shared `/platform/tenant-context` service yet. | verified 2026-10-13 (only `PlatformTenantContextResource` in platform-operations) |
| FEI credentials release | Still hard-codes `tenantAlias: lux` (`values.yaml:54`). | verified 2026-10-13 |
| Keycloak login theme | Stock `keycloak.v2`. The `operations-keycloak-theme` module was never built or deployed. | BRAND 2026-10-12; not re-verified |
| UI release gate (UIAUD) | "Final Closure Evidence" is empty. Its findings on the legacy rewrite predate the current FEI repository. | UIAUD; undated, carries a 2026-08-14 banner |

**Branding Option A build order (BRAND §5):**
1. `brand.mjs` check and compile, with a test for every rejection rule, plus `applyBrand(context)`.
2. A `shared/brands/kriyagentic/` folder and a `brand` field per tenant, checked at deploy time.
3. A `platform-brands` ConfigMap, with `brand.css` compiled at render.
4. A shared tenant-context service in platform-operations, on all three frameworks:
   - `/platform/tenant-context`
   - `/platform/brand/{key}/{file}`
   - `/platform/*` routed on every tenant host
   - REST tests over real HTTP
5. A forward Liquibase changeset adding `display_name` and `brand` to `tenant_registry`; `listTenants` returns them.
6. App changes:
   - the Studios read tenant context before `keycloak.init`;
   - FEI `config.js` drops per-deployment fields;
   - the FEI credentials release stops hard-coding `lux`;
   - the dashboard moves to the shared endpoint.
7. A K3s test with two tenants on two brands, a Helm render diff and RAT.

**Option B (later):**
- a `brand` field on `POST /v1/tenants`;
- brand APIs guarded by the platform-operator role;
- a private bucket via `forwardmeasure-object-storage`;
- moving redirect URIs, secrets and certificate SANs out of Helm.

**Open questions from BRAND:** sign-in hint through the `organization` scope; brands outside the apps (emails, exports, reports); custom fonts.

**UI release gate evidence still to record (UIAUD):**
- contract/client drift;
- frontend builds;
- Helm lint, schema and render for three tiers;
- one clean full regression;
- clean K3s deployment and recovery;
- authenticated, tenant-routed browser acceptance for FEI Studio, OpenWorkflow Studio and the Dashboard;
- a disposition for every finding.

### 3.6 Cloud infrastructure

| Phase | Status | Evidence |
|---|---|---|
| GCP (`k8s-infrastructure/opentofu/gcp`, plus `openworkflow-k8s-setup` for production) | In use | K8S (undated); only `gcp/` exists, verified 2026-10-13 |
| AWS (EKS, RDS, S3, Route 53, Secrets Manager) | Not started | no `aws/` directory, verified 2026-10-13 |
| Azure (AKS, Azure PostgreSQL, Blob, Key Vault) | Not started | no `azure/` directory, verified 2026-10-13 |
| Shared environment-output schema across clouds | Not started | K8S |

- **Unstated relationship.** How `k8s-infrastructure/opentofu/gcp` relates to `openworkflow-k8s-setup` is not stated in the sources.
- **Single-wildcard routing.** Tenants may later get separate clusters, so do not treat the single-wildcard DNS and gateway design as permanent (working notes, 2026-08-24).

---

## 4. OpenWorkflow (forwardmeasure-openworkflow)

**Summary as of 2026-10-13.** FOWF's detailed status, open items and evidence are maintained only in
the [FOWF implementation status](../../forwardmeasure-openworkflow/docs/implementation-status.md);
this section gives the headline per area and links there. Work happens on branch
`rehabilitation/fowf-completion`, not `develop`. The working tree holds the completed (uncommitted)
move of 77 Markdown files into FOWF's `docs/archive/`, the six new Quarkus image digests, the removal
of unused istio/cert-manager/external-secrets chart repositories from FOWF's Helmfile, updated
archive paths in `deploy/acceptance/verify-wp11.sh`, and `requirements.csv` link updates.

| Area | Headline (2026-10-13) | Detail |
|---|---|---|
| Definitions, compiler, publication | Open Workflow 1.0.3, all 12 task kinds; `DRAFT → PUBLISHED → DEPRECATED` with at most one PUBLISHED revision (unique index) and atomic deprecation at publish; compile once at publication; uniform start revisions; publication by name and DID-URL resources done. Construct-restriction centralisation open (F-19). | [FOWF status §3, §7.1](../../forwardmeasure-openworkflow/docs/implementation-status.md#3-definition-contract-and-publication-state) |
| Pekko engine | PostgreSQL and Cassandra journals; tenant tables stay in PostgreSQL. Invalid start input now 202 then FAILED (`92a7b1c2`). 2026-10-09 recovery fixes in source; the nested-reply decoder `1a60efd6` was never packaged (F-23). | [FOWF status §5, §7.1](../../forwardmeasure-openworkflow/docs/implementation-status.md#71-further-status-by-area) |
| Kafka Streams engine | Centralised 19-topic catalogue (6 partitions) with 5-minute topic wait; carried execution definitions; large inputs fixed by the shared placement rule; lifecycle events, fork-branch error routing, active tasks and listen correlation keys added (`2b042da3`). Drain `openworkflow.commands` before every engine upgrade (F-03). Scheduled and CloudEvent starts follow the placement rule since 2026-10-13 (uncommitted, tested); schedules are not yet armed in production (F-14). | [FOWF status §5–§6](../../forwardmeasure-openworkflow/docs/implementation-status.md#6-engine-separation-and-deployment-state) |
| Eventing (CloudEvents) | Shared modules `openworkflow-cloudevents` and `openworkflow-cloudevents-kafka`; identical lifecycle events on both engines; emit→listen closed loop. Lifecycle events were consumed out of order (keyed per event) until a 2026-10-13 fix, uncommitted, now keyed by tenant and source. Open: ingress has no OpenAPI contract and differs per engine (F-15); Pekko's topic is provisioned since 2026-10-13 (F-16, uncommitted); `OksCloudEventIngressResourceTest` failure unexplained (T-14). | [FOWF status §5](../../forwardmeasure-openworkflow/docs/implementation-status.md#5-execution-data-and-projection-state) |
| Data placement and overflow | One durable rule: inline only within the offload threshold and 32 KiB (inclusive); larger values in object storage. Placement inventory and boundary tests outstanding (F-04, T-02). | [FOWF vision §16](../../forwardmeasure-openworkflow/docs/architecture-design.md#16-data-references-limits-and-object-storage) |
| Execution management | Start `POST /v1/workflow-executions` with required `Idempotency-Key`; parked engine events; unknown-outcome rule implemented in source on both engines and the main executors (no fault evidence, F-05); one engine per installation, no per-tenant routing (F-22). Execution management auto-creates actors for unknown subjects, contrary to the platform rule (F-17, fix in progress). | [FOWF status §5](../../forwardmeasure-openworkflow/docs/implementation-status.md#5-execution-data-and-projection-state) |
| Human tasks | Domain, persistence, API, engine bridges and Studio workspace done; no public escalate/reopen operations (F-01); A2UI renders as raw JSON (F-02); state-rule decision needed (F-20). | [FOWF status §7](../../forwardmeasure-openworkflow/docs/implementation-status.md#7-studio-and-human-task-state) |
| Operation adapters | Separate Kafka and Pekko adapter bridges for all frameworks; Kubernetes Job verified on K3s (2026-09-07); runtime evidence beyond HTTP, gRPC and Kubernetes Job missing (F-09); callback null authentication plan unresolved (F-21). | [FOWF status §7.1](../../forwardmeasure-openworkflow/docs/implementation-status.md#71-further-status-by-area) |
| Studio | All 12 task kinds; on the shared presentation system (2026-10-12); browser acceptance not executed (F-11). | [FOWF status §7](../../forwardmeasure-openworkflow/docs/implementation-status.md#7-studio-and-human-task-state) |
| Tenant administration | `POST /v1/tenants` done; live HTTP tests cover GET only; some per-tenant setup still Helm-only (F-10). | [FOWF status §7.1](../../forwardmeasure-openworkflow/docs/implementation-status.md#71-further-status-by-area) |
| Requirement ledger | `requirements.csv`: 93 rules, 86 NOT RUN, 6 FAIL, 1 WRITTEN/NOT RUN, last refreshed 2026-10-04 (F-13). | [FOWF status §1](../../forwardmeasure-openworkflow/docs/implementation-status.md#1-reading-the-status) |

---

## 5. Entity Intelligence (forwardmeasure-entity-intelligence)

FEI is proprietary; every file carries the ForwardMeasure header, and RAT denies the Apache licence family.

### 5.1 Reference populations and contracts

| Item | Status | Evidence |
|---|---|---|
| Schema-driven ingestion | Done. No compiled target model class remains (`TARGET_MODEL_CLASS` / `WorldCheckTargetRecord` absent). | verified 2026-10-13 |
| Immutable target contract | Done (committed 2026-10-13, `d9b161b`). Screening schema plus index mapping, chosen at creation, compatibility-checked when custom, stored with digests in `target_contract`. | PC §15; commit verified 2026-10-13 |
| Input definitions | Done. Owned by the population, with numbered immutable versions (source contract, mapping, parsing); ingestion selects a definition and version. | PC §15 |
| Delta rule | A delta must keep its baseline's source identity namespace | PC §15 |
| Contract transport | Workers fetch admitted contracts by revision ID and digest; the 120K Job-environment snapshot is gone | PC §15; commit message |
| Pinned sources | Version plus CRC32C/MD5 and size are pinned at admission and streamed at exactly that version. Spark stages the pinned version; unversioned overwrites are refused. | PC §15; object-storage `8c45ecc`, FDS `18bf2fc`, verified 2026-10-13 |
| Strict request bodies | Every FEI request body rejects undeclared properties with 400, on all frameworks | PC §15 |
| Lifecycle: full/delta, activation, rollback, retention | Done. Delta without an active base gives 409; rollback reuses the superseded index; purge is manual with keep-last-N. | AUD 2026-10-10; working notes 2026-09-16 |
| Liquibase | Forward changesets `2026-10-13-*`. NOT NULL rules are `NOT VALID`; earlier rows stay readable; tenants are reset after rollout. | PC §15 |

**Status of the 2026-10-13 work:**
- It is implemented, compiled and offline-tested, and it is deployed (2026-10-13).
- Regression scenarios R1–R18 have **not** been run in the integration suites.

### 5.2 Monitored populations, membership and provenance (step D)

| Item | Status | Evidence |
|---|---|---|
| Monitored population management | Done (2026-10-13, `d9b161b`): create, update, retire, delete; input definitions; ingest as update or full refresh. | verified 2026-10-13 |
| Source records and provenance | Source records keep versioned content; assertions record their provenance | commit `d9b161b` |
| Membership | `monitored_population_membership` with supports. A full refresh and retirement withdraw supports. Deletion unlinks memberships; merge carries them to the survivor. Screening history survives entity deletion. | commit `d9b161b`; PC §14 decisions |
| Screening by membership | A population run screens a monitored population's active members and reports unresolved ones. The worker pages subjects from screening-service. | commit `d9b161b` |
| Fixes | The generic pipeline trigger refuses a monitored population's own pipeline (409). LIKE filters escape `%` and `_`: a filtered bulk delete with `subject_name:"%"` used to delete everything. | commit `d9b161b` |
| Workflow versions | population-screening 1.1.0, correlated-sources-ingestion 1.1.0, simple-source-ingestion 1.2.0 | commit `d9b161b` |
| Merge test | Merge has no REST operation; its membership test is at the application layer | working notes 2026-10-13 |

**Explicitly deferred (user decision, 2026-10-12, PC §14):**
- merge into a new entity;
- unmerge/split;
- automatic merge or cross-source matching at ingestion.

Monitored ingestion still creates one resolved entity per unseen record. Shared entities arise only through merge or investigation resolution.

### 5.3 Ingestion: simple and correlated (Spark)

| Item | Status | Evidence |
|---|---|---|
| Full WorldCheck file | 5,611,953,182 bytes, 5,818,856 records, API to READY in 14m08s, zero rejected (Quarkus API, Pekko/PostgreSQL) | RMS 2026-10-08 |
| Simple, correlated and resolution ingestion through the public API | 9/9 cells passed 2026-10-09 (WorldCheck, configured pipeline overrides, two-tenant full/delta, simple/correlated resolution, Customer Master with all 42 fields) | FEI complete matrix evidence |
| Correlated assertion union | Fixed 2026-10-09 (Spark image `assertion-union-20261009-1`) | RMS |
| Parsing overrides | Delimiter, quote and encoding on both population APIs, pipeline triggers and Studio | RMS 2026-10-08; AUD |
| Contract document reads | `IngestionResources.read` still returns bytes for contract documents, bounded at 16 MiB (`MAX_CONTRACT_BYTES`). Data sources stream. Kept deliberately. | verified 2026-10-13 |

**Open:**
- **Scale.** Hundreds-of-GB execution has not been established. The design target is hundreds of GB with bounded memory (SCO, AUD).
- **Encoding.** Latin-1 versus CP1252 for WorldCheck awaits supplier confirmation (AUD 2026-10-10).
- **Provider field ledger audit.** Covers all 35 WorldCheck and 42 Customer Master fields (AUD).
- **Encoding detection.** There is no automatic raw-byte encoding detection in the forwardmeasure-* repositories. The ICU dependency is present in FEI ingestion-service and FDS transforms, but no detector symbol was found (verified 2026-10-13). The archived screening handoff describes a 7-step detection contract to port.
- **Spark/GCS runtime rules to port** (SO 2026-08-08; not found in FEI by name, 2026-10-13):
  - an identical immutable Spark driver and executor tuple;
  - an aligned GAX/Google HTTP/GCS dependency family;
  - a decimal `fs.gs.block.size` setting.

### 5.4 Screening

**Present in the forwardmeasure-* repositories (name-level check, 2026-10-13; behaviour not re-verified):**
- `forwardmeasure-entity-matching`:
  - entity-kind compatibility gate (`incompatible_entity_kind`);
  - GLEIF legal-form lexicon;
  - organisation-noise normalisation (`NameNormalizer`, discriminative names);
  - identifier aliases (`US-TIN`, `SG-BRN`);
  - fuzzy recall;
  - `RealDataScreeningRegressionTest`.

  As of 2026-08-10 it owned the PERSON-only recall clause, the strict final gate, negative controls, the `locations.country_name` binding and the PwC distinctive-brand rule (SO; not re-verified).
- FEI screening-service:
  - scoring policy override;
  - result listing with `include_raw_hit`;
  - a streamed population-run export (`PopulationScreeningRunExportWriter`);
  - task-status HTTP semantics (200 for GET with status in the body, 202 for POST).
- Recall exclusion: low-quality aliases are excluded from strong matching evidence (entity-matching `c8aaa61`, 2026-10-08).

**Not found by name in the forwardmeasure-* repositories (2026-10-13):**
- a persisted `scoring_policy_version`;
- keyset cursor paging (`next_cursor`);
- the opt-in full-export scoring backtest;
- the implausible-DOB policy (only one FEI match for "plausib").

**Greenfield screening port backlog** (SO §16, 2026-08-08; items confirmed above are excluded):
1. One normative recall query builder shared by the single and population paths.
2. Kind-specific containment rules; no generic two-token fallback.
3. A stable score scale: full configured denominator, missing evidence contributes zero.
4. Implausible person DOBs treated as unavailable evidence.
5. Persist the effective scoring policy and its version with each run.
6. Bounded OpenSearch retry with instrumentation.
7. Keyset traversal with optional projections, a composite index, and asynchronous export to object storage.
8. A reliable outbox across all claimants.
9. Port the 15 labelled false-negative fixtures plus wrong-kind, PwC→Hong Kong, distinct-fund/parent and transliteration controls.
10. Labelled recall@K, candidate-volume, encoding, retry, progress, cursor, export and policy tests.
11. Both gates: a real-data regression with the exact accepted UID set, and a full-export scoring backtest.

- **Acceptance target** (SO; data from the retiring system's POV): the 348-ID Firco disposition, 331 valid hits + 12 valid zero-hits + 5 unscreenable omissions, with 0 unexplained misses. Verify against the current fixtures before reuse.
- **Unresolved contradiction** in the source: whether to keep the controlled long-name recall fallback. §16 says not to add a generic two-token fallback; §17 says to keep the controlled long-name fallback. Resolve with the user.

### 5.5 Resolution and entities

| Item | Status | Evidence |
|---|---|---|
| Entity reads | 27, 27 and 28 HTTP cases on Q/S/M (2026-10-09) | component evidence JSON |
| Classification merge fix | Source checkpoint `4ba0435`. Never packaged or image-verified. | HO 2026-10-09 |
| Merge | Absorb-into-survivor with hard delete of the absorbed entity. No public REST endpoint. | PC §14; HO |
| Resolution worker without OpenSearch for the RESOLUTION sink | Fixed 2026-10-09 (`f66333e`) | RMS |
| Customer Master typed dates at the worker REST boundary | Fixed 2026-10-09 | RMS |
| Graph, UBO and audit stubs; assertions path-shape decision | Deferred (Phase 8b) | working notes 2026-09-16; not re-verified |

### 5.6 Investigations, evidence and information extraction

| Item | Status | Evidence |
|---|---|---|
| Investigations, evidence CRUD, multipart upload and content download | Done on all three frameworks (2026-09-16). Upload streams via object storage with checksums. | working notes 2026-09-16; IE 2026-10-12 |
| Extraction schema | `InformationExtractionStatus`, `InformationExtractionResult`, `AmlKycExtractionResult` and `ExtractionError` are defined in the OpenAPI document | IE 2026-10-12 |
| Extraction domain | The `LlmExtraction` entity, service and repository exist. Nothing dispatches an extraction; no workflow or worker exists. | verified 2026-10-13 |
| Extraction pipeline | Not started. See [8.2](#82-information-extraction-agent-studio). | IE |

### 5.7 FEI Studio

| Item | Status | Evidence |
|---|---|---|
| Presentation system migration | Done (2026-10-12, `d1d2d68`) | verified 2026-10-13 |
| Reference population target, input definitions and version selection | Done (2026-10-13) | PC §15 |
| Monitored Populations page, memberships tab, screening by membership, server-side search and paging, ingestion history | Done (2026-10-13) | commit `d9b161b` |
| Playwright e2e harness | Reworked in source by 2026-10-07: DID-derived tenant ID, control-plane database, a guard against the retired `ENTITYINTELLIGENCE_*` spelling. A live monitored-population spec was added 2026-10-13. | verified 2026-10-13 (`environment.ts`) |
| Browser acceptance against the real stack | Not executed | testing evidence |

### 5.8 FEI deployment and publication

| Item | Status | Evidence |
|---|---|---|
| Workflow bundle (`fei` domain); name lookup; old publisher removed | Done. `WorkflowDefinitionPublisherMain` is absent; no `*_WORKFLOW_ID` settings remain. | verified 2026-10-13 |
| Published-revision resolver | Uses exactly one published revision per workflow. The oldest-revision bug of 2026-10-02 is gone. | verified 2026-10-13 |
| Helm releases | `entity-intelligence-credentials`, `-services`, `-studio-route`, `-workflow-bundle` | verified 2026-10-13 |
| K3s verification | Uses Strimzi like production (2026-10-11, `489f9ef`) | verified 2026-10-13 |

**Open (FEI, all from the sources named):**
- **Integration suites.** Run R1–R18 on all three frameworks and on the affected worker runtimes (PC §10). Next step: the user runs them after the 2026-10-13 deploy.
- **Skipped CRUD cases.** Execute the four Quarkus `ReferencePopulationCrudResourceTest` cases (HO R1). They passed on 2026-10-09 at 18:17 EDT in the shared-API evidence, but the later report shows them skipped.
- **Operation evidence.** Every FEI operation, derived from the OpenAPI document, needs real-HTTP evidence on all three frameworks (AUD).
- **Classification merge image.** Package and image-verify the classification-merge fix (HO R7).
- **Coverage.** The FEI coverage diagnostic was 70.17% lines and 55.84% branches on 2026-10-09; the gate is 85/85 (HO).
- **Delete-path audit.** Audit the merge and deletion paths for other unescaped LIKE patterns. The sibling-bug sweep after `LikePatterns` is not recorded.

---

## 6. Data Streaming (forwardmeasure-data-streaming)

| Item | Status | Evidence |
|---|---|---|
| One canonical IngestionSpec (`sources: []`) | Done | AUD 2026-10-10 |
| Delivery engines | Kafka Streams and Pekko Streams, bounded and continuous. Spark is optional staging. | AUD |
| Launcher on three frameworks | Done; deployed via `java-microservice` (`data-streaming-launcher`) | verified 2026-10-13 (helmfile) |
| Direct and FOWF-managed admission | Done. Bounded runs are Jobs; continuous runs are Deployments. | 39/39 cells, 2026-10-09 |
| Pinned streaming sources | The Pekko runner decodes text while streaming the pinned object version (2026-10-13, `18bf2fc`) | verified 2026-10-13 |
| Micronaut execution serialisation, continuous-smoke DNS, OKS topic provisioning in fixtures | Fixed 2026-10-11 (`e189c72`) | verified 2026-10-13 |
| Aggregate coverage ownership of the launcher bindings | Done (2026-10-11, `07440a7`) | verified 2026-10-13 |
| RFC 9457 errors and the 401/404 security matrix on three frameworks | Done in source 2026-10-01 | working notes |

**Open:**
- **Start-input matrix.** `FowfStartInputRuntimeMatrixAcceptanceTest` failed 9/9 overnight on 2026-10-12/13. Both causes were engine issues fixed in `92a7b1c2`:
  - the Kafka state cap;
  - Pekko's synchronous 409.

  The test has not been re-run since. (Overnight findings, 2026-10-13.)
- **Applicability audit.** Audit the full applicability table:
  - source cardinality × bounded/continuous × engine × direct/workflow × framework × FOWF backend;
  - SINV-01;
  - direct continuous invocation, which is outside the 18-cell baseline (AUD).
- **Kafka semantics evidence.** Explicit evidence is needed for malformed rows, the read-committed frontier, sink failure, timeout/cancel and drain (TG).
- **Engine separation.** Shared modules (`-core`, JDBC and object-storage connectors) must not depend on `pekko-stream`. Kafka Streams and Spark images must carry no Pekko (working notes, 2026-10-04). Whether this is resolved was not re-verified.
- **Connector parity.** The Spark connector dispatch accepted only `jdbc` while Pekko also accepted `sql`, and the object-storage connector was unwired. Both were found on 2026-09-14, before the FDS redesign, and not re-verified.
- **Coverage.** The FDS diagnostic was 93.63% lines and 86.39% branches (not the per-module gate) on 2026-10-09. The gate result is unknown.
- **Uncommitted overnight drafts (2026-10-13).** These were not found committed:
  - `StreamingTextRows` option validation in the Pekko executor;
  - an OpenSearch private-CA TLS end-to-end test and `TransformCharacteristics` tests in `data-streaming-api`;
  - `LaunchRequestsTest` and planner Spark preconditions;
  - `LauncherProblemMappersTest`;
  - a Spring `LauncherSpringApplicationTest`.

---

## 7. Decision Engine (forwardmeasure-decision-engine)

| Item | Status | Evidence |
|---|---|---|
| Drools core, gRPC bindings on three frameworks, Valkey fact windows | Done | AUD; FDE matrix |
| Tenant isolation defects found 2026-10-06 | All three fixed in source:<br>1. Fact-window keys are tenant-prefixed by `TenantScopedRuleEvaluators`.<br>2. KIE release IDs are random UUIDs.<br>3. All three bindings use `VerifiedJwtTenantResolver`. | verified 2026-10-13 |
| Packaged gRPC conformance | Passed on Q/S/M (2026-10-08), including cross-tenant Valkey windows. The lifecycle and rejected-update extensions passed 3/3 on 2026-10-09. | testing evidence |
| Workflow and Valkey deployment matrix | 15/15 (2026-10-09). It used released `decision-engine-*:1.1.0` images; whether those contain the 2026-10-08 Micronaut readiness and issuer fixes is not stated. | HO; testing evidence |
| Evaluate workflow 1.0.1 published; shared reconcile install; digests refreshed | Done (2026-10-11/12) | verified 2026-10-13 (git log) |
| Enabled in production selection | `enableFde: true` | verified 2026-10-13 |

**Open:**
- **Rehabilitation acceptance list** (AUD 2026-10-10). Each case needs its own evidence:
  - a populated old-schema upgrade with counter seeding under restricted roles;
  - cache-warm tenant deactivation;
  - real authorization TTL;
  - Valkey script eviction and response loss;
  - read-only-root startup with a writable compiler temp directory;
  - secret rotation;
  - recovery from an unavailable dependency;
  - proto compiler packaging without runtime Maven network access.
- **Coverage.** The diagnostic was 82.98% lines and 68.05% branches. There is a class-ID mismatch on `DecisionEngineQuarkusBinding$TransactionalRulesets` (HO R2).
- **Uncommitted tests (2026-10-09).** `ProtoStructMapperTest` and conformance additions were uncommitted then. FDE has had commits since and has no uncommitted files on 2026-10-13, so these are probably committed; not verified individually.
- **Image freshness.** Verify that the released service images contain the 2026-10-08/09 fixes, or rebuild them.

---

## 8. Agent OS and the information-extraction agent studio

### 8.1 Agent OS (forwardmeasure-agent-os)

| Item | Status | Evidence |
|---|---|---|
| Purpose | Unknown to the owner; never documented. A read-only analysis handover was committed 2026-10-12 (`docs/agent-os-purpose-analysis-handover-2026-10-12.md`). No report exists yet. | verified 2026-10-13 |
| Built by the reactor | Yes (listed in `reactor.xml`) | handover 2026-10-12 |
| Modules with production code (main/test Java files) | `agent-os-domain` 21/5, `-governance` 29/2, `-execution` 20/1, `-policy` 6/0, `-actor-bindings` 3/4, `-deployments` 6/0, `-openworkflow-client` 2/0, `-policy-opa` 1/1, `-policy-drools` 1/1, `-schema-validation` 1/2, `-database-migrations` 1/1, `-database-migration-service` 1/0, `-api-language-bindings` 99/0 | verified 2026-10-13 (file counts) |
| Empty modules | `agent-os-studio`, `-inbound-protocol-mapping`, `-conformance-tests` have no Java source | verified 2026-10-13 |
| Deployment | No Agent OS release in the platform helmfile. The only "agent-os" references are in the OPA policy release. | verified 2026-10-13 |
| Last functional commits | 2026-09-29 (adopt the renamed `WorkflowExecution` client), 2026-10-05 (compilation and bootstrap fixes) | verified 2026-10-13 |

**Decisions recorded elsewhere:**
- **Agent home.** Agents live on `forwardmeasure-agent-os`, not in FEI (user decision 2026-10-07, working notes).
- **Execution principle.** An agent invocation is an OpenWorkflow execution; no second workflow runtime (UIA). This conflicts with Agent OS having its own `agent-os-execution` module. The analysis must resolve it.
- **Studio plan.** `agent-os-studio` was planned as Phase 2 of the Flowise-parity Studio plan (10 milestones, none started; working notes, 2026-08-27).

**Open:**
- Run the purpose analysis, then decide whether to make Agent OS the agent runtime, fold it into OpenWorkflow or FEI, or retire it.
- Request-contract and security parity were not adopted in Agent OS (working notes, 2026-10-01).
- The ambiguous `AGENT_OS_DATABASE_*` naming is unchanged (XR 2026-09-29).
- Overnight draft (2026-10-13): `quarkus-jacoco` for the Quarkus actor binding; not committed.

### 8.2 Information-extraction agent studio

| Item | Status | Evidence |
|---|---|---|
| The agent | The user has "an initial implementation". Its location, language, branch and invocation contract were not supplied. Agent OS is not established as its home. | IE 2026-10-12 |
| FEI side | Evidence upload exists, plus the extraction schema and the `LlmExtraction` domain. No extraction request, dispatch, workflow, worker or Studio flow exists. | IE; verified 2026-10-13 |
| Tests | None executed | IE |

**Target journey (IE):**
1. Studio investigation and evidence.
2. Authorized extraction request.
3. Durable dispatch through OpenWorkflow.
4. A real agent or model.
5. Validated result.
6. FEI persistence.
7. Studio status and results.
8. Assertions and candidates under the agreed review policy.

**Open questions (IE):**
- agent location and deployment;
- first document formats and OCR;
- model, provider, endpoint and credentials;
- analyst-initiated or automatic;
- automatic persistence versus analyst review;
- reruns, corrections and duplicates;
- provenance granularity;
- token usage and partial-failure reporting.

**Proposed acceptance** (IE §6, not yet claims):
- invocation from a real investigation;
- durable status;
- tenant-correct evidence bytes;
- schema validation including malformed and partial output;
- provenance display;
- idempotent reruns without repeated billed calls;
- denial before retrieval and model invocation;
- per-framework and per-engine evidence.

---

## 9. Platform operations (forwardmeasure-platform-operations)

| Item | Status | Evidence |
|---|---|---|
| Platform Dashboard, 3-layer on Quarkus, Spring and Micronaut | Done. Each binding booted and was HTTP-tested (2026-08-25). | working notes 2026-08-25 |
| Presentation system, Title Case labels; images pushed for all three bindings | Done (2026-10-12, `dfe2127`) | verified 2026-10-13 |
| Runtime display name | Done (2026-10-06) | DEP |
| Framework selection | Not under central selection. The release uses a single `imageVersions.platformDashboard` entry, and `build-product-images.sh` does not build it. | verified 2026-10-13 |
| Correctness gaps | Older tenant authorization, single-host routing, incomplete health inventory (see the repository README) | DEP 2026-10-06; not re-verified |
| Shared tenant-context service | Planned (BRAND decision D5); not started | verified 2026-10-13 |

- **Dashboard status conflict.** UIAUD lists dashboard finding UI-003 as "corrected". DEP (2026-10-06, newer) says dashboard work is paused, with the gaps above. DEP is used here.
- **Uncommitted overnight draft (2026-10-13):** `quarkus-jacoco` for the Quarkus binding.

---

## 10. Shared libraries

| Library | State | Evidence |
|---|---|---|
| `forwardmeasure-jpa` | Database-per-tenant (`TenantDatabase.forAlias` → `forwardmeasure_<alias>`), `TenantRegistry`, `TenantDatabaseResolver`, registry changelog, Quarkus/Spring/Micronaut providers. Micronaut transaction proxy made public (2026-10-07). The shared library's own tests had 113/113 passing on 2026-09-17. | verified 2026-10-13 (registry changelog; no commits since 2026-10-07); working notes 2026-09-17 |
| `forwardmeasure-object-storage` | S3, GCS and Azure. Defects fixed 2026-10-07: S3 endpoint selection, GCS URL rewrite/zero-length/range/close, Azure continuation. Version pinning and checksums on all three providers (2026-10-13, `8c45ecc`). `StorageClientSettings`, one named-storage configuration record (2026-10-13, `4059e24`). | verified 2026-10-13 |
| `forwardmeasure-testcontainers` | PostgreSQL, Kafka, OpenSearch (JVM options), Keycloak (alternate issuer), K3s (networked), MinIO, GCS emulator, Valkey, Cassandra, Jupiter and Quarkus adapters. Fixture tests added 2026-10-13 for Cassandra, GCS, Keycloak and Kubernetes. | verified 2026-10-13 (git log) |
| `forwardmeasure-authzen` | Tenant-scoped service-client tokens (2026-10-06); real token audience provisioning; production AFFIRMATIVE strategy for overlapping roles; wrong-issuer tokens for regressions (2026-10-08) | verified 2026-10-13 (git log) |
| `forwardmeasure-entity-matching` | See [5.4](#54-screening). Low-quality aliases excluded from strong evidence (2026-10-08). | verified 2026-10-13 |
| `forwardmeasure-database-migrations` | Migration API, JDBC, Liquibase. No change since 2026-10-07. | verified 2026-10-13 |
| `forwardmeasure-nlp` | Named-entity-recognition Java client (generated, 7 operations). No change since 2026-10-07. | VC; verified 2026-10-13 |
| `forwardmeasure-presentation-system` | Created 2026-10-12. Tokens, `kg-` components, shell. The version property lives in the platform. | verified 2026-10-13 |

**Open:**
- **Storage configuration copies.** About 9 anonymous `NamedStorageClientConfig` classes remain to migrate to `StorageClientSettings` (2026-10-13 change record). Uses verified on 2026-10-13:
  - FEI `TransformSpecUriLoader`, `EvidenceStorageClientFactory`, `IngestionStorage`;
  - FDS tests.
- **Unverified fixture branch.** The Keycloak token HTTP interruption branch is unverified (VC 2026-10-07).
- **OpenSearch fixture migration.** Ad hoc OpenSearch containers in entity-matching and FEI should move to the shared OpenSearch fixture, which now exists (working notes, 2026-09-06; usage not re-verified).
- **Uncommitted overnight drafts (2026-10-13).** Not committed (no forwardmeasure-jpa commits since 2026-10-07):
  - `jpa-datasource`: template validation and `close()` of plain and failing pools;
  - `jpa-testcontainers`: extension misuse via EngineTestKit;
  - `jpa-contract-tests`: contracts that catch broken implementations;
  - `quarkus-jacoco` in `jpa-quarkus`.

---

## 11. Cross-cutting open items

1. **Run the suites against the 2026-10-13 state.** Matrix results certify the 2026-10-09 images only. Every matrix used a different image set, and none contains all fixes (testing evidence, HO). Rerun:
   - the FOWF, FEI, FDS and FDE matrices on current images;
   - FEI R1–R18;
   - the FDS start-input matrix;
   - `LifecycleCloudEventParityTest`;
   - `OksCloudEventIngressResourceTest`.
2. **Coverage gates.** No product has a recorded pass. HO R2–R5:
   - align class IDs;
   - write the finite behaviour-based regression plan;
   - write the missing fixtures;
   - pass 85/85 with zero missed classes.

   The latest FOWF measurement is the 2026-10-11 run 34 final gate: 92.9% lines, 79.7% branches, 22 missed classes (testing wrap-up of 2026-10-11); it replaces the 2026-10-09 retained-data check (branches 0.79, 68 missed classes) as the newest figure. FOWF (2026-10-11, `76f82dcd`) and FDS (2026-10-11) then changed aggregate ownership; the effect is unknown. FOWF detail: [FOWF testing §7](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#7-coverage-and-architecture-gate-state).
3. **Live acceptance.** No acceptance run against `gcp-openworkflow-prod` is recorded. The "deployed verified" label is unattained for every row (testing evidence).
4. **Image reconciliation.** Reconcile source fixes with built, published and deployed images (HO R7). Known unpackaged fixes:
   - Pekko nested-reply decoder;
   - FEI classification merge;
   - Spring and Micronaut images for most FEI services.
5. **Framework-selective Maven profiles.** Deferred until testing is complete (HO).
6. **JVM conditions.** Runtime evidence ran with C1 only (`-XX:TieredStopAtLevel=1`) after Temurin C2 crashes. Normal-JVM verification is outstanding, and the Temurin 25.0.4.1 crashes are unexplained (VC, RMS).
7. **Livy.** Livy needs a writable fork (CB 2026-10-07).
8. **High availability** (working notes, 2026-09-14; not re-verified):
   - Kafka Streams partition strategy;
   - HPA and PDB use per service;
   - Keycloak as a single fail-closed policy decision point for all writes.

---

## 12. Uncommitted work at the time of writing

Checked 2026-10-13 with `git status`.

| Repository | Uncommitted |
|---|---|
| forwardmeasure-entity-intelligence | `MonitoringScreeningHit` single `@PrePersist` fix (deployed); `deploy/helmfile/environments/base.yaml` digests |
| forwardmeasure-platform | Istio chart repository URL (`00-foundation.yaml.gotmpl`); workflow-publisher digest (`shared/image-versions.yaml`) |
| forwardmeasure-data-streaming | `deploy/helmfile/environments/base.yaml` |
| forwardmeasure-openworkflow (branch `rehabilitation/fowf-completion`) | 77 Markdown files moved into `docs/archive/` and the three consolidated documents (move complete, uncommitted); handover files, `README.md`; `deploy/helmfile/helmfile.yaml.gotmpl` (unused istio, cert-manager and external-secrets chart repositories removed); six Quarkus image digests in `deploy/helmfile/environments/image-versions.yaml`; `deploy/acceptance/verify-wp11.sh` (archive paths); `requirements.csv`; untracked `__pycache__/` directories under `deploy/helmfile/scripts/` (FOWF F-18) |
| All other forwardmeasure-* repositories | Clean |

Overnight drafts of 2026-10-13 that are not in any repository are listed in sections 6, 8, 9 and 10.

---

## 13. Superseded statements

| Older statement | Replaced by |
|---|---|
| "Nothing forwardmeasure-* is deployed anywhere" (XR 2026-09-29) | Deployed since 2026-10-06; redeployed 2026-10-13 |
| FDE "switched on" (XR 2026-09-29), then "disabled/deferred" (IDH 2026-10-05/06) | `enableFde: true` in the production selection (verified 2026-10-13) |
| Coverage gate 85% lines / 75% branches (FOWF pom and working notes, 2026-10-03) | 85/85 with zero missed classes in the platform parent (JBP, 2026-10-07; AUD 2026-10-10) |
| Keycloak chart 0.0.28 and platform-secrets 0.1.7 to publish (IDH) | Published 0.0.29 and 0.1.8 (IDH 2026-10-05). Keycloak 0.0.31 was prepared on 2026-10-06 (DEP); its publication is not recorded. |
| Helmfile 1.3.2 (XR) | Helmfile 1.8.1 pinned in `deploy/tool-versions.env` (2026-10-12) |
| Schema per tenant via `search_path` (PAE 2026-08-09) | Database per tenant with product schemas (verified 2026-10-13) |
| FEI ingestion frozen pending the FDS redesign (working notes 2026-09-21) | The FDS redesign landed; FEI ingestion was repaired and extended through 2026-10-13 |
| FEI reference ingestion hard-wired to `WorldCheckTargetRecord` (working notes 2026-10-01) | Schema-driven (verified 2026-10-13) |
| Population contract and step D work "uncommitted" (working notes, evening of 2026-10-12 EDT) | Committed 2026-10-13: FEI `d9b161b`, platform `c24c4c9`, object-storage `8c45ecc`, FDS `18bf2fc` |
| PC §15: "Remaining: step D" | Step D implemented and committed 2026-10-13 |
| PC §7: edit the initial FEI Liquibase definitions directly | Forward changesets with `NOT VALID` constraints (PC §15) |
| FEI revision resolver takes the oldest published revision (XR 2026-10-02) | One published revision per workflow; the resolver uses it (verified 2026-10-13) |
| FEI pre-install TO-DOs: bundle, name lookup, delete publisher (XR 2026-10-01) | Done (verified 2026-10-13) |
| FDE tenant-isolation defects (working notes 2026-10-06) | Fixed in source (verified 2026-10-13) |
| Kafka Streams carries Pekko through `ProtocolTransport` (working notes 2026-10-04) | `ProtocolTransport` is in `openworkflow-engine-api` (verified 2026-10-13) |
| Human task excluded from `EngineCapabilities` (working notes 2026-08-29) | Both engines declare all call and run kinds (verified 2026-10-13) |
| `DebugThrowableExceptionMapper` deployed in definition management (working notes 2026-08-27) | Absent from source (verified 2026-10-13) |
| Kafka Streams start input above 32 KiB: fixtures written, not executed (HO 2026-10-10) | One placement rule for both engines (2026-10-13) |
| Overnight decision needed on the Start wire pin (2026-10-13) | Resolved by draining topics before upgrade (`92a7b1c2`) |
| Platform Dashboard in the platform repository (UIA, UIAUD) | `forwardmeasure-platform-operations` (DEP, BRAND) |
| Shared UI packages `@forwardmeasure/ui-*` in a future repository (UIA) | `@forwardmeasure/presentation-system` (2026-10-12) |
| Product names OKS Studio and Entity Intelligence Workbench (UIA, UIAUD, PAE) | OpenWorkflow Studio and FEI Studio |

---

## 14. Consolidated TO-DO table

Status values: **Not started**, **Partial**, **Written, not run**, **Open defect**, **Decision needed**.

| # | Area | Item | Source / date | Priority | Status |
|---|---|---|---|---|---|
| 1 | Cross-cutting | Rerun the FOWF, FEI, FDS and FDE matrices on the 2026-10-13 images | HO 2026-10-09; 2026-10-13 changes | Next | Not started |
| 2 | FEI | Run R1–R18 on Quarkus, Spring and Micronaut and the affected worker runtimes | PC §10/§15 2026-10-13 | Next | Written, not run |
| 3 | FDS | Re-run `FowfStartInputRuntimeMatrixAcceptanceTest` (failed 9/9 before the engine fixes) | overnight findings 2026-10-13 | Next | Not started |
| 4 | OpenWorkflow | `OksCloudEventIngressResourceTest` (202 but nothing published): see [FOWF testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) T-14 | overnight findings 2026-10-13 | Next | Open defect |
| 5 | FEI | Commit the `MonitoringScreeningHit` `@PrePersist` fix (already deployed) | 2026-10-13 | Next | Partial |
| 6 | Platform | Commit the Istio chart repository move and the workflow-publisher digest | 2026-10-13 | Next | Partial |
| 7 | OpenWorkflow | Record `LifecycleCloudEventParityTest`, `OksCloudEventPublicationIntegrationTest` and `LargeWorkflowDataTest` results as evidence: see [FOWF testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) T-13 and T-15 | commits 2026-10-13 | Next | Partial (session record: parity 8/15 before fixes, `LargeWorkflowDataTest` passed, publication test not run) |
| 8 | Operations | Drain `openworkflow.commands` (or pause admission) before every engine upgrade: see [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) F-03 | `92a7b1c2` 2026-10-13 | Next | Rule in force |
| 9 | OpenWorkflow | CloudEvent-start and scheduled-start placement and the 32,767/32,768/32,769-byte inventory: see [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) F-04 and F-14 | AUD 2026-10-10; traced 2026-10-13 | Next | Fix in progress (F-14) |
| 10 | Cross-cutting | Coverage: align class IDs, write the behaviour-based regression plan, write fixtures, pass 85/85/0 for FOWF, FEI, FDS, FDE | HO R2–R5 2026-10-09 | Open | Partial |
| 11 | Cross-cutting | Package and verify unpackaged fixes (Pekko nested-reply decoder, FEI classification merge); push Spring and Micronaut images | HO R7 2026-10-09; verified 2026-10-13 | Open | Not started |
| 12 | Cross-cutting | First acceptance run against the live cluster | testing evidence 2026-10-09 | Open | Not started |
| 13 | FEI | Execute the four skipped Quarkus `ReferencePopulationCrudResourceTest` cases | HO R1 2026-10-09 | Open | Not started |
| 14 | FEI | Real-HTTP evidence for every FEI operation on all three frameworks | AUD 2026-10-10 | Open | Partial |
| 15 | FEI | Provider ledger audit (35 WorldCheck, 42 Customer Master fields); Latin-1 vs CP1252 supplier confirmation | AUD 2026-10-10 | Open | Not started |
| 16 | FEI | Hundreds-of-GB ingestion with bounded memory | SCO 2026-10-08; AUD | Later | Not started |
| 17 | FEI | Screening port backlog (5.4 items 1–11) | SO 2026-08-08; checked 2026-10-13 | Open | Partial |
| 18 | FEI | Decide on the controlled long-name recall fallback | SO §16 vs §17 | Open | Decision needed |
| 19 | FEI | Raw-byte encoding detection; Spark/GCS runtime rules | SO 2026-08-08; checked 2026-10-13 | Open | Not started |
| 20 | FEI | Merge into a new entity, unmerge/split, automatic cross-source matching | PC §14 2026-10-12 | Later | Not started (deferred by user) |
| 21 | FEI | Graph, UBO and audit stubs; assertions path-shape decision (Phase 8b) | working notes 2026-09-16 | Later | Not started |
| 22 | FEI | Studio browser acceptance against the real stack, including parsing overrides | RMS 2026-10-08; UIAUD | Open | Not started |
| 23 | FEI / Platform | Decide whether tenant creation provisions the FEI schema automatically | IDN 2026-10-05 | Open | Decision needed |
| 24 | FEI | Audit other LIKE patterns after the `LikePatterns` fix | `d9b161b` 2026-10-13 | Open | Not started |
| 25 | Information extraction | Answer the IE open questions; build request → dispatch → agent → validated persistence → Studio | IE 2026-10-12 | Open | Not started |
| 26 | Agent OS | Run the purpose analysis; decide the product's future | handover 2026-10-12 | Open | Not started |
| 27 | Platform | Branding Option A, steps 1–7 | BRAND 2026-10-12 | Open | Not started |
| 28 | Platform | Branding Option B | BRAND 2026-10-12 | Later | Not started |
| 29 | Platform operations | Shared tenant-context service with `/platform/tenant-context` and brand files on three frameworks | BRAND D5 2026-10-12 | Open | Not started |
| 30 | Platform operations | Dashboard under central framework selection and the image builder; fix tenant authorization, single-host routing, health inventory | DEP 2026-10-06; verified 2026-10-13 | Open | Not started |
| 31 | Platform | Namespace cutover of `gcp-openworkflow-prod` to the standard layout | DEP 2026-10-06; verified 2026-10-13 | Open | Not started |
| 32 | Platform | Engine-change migration design | DEP 2026-10-06 | Later | Not started |
| 33 | Platform | Execute the 18-combination deployment-selection rendering regression | DEP 2026-10-06 | Open | Written, not run (per DEP) |
| 34 | Platform | Independent build-cache acceptance | MBC 2026-10-07 | Open | Not started |
| 35 | Platform | Identity acceptance ID-01 to ID-10; remaining 2026-10-05 identity fixtures; ESO suite on a disposable cluster | IDN/IDH 2026-10-05 | Open | Partial (AuthZEN tests ran 2026-10-07/08) |
| 36 | Platform | Gateway-hop case in the K3s matrix | IDH 2026-10-05 | Open | Not started |
| 37 | Platform | Explicit administrative revocation (packs are additive) | IDN O4 2026-10-05 | Open | Not started |
| 38 | Platform | Confirm the 2026-10-05 operator actions (installer rerun, request-ID echo, human-task secret apply) | IDH 2026-10-05 | Open | Unknown |
| 39 | Platform | Confirm the `async_task` recovery error is gone live | IDH 2026-10-06 | Open | Unknown |
| 40 | Platform | PgBouncer or managed pooling as tenants grow | XR 2026-09-29 | Later | Not started |
| 41 | Platform | AWS and Azure infrastructure; shared environment-output schema | K8S (undated) | Later | Not started |
| 42 | Platform | Record the UI release-gate closure evidence | UIAUD | Open | Not started |
| 43 | Platform | FDS legacy fixture repetition removal | SCO 2026-10-08 | Later | Not started |
| 44 | Platform | NLP migration to RFC 9457 | working notes 2026-10-01 | Later | Not started |
| 45 | OpenWorkflow | Update the requirement ledger: see FOWF F-13 | verified 2026-10-13 | Open | Not started |
| 46 | OpenWorkflow | Execution API acceptance with a real engine and real publication; replace SQL seeding: see FOWF T-17 and F-05 | TG 2026-10-07; CB | Open | Not started |
| 47 | OpenWorkflow | Human-task parity tests set up through the API: see FOWF T-17 | TG 2026-10-07 | Open | Not started |
| 48 | OpenWorkflow | Human-task state rules: see FOWF F-20 | working notes 2026-09-04 | Open | Decision needed |
| 49 | OpenWorkflow | Real-service executor evidence: see FOWF F-09 and T-09 | AUD 2026-10-10 | Open | Not started |
| 50 | OpenWorkflow | Callback-authentication finding: see FOWF F-21 | TG 2026-10-08 | Open | Unknown |
| 51 | OpenWorkflow | Construct restrictions: see FOWF F-19 | working notes 2026-09-07 | Open | Not started |
| 52 | OpenWorkflow | Partition strategy, HPA/PDB, per-tenant engine routing: see FOWF F-22 | working notes 2026-09-14 | Later | Not started |
| 53 | OpenWorkflow | Studio browser acceptance: see FOWF F-11 and T-10 | testing evidence | Open | Not started |
| 54 | FDS | Full applicability audit; SINV-01; direct continuous invocation | AUD 2026-10-10 | Open | Not started |
| 55 | FDS | Evidence for malformed rows, read-committed frontier, sink failure, timeout/cancel, drain | TG 2026-10-07 | Open | Unknown |
| 56 | FDS | Confirm no Pekko in shared modules or Kafka/Spark images; connector parity (`jdbc`/`sql`, object storage) | working notes 2026-09-14 / 2026-10-04 | Open | Unknown |
| 57 | FDE | Rehabilitation acceptance list (8 cases) | AUD 2026-10-10 | Open | Not started |
| 58 | FDE | Confirm the released service images contain the 2026-10-08/09 fixes | HO 2026-10-09 | Open | Unknown |
| 59 | Shared libraries | Migrate about 9 `NamedStorageClientConfig` copies to `StorageClientSettings` | 2026-10-13 | Open | Not started |
| 60 | Shared libraries | Commit or discard the overnight test drafts (jpa, data-streaming, platform-operations, agent-os) | overnight findings 2026-10-13 | Next | Partial |
| 61 | Shared libraries | Move ad hoc OpenSearch containers to the shared fixture | working notes 2026-09-06 | Later | Unknown |
| 62 | Cross-cutting | Normal-JVM (C2) verification; explain the Temurin 25.0.4.1 crashes | VC 2026-10-07; RMS | Open | Not started |
| 63 | Cross-cutting | Writable Livy fork | CB 2026-10-07 | Later | Not started |
| 64 | Cross-cutting | Framework-selective Maven profiles | HO 2026-10-09 | Later | Not started |
| 65 | OpenWorkflow | Code gaps found 2026-10-13: scheduled/CloudEvent starts bypassed placement (F-14), CloudEvent ingress without a contract (F-15), Pekko CloudEvents topic not provisioned (F-16), actors auto-created for unknown subjects (F-17); hygiene (F-18). Also found: neither engine arms schedules in production (F-14) | traced 2026-10-13 | Next | Kafka placement, F-16 and F-18 done (uncommitted, tested); schedule activation with F-15 decided and planned in FOWF `docs/schedule-activation-plan.md`; actor provisioning (F-17) implemented 2026-10-20 against the tenant-wide `identity.actor` (FOWF `docs/actor-provisioning-plan.md` §5), not yet verified |

"FOWF F-xx" rows are in the [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog);
"FOWF T-xx" rows are in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work).
Those documents hold the full description; this table only tracks cross-product priority.

---

## 15. Sources

### Archived documents (moving to `docs/archive/`)

| Key | Document |
|---|---|
| ACC | [access.md](archive/access.md) |
| ARCH | [architecture.md](archive/architecture.md) |
| AUD | [independent-parity-audit-request-2026-10-10.md](archive/independent-parity-audit-request-2026-10-10.md) |
| BRAND | [tenant-branding-design-2026-10-12.md](archive/tenant-branding-design-2026-10-12.md) |
| CB | [checkpoint-backup-2026-10-07.md](archive/checkpoint-backup-2026-10-07.md) |
| DEP | [deployment-configuration-and-cutover.md](archive/deployment-configuration-and-cutover.md) |
| HO | [claude-testing-completion-handover-2026-10-09.md](archive/claude-testing-completion-handover-2026-10-09.md) |
| IDH | [identity-implementation-handover-2026-10-05.md](archive/identity-implementation-handover-2026-10-05.md) |
| IDN | [identity-service-clients-and-tenant-membership-design-2026-10-05.md](archive/identity-service-clients-and-tenant-membership-design-2026-10-05.md) |
| IE | [information-extraction-agent-studio-handover-2026-10-12.md](archive/information-extraction-agent-studio-handover-2026-10-12.md) |
| INV | [testing-boundary-inventory-2026-10-07.json](archive/testing-boundary-inventory-2026-10-07.json) |
| JBP | [java-build-platform.md](archive/java-build-platform.md) |
| K8S | [k8s-infrastructure-roadmap.md](archive/k8s-infrastructure-roadmap.md) |
| MAN | [forwardmeasure-engineering-manifesto.md](archive/forwardmeasure-engineering-manifesto.md) |
| MBC | [maven-build-cache.md](archive/maven-build-cache.md) |
| PAE | [platform-architecture-explained.md](archive/platform-architecture-explained.md) |
| PC | [population-contract-and-membership-implementation-plan-2026-10-12.md](archive/population-contract-and-membership-implementation-plan-2026-10-12.md) |
| RMS | [runtime-matrix-status-2026-10-08.md](archive/runtime-matrix-status-2026-10-08.md) |
| SCO | [shared-contract-ownership-2026-10-08.md](archive/shared-contract-ownership-2026-10-08.md) and [.json](archive/shared-contract-ownership-2026-10-08.json) |
| SO | [entity-intelligence-screening-optimisations-handoff.md](archive/entity-intelligence-screening-optimisations-handoff.md). Its "implemented" statuses refer to the retiring legacy system, not to these repositories. |
| TG | [testing-guide-for-claude-2026-10-07.md](archive/testing-guide-for-claude-2026-10-07.md) |
| UIA | [ui-architecture.md](archive/ui-architecture.md) |
| UIAUD | [production-ui-delivery-architecture-audit.md](archive/production-ui-delivery-architecture-audit.md) |
| VC | [version-consolidation-2026-10-07.md](archive/version-consolidation-2026-10-07.md) |
| XR | [cross-repo-handover-2026-09-29.md](archive/cross-repo-handover-2026-09-29.md) |

**Runtime evidence files (2026-10-09), also archived:**
- `fowf-basic-runtime-evidence-2026-10-09.json`
- `fowf-overflow-runtime-evidence-2026-10-09.json`
- `fowf-activity-projection-evidence-2026-10-09.json`
- `fowf-wire-recovery-evidence-2026-10-09.json`
- `fowf-tail-corrections-evidence-2026-10-09.json`
- `fowf-unblocked-components-evidence-2026-10-09.json`
- `fei-complete-runtime-evidence-2026-10-09.json`
- `fei-component-corrections-evidence-2026-10-09.json`
- `fde-runtime-evidence-2026-10-09.json`
- `fde-grpc-lifecycle-evidence-2026-10-09.json`
- `fds-runtime-evidence-2026-10-09.json`
- `shared-api-ownership-evidence-2026-10-09.json`

### Other inputs

- **2026-10-13 change record:** OpenWorkflow commits `92a7b1c2`, `2b042da3`, `97f32483` and `b9a2df5a`; the deploy to `gcp-openworkflow-prod`; and the known open items. All were given with this consolidation task.
- **Overnight test findings of 2026-10-13:** a session scratch file, not in any repository. Its items appear in sections 6, 8, 9 and 10, and its FOWF items in the FOWF implementation status (§5) and FOWF testing document (§3.4).
- **"Working notes":** dated project notes kept by Claude between sessions, 2026-07-30 to 2026-10-13. They are not repository documents; claims taken from them carry their date.
- **Verification on 2026-10-13:** `git log`, `git status` and source reads in the forwardmeasure-* repositories, as cited inline. Nothing was built or executed for this document.
