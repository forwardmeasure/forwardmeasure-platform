# Runtime matrix accounting — October 8, 2026

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

## FEI: 27 parameterized scenario invocations

Source: [ReferencePopulationPublicApiAcceptanceIT](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-ingestion-k3s-verification/src/test/java/com/forwardmeasure/entityintelligence/ingestion/k3s/ReferencePopulationPublicApiAcceptanceIT.java).
Production and test compilation passed in `fei-provider-semantics-all-frameworks-compile.log`.

| Deployment | WorldCheck public ingestion/indexing/screening | Full/delta lifecycle | Simple + correlated resolution |
| --- | --- | --- | --- |
| Quarkus / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Quarkus / Pekko / PostgreSQL | Shared-helper scenario refreshed and passed 19:38 EDT on current images | No retained passing acceptance evidence | No retained passing acceptance evidence |
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
Thus 26 invocations lack retained passing evidence; the equivalent shared-helper pass has now been
refreshed on current images (see evening continuation).
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

Concrete gaps:

1. All 12 direct classes contain a curl-container fallback. A stand-in can write a constant document
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
