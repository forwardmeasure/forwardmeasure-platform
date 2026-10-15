# Testing completion handover for Claude — 2026-10-09

## Authority and honest status

**STOPPED at the user's instruction. This document does not authorize resuming engineering work.** The only subsequent authorization was to inspect existing evidence and write this handover. Do not run builds/tests, change production or test source, commit, push, publish images, or deploy without a new instruction to resume. Preserve all uncommitted changes.

**No, all remaining regression fixtures have not been written.** The runtime matrix fixtures below exist and have recorded passing executions. The additional coverage/API regression work is incomplete. There is no complete test-by-test plan for the missing cases, no defensible exact remaining fixture count, and no reliable completion ETA. Earlier suggestions that only execution remained were incorrect.

This handover supersedes the *current status* statements in `runtime-matrix-status-2026-10-08.md` that still say runs are queued or in progress. It does not overwrite historical failures. No new builds or tests were run to prepare this document; existing logs and XML were inspected.

Workspace: `/home/pn/Documents/code/forwardmeasure`. Evidence directory, abbreviated **LOGS** below:

`/home/pn/.local/state/forwardmeasure/validation/20261008-post-rollout/`

At the stop checkpoint, the worker/wire continuation had finished with exit 0; the retained-data FOWF coverage gate had finished with exit 1. Neither was waiting for user input. Logs outside Git are not protected merely by pushing source commits.

## What is already done — do not reopen these as an unspecified backlog

| Work | Recorded evidence | Limit of the claim |
|---|---|---|
| FOWF basic runtime matrix | 9/9 cells passed | Recorded images |
| FOWF overflow matrix | 9/9 cells, 54 scenarios passed | Recorded images |
| FEI complete runtime matrix | 9/9 cells: Quarkus/Spring/Micronaut × Kafka/PostgreSQL, Pekko/PostgreSQL, Pekko/Cassandra | WorldCheck ingestion/screening, request overrides, two-tenant full/delta, simple/correlated resolution, Customer Master persistence; recorded images |
| FDS runtime matrices | 39/39 cells, 57 tests; 13 cells per framework | Direct, bounded and continuous flows; recorded images |
| FDE runtime matrices | 15/15 cells | Nine workflow combinations plus six framework × embedded/external Valkey deployment cells |
| Full WorldCheck file | 5,611,953,182 bytes; 5,818,856 rows; API through Quarkus/Pekko/PostgreSQL to OpenSearch READY, zero rejects | One full-export execution, about 14m08s; not an executed 100 GB or TB benchmark |
| Shared API ownership cleanup | 140 passing tests: 68 schema/model/handler and 72 real HTTP cases | Neutral platform ownership of generated Problem/Violation; source/framework verification, not a rerun of all image cells |

Evidence manifests in this directory:

- `fei-complete-runtime-evidence-2026-10-09.json`
- `fds-runtime-evidence-2026-10-09.json`
- `fde-runtime-evidence-2026-10-09.json`
- `fowf-activity-projection-evidence-2026-10-09.json`
- `shared-api-ownership-evidence-2026-10-09.json`

Read `runtime-matrix-status-2026-10-08.md` for the earlier FOWF basic/overflow evidence and FEI `docs/full-file-ingestion-repair-2026-10-08.md` for full-file details. These counts are different units (cells, scenarios, test cases), not additive unique-test totals.

Some later runs used `-XX:TieredStopAtLevel=1` after captured Temurin C2 crashes, including the explicit shared OpenSearch JVM override. They are functional evidence, not production-default JIT/performance certification. Matrices certify their recorded image identities, not every subsequent source change or whatever is currently deployed.

## Exact known unfinished work

