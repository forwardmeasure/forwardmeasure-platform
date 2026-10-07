# FEI, FOWF and FDS regression guide for Claude

Status: governing testing guidance agreed with the user on 2026-10-07. Codex owns the current
repair. This guide does **not** claim that the outstanding tests have been repaired or executed.

## 1. The settled policy

The user explicitly agreed to **both direct tests and REST tests**, after discussing the limits
of REST-only coverage. Preserve useful focused coverage and add missing acceptance coverage.
Quarkus, Spring and Micronaut are all required. The earlier FEI REST-only handover is superseded
on that point; it must not be used to delete legitimate algorithm, persistence or recovery tests.

The decisive distinction is what a test proves:

| Layer | Correct entry point and infrastructure | What it proves | What it cannot prove |
|---|---|---|---|
| Pure algorithm/value object | Construct the component and use fixed inputs | Mapping, normalization, matching and invariants | DI, HTTP, authorization or persistence |
| Business service integration | Obtain the production service from its framework container; real migrated PostgreSQL and other required services | Service rules, managed transactions, persistence, tenant routing and concurrency | HTTP binding or JWT admission |
| Repository/connector/storage contract | Exercise the actual persistence adapter deliberately, with real database/schema and supported shared fixtures | Queries, locking, rollback, paging, backend parity and recovery | Application wiring or user-visible admission |
| HTTP contract/acceptance | Start the real framework application; real HTTP; real identity and AuthZEN policies | Routing, generated DTO binding, validation, authentication, authorization, DI and observable business behavior | Worker/image dispatch unless that path actually runs |
| Workflow/worker acceptance | Admit through the owning API; dispatch current packaged workers; observe terminal state and persisted effects | Cross-process ingestion and recovery | Framework/backend cells that were not executed |
| Studio acceptance | Real browser and API stack, with supported fixture identity | The usable user flow and its displayed state | Other frameworks or engine backends without corresponding runs |

Do not describe a direct call to a `Resource`, launcher or service as REST acceptance. Do not
use an in-memory repository or permit-all authorization in an acceptance fixture. Constructing
a value object called `CapabilityResource` is harmless. A name containing `Service` does not
make a pure matching algorithm an application service.

Production-managed service tests must resolve the actual production factories/producers and
transaction interceptors. Moving `new Repository()` into a shared test helper does not meet that
requirement. Never add production methods or public endpoints solely to make coverage convenient;
identify the missing observable capability and justify any production API change independently.

## 2. What the scans actually establish

The supplied Claude scans identify real gaps, but their label “clear violation” is too broad under
the final two-layer policy. A direct repository contract can be legitimate; a direct launcher call
can provide dispatch evidence; neither is evidence of successful HTTP admission. Assess the
assertions and fixture wiring, not the class name or number of `new` expressions.

Source inspection confirms the constructions below. The companion
[static inventory](testing-boundary-inventory-2026-10-07.json) records current candidate files and
patterns, excludes production factories, and deliberately makes no pass/fail verdict. Counts change
as the current repair proceeds. Representative methods, lifecycle setup and production call paths
were inspected; the scan itself did not execute any tests.

### FEI findings and required treatment

