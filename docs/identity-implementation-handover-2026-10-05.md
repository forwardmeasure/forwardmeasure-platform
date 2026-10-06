# Identity implementation handover — 2026-10-05

## Committed deployment checkpoint — 2026-10-06

The operator reports FEI Studio available and bootstrapped workflows visible in FOWF Studio.
Read-only inspection confirms all 12 product Deployments ready, FEI Studio HTTPS 200 at
https://lux.kriyagentic.com/ei/studio/, and the full installer's completion message. The shared FEI
Studio backend is tenant-routed; the earlier dedicated hostname section is superseded by FEI's
2026-10-06 tenant-URL correction and docs/access.md. Behavioral validation remains incomplete.

Local source checkpoint commits (not pushed by Codex):

| Repository | Commit |
|---|---|
| forwardmeasure-authzen | e04e16a |
| forwardmeasure-openworkflow | 86156c7e |
| forwardmeasure-data-streaming | c0cbfbc |
| forwardmeasure-entity-intelligence | 72f9a2e |
| forwardmeasure-decision-engine | 30f75b6 |
| openworkflow-k8s-setup | 4f4415c |

This document and platform changes are included in the accompanying platform checkpoint commit.
Helm charts and the other inspected supporting repositories were already clean. Ignored credential
files, infrastructure state, build output and Python caches were excluded. No tests or image builds
were run for the commit operation; earlier compilation/render evidence remains recorded below and
in product handovers. FDE remains deferred despite its existing build/installer fixes being committed.

Debugging starts with user-reported functional reproductions and observed service failures. One
confirmed background error is execution-management's recovery thread reporting missing `async_task`.
Investigate migration coverage and schema selection before asserting dispatch recovery works.
Each fix should have a reproduction, regression code, compilation evidence, exact image rebuild
scope and post-deploy verification. The standing instruction is to compile tests without running
them until explicitly authorized.

## Original implementation record

Codex implemented the revised [design](identity-service-clients-and-tenant-membership-design-2026-10-05.md).
Claude should verify the source and fixtures independently. No tests, image builds, chart publication,
GSM fetches, infrastructure apply or cluster operations were run in this implementation task.
Production and test-source compilation and offline configuration rendering were performed.

## Deployment follow-up — 2026-10-05

### AuthZEN request-ID rewriting at the gateway

After the launcher correction, FDS launcher deployed, but `data-streaming-workflow-bundle-workflows`
repeatedly failed publication with definition-management HTTP 503: `AuthZEN did not preserve audit
correlation`. Registry resource uploads succeeded. The shared authorization client received HTTP 200
from Keycloak but rejected the mismatched request-ID header.

Read-only diagnostic requests with no credentials isolated the fault: an invalid AuthZEN request
through the public HTTPS endpoint returned HTTP 400 with a replacement UUID; the identical request
directly to Keycloak through a temporary localhost port-forward returned HTTP 400 with the exact
supplied `fowf-authzen-diagnostic-20261005` ID. The port-forward was stopped. Gateway configuration
inspection confirmed the `auth.kriyagentic.com` TLS filter chain did not preserve external request
IDs and retained UUID tracing mutation. Keycloak 26.7.4's AuthZenRequestIdFilter echoes its incoming
header, consistent with the direct result.

The local platform-bootstrap chart now supports `preserveRequestId` per HTTPS listener. GCP enables
it only for auth-https. Its EnvoyFilter selects the platform gateway workload, GATEWAY context, port
443 and the exact auth SNI. The HTTP connection manager preserves incoming IDs and disables UUID
trace-bit mutation/request-ID sampling. Other listeners retain their defaults. This treats request
IDs as caller-provided correlation metadata; they are not authentication or authorization evidence.
The client's strict correlation validation and policy decision enforcement remain unchanged.

Offline rendering passed: `/tmp/fowf-authzen-request-id-render-2026-10-05.yaml`. Python regression
fixtures cover listener/workload scope, both preservation settings, disabled gateways and other
controllers; syntax and whitespace checks passed, tests not executed. No live configuration was
changed. Rerun the full installer after its current invocation exits; platform-certificates owns
this local chart and applies it before product publishing. No images or published chart versions
need rebuilding. Confirm the exact echo through public HTTPS with both an arbitrary string and a
UUID after deployment, and confirm authenticated bundle publication succeeds. The gateway case
must also be covered in the k3s integration matrix; direct Keycloak Testcontainers bypassed this hop.

