# Runtime matrix accounting — October 8, 2026

## October 9 fixture completion and runtime preparation

The previously missing matrix fixtures are now written and compile. **No new matrix execution
passes are claimed by this checkpoint.** Historical results below remain separate evidence.
The old 27-case count is superseded: count deployment cells and scenario families separately.

| Suite | Current written scope | Compilation | Runtime evidence still needed |
| --- | --- | --- | --- |
| FEI public API | Nine disposable deployments: three frameworks × Kafka Streams, Pekko/PostgreSQL, Pekko/Cassandra. Each complete-cell entrypoint runs WorldCheck ingestion/screening, configured pipelines, two-tenant full/delta lifecycle, simple/correlated resolution, and State Street central-contract ingestion | `fixture-writing-fei-runtime-compile-02.log`, October 9 00:34 EDT | All nine **complete** cells; historical partial Quarkus/Pekko/PostgreSQL passes are retained below |
| FOWF API/recovery | Nine real framework/engine/backend deployments; publication, admission, HTTP effect, authorization/tenancy, idempotency and restart | Shared fixture and three FDS leaves compiled | Nine cells |
| FOWF overflow | Nine deployments, each with six scenarios: inline, stored response recovery, same-length corruption, missing object, maximum response rejection, real storage write failure | `fixture-writing-fds-overflow-compile-03.log`; FOWF full production/test reactor `fixture-writing-fowf-endpoint-full-compile-02.log` | Nine invocations / 54 scenario executions; neither number is a build count |
| FDS direct | Twelve real-worker classes across all three frameworks, two providers and two executors; no curl substitute | Three launcher leaves compiled | Current worker execution for each class |
| FDS bounded/continuous | Twelve bounded classes and six continuous classes; continuous cases assert actual two-wave OpenSearch data. Six Pekko bounded and three Pekko continuous classes also run with Cassandra selected | Three launcher leaves and shared fixture compiled | These configurations remain unverified; class counts are not method counts |
| FDE workflow adapter | Nine real FOWF deployments invoking the production FDE gRPC workflow; allowed, denied and cross-tenant requests; state retained after FDE restart | `fixture-writing-fde-deployment-compile-03.log`, October 9 00:38 EDT | Nine cases |
| FDE deployment | Six actual product Helmfile deployments: three frameworks × embedded/external Valkey; authenticated evaluation and retained facts after pod replacement | Same FDE compile log | Six cases |

Shared infrastructure now includes a caller-owned GCS emulator container in
`forwardmeasure-testcontainers-gcs` and Docker-network attachment for the shared Kubernetes fixture.
The emulator exercises actual storage HTTP operations; it does not establish Google IAM behavior.
FDE and FOWF acceptance reuse `PublicWorkflowAcceptanceClient` and `RealFowfWorkflowFixture`;
no duplicated in-process resource/service implementation stands in for their public APIs.

FOWF adds optional `openworkflow.operations.protocol.storage.endpoint` configuration. Absent the
setting, provider endpoint behavior is unchanged. This allows deployed engines/adapters to use the
real storage emulator. New local images are required for engine-kafka-streams, engine-pekko and
operation-adapter-kafka-streams, each across Quarkus/Spring/Micronaut: nine affected images.
The selected-module bounded package/image build passed at October 9 00:41 EDT with tag
`overflow-endpoint-20261009-1` (`overflow-runtime-local-images-02.log`, 1m38s). All nine images
exist locally. Attempt 01 failed on an empty-registry image name; corrected to `docker.io`.
No images were published and no operator deployment was changed. The final shared-fixture and
all-three-launcher compilation passed at 00:42 EDT (`fixture-writing-fds-final-compile.log`).

FEI runner: `scripts/run-public-ingestion-matrix.sh`, with `FEI_IMAGE_TAG` and `FOWF_IMAGE_TAG` set
to current local images. It provisions one isolated cell at a time, stops at failure and writes
per-cell logs plus CSV. Compilation and shell syntax validation passed; the script has not run.
FOWF shared fixtures accept per-component/framework image overrides such as
`-Dopenworkflow.acceptance.execution-management.quarkus.image=forwardmeasure/openworkflow-execution-management-quarkus:<tag>`
to use current fixes without rebuilding unaffected images. Runtime logs record actual selected images.