| Group | Finding | Required action |
|---|---|---|
| Four `domain/service/impl` tests | They originally bound new repositories to a hand-built persistence context and constructed services. The merge test expected failure without a caller transaction despite the production service being transactional. | Keep useful business assertions. Run them through production-managed services on all three frameworks; add authenticated HTTP scenarios for the supported business operations. Verify interceptor-owned commits. |
| Nineteen `domain/repository` tests, `DomainEntityGraphIntegrationTest`, `DomainTestSupport` | Query/graph coverage is mixed with custom Hibernate bootstrap. | Retain narrowly identified repository/graph contracts where they test storage behavior. Consolidate supported migrated infrastructure and avoid using this harness for application-service acceptance. Map duplicate business scenarios to managed-service/HTTP contracts. |
| `ReferencePopulationApplicationServiceTest`, `ReferencePopulationIngestionApplicationServiceTest`, `ReferencePopulationRetentionApplicationServiceTest`, `ReferencePopulationIndexingAndComparisonIntegrationTest`, `IngestionPipelineFinderConcurrencyTest` | Direct application construction bypasses framework and HTTP admission; concurrency/index lifecycle assertions may still be valuable. | Preserve concurrency and index-integrity assertions as focused integrations. Add API admission, request validation, tenant/auth failure and full/delta lifecycle acceptance. |
| `EntityApplicationServiceTest`, `AssertionApplicationServiceTest`, `ResolutionCascadeServiceTest` | Large manually assembled collaborator graphs cannot prove production wiring. | Resolve real services through DI; exercise entity/assertion/investigation actions over REST. Check commit/reload and dependent-record outcomes. |
| `PopulationScreeningExportApplicationServiceTest` | Direct export service entry does not prove endpoint, authorization or serialization. | Keep export-format tests and add authenticated HTTP export, status, content and denial checks. |
| `CustomerMasterIngestionWorkerIntegrationTest`, `SimpleSourceIngestionWorkerProgressCheckpointingIntegrationTest`, `PopulationScreeningWorkerIntegrationTest` | Direct workers are legitimate targets, but custom persistence/service assembly and bypassed callbacks leave wiring gaps. | Use packaged workers and real callback contracts where applicable; use managed services for business verification. Keep deterministic checkpoint/failure tests, and add API-originated runs. |
| `IngestionPipelineFowfK3sVerificationTest` | A manually seeded revision plus permit-all direct launcher bypasses FEI admission even if Kubernetes and FOWF are real. | Retain only accurately scoped component evidence during migration. Add/execute FEI trigger API acceptance with actual credentials, workflow completion and persisted results. |
| `ScreeningMatchServiceIntegrationTest`, `CanonicalScreeningIngestionIntegrationTest`, `HashingTextEmbeddingServiceTest` | Computational and real-OpenSearch component tests. | Keep. They cover distinct matching/mapping failures; they do not replace API acceptance. |

The four managed-service contracts are currently being refactored; their presence in source is not
proof that all three framework runs have passed. Do not delete the original business assertions
before their replacements are compiled and verified.

### FDS findings and required treatment

| Group | Finding | Required action |
|---|---|---|
| `IngestionRunResourceTest`, `WorkflowRunResourceTest` | Construct resources directly; permit-all authorization cannot establish endpoint security. | Label any retained serialization/delegation checks as unit tests. Add real HTTP admission/denial contracts in all launcher deployment leaves. |
| `DirectIngestionLauncherTest`, `WorkflowIngestionLauncherTest` | Direct launcher tests can cover planning and request-to-job construction. | Keep useful focused contracts; obtain production-managed launchers for integration wiring. Require REST acceptance separately. |
| Four `DirectIngestionLauncher*RealImageIntegrationTest` classes | Real Jobs/images and OpenSearch effects, but launcher entry is direct and generally permit-all. | Preserve costly, meaningful Spark/Kafka/Pekko dispatch assertions. Make equivalent business acceptance originate from the launcher REST API and real AuthZEN identity. |
| `DirectIngestionLauncherKeycloakIntegrationTest` | Source confirms a **real Keycloak denial** for an ungranted cancel, despite direct launcher construction. It is not a permit-all test. | Keep the denial-ordering regression as scoped security integration; add HTTP JWT/claims/routing coverage. Do not discard it solely because the launcher is constructed. |
| Launcher deployment matrix tests | Real framework/HTTP entry exists, but “smoke” or HTTP 202 alone does not establish completed ingestion. | Audit every matrix cell for terminal completion, persisted document assertions, fixture authorization and current images. |
| Pekko/Kafka/Spark engines, runners, field mapping, job launcher | Jobs invoke these runtime components; they often have no REST endpoint. | Keep direct runtime contracts, including retries, malformed rows, key policy, drain and commit/ack ordering. Tie representative scenarios to API-originated Jobs. |
| `JpaPagingSourceTest`, `PekkoJpaSourceIntegrationTest` | The connector intentionally reads a repository. | Keep real adapter contracts with the shared database fixtures. A test-only repository for this explicit boundary is not a fabricated application implementation. |

### FOWF findings and required treatment

