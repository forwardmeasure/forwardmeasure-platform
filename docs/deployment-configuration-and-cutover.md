# Deployment configuration and namespace cutover

October 6, 2026 implementation; no cluster change has been performed by this work.

## One selection per invocation

Edit `deploy/helmfile/shared/deployment.yaml`. The defaults are Quarkus, Kafka Streams,
FDE enabled and display name `kriyagentic`. Resolution order is an explicitly supplied environment
variable, the named environment's shared configuration, the shared top-level configuration, then
the resolver's built-in default. Empty/invalid environment values fail rather than silently falling
back. An explicit YAML `false` remains false.

| Configuration | Optional override | Values |
|---|---|---|
| `framework` | `FORWARDMEASURE_FRAMEWORK` | `quarkus`, `spring`, `micronaut` |
| `engine` | `FORWARDMEASURE_ENGINE` | `kafka-streams`, `pekko-postgresql`, `pekko-cassandra` |
| `enableFde` | `FORWARDMEASURE_ENABLE_FDE` | `true`, `false` |
| `displayName` | `FORWARDMEASURE_DISPLAY_NAME` | Non-empty printable display text |
| `namespaceLayout` | `FORWARDMEASURE_NAMESPACE_LAYOUT` | `standard`, `legacy` |

`product-selection.sh` resolves, validates and prints the non-secret selection before image lookup
or cluster writes. It exports an internal JSON snapshot for child installs and Helmfile rendering.
Children validate and consume that snapshot; they do not recompute choices after files/environment
change. Do not export `FORWARDMEASURE_RESOLVED_SELECTION` in a shell profile. A snapshot from a
different environment is rejected. Direct Helmfile rendering invokes the same resolver. Install
entrypoints are the supported mutation path; raw `helmfile sync` bypasses installer preflight.

Framework images, digest selection, engine-dependent releases, migration Jobs, identity clients,
worker permissions/endpoints and workflow bundles consume the shared selection. Product image
versions remain in their existing inventories. Selecting a framework with missing image digests
does not invent a tag-only fallback: build/push the selected images before installation.

Example overrides for a **new** environment:

```bash
FORWARDMEASURE_FRAMEWORK=spring FORWARDMEASURE_ENGINE=pekko-postgresql \
  ./deploy/install-platform.sh <environment>
```

Successful full installs record the selection in the non-secret ConfigMap
`forwardmeasure-platform/forwardmeasure-deployment-selection`. Preflight also inspects Helm
releases, so the first run after upgrading the installer protects existing unrecorded deployments.
API/authentication errors fail preflight rather than being treated as an empty cluster.
Run only one installer against a cluster at a time.

## Engine transitions and FDE retirement

An existing different engine, or a recorded engine change, blocks installation before mutations.
There is deliberately no `ALLOW_ENGINE_CHANGE=true` escape hatch. Engine persistence histories,
command identities, running operations and recovery positions need a separate migration design.
Keeping tenant databases while changing engines is not by itself a migration.

Disabling FDE while its Helm release exists also blocks: omitting a product from the installer
does not uninstall it or make running workflows independent of it. Explicitly drain/retire the FDE
workflow dependency and its release before selecting `enableFde: false`.

## Namespace migration

| Product | Legacy namespace | Standard namespace |
|---|---|---|
| FOWF | `forwardmeasure-openworkflow` | unchanged |
| Platform | `forwardmeasure-platform` | unchanged |
| FDS | `data-streaming` | `forwardmeasure-data-streaming` |
| FEI | `entity-intelligence` | `forwardmeasure-entity-intelligence` |
| FDE | `decision-engine` | `forwardmeasure-decision-engine` |

New environments default to standard names. The named `gcp-openworkflow-prod` configuration
explicitly retains `legacy` until the operator completes the cutover below. Namespace manifests
are generated from the same snapshot; the installer no longer unconditionally creates legacy
product namespaces. Service names and database/schema/tenant identities remain unchanged.

Before cutover, inventory the actual releases, replica counts, active Jobs, PVC ownership, Kafka
consumer identities, external persistence and GCP Workload Identity bindings. Record image digests
and values for rollback using the existing protected operator backup process; do not commit Secrets.
Choose a maintenance window and stop new FOWF executions that dispatch to FDS/FEI/FDE. Drain or
explicitly resolve in-flight work before changing routing; do not run old and new worker consumers
simultaneously and assume deduplication makes that harmless.

1. Using `openworkflow-k8s-setup/Makefile` (`make infra-plan` / `make infra-apply`), add the new namespace/service-account pairs to each
   affected runtime identity's `additional_bindings` (see that repository's
   actual variable contract). Keep old bindings until rollback is no longer needed. Apply and
   verify identity access before moving workloads; changing a Helm namespace alone does not update IAM.