| ID | Remaining action after explicit resume | Evidence / completion condition |
|---|---|---|
| R1 | Execute the four existing Quarkus reference-population CRUD tests skipped after the earlier application boot failure. Repair only if that focused run exposes a failure. | `ReferencePopulationCrudResourceTest` current Surefire XML has tests=4, skipped=4, errors=0, failures=0. Latest worker-only rerun did not include it. Completion requires four actual passes, no skips. This is an execution gap, not four unwritten fixtures. |
| R2 | Correct coverage evidence alignment and ownership before deciding which additional fixtures are missing. | FOWF `JpaWorkflowExecutionQueryRepository$Cursor` class-ID mismatch; FDE `DecisionEngineQuarkusBinding$TransactionalRulesets` mismatch. FEI/FDE diagnostics predate latest passing tests. Remote/container tests do not automatically contribute to local Maven coverage. Produce aligned per-class reports and identify applicable module/aggregate ownership. |
| R3 | Produce the missing finite, behavior-based regression plan. | Map uncovered methods/branches to existing test cases and collection gaps. Name each genuinely missing scenario, test class, framework scope and assertion. This plan has **not** been completed; the diagnostic class list is not such a plan. |
| R4 | Write and execute the missing meaningful coverage/API fixtures from R3. | Known FEI/FOWF/FDE areas are listed below. Do not call them all written. Do not replace behavioral assertions with invocation-only coverage tests. FDS may have collection/gate work rather than additional missing behaviors; verify before adding tests. |
| R5 | Pass the actual configured coverage checks for FOWF, FEI, FDS and FDE. | Require 85% handwritten lines and branches; preserve existing zero-missed-class rules where configured. FOWF's actual retained-data gate currently fails. No complete current gate pass across all four has been established. Diagnostic percentages are not gate passes. |
| R6 | Preserve and checkpoint the latest tested changes and evidence, only after renewed authorization. | FEI seven test/fixture/POM files and FDE two test files remain uncommitted; details below. Update stale status text and record latest report hashes. Do not stage unrelated deployment or credential changes. |
| R7 | Reconcile source fixes with the images actually built/published/deployed. | Latest FEI classification-merge and FOWF nested-reply fixes have component/framework evidence, but this handover does not establish deployment of those exact changes. Use existing targeted image instructions; publication/deployment require authorization. |

R2–R5 are substantial unfinished coverage work, not merely one final rerun. A precise number of additional test cases cannot be responsibly supplied from current evidence. This missing accounting is itself an unfinished deliverable, not a request to expand the user's scope.

## Current gate and diagnostic evidence

**Actual failed Maven gate:** `LOGS/fowf-retained-coverage-gate-01.log`, `jacoco:check (enforce-aggregate-coverage)` on `openworkflow-coverage`:

- Branch ratio **0.79**, minimum **0.85**.
- Missed classes **68**, maximum **0**.
- Execution-data mismatch for `com/forwardmeasure/openworkflow/execution/query/persistence/JpaWorkflowExecutionQueryRepository$Cursor`.
- BUILD FAILURE, 4.474 seconds. This was a retained-data aggregate check, not a fresh full-reactor verification.

The standalone aggregate XML at `openworkflow-coverage/target/site/jacoco-aggregate/jacoco.xml` is empty of class/counter detail. Do not use it to enumerate the 68 classes or claim complete reporting. The gate used staged classes and `target/jacoco-aggregate.exec`; generate an appropriately scoped detailed report after resume. The diagnostic scope below differs, so its missed-class count is not the gate's 68.

| Existing diagnostic XML under LOGS | Lines | Branches | Limitation |
|---|---:|---:|---|
| `fds-component-partial-diagnostic.xml` | 93.63% | 86.39% | No class-ID mismatch; not proof of all configured Maven gates |
| `fei-after-api-diagnostic.xml` | 70.17% | 55.84% | No class-ID mismatch; predates latest 24 passing worker API tests |
| `fde-after-rejections-diagnostic.xml` | 82.98% | 68.05% | One enhanced Quarkus class mismatch; predates latest seven passing tests |
| `fowf-after-components-diagnostic.xml` | 88.84% | 77.83% | Cursor mismatch; differs from actual aggregate gate scope |

These diagnostics analyzed handwritten source-associated class files against collected JaCoCo data. They do not establish canonical module ownership, latest-source completeness, or proof that every uncovered branch lacks an existing test. Generated code is exempt by provenance; do not exclude handwritten code just to pass. Review platform policy `CoverageOwnership.java`, `GeneratedCoverage.java`, and `RequireCoverageDataMojo.java`. FEI's aggregate currently owns domain coverage; other module checks still matter. FDS/FDE coverage must be checked through their configured ownership rather than inventing a new aggregate.