| Group | Finding | Required action |
|---|---|---|
| `WorkflowDefinitionManagementServiceTest`, `WorkflowExecutionManagementServiceTest`, `HumanTaskApplicationServiceTest` | Memory stores/recording providers can check state-machine decisions, but cannot establish DB transactions, locking or DI. | Retain valuable pure decisions as explicitly named unit tests. Add managed-service integration and authenticated REST lifecycle tests. |
| `WorkflowManagementResourceTest`, `HumanTaskResourceTest`, `AuthorizationSmokeSpringTest` | Direct resource construction is not HTTP. Some also construct full persistence graphs. | Add real framework HTTP tests; keep only distinct narrow resource/unit assertions where useful. Remove misleading acceptance claims. |
| `WorkflowDefinitionRepositoryTest`, `JpaWorkflowExecutionRepositoryTest`, `JpaWorkflowExecutionProjectionStoreTest` | Real JPA contracts can detect persistence defects; custom application graphs bypass production assembly. | Keep storage-level invariants; migrate business-service wiring to production DI and add REST admission/history/query verification. |
| `AsyncTaskUpgradeAndCommandRecoveryTest` | Real upgrade, lost acknowledgement and restart recovery checks are valuable. The migration fixture deliberately inspects stored state. | Keep the upgrade/recovery contract, historical schema fixture and restricted-role checks. Also prove API-admitted commands recover with unchanged identity; do not rewrite applied migrations. |
| `HumanTaskConcurrencyTenancyContractTest`, `JpaHumanTaskRepositoryContractTest` | Lease races, duplicate command races and tenant isolation need real concurrent transactions. | Preserve barrier-based storage tests and add API races. A serial memory fake cannot prove these invariants. |
| `HumanTaskManagement*ApiParityTest` | HTTP parity exists, but test hooks build `JpaHumanTaskRepository` directly for setup/checks. | Separate supported setup/observation from the behavior under test; API operations perform business mutations. Prefer production-managed services or migration fixtures for prerequisites, with restricted read-only checks for otherwise invisible invariants. |
| `KafkaEngine*BindingTest`, `OksCloudEventIngressResourceTest`, `OksCloudEventIngressResourceKafkaIntegrationTest`, `CloudEventIngressResourceTest`, `CloudEventIngressResourceResponseTest` | Direct binding/resource tests and authorization doubles do not prove real endpoint authentication. | Keep distinct wiring/runtime assertions and add authenticated real HTTP ingress/command acceptance for every framework. |
| `AuthzenAuthenticatedActorProviderTest`, `AuthorizedOperationExecutorsTest`, `OperationAuthorizationTest`, `AuthzenOperationSecurityResolverTest`, Kafka/Pekko adapter runtime tests | Recording authorization can prove exact resource/scope/context propagation and that denial prevents execution. | Keep these deterministic contracts, including denied/contextless cases. Add real Keycloak/AuthZEN workflow-to-adapter runs; no claim of live PDP behavior from a double. |
| `CloudEventOutboxHandlerTest`, `SubscriptionProjectionHandlerTest` | Memory/recording stores can isolate event decisions, not durable delivery. | Keep useful decisions; require durable outbox/subscription restart and duplicate-delivery contracts. |
| `HibernateSessionWorkflowExecutionQueryRepositoryTest`, `JpaSubworkflowPlanResolverTest` | Persistence internals have no independent public REST API. | Keep real persistence integration. Add end-to-end workflow evidence that the engine uses them correctly. |
| PostgreSQL/Cassandra subscription repositories, `TenantAwareCloudEventSubscriptionRoutingTest`, `CloudEventSubscriptionRepositoryParityTest`, `RealCloudEventOutboxRecoveryTest`, `RealSubworkflowRecoveryTest` | Direct backend parity and restart contracts are appropriate. | Retain both backends and their distinct failure modes. Consolidate raw containers through the shared infrastructure. Add representative REST-admitted workflow recovery; do not invent REST endpoints for internal stores. |

Some inspected FOWF tests instantiate raw PostgreSQL, Redpanda or Cassandra containers. That is a
separate shared-infrastructure issue, not a reason to remove their backend assertions. PostgreSQL
and Kafka wrappers already exist. If an equivalent Cassandra wrapper is missing, add the reusable
support in `forwardmeasure-testcontainers` rather than duplicating lifecycle code in each product.