## Historical fixture-first checkpoint — October 8 evening

The user reaffirmed the required order: finish **all fixture implementation first**, then execute
remaining matrix runs. Do not restart scenario runs simply because an individual fixture becomes
ready. Compilation is separate and may continue. The tables below retain historical accounting;
they are not a claim that all their fixtures are written.

| Work | Written | Compiled | Executed |
| --- | --- | --- | --- |
| FEI configured pipeline parsing, replay/conflict, owned status/index | Yes | Yes | Passed 1/1 at 23:06 EDT, before the fixture-first correction |
| FEI State Street central-contract person/organization ingestion, exact raw/typed assertions | Yes; shared matrix plus disposable two-tenant entrypoint | Yes, `fixture-writing-fei-compile-03.log` | No |
| FEI two-tenant lifecycle provisioning | Written for disposable Quarkus/Pekko/PostgreSQL | Yes, `fixture-writing-fei-compile-03.log` | No |
| FEI all nine self-provisioned runtime deployments | Incomplete | No complete harness | No complete matrix |
| FEI correlated resolution configuration and real delivery fixture | Written: test-only pipeline configuration, REST source-profile creation, real Spark/Kafka/resolution delivery, two-tenant assertions | Yes, `fixture-writing-fei-compile-03.log` | No |
| FDS twelve direct fixtures requiring real current workers, curl fallback removed | Yes, all three frameworks | Yes, bounded compile logs 01–03 | Not rerun |
| FDS full-framework selection, optional Cassandra, six continuous real-data fixtures | Written; backend selector and real worker image prerequisites explicit | Yes, all three leaves plus shared fixture (`fixture-writing-fds-compile-03.log`) | Not rerun; no matrix pass claimed |
| FOWF nine-runtime public API, HTTP adapter effect and restart recovery | Written; includes real tenant provisioning and isolation checks | Yes, including tenancy extension, `fixture-writing-fds-compile-05.log` | No |
| FOWF deployed overflow/fault matrix | Incomplete: packaged GCS storage lacks an emulator endpoint configuration | No complete harness | Existing component evidence only |
| FDE adapter-to-gRPC and embedded/external Valkey deployment fixtures | Incomplete | No complete harness | Existing framework evidence only |

State Street adds a fourth parameterized FEI scenario; do not continue quoting the old 27-case
inventory as the final scope. Its checked-in 42-column person/organization inputs are synthetic,
and it consumes the central State Street source/mapping contracts. No customer export is copied.
FDS direct fixtures now require `fds.acceptance.pekko.image` / `fds.acceptance.kafka-streams.image`
and import the selected local image through the shared Kubernetes fixture with a real digest.
They no longer switch to writing a fabricated OpenSearch document when registry credentials are
missing. This removes false acceptance, but does not itself establish passing runtime evidence.
The FDS continuous fixtures now dispatch an actual executor and produce two waves only after
Deployment readiness, checking transformed fields, a later update and exactly two stable document
IDs. The shared runtime selects the framework for all four FOWF services and uses real Cassandra
through the shared container plus the production migration image when
`-Dfds.acceptance.pekko.persistence=CASSANDRA` is selected. PostgreSQL remains the default.
These additions have compile evidence only. The all-combination execution inventory still needs
to be reconciled with these selectors; writing a selector is not executing a matrix cell.