## Concrete areas to turn into the missing fixture plan

The following are observed diagnostic gaps, **not a claim that every listed behavior has no existing test**. Refresh data and inspect existing assertions first. Numbers below are missed branches in the listed, older diagnostic snapshots.

| Product / class | Missed branches | Behaviors to reconcile with tests |
|---|---:|---|
| FEI `IngestionPipelineTriggerApplicationService` | 159 | Trigger admission, cumulative progress, simple/correlated input construction, monitored-population trigger, source configuration |
| FEI `ReferencePopulationComparisonApplicationService` | 107 | Match validation/evaluation, fingerprinting, set-element identity |
| FEI `InvestigationApplicationService` | 70 | Create/delete/update, evidence upload/update and export |
| FEI `ResolutionCascadeService` | 56 | Identifier lookup, existing-subject selection, candidate/name/token matching |
| FEI `ReferencePopulationIngestionApplicationService` | 53 | Trigger, progress, status |
| FEI `ReferencePopulationApplicationService` | 52 | Activate, verify artifact, update, rollback |
| FEI `ReferencePopulationDeltaApplyWorker` | 47 | Apply/reindex, index metadata compatibility, result ownership, merged metadata |
| FEI `PopulationScreeningApplicationService` | 42 | Trigger, subject recording, threshold selection, completion |
| FEI `ReferencePopulationDeltaApplicationService` | 42 | Trigger and status |
| FEI `IngestionContract` | 41 | Constructor validation, index mapping, parsing overrides, raw source-data preservation |
| FOWF `WorkflowEntity` | 618 | Nested forks, protocol observation, command handling, subworkflow completion |
| FOWF `WorkflowExecutionEngine` | 335 | Interaction effects/finalization, operation replacement, completion, pending operation paths |
| FOWF `JpaHumanTaskRepository` | 79 | Listing/cursors/status/assignment/review-session handling |
| FOWF `OpenWorkflowCompiler` | 77 | Task keywords, gRPC validation, document references, human-task and secret validation |
| FOWF `OpenApiCallAdapter` | 72 | Structured documents, encoding, nullable schema normalization, server/request-body preparation |
| FOWF `HumanTaskStateMachine` | 56 | Eligibility, available actions, decisions, leases and transition events |
| FOWF `KafkaOperationAdapterDispatcher` | 55 | Cancellation, checkpoint persistence, completion/definition loops, startup and dead-letter paths |
| FOWF `ProtocolOperationCoordinatorEntity` | 50 | Observed state/claims, operation matching, deadlines, cancellation and launch |
| FOWF `ClassicJacksonJsonNodeSerde` | 43 | Node conversion and numeric serialization |
| FOWF `PekkoExecutionActivity` | 41 | Activity/effect construction |
| FDE `DroolsRuleEvaluator` | 12 | Evaluation, unload/retirement and construction |
| FDE `RulesetMigrationsMain` | 10 | Required settings, URL selection and migration entry-point behavior |
| FDE `EvaluationServiceImpl` | 9 | Evaluation/error mapping; latest wire cases may reduce this |
| FDE `TenantExecution` / `MicronautTransactionalServiceProxy` | 8 each | Tenant execution, object methods, invocation and transaction boundaries |
| FDE `ProtoStructMapper` | 7 | Latest four unit cases address wire conversions; refresh before adding more |
| FDE tenant interceptors/resolvers, evaluator cache, management/admin, Valkey store | Multiple smaller gaps | Tenant rejection/context, cache lifecycle, rejected updates, fact-window behavior; inspect exact methods in XML |

Do not delete useful direct computation or persistence-contract tests. The agreed approach is both focused direct tests and real REST/gRPC acceptance for exposed behavior across Quarkus, Spring and Micronaut. Use shared `forwardmeasure-testcontainers` infrastructure. Constructing a resource with `new`, permit-all authorization, or a service/repository directly does not establish HTTP/security/framework coverage. Read-only SQL observations are not substitutes for API mutations. FEI merge currently has no public REST endpoint; managed-service merge tests must not be labeled REST proof. See `testing-guide-for-claude-2026-10-07.md`.

## Latest executed evidence, distinct from written/compiled work