References: [Keycloak 26.7.4 filter](https://github.com/keycloak/keycloak/blob/26.7.4/authzen/services/src/main/java/org/keycloak/authorization/authzen/AuthZenRequestIdFilter.java),
[Istio scoped connection-manager patches](https://istio.io/latest/docs/reference/config/networking/envoy-filter/),
[Envoy UUID tracing settings](https://www.envoyproxy.io/docs/envoy/latest/api-v3/extensions/request_id/uuid/v3/uuid.proto).

### Human-task signing secret omission

Live read-only inspection found human-task management at `1/2 CreateContainerConfigError`:
`OPENWORKFLOW_HUMAN_TASK_REVIEW_SECRET` was absent from `openworkflow-identity-credentials`.
The application container had not started. The shared human-task Helmfile requires this key for
Quarkus, Spring and Micronaut; the old comment incorrectly claimed an application-default fallback.

The local fix adds a dedicated 64-character random secret to the existing GCP Tofu resource loops,
with GSM name `<cluster>-platform-openworkflow-human-task-review-secret`. The tracked composition
wrapper fetches it into the fake store, replacing stale entries; the central ExternalSecret maps it
to the required runtime key. No operator credential files were read or edited. The key is stable
across ordinary applies and is independent of OAuth and database credentials.

Offline central chart rendering passed and includes the key in the correct namespace/Secret:
`/tmp/fowf-human-task-secret-render-2026-10-05.yaml`. Python syntax, Tofu formatting and whitespace
checks passed. Regression fixtures cover the runtime mapping and repeated composition with a stale
entry. Tests were not executed. No live infrastructure or cluster changes were made by Codex.

Operator action: run `make CLOUD=gcp infra-plan` and `make CLOUD=gcp infra-apply` in
`openworkflow-k8s-setup`, then rerun the full platform installer. This addition normally creates
three resources; review the full plan for other pending changes. `infra-apply` regenerates its plan.
No application image rebuild or chart publication is needed. Deployment recovery remains unverified
until that apply/install finishes.

### Earlier deployment evidence

The preceding implementation evidence and repository baselines describe the original handover.
The operator subsequently reported building/pushing the affected images and applying infrastructure.
Published chart versions are Keycloak `0.0.29`, platform-secrets `0.1.8`, and workflow bundle `0.1.4`;
these supersede the pre-publication versions below.

The installer then exited after platform configuration with `No platform ExternalSecrets reference
fake-secret-store`. Live metadata inspection confirmed that Helm sets `app.kubernetes.io/managed-by`
to `Helm`, replacing the chart's `platform-secrets` value. Both synchronization and consumer reload
incorrectly selected the latter. The local scripts now identify ownership through
`meta.helm.sh/release-name=platform-secrets`; synchronization also requires the named ClusterSecretStore,
and reload retains the identity-client label filter. Read-only inventory checks found 24 matching
synchronization resources and five identity credential resources. Python syntax and whitespace checks
passed. Regression fixtures now model installed Helm labels and filter selectors, and cover exclusion
of other releases and unrelated stores; tests have not been executed. No cluster mutations were made
by Codex during this diagnosis. Rerun the full installer to exercise the correction; no image rebuild
or chart publication is needed for this script-only fix. Application deployment remains incomplete.

## Review boundary

These are uncommitted working-tree changes. The working trees already contained unrelated deployment,
image-inventory, infrastructure and FOWF delivery changes; do not attribute the entire diff to this
identity batch, discard earlier work, or commit everything indiscriminately.

Current repository HEADs (not the implementation itself):

| Repository | HEAD |
|---|---|
| FOWF | `4534d153` |
| AuthZEN | `c0f1667` |
| FDS | `bc8b91f` |
| FEI | `870b649` |
| FWP | `9a8ffc8` |
| helm-charts | `fc6e1bf` |
| openworkflow-k8s-setup | `9baa765` |

## What changed

- FWP owns the nine-client inventory, including enabled/claim/authz booleans and workload tenant roles.
  The old first-tenant claim lists and one-service-client-per-tenant preflight rule are removed.
- Tofu adds eight client-secret entries to its existing generated-secret resource loops. That is
  normally 24 additional resources relative to the preceding working tree, not necessarily relative
  to HEAD: other uncommitted Tofu changes predate this batch. No plan was run.
- The tracked `with-client-secrets.yaml.gotmpl` composes generated GSM values into the local fake-store
  credentials layer. No generated values need to be pasted into credential files. Credential files
  were not inspected; an early narrow insertion into the user-provided prod template was subsequently
  removed in favour of this tracked wrapper. Do not open or log credential material during review.
- The Keycloak chart accepts boolean false, creates/updates clients and claim scopes, and no longer
  mutates tenant membership. Its source version and FWP pin are now `0.0.28`.
- ESO templates enumerate five FOWF, three FDS and six FEI final Secret keys. Apicurio JSON uses
  `dict | toJson`, including escaped secret content. Realm-admin Secrets for the retired FDS/FEI
  bootstrap Jobs are no longer delivered.
- The platform-secrets chart labels identity-client ExternalSecrets for selective consumer reload.
  Its source version and FWP pin are now `0.1.7`.
- Old product credential-writer releases are explicitly `installed: false`, allowing Helm to remove
  their Jobs/configuration/RBAC from an existing installation. The old chart directories are retained
  as uninstall coordinates, not invoked as writers.
- FOWF capability packs now support explicit resource-server client IDs, declared service-account or
  existing-user memberships with optional alias restrictions, and role/resource-specific grants.
  Existing global scope grants remain supported for existing FOWF packs.
- The same selected pack ConfigMap is mounted by bootstrap and runtime tenant administration. Its
  hash triggers runtime process reload. GCP selects FOWF, FDS and FEI packs; local selections remain
  explicit. The publisher's role must be declared; the special Java membership mutation is gone.
- FEI grants retain their resource-specific meaning. FDS now has its own pack because its launcher
  performs AuthZEN checks against `datastreaming`; setting that client to authz=false was incorrect.
- Human administrator scope remains explicit and defaults to the first configured tenant. Membership
  reconciliation is additive: removing declarations does not revoke existing grants or permissions.
- `TenantClientCredentials` discovers memberships from the configured OAuth endpoint and returns only
  a validated, exact-one-Organization token for the expected TenantId. Discovery is fresh for a new
  tenant; tenant token/API caches are bounded. AuthZEN evaluator credentials remain independent.
- FDS binds generated execution clients to the actor tenant in all three frameworks. FEI ingestion
  and screening services bind execution/definition clients similarly; workflow ID caches no longer
  cross tenants. Worker callbacks, including the Spark driver, request the asserted tenant's token.
- ESO synchronization waits for a unique marker on the *target* Secret and Ready, across namespaces.
  Identity is always synced so secret-only changes reach Keycloak. Reload compares ESO data hashes
  with controller pod-template annotations, and re-entry waits for unfinished rollouts. Jobs are never
  replayed; rotation requires a maintenance/drain window. Full installer reruns recover interruptions.

## Explicit FEI onboarding boundary

The owner was asked whether automatic FEI database provisioning belongs in this change. Pending an
answer, implementation preserves two-step onboarding: the FOWF tenant API creates the FOWF database,
FEI identity grants/members and configured workflows; the FEI migration step must run before FEI data
operations. The API's ACTIVE status does not prove FEI schema readiness. The new runtime integration
fixture proves FOWF database + Keycloak state, not end-to-end FEI ingestion. Do not describe automatic
FEI schema provisioning as implemented.

Existing API-created ACTIVE tenants have a reconciliation path: repeat the authenticated tenant
creation request with the same alias and display name after the pack update. The batch Helm tenant
list does not automatically discover them. Do not enumerate all Keycloak Organizations or reactivate
deleted tenants. This is cluster-level pack entitlement, not a new per-tenant licensing subsystem.

## Compilation and configuration evidence

Commands were run from `forwardmeasure-openworkflow` through the bounded build wrapper. AuthZEN and
FDS used direct jar/install goals solely to make changed library dependencies available locally;
these commands did not advance through the test or image-packaging lifecycle.

```bash
scripts/build-bounded.sh -f ../forwardmeasure-authzen/pom.xml \
  -DskipTests -DskipITs -Dmaven.test.skip=false test-compile jar:jar install:install \
  > /tmp/identity-authzen-compile-2026-10-05.log 2>&1
scripts/build-bounded.sh -f ../forwardmeasure-data-streaming/pom.xml \
  -DskipTests -DskipITs -Dmaven.test.skip=false test-compile jar:jar install:install \
  > /tmp/identity-fds-compile-2026-10-05.log 2>&1
scripts/build-bounded.sh -DskipTests -DskipITs -Dmaven.test.skip=false test-compile \
  > /tmp/identity-fowf-compile-2026-10-05.log 2>&1
scripts/build-bounded.sh -f ../forwardmeasure-entity-intelligence/pom.xml \
  -DskipTests -DskipITs -Dmaven.test.skip=false test-compile \
  > /tmp/identity-fei-compile-2026-10-05.log 2>&1
```

All four completed with BUILD SUCCESS. FOWF covers its full 189-module reactor, both engines and all
frameworks; FDS/FEI cover their full reactors and all frameworks. FEI trailing whitespace was removed
after compilation; no Java behaviour changed afterward. Compilation is not test execution.

Offline artifacts:

- `/tmp/identity-fwp-render.yaml`: platform base state, embedded public values. An existing NER/model-cache
  dependency warning remains in this base profile.
- `/tmp/identity-fowf-render.yaml`: FOWF CI state (an earlier render before the final explicit pack
  selection); `/tmp/identity-fowf-prod-render.yaml`: final production-profile state rendered with
  `OPENWORKFLOW_VERSION=1.1.0`, without credential files.
- `/tmp/identity-fds-render.yaml`, `/tmp/identity-fei-render.yaml`: production-profile embedded states.
- `/tmp/identity-keycloak-manifests.yaml`: local Keycloak chart rendered from public base values.
- `/tmp/identity-bootstrap-manifests.yaml`: local bootstrap chart render from the final production-profile values (all three packs).
- `/tmp/identity-eso-manifests.yaml`: actual platform-secrets chart rendered with synthetic/public
  configuration, using `deploy/helmfile/scripts/render-identity-fixture.py`.
- Python AST parsing, shell syntax checks, `git diff --check` and `tofu fmt` were used as static checks.

The GSM-backed production credential wrapper was not rendered against real credentials. The two new
chart versions have not been packaged/published. No existing image digest should be treated as covering
these source changes.

## Fixtures for independent verification — all NOT RUN

| Area | Fixture |
|---|---|
| Real Keycloak: two tenants, concurrent scoped tokens, nonmember rejection, newly added membership | AuthZEN `TenantClientCredentialsKeycloakIntegrationTest` |
| Malformed, multiple-Organization and wrong-tenant token responses | AuthZEN `TenantClientCredentialsClaimsTest` |
| Exact pack resource grants, member validation and alias scope | FOWF `CapabilityPackLoaderTest`, `TenantOrganizationReconcilerTest` |
| Real Keycloak: separate resource servers, existing users, two-tenant memberships, idempotence, invalid identity/group failures | FOWF `HttpKeycloakOrganizationAdminTest`, `TenantProvisioningMainTest` |
| Real PostgreSQL + Keycloak: runtime FOWF tenant creation also reconciles FEI pack/members; repeated same-alias request | FOWF `TenantAdministrationServiceRealPostgresqlKeycloakIntegrationTest` |
| FDS deployed policy exactly matches six actions and two resources | FDS `DataStreamingCapabilityPackTest` |
| Concurrent tenant-specific definition resolution and independent workflow-name caches | FEI `PublishedWorkflowRevisionResolverTest` |
| Actual bootstrap function accepts true/false, rejects string false; transport records requests | Keycloak chart `tests/test_service_client_bootstrap.py` |
| Central inventory, duplicate/reserved keys, composed Secret key sets | FWP `scripts/tests/test_identity_manifests.py` |
| Stale Ready and interrupted/repeated sync across namespaces | FWP `scripts/tests/test_sync_platform_secrets.py` |
| Unchanged credentials, interrupted rollout, refusal to replay active Jobs | FWP `scripts/tests/test_reload_secret_consumers.py` |
| Real ESO adoption, exact keys, JSON escaping, rotation, reruns and foreign-owner conflict | FWP `scripts/integration-tests/test_eso_client_secrets.py` |

Commands for the owner to run when ready (they have **not** been run here):

```bash
cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-openworkflow
set -o pipefail
scripts/build-bounded.sh -f ../forwardmeasure-authzen/pom.xml \
  -Dtest='TenantClientCredentials*Test' -Dsurefire.failIfNoSpecifiedTests=false \
  -DskipTests=false -Dmaven.test.skip=false test 2>&1 | tee /tmp/identity-authzen-tests.log
scripts/build-bounded.sh \
  -pl openworkflow-tenant-administration/openworkflow-tenant-administration-infrastructure -am \
  -Dtest=CapabilityPackLoaderTest,TenantOrganizationReconcilerTest,HttpKeycloakOrganizationAdminTest,TenantProvisioningMainTest,TenantAdministrationServiceRealPostgresqlKeycloakIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false -Dmaven.test.skip=false \
  test 2>&1 | tee /tmp/identity-fowf-tests.log
scripts/build-bounded.sh -f ../forwardmeasure-data-streaming/pom.xml \
  -pl forwardmeasure-data-streaming-launcher-application -am \
  -Dtest=DataStreamingCapabilityPackTest -Dsurefire.failIfNoSpecifiedTests=false \
  -DskipTests=false -Dmaven.test.skip=false test 2>&1 | tee /tmp/identity-fds-tests.log
scripts/build-bounded.sh -f ../forwardmeasure-entity-intelligence/pom.xml \
  -pl forwardmeasure-entity-intelligence-workflow-launch-support -am \
  -Dtest=PublishedWorkflowRevisionResolverTest -Dsurefire.failIfNoSpecifiedTests=false \
  -DskipTests=false -Dmaven.test.skip=false test 2>&1 | tee /tmp/identity-fei-tests.log
```

```bash
cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform
set -o pipefail
python3 -m unittest discover -s deploy/helmfile/scripts/tests -p 'test_*identity*.py' -v \
  2>&1 | tee /tmp/identity-manifest-tests.log
python3 -m unittest discover -s deploy/helmfile/scripts/tests -p 'test_*secret*.py' -v \
  2>&1 | tee /tmp/identity-secret-lifecycle-tests.log
python3 -m unittest discover \
  -s ../helm-charts/charts/keycloak-helm-chart/helm-chart-sources/tests -v \
  2>&1 | tee /tmp/identity-keycloak-bootstrap-tests.log
```

The separate real ESO suite needs an explicitly selected **disposable** cluster with ESO 2.0.1 and
its CRDs. Set `IDENTITY_TEST_CONTEXT` to that context; it uses synthetic values and isolated namespaces.
It does not install the operator or use the production credential file.

```bash
python3 -m unittest discover -s deploy/helmfile/scripts/integration-tests -v \
  2>&1 | tee /tmp/identity-eso-integration-tests.log
```

Review must also exercise the selected-framework HTTP tenant API and real FEI work after FEI schema
migration; the service-level provisioning fixture is not a substitute for that deployment check.

## Exact publication scope for the current Quarkus deployment

| Repository | Image repository |
|---|---|
| FOWF | `forwardmeasure/openworkflow-tenant-provisioning` |
| FOWF | `forwardmeasure/openworkflow-tenant-administration-quarkus` |
| FDS | `forwardmeasure/data-streaming-launcher-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-ingestion-service-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-screening-service-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-ingestion-worker-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-screening-worker-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-resolution-delivery-worker-quarkus` |
| FEI | `forwardmeasure/entity-intelligence-ingestion-worker-spark` |

Publish the changed shared AuthZEN/FDS libraries before dependent image packaging. Substitute the
selected framework suffix if the cluster switches framework; the Spark image has no framework suffix.
FDE remains disabled. The identity batch does not require rebuilding FOWF engines/adapters, FDS executor
images, FEI resolution-service, delta-apply worker or Studio solely because a shared jar gained an unused
class. Independently changed pre-existing source may have its own rebuild requirements.

Also package/publish `keycloak` **0.0.28** and `platform-secrets` **0.1.7** from helm-charts, then refresh
only the affected digest inventory entries. Run the full platform installer for adoption/rotation;
a standalone product installer does not perform the complete identity cutover. Verify the actual Tofu
plan and apply the missing GSM resources before that render/install.

## Upstream semantics checked

ESO 2.0.1 hashes `Secret.Data`, so the per-install sync marker does not itself change the data hash
used for reload decisions. Its controller also reports conflicting Secret ownership. See the
[version-pinned controller source](https://raw.githubusercontent.com/external-secrets/external-secrets/v2.0.1/pkg/controllers/externalsecret/externalsecret_controller.go).
Metadata-only templates preserve fetched data; composed templates must declare every desired output
key under Replace semantics. See the [version-pinned template application](https://raw.githubusercontent.com/external-secrets/external-secrets/v2.0.1/pkg/controllers/externalsecret/externalsecret_controller_template.go).