## 3. Infrastructure and fixture rules

1. Use `forwardmeasure-testcontainers` classes and extensions. Existing entry points include
   `PostgreSqlTestContainer`, `KafkaTestContainer`, `OpenSearchTestContainer`, `KeycloakTestContainer`,
   `KubernetesTestContainer`, `MinioTestContainer`, `WithPostgreSqlContainer`, `WithKafkaContainer`,
   `WithOpenSearchContainer` and `WithKubernetesContainer`. Check the current API before use.
2. FEI database fixtures use `EntityIntelligenceTenantProvisioner` or
   `WithMigratedEntityIntelligenceTenant`, which build on the shared PostgreSQL extension and apply
   the real migration sequence. A database container alone is not a correctly migrated application.
3. Boot the real application/bindings. Inject the real service interfaces; use the framework's
   transaction manager. Do not bind new repositories to a test-created EntityManager to simulate DI.
4. Provision actual identity, tenant registry and AuthZEN grants with the shared AuthZEN/Keycloak
   fixture. Use a valid denied identity as well as permitted identities. Do not log real credentials.
5. Keep tests independent. Bulk deletion and tenant-wide counts need isolated tenant databases or
   rigorously scoped data. Never point destructive tests at a production endpoint or database.
6. Create business data through the API when testing that API path. For focused service contracts,
   setup through the managed service is appropriate. Prerequisites with no supported creation path
   may use reviewed test-only migration fixtures. Do not prepopulate the expected ingestion output.
7. SQL is appropriate for migration catalogs, privileges, upgrade snapshots and limited independent
   storage observation. It must not implement the business operation or repair state to make an
   acceptance test pass. Read JDBC booleans with `getBoolean()` and check SQL NULL explicitly.
8. Preserve deployed migration history; fixes use new migrations when required. Do not replace real
   migration validation with `hbm2ddl=create-drop`. Never edit generated Java directly.
9. Use distinct transactions/connections for concurrency and commit/reload assertions. For a
   transactional service, include a caller-without-outer-transaction case to prove its interceptor.
10. Reuse domain assertions where the observable contract is identical, but make each framework
    instantiate its real application. One Quarkus result does not establish Spring or Micronaut parity.

## 4. Required acceptance scenarios

### FEI ingestion and screening

A valid acceptance starts with an authenticated public request and verifies the actual effects:

- WorldCheck: the checked-in premium CSV, canonical source schema, mapping document and explicit
  OpenSearch index JSON are supplied as separate inputs. Override a different population default
  to detect ignored request fields. Verify all expected record IDs and rejected-row counts.
- Check actual workflow terminal success, FEI revision/progress status and OpenSearch documents;
  READY or HTTP 202 alone is insufficient. A fake acknowledgement is not successful indexing.
- Verify server-owned index naming/ownership, strict canonical mappings and metadata. The shared,
  versioned canonical document retains all raw fields under `source_data`; only explicitly mapped
  raw fields are searchable. Stored-only fields must still round-trip in `_source`.
- Verify public screening finds both a person and an organization, including the premium sample's
  blank-FIRST-NAME organization. An unrelated query must not produce a spurious match.
- Cover simple-source, correlated and resolution ingestion. Correlation assertions must detect
  missing/blank keys, join/cardinality errors and wrongly associated source records.
- Cover full and delta revisions, updates/deletes, activation, rollback, retention/purge eligibility,
  idempotent retries, concurrent triggers, tenant isolation and protected index ownership.
- Exercise malformed schema/mapping/index inputs and reject incompatible document contracts before
  claiming success. Add worker failure, callback retry, restart/recovery and accurate partial progress.
- Studio tests must invoke the real API path and verify visible progress/failure, activation and
  screening behavior. UI unit tests and a successful TypeScript build are separate evidence.

Framework axis: Quarkus, Spring, Micronaut. Where workflow engine behavior is in scope, also cover
Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra. This is a nine-cell acceptance matrix, not three
names attached to one runtime. Record any deliberately narrower component-test matrix explicitly.

### FOWF workflow lifecycle