| Selection | Actual result | Log under LOGS |
|---|---|---|
| FEI `IngestionWorkerOperationsResourceTest`, all three frameworks | 24/24 passed, eight each, no skips; compiled during run | `fei-worker-api-corrected-03.log` (2m51s) |
| FDE mapper + packaged conformance | Four `ProtoStructMapperTest` cases and three framework conformance cases passed; compiled during run | `fde-evaluation-wire-01.log` (1m52s) |
| FOWF previously failed tail selection | Nine focused cases passed | `fowf-tail-failures-corrected-02.log` |
| FOWF nine previously skipped component modules | 60 original passes plus one corrected tenant-pool case = 61 passes | `fowf-component-unblocked-01.log` and `fowf-tenant-pool-corrected-01.log` |
| FEI Quarkus reference-population CRUD | Four skipped, not four passes | Current `ReferencePopulationCrudResourceTest` Surefire XML |
| FOWF retained coverage gate | Failed | `fowf-retained-coverage-gate-01.log` |

The original FOWF unblocked Maven run failed on that one tenant fixture; the subsequent targeted correction passed. Do not relabel the original run successful. The combined manifest is `fowf-unblocked-components-evidence-2026-10-09.json` (currently untracked).

Full FEI and FOWF production/test compilations previously passed in `fei-corrections-full-compile-01.log` and `fowf-corrections-full-compile-01.log`; later test changes compiled in the focused runs above. These are not evidence of a new all-repository full build after every subsequent change.

## Changes to preserve and backup status

Last recorded HEAD/checkpoint on `origin/backup/full-file-ingestion-20261008`:

| Repository | Checkpoint |
|---|---|
| platform | `4446036` |
| FOWF | `c7b42f03` |
| FEI | `4ba0435` |
| FDS | `337c4f4` |
| FDE | `85fd859` |

Latest tested, uncommitted FEI changes (seven files):

- `forwardmeasure-entity-intelligence-deployments/ingestion-service/{quarkus,spring,micronaut}/pom.xml`: shared OpenSearch test dependency.
- Each of those framework leaves' `src/test/java/.../IngestionWorkerOperationsResourceTest.java`: canonical WorldCheck contracts, admitted resolution sinks, cumulative replay-safe progress, rejected-record failure, owned/finalized real OpenSearch artifact and READY checks, duplicate/stale completion behavior.
- `forwardmeasure-entity-intelligence-migrations/src/main/resources/db/changelog/test-fixtures/ingestion-worker-operations.xml`: test-only revisions/contracts/sinks. No deployed migration history changed.

Latest tested, uncommitted FDE changes (two files):

- `decision-engine-grpc/src/test/java/com/forwardmeasure/decisionengine/grpc/ProtoStructMapperTest.java`: nested values/nulls, invalid map keys and unsupported objects.
- `decision-engine-conformance-tests/src/test/java/com/forwardmeasure/decisionengine/conformance/DecisionEngineContainerConformanceTest.java`: real gRPC wire output, invalid requests, missing rules, invalid result shapes and continued valid evaluation across all three packaged frameworks.

Unrelated dirty files include FEI `CODEX_HANDOVER.md`, deployment environment/image files in FEI/FOWF/FDS/FDE, and platform Helmfile/install/secret-consumer scripts, tests and cluster/credential configuration. **Do not stage these wholesale, inspect credentials, reset them, or claim they are this testing task's changes.** Recheck Git status before any future authorized checkpoint. This handover itself is left uncommitted under the stop instruction.

## Production fixes and targeted image handoff

Existing fixes are not an excuse to claim the complete test program is finished:

- FEI classification merge now queries persisted survivor classifications rather than trusting a stale inverse collection. Earlier component/framework evidence: `fei-component-corrections-evidence-2026-10-09.json` (114 passing cases). Affected image: resolution service for each deployed framework. Exact targeted build/push/deployment instructions are in FEI `docs/full-file-ingestion-repair-2026-10-08.md`, tag `classification-merge-20261009-1`.
- FOWF nested `WorkflowReply` decoder repair, checkpoint `1a60efd6`, retains outgoing serialization and backward-read expectations. Evidence: `fowf-wire-recovery-evidence-2026-10-09.json` (127 focused cases). Affected images: Pekko engine and Pekko operation adapter for each deployed framework; PostgreSQL/Cassandra use the same framework images. Exact commands are in FOWF `docs/execution-activity-projection-repair-2026-10-09.md`, tag `pekko-coordinator-reply-20261009-1`. No packaged matrix rerun or publication of that decoder change is claimed here.