Persistent logs: `~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.

This audit corrects the statement “27 runs plus outstanding FOWF/FDS/FDE matrices.” That
statement combined scenario invocations, test classes, deployment configurations and unfinished
test infrastructure. They are different units and must not be summed into a remaining-run total.
No builds or tests were executed for the initial audit; subsequent execution is recorded below. Source inspection and retained execution evidence
are distinguished below; absence of a local report does not prove a test has never run.

## Subsequent full-file repair (separate from the matrix)

See [full-file implementation and live evidence](../../forwardmeasure-entity-intelligence/docs/full-file-ingestion-repair-2026-10-08.md).
October 8 run 05 **PASSED** at 19:01 EDT: the entire **5,611,953,182-byte / 5,818,856-record**
external WorldCheck export went through the Quarkus API, real Pekko/PostgreSQL FOWF and actual worker.
READY, zero rejected, exact OpenSearch count, finalized index metadata, raw samples and canonical
indexed queries all passed. Source SHA-256 matches the independent full-file scan. Admission to READY
was **14m08s**; sampled worker working set peaked at **~555 MiB**, kernel process VmHWM at **~614 MiB**
under a 1 GiB heap / 2 GiB Job memory. This is a separate full-file scale acceptance, not completion
of the activation/screening scenario or nine-runtime matrix below. Run 03's bad name-order assertion
and run 04's editor-crash interruption remain recorded and are not counted as passes.
Parsing overrides have since been added to both population APIs, configured pipeline triggers and
Studio. All three API/worker frameworks and Studio compiled; new override execution is deliberately
left to the operator's Studio testing, per their instruction. The inventory below remains historical
matrix accounting and does not describe the separate full-file test fixture.

## Evening continuation after operator build/push

Runbook correction `15cddaa` is pushed to FEI's backup branch. The operator reports publishing the
new images and is independently installing the dev deployment. Against current sources, the bounded
contract/Spark run passed **11/11** (9 contract cases and 2 real Spark/Kafka cases), and Studio
serialization passed **3/3**. This is newly executed component evidence; the per-request override
HTTP/browser behavior and remaining runtime cells are not claimed passed. Logs and method-level
summary are under `~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.
The Quarkus/Pekko/PostgreSQL public WorldCheck ingestion/activation/screening refresh **PASSED 1/1**
at 19:38:15 EDT, 3m57s, against `full-file-ingestion-20261008-1` in disposable K3s. Raw fields,
selected canonical values, explicit vendor indexing, finalized ownership, activation and positive/
negative screening all passed. The disposable cluster was removed. This closes the refresh for the
first scenario in that runtime cell; it is not full/delta/resolution or other framework parity.
[Current image/source fingerprints and logs](../../forwardmeasure-entity-intelligence/docs/post-rollout-regression-evidence-2026-10-08.json).

## Delta lifecycle continuation — October 8, 20:22 EDT

A new disposable entrypoint, `IngestionPipelineFowfK3sVerificationTest#fullDeltaRollbackAndRejectedRowsUsePublicApi`,
passed **1/1** on Quarkus/Pekko/PostgreSQL in 4m54s. It shares the lifecycle assertions with the
matrix: missing-baseline rejection, full admission/replay/activation, staging, actual delta worker
and authenticated activation callback, wrong-owner/trigger rejection, rollback and preservation
after rejected rows. This entrypoint is single-tenant: the two-tenant isolation assertions in the
matrix still need execution, so this is partial coverage of that cell, not a second complete cell.

Attempt 02 exposed a production delta-status HTTP 500 (lazy relationship read after session closure).
The fix and the same correction in configured-pipeline status compiled across all three frameworks.
Attempt 03 passed using local `entity-intelligence-ingestion-service-quarkus:delta-status-repair-20261008-1`.
Configured-pipeline status subsequently passed its public-API regression at 23:06 EDT (see checkpoint above). Only the selected ingestion API needs
an additional published image; no user deployment or image push was performed by these tests.
[Source/image/log evidence](../../forwardmeasure-entity-intelligence/docs/delta-lifecycle-regression-evidence-2026-10-08.json).

## FEI: 27 parameterized scenario invocations

Source: [ReferencePopulationPublicApiAcceptanceIT](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-ingestion-k3s-verification/src/test/java/com/forwardmeasure/entityintelligence/ingestion/k3s/ReferencePopulationPublicApiAcceptanceIT.java).
Production and test compilation passed in `fei-provider-semantics-all-frameworks-compile.log`.

| Deployment | WorldCheck public ingestion/indexing/screening | Full/delta lifecycle | Simple + correlated resolution |
| --- | --- | --- | --- |
| Quarkus / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Quarkus / Pekko / PostgreSQL | Shared-helper scenario refreshed and passed 19:38 EDT on current images | Single-tenant lifecycle passed 20:22 EDT; two-tenant isolation pending | No retained passing acceptance evidence |
| Quarkus / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Pekko / PostgreSQL | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Pekko / PostgreSQL | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |

Exact methods and assertions:

- `premiumWorldCheckFileSchemaMappingAndIndexAreInvokedThroughThePublicApi`: real hosted
  three-record CSV/schema/mapping/index inputs, public admission/status, worker completion,
  owned OpenSearch documents and raw fields, activation and positive/negative screening.
- `fullDeltaRollbackIsolationAndRejectedRows`: tenant isolation, missing-baseline rejection,
  full revisions and distinct indices, activation, staged/applied delta, rollback, rejected-row
  failure and preservation of the active baseline.
- `simpleAndCorrelatedResolutionUseRealApiAndRemainTenantScoped`: source-feed profile creation,
  simple resolution admission, correlated pipeline trigger, workflow completion, resulting entity
  and assertions, tenant isolation. This uses generic resolution fixtures, not the State Street
  provider file. Provider-specific State Street public-API verification still needs wiring.

These are three invocations on each of nine deployments, not 27 separate full builds. The runner
requires endpoints, fixture hosting, tenant tokens and a preprovisioned correlated pipeline; it does
not yet provision all nine stacks itself. The prior WorldCheck pass is
`fei-worldcheck-public-k3s-05.log`, one case through `IngestionPipelineFowfK3sVerificationTest`.
Thus 26 complete invocations lack retained passing evidence; one of these now has passing
single-tenant lifecycle coverage but still lacks its two-tenant checks. The WorldCheck shared-helper
pass has been refreshed on current images (see evening continuation).
This is not evidence that 26 executions failed.

The new provider semantics have separate passing evidence: 23 focused FEI tests plus two matching
decoder tests. These include real OpenSearch and PostgreSQL. The external ten-record WorldCheck
sample was checked for mapping/validation, not public-API ingestion. Neither it nor the 5.6 GB
export is the existing public-API matrix fixture.

## FDS: 30 named matrix classes, with fixture gaps

All classes are under `forwardmeasure-data-streaming-deployments/launcher/{quarkus,spring,micronaut}/src/test/java`.
The following ten class patterns each exist for `Quarkus`, `Spring` and `Micronaut`:

| Class pattern (`F` is the framework name) | Cases/configurations represented |
| --- | --- |
| `DirectIngestionMatrixFPekkoSmokeTest` | Direct WorldCheck / Pekko |
| `DirectIngestionMatrixFKafkaStreamsSmokeTest` | Direct WorldCheck / Kafka Streams |
| `DirectIngestionMatrixFPekkoTestCustomerMasterSmokeTest` | Direct customer master / Pekko |
| `DirectIngestionMatrixFKafkaStreamsTestCustomerMasterSmokeTest` | Direct customer master / Kafka Streams |
| `WorkflowBoundedMatrixFPekkoSmokeTest` | Workflow-managed WorldCheck / Pekko |
| `WorkflowBoundedMatrixFKafkaStreamsSmokeTest` | Workflow-managed WorldCheck / Kafka Streams |
| `WorkflowBoundedMatrixFPekkoTestCustomerMasterSmokeTest` | Workflow-managed customer master / Pekko |
| `WorkflowBoundedMatrixFKafkaStreamsTestCustomerMasterSmokeTest` | Workflow-managed customer master / Kafka Streams |
| `WorkflowContinuousMatrixFPekkoSmokeTest` | Workflow-managed Deployment readiness / Pekko |
| `WorkflowContinuousMatrixFKafkaStreamsSmokeTest` | Workflow-managed Deployment readiness / Kafka Streams |

This is 12 direct + 12 bounded workflow + 6 continuous-lifecycle classes, not 30 test methods:
some classes contain additional authorization tests. It is an inventory, not a verified unrun count.

Historical gaps identified by the initial audit (current repairs are in the checkpoint above):

1. All 12 direct classes contained a curl-container fallback. A stand-in can write a constant document
   without running the ingestion parser/mapping. Acceptance must require the real current worker.
2. The bounded workflow tests use `RealFowfWorkflowFixture`, whose engine/definition/adapter images
   are Quarkus and whose Pekko setup uses PostgreSQL. A Spring/Micronaut launcher does not prove
   a Spring/Micronaut FOWF stack. An execution-service framework overload does not close this gap.
   Cassandra is absent from this matrix. Extend actual runtime selection before claiming parity.