2. Preserve required state and drain/stop old workers, launchers and services. Retire the old product
   Helm releases deliberately. Do not delete namespaces/PVCs as a shortcut; resolve any namespaced
   storage migration and remaining workload Jobs before proceeding.
3. Remove the named environment's legacy override (or set `FORWARDMEASURE_NAMESPACE_LAYOUT=standard`
   for this invocation). Review rendering of Secrets/ExternalSecrets, RBAC, worker dispatch namespace
   grants, internal URLs, network policy and tenant HTTPRoutes. FOWF-hosted migration/bundle Jobs stay
   in the FOWF namespace.
4. After the old releases are retired and the state/identity plan is complete, remove the old
   `forwardmeasure-deployment-selection` ConfigMap as an explicit transition acknowledgment. Do not
   remove it during routine installs. Preflight will still reject incompatible existing releases.
5. Run the normal full installer. It creates selected namespaces before delivering Secrets, installs
   products in order, refreshes endpoints/bundles and records the successful selection. Validate new
   workload identity, API access and worker dispatch before allowing new workflow executions.
6. Retain old namespace resources and old IAM subjects for the agreed rollback period. Clean them
   only after confirming no active Jobs, PVC data or consumers remain dependent on them.

Rollback requires the same discipline: stop the new workers first, account for writes made after
cutover, restore the old selection/releases/digests and routing, verify old IAM bindings, then resume.
If persistence cannot safely roll back, stop and recover it explicitly. Neither the installer nor
this runbook claims an automatic cross-engine or stateful namespace migration.

## Branding and image scope

**Operator scope update:** dashboard work is paused. Its local source changes are retained for
later review, but the new dashboard chart/configuration wiring has been removed from this rollout.
Rebuild only the two selected Studio images for the current branding rollout; the dashboard image
listed in the earlier follow-up below is deferred.

FOWF/FEI hosts serve `platformDisplayName` in their uncached runtime `config.js`; all three framework
hosts support `forwardmeasure.platform.display-name`. Helm passes `FORWARDMEASURE_PLATFORM_DISPLAY_NAME`.
The SPA uses text rendering for headers, accessibility text, startup/error screens and document title.
The name never determines OIDC realm/client, tenant DID, API path, database or Java package.
Keycloak's bootstrap Job reconciles realm display name through its existing JSON admin call.

The current Quarkus deployment needs only `openworkflow-studio-quarkus` and
`entity-intelligence-studio-quarkus` rebuilt for these Studio changes. Other framework installations
need their corresponding two Studio images. Namespace/selection changes require no Java image rebuild.
Keycloak changes are chart-source changes (prepared source version `0.0.31`); publish it and update
the repository-mode pin before using the remote chart. The existing published pin is unchanged.
The operator subsequently supplied the `forwardmeasure-platform-operations` checkout. Dashboard
runtime branding is now implemented through the same property in all three framework bindings;
the current chart passes it to its Quarkus image. This adds one image to the branding rebuild scope:
`forwardmeasure/forwardmeasure-platform-operations-quarkus:1.0.0`.
The dashboard remains outside the product image builder and is not yet covered by central framework
selection. Its older tenant authorization, single-host routing and incomplete health inventory are
documented in the [dashboard source review](../../forwardmeasure-platform-operations/README.md).
Branding implementation does not resolve those correctness gaps.

## Verification status and independent review

FOWF full production/test compilation: `/tmp/fowf-branding-compile-2026-10-06.log`, successful.
FEI full production/test compilation: `/tmp/fei-branding-final-compile-2026-10-06.log`, successful.
Both include all framework hosts and frontend compilation. Tests were not executed.
The dashboard's complete reactor also passed production/test-source and frontend compilation:
`/tmp/platform-dashboard-branding-compile-2026-10-06.log`. Dashboard chart lint and synthetic
custom-name rendering passed; real authentication/deployment behavior was not exercised.
The full platform compatibility reactor passed Maven model validation without cache reuse:
`/tmp/platform-cache-reactor-model-validation-2026-10-06.log`. Five product/platform Helmfile builds
and a non-default Spring/Pekko Cassandra/FDE-disabled selection rendered successfully; the complete
18-combination rendering regression is authored in `deploy/tests/test_deployment_selection_rendering.py`
and has not been executed. Keycloak chart lint and synthetic rendering passed.
Selection and transition regressions are in `deploy/tests/test_deployment_selection.py`; frontend
branding tests are alongside each Studio's `branding.ts`. Review the existing current-cluster override
before testing any namespace change. Exercise all 18 framework/engine/FDE combinations, immutable
snapshot propagation, unknown values, false flags, existing engine detection, recorded selection
mismatch, FDE retirement, repeated install and namespace rollback. Real k3s/identity/worker behavior
remains to be exercised after permission to run tests; rendered YAML is not runtime proof.