Those documents specify bounded dependency compilation, selected leaf image packaging/push, matching tag/digest updates, Helmfile diff and rollout/readiness checks. They are instructions for a later authorized action, not actions taken while stopped. Do not do an unnecessary full rebuild or clear `.m2`.

Canonical WorldCheck/Customer Master contracts live in platform `forwardmeasure-platform-data-contracts/src/main/resources/contracts/`: `worldcheck-source-v1.json`, `worldcheck-mapping-v1.json`, `screening-document-v1.json`, `customer-master-source-v1.yaml`, `customer-master-resolution-mapping-v1.yaml`, and `opensearch/` mappings. Preserve shared versioned screening fields, all raw fields under `source_data`, explicit indexing of selected raw fields, and request parsing overrides. Do not create competing copies or edit generated Java directly.

## Suggested first steps if the user explicitly resumes

1. Preserve the latest evidence and uncommitted files; read this handover and relevant repository instructions. Do not repeat already-passed matrices simply because old prose says pending.
2. Run only the skipped CRUD class first; inspect coverage class alignment and ownership, then refresh diagnostics using the latest retained data.
3. Write the missing finite scenario list before promising a test count or ETA. Separate missing tests, written-but-skipped tests, uncollected coverage, and proven production defects.
4. Implement/run those focused regressions and actual gates. Keep written, compiled, executed, gate-passed and image-deployed evidence separate.
5. With authorization, commit only reviewed owned changes, push their backup, and reconcile affected images. Report remaining failures explicitly rather than ending with an unspecified “coverage/API work remains.”

All Maven execution must use `/home/pn/Documents/code/forwardmeasure/forwardmeasure-openworkflow/scripts/build-bounded.sh`, absolute POM paths and the existing shared Maven lock. Coordinate with any running build. Retained command scripts are `LOGS/verify-worker-and-wire-03.sh` and `LOGS/verify-fowf-retained-coverage-gate-01.sh`. Do not blindly rerun the earlier broad collection script: it selected legacy real-image tests with unsuitable default image assumptions.

For R1 only, after authorization, the bounded FEI selection is `-pl forwardmeasure-entity-intelligence-deployments/ingestion-service/quarkus -am -Dtest=ReferencePopulationCrudResourceTest -Dsurefire.failIfNoSpecifiedTests=false -Pcoverage -DskipTests=false -DskipITs=true -Dmaven.test.skip=false -Dcontainer-image.build=false -Dcontainer-image.push=false -Ddocker.skip.push=true test`, with `-f /home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-intelligence/pom.xml`. Preserve required environment/JVM settings from the last successful worker script. Do not use `skipTests=true` and call a skipped coverage check a passing gate.

Framework-selective Maven profiles were requested as a deferred optimization **after testing**. They are not a new completion gate. Hundreds-of-GB execution benchmarks were not performed; the agreed immediate full-export proof above was performed. Do not add new matrix axes or silently expand acceptance scope.


## Narrowly resumed repair — October 10, 2026

The user subsequently authorized implementation and fixtures for Kafka's workflow-start input rejection above 32 KiB. This does not resume the broader R1–R7 program or authorize running tests, committing, publishing or deploying. The gateway now stores oversized start input before Kafka publication and uses the exact published command fingerprint for outcome lookup. Seven new real Kafka/storage component invocations and nine packaged public-HTTP runtime selections are written. Full FOWF production/test compilation and the FDS launcher dependency-closure compilation passed; **none of these new fixtures has been executed**.

See [the scoped repair, fixture inventory and image instructions](../../forwardmeasure-openworkflow/docs/kafka-start-input-overflow-repair-2026-10-10.md). Only Kafka engine images are affected by this production patch. Prior response-overflow matrix passes are not evidence for oversized start-input acceptance. Preserve the other session's changes in FOWF, FEI and FDS.