Use real definition publication and execution admission; do not insert a synthetic “already
published” definition or a successful execution as the operation under test. Cover valid and invalid
publication, duplicate/idempotent admission, pinned engine selection, stale command versions,
pause/resume/cancel, persisted query/history/error responses, human-task claim/complete/lease races,
authorization denial and cross-tenant access. Recovery cases need durable pre-failure admission,
process/actor restart, original command identity and observable single logical effect.

The operation adapter is reached by the real workflow. Test actor/tenant propagation and a live
PDP denial that prevents the external operation. Keep deterministic adapter authorization unit tests
for exact request construction and fail-closed ordering.

### FDS ingestion execution

Start through the launcher HTTP API; inspect the admitted run/Jobs and terminal result. Use current
image versions and declared build dependencies. Cover direct and FOWF-managed execution, supported
bounded/continuous modes, and Pekko/Kafka/Spark paths. For Spark staging plus delivery, require both
actual Jobs and verify the final indexed documents. Check malformed rows, source key policy,
read-committed Kafka frontiers, sink failure propagation, timeout/cancel, retry/idempotency and drain.
Do not count an unknown-revision 404 as engine dispatch evidence.

## 5. Test design that catches real regressions

Before writing a test, record its concrete failure: “a malformed CSV was acknowledged but not
indexed”, “a callback failed after persistence”, or “the framework never started the runtime”.
Choose assertions that would fail with that defect present. Reuse independent expected fixtures;
do not derive expected output with the mapper/normalizer under test.

Include success and relevant failure cases, exact identities/counts and durable re-reads. Avoid
assertions such as “not 500”, “non-null”, “202 returned”, or “a method was called” as the sole
acceptance result. Retrying must have a bounded deadline and report the last observed state.
Do not use broad catches, skipped failing tests, lower thresholds or extra exclusions to pass a gate.

More tests should mean more failure modes covered, not cloned assertions in many classes. Shared
contracts can reduce duplication across frameworks; framework adapters must still boot separately.
Preserve focused regressions during migration until replacement evidence is recorded. Code with no
REST caller may serve a worker, scheduler or recovery loop; trace those callers before labelling it
unreachable. Do not delete it solely because the OpenAPI file lacks a corresponding operation.

## 6. Coverage, builds and evidence

The user's policy is **85% lines and 85% branches**, with generated code exempt. Preserve the
existing missed-class policy unless separately authorized. Aggregate coverage may combine real
cross-module service and REST execution; generated sources must be classified correctly even when
handwritten and generated classes share a package. Handwritten adapters are not generated code.

Use the bounded wrapper and its shared Maven lock. Always provide an absolute `-f` because the
wrapper changes directory. Keep Maven cache enabled; do not wipe the user's repository or build
cache. Cache hits never constitute fresh execution evidence. Generated-source registration,
resources needed by generators and runtime packaging must remain correct on cold and warm builds.
Coordinate with the active build; do not launch competing Maven processes or add `-T`.

Compile production **and tests** using `test-compile -DskipTests=true -DskipITs=true
-Dmaven.test.skip=false`. Execute tests only when authorized; execution is authorized for the current
Codex-led repair, not implicitly for every future Claude session. Use focused modules/dependency
closures first. Do not deploy, publish images, commit or push without explicit authorization.

Every handover must separate:

| Evidence | Required report |
|---|---|
| Written | File, scenario, entry point, real dependencies and remaining fixture prerequisites |
| Compiled | Exact command, module/framework scope, log, exit status and whether test sources compiled |
| Executed | Selected tests, count, pass/fail/skip, framework/backend, actual image identity and log |
| Accepted | Required matrix cells passed, persisted effects verified, known gaps disclosed |

A test written behind an unset environment property is not executed. A skipped matrix cell is a
gap. A reactor's formatting phase reporting SUCCESS is not a passing regression suite. Record JVM
version and process-local workarounds when they affect reproducibility. Do not inspect crash-dump
credentials while diagnosing native failures.

## 7. Current repair sequence and ownership

Codex owns fixes and regression development. Claude should use this guide for future work and
avoid concurrent edits/builds without coordination. Existing uncommitted changes must be preserved.

1. Stabilize the shared build/cache and actual failing regressions; keep current evidence in the
   FEI ingestion handover. Fix production defects exposed by tests rather than weakening assertions.