3. All six continuous classes use busybox and prove Kubernetes Deployment readiness, not actual
   continuous FDS data processing. Retain that useful lifecycle coverage and add real data assertions.
4. The two currently retained matrix XML reports are the Quarkus and Spring Kafka Streams classes,
   each selecting only `rejectsAGenuineTokenFromAnUnexpectedIssuer`. Both pass, but neither is a
   passing ingestion dispatch. Older two-Job simple/correlated and executor component passes remain
   valid separate evidence; they do not establish these launcher combinations.

## FOWF: nine deployed runtime configurations remain incomplete

Use the same nine framework/engine/backend combinations listed for FEI. This is a deployment
coverage requirement, not nine fully implemented tests waiting in a queue. The missing integrated
proof is real definition publication and execution admission followed by actual selected-engine
execution, lifecycle/history/idempotency, authorization/tenancy and adapter side effects.

Overflow acceptance on those deployments must prove actual object-storage offload/materialization,
terminal persisted results, oversized-input rejection, storage failure and digest mismatch. Durable
restart/recovery must retain execution/artifact identity on Kafka, Pekko/PostgreSQL and Pekko/Cassandra.
Exhaustive algorithm boundary inputs stay in component tests; they need not all be duplicated nine times.

Already passed, and not invalidated by provider mapping changes:

- 345 execution/event HTTP cases across the three frameworks (`fowf-document-identity-http-suite.log`).
  These prove API behavior; they do not all run a real workflow engine.
- Kafka overflow/integrity 18, Pekko PostgreSQL/Cassandra overflow recovery 4, and shared Kafka
  restoration evidence recorded in the repair handover. They are component/runtime evidence,
  not a nine-deployment public-API acceptance result.
- Real Quarkus/Pekko/PostgreSQL FOWF dispatch through the FEI K3s cases.

The FEI repair handover records that the third combined Kafka run passed **94 cases across 18
classes**, zero failures/errors/skips, at 10:42:20 on October 8 (20m51s). It includes the repaired
retry-deadline assertion, all 26 restoration cases and multi-instance rebalance. Its historical log
was `/tmp/fowf-kafka-engine-combined-03-20261008.log`; that temporary file is no longer present at
this evening's audit, so this is retained handover evidence, not a newly reverified log. The earlier
statement that a clean combined rerun was outstanding was stale. The nine-deployment acceptance
harness/scenario work remains incomplete; this component-suite pass does not close those cells.

## FDE: packaged framework acceptance is complete; two integration areas remain

`DecisionEngineContainerConformanceTest.authenticatedFrameworkRoutesEachTenantToItsOwnPhysicalDatabase`
passed for Quarkus, Spring and Micronaut with real Keycloak, PostgreSQL and Valkey. The handover
records `fde-authenticated-shared-fixtures-conformance-20261008.log`: three cases, no failures,
errors or skips. This is recorded prior execution evidence; its former `/tmp` log is not newly
recreated by this audit. Do not call the three-framework FDE matrix outstanding.

Remaining requirements are:

1. Real FOWF workflow -> operation adapter -> authenticated FDE gRPC evaluation -> workflow result,
   including resolved credentials, tenant propagation and denied authorization.
2. Real deployment verification for both Helmfile Valkey modes: an embedded release and an externally
   supplied service. These are deployment shapes, not additional FDE runtime engines.

These are unfinished integration areas, not an enumerated additional matrix of ready-to-run cases.

## Separate outstanding work and reporting rule

The full 5.61 GB acquisition/streaming acceptance is complete as recorded above; the old
whole-file-materialization blocker is repaired. Real Studio override/API acceptance, shared API
component deduplication, remaining fault injection, hundreds-of-GB scale execution and final
aggregate coverage gates remain separate work. They must not be hidden in “matrix runs.”

Preserve existing passing evidence unless a relevant change requires refresh. Record each future
execution by scenario, framework, actual engine/backend, image identity, source revision and log.
Reuse deployed fixtures where practical; do not count overlapping FEI/FDS/FOWF observations as
independent new builds or automatically multiply unrelated axes. No honest whole-stack “runs left”
total exists until unfinished fixture/scenario work is enumerated.