2. Complete the four FEI production-managed service contracts across all frameworks. Preserve their
   meaningful assertions; independently review any proposed new service API for test-only motivation.
3. Execute the real WorldCheck and simple/correlated/resolution API paths, current-image FOWF
   dispatch, lifecycle/recovery and Studio acceptance. Build only the required image closure.
4. Triage the remaining scan candidates by the layers above. Retain legitimate storage/runtime/unit
   contracts, replace fabricated application graphs, and add missing authenticated HTTP cases.
5. Measure aggregate handwritten coverage and close behavioral gaps. Report exact remaining matrix
   cells and blockers. Finish with the affected-image build commands and deployment steps.

The full repair is not complete. Earlier successful worker/index tests do not close public API,
framework matrix or Studio acceptance. The FEI WorldCheck API case is written and compiled but has
not yet executed; the unchanged three-row premium sample is representative, not a full vendor file.

## 8. Source pointers reviewed during this analysis

- [AssertionAndResolvedEntityServicesTest](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-domain/src/test/java/com/forwardmeasure/entityintelligence/domain/service/impl/AssertionAndResolvedEntityServicesTest.java)
- [EntityMergeAndDeletionServiceImplTest](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-domain/src/test/java/com/forwardmeasure/entityintelligence/domain/service/impl/EntityMergeAndDeletionServiceImplTest.java)
- [ScreeningPopulationAndPromptServicesTest](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-domain/src/test/java/com/forwardmeasure/entityintelligence/domain/service/impl/ScreeningPopulationAndPromptServicesTest.java)
- [CanonicalScreeningIngestionIntegrationTest](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-ingestion-worker/src/test/java/com/forwardmeasure/entityintelligence/ingestionworker/CanonicalScreeningIngestionIntegrationTest.java)
- [ReferencePopulationPublicApiAcceptanceIT](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-ingestion-k3s-verification/src/test/java/com/forwardmeasure/entityintelligence/ingestion/k3s/ReferencePopulationPublicApiAcceptanceIT.java)
- [DirectIngestionLauncherKeycloakIntegrationTest](../../forwardmeasure-data-streaming/forwardmeasure-data-streaming-launcher-application/src/test/java/com/forwardmeasure/datastreaming/launcher/application/DirectIngestionLauncherKeycloakIntegrationTest.java)
- [DirectIngestionLauncherSparkTwoJobRealImageIntegrationTest](../../forwardmeasure-data-streaming/forwardmeasure-data-streaming-launcher-application/src/test/java/com/forwardmeasure/datastreaming/launcher/application/DirectIngestionLauncherSparkTwoJobRealImageIntegrationTest.java)
- [WorkflowBoundedMatrixQuarkusKafkaStreamsSmokeTest](../../forwardmeasure-data-streaming/forwardmeasure-data-streaming-deployments/launcher/quarkus/src/test/java/com/forwardmeasure/datastreaming/launcher/quarkus/WorkflowBoundedMatrixQuarkusKafkaStreamsSmokeTest.java)
- [WorkflowDefinitionManagementServiceTest](../../forwardmeasure-openworkflow/openworkflow-definition-management/openworkflow-definition-management-application/src/test/java/com/forwardmeasure/openworkflow/definition/management/WorkflowDefinitionManagementServiceTest.java)
- [WorkflowExecutionManagementServiceTest](../../forwardmeasure-openworkflow/openworkflow-execution-management/openworkflow-execution-management-application/src/test/java/com/forwardmeasure/openworkflow/execution/management/WorkflowExecutionManagementServiceTest.java)
- [AsyncTaskUpgradeAndCommandRecoveryTest](../../forwardmeasure-openworkflow/openworkflow-execution-management/openworkflow-execution-management-jpa/src/test/java/com/forwardmeasure/openworkflow/execution/persistence/AsyncTaskUpgradeAndCommandRecoveryTest.java)
- [JpaWorkflowExecutionRepositoryTest](../../forwardmeasure-openworkflow/openworkflow-execution-management/openworkflow-execution-management-jpa/src/test/java/com/forwardmeasure/openworkflow/execution/persistence/JpaWorkflowExecutionRepositoryTest.java)
- [HumanTaskConcurrencyTenancyContractTest](../../forwardmeasure-openworkflow/openworkflow-human-task/openworkflow-human-task-jpa/src/test/java/com/forwardmeasure/openworkflow/humantask/domain/repository/HumanTaskConcurrencyTenancyContractTest.java)
- [HumanTaskManagementQuarkusApiParityTest](../../forwardmeasure-openworkflow/openworkflow-deployments/human-task-management/quarkus/src/test/java/com/forwardmeasure/openworkflow/deployment/humantask/quarkus/HumanTaskManagementQuarkusApiParityTest.java)
- [AuthorizedOperationExecutorsTest](../../forwardmeasure-openworkflow/openworkflow-operation-adapter/openworkflow-operation-adapter-core/src/test/java/com/forwardmeasure/openworkflow/operation/AuthorizedOperationExecutorsTest.java)
- [AuthzenOperationSecurityResolverTest](../../forwardmeasure-openworkflow/openworkflow-operation-adapter/openworkflow-operation-adapter-kafka-streams/src/test/java/com/forwardmeasure/openworkflow/adapter/kafka/AuthzenOperationSecurityResolverTest.java)
- [CloudEventSubscriptionRepositoryParityTest](../../forwardmeasure-openworkflow/openworkflow-engine/openworkflow-pekko-engine/openworkflow-pekko-persistence-contract-tests/src/test/java/com/forwardmeasure/openworkflow/persistence/CloudEventSubscriptionRepositoryParityTest.java)
- [RealCloudEventOutboxRecoveryTest](../../forwardmeasure-openworkflow/openworkflow-engine/openworkflow-pekko-engine/openworkflow-pekko-persistence-contract-tests/src/test/java/com/forwardmeasure/openworkflow/persistence/RealCloudEventOutboxRecoveryTest.java)

## 9. FDE deployment acceptance supplement

FDE exposes gRPC. Apply the same authenticated public-interface rule through real gRPC calls to
each packaged Quarkus, Spring and Micronaut server; do not invent a REST interface for the test.

Inspection of `DecisionEngineContainerConformanceTest` found an opt-in property
`decision.engine.conformance.live` that the ordinary full-suite profile does not enable. The
fixture still sends unsigned tenant metadata and omits JWT/AuthZEN deployment configuration,
although current production bindings require verified tokens. It also starts raw PostgreSQL
Testcontainers instead of the shared PostgreSqlTestContainer. It is not current deployment
acceptance evidence merely because its source compiles.

Repair requirements:

- Use shared PostgreSQL and Keycloak fixtures. Start real migration and service images; record
  their tags/digests. Limit resources and execute framework containers sequentially.
- Provision tenant registry records through the owning provisioning path. FDE migrations
  deliberately do not register tenants themselves; creating tenant databases alone is insufficient.
- Configure issuer, container-reachable JWKS, expected audience, organization client and AuthZEN
  confidential-client secret. Mint real tokens with the expected audience and tenant roles.
  The existing generic AuthZEN fixture realm has no explicit audience mapper; verify issued claims
  and provision the appropriate mapper rather than disabling production audience validation.
- Give two distinct actors separate tenant memberships. Prove cross-tenant ruleset and stateful
  window isolation with the same ruleset names and caller keys. Never select the tenant using only
  an unsigned header. Reject conflicting metadata, missing/invalid tokens and wrong audiences.
- Exercise separate evaluate/manage/admin grants. An evaluate-only caller must be unable to
  upload rules or clear caches. Denials must leave rules and state unchanged.
- Retain golden stateless/stateful evaluations, pinned versions, cache lifecycle and public health
  probes. Read-only JDBC observation may independently verify physical database isolation after
  gRPC writes; it must not create the expected business rows.
- Exercise the actual FOWF-to-FDE adapter/token path separately; direct gRPC acceptance cannot
  establish workflow publication, dispatch, credential resolution or adapter egress correctness.

Existing `VerifiedJwtTenantResolverTest` is a useful direct signature/claims contract. It does not
prove packaged-server wiring, real authorization or transaction commit before RPC success. Its
fixture must also satisfy the production audience contract. No FDE full-framework pass is claimed
by this analysis.
