# FEI, FOWF and FDS regression guide for Claude

Status: governing testing guidance agreed with the user on 2026-10-07. Codex owns the current
repair. This guide does **not** claim that the outstanding tests have been repaired or executed.

Current execution accounting is maintained in [the runtime matrix ledger](runtime-matrix-status-2026-10-08.md).
The dated diagnosis sections below describe findings at inspection time. Since then the full
5,611,953,182-byte WorldCheck API/worker/OpenSearch run and packaged authenticated FDE conformance
passed; neither proves the outstanding runtime matrices. On October 9 the missing FEI runtime,
FOWF overflow and FDE adapter/Valkey deployment fixtures were written and compiled. Shared
`PublicWorkflowAcceptanceClient`/`RealFowfWorkflowFixture` helpers and the shared GCS/Kubernetes
containers are the reusable fixture implementations; do not copy their infrastructure into new tests.
The fixtures must execute before their assertions can be reported as verified.

### October 9: defects caught only by packaged framework runs

The basic FOWF API/recovery matrix now passes all nine combinations; see the ledger and its JSON
evidence for the exact scope. Its first runs exposed three concrete framework wiring gaps:

- Spring/Kafka loaded unused JPA configuration and failed startup because a repository bean was
  absent. The Kafka-only deployment now excludes database auto-configuration and supplies tenant scope.
- Spring/Pekko/Cassandra eagerly created a relational tenant registry from the Cassandra journal
  endpoint. All three bindings now use the separate relational tenant settings.
- Micronaut/Kafka returned HTTP health as ready while its lazy engine factory had never run. An
  independently started adapter failed because the engine-owned checkpoint topic did not exist.
  The engine now starts eagerly and readiness checks the actual runtime.

Calling a binding's factory method directly can prove its method body calls `start()`, but cannot
prove that the framework ever invokes it during startup. Keep focused tests as component evidence;
require the real application to become ready and execute a public request before declaring the
framework operational. Do not create production-owned topics in a fixture to hide this kind of
startup defect. These failures and their repaired-image reruns are preserved in the matrix ledger.

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

The selected FEI managed-service and HTTP replacements have since compiled and passed on all
three frameworks: 25 cases per framework, 75 total (57 managed-service and 18 HTTP cases).
The run sequence and repaired wiring/transaction defects are recorded in the FEI handover under
“Three-framework service contracts pass; backup verified”. These results do not establish complete
ingestion acceptance or close every audit row above. Do not delete original business assertions
before their corresponding replacements are compiled and verified.

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

### No substitute worker in ingestion acceptance

A shell HTTP client is acceptable for an actual API request or a readiness probe. It must never
replace the ingestion worker or write the expected result directly into the destination. Twelve
FDS direct matrix fixtures previously selected a curl image when registry credentials were absent
and wrote a constant OpenSearch document. That proves dispatch/connectivity only; it cannot prove
parsing, mapping or ingestion. The fallback has been removed across Quarkus, Spring and Micronaut.

Require an explicitly selected current local executor image, import it through the shared
Kubernetes fixture and pin its digest. Missing prerequisites must fail clearly before infrastructure
startup; they must not silently select a stand-in or report a skipped case as passing. Seed source
rows/files/topics only. Observe actual completion, transformed fields, stable IDs, updates and
rejected records through the destination/API. A successful Job alone is insufficient.

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

For real-time recovery tests, timestamp the command when it is dispatched, after fixture startup
and definition admission. Observe the pending durable deadline before stopping the original
process and require that shutdown finishes before it can fire. After restart, a due timer may
already have fired and been atomically removed from the live store. Verify the original timer
identity/deadline in committed firing history and the resulting terminal state, rather than
requiring observation of that transient store entry. Never silently reset a deadline on restart.
The earlier store-based assertion passed alone but failed in the 94-case combined run. The revised
history-based case compiled and passed in `/tmp/fowf-kafka-retry-history-repair-20261008.log`;
the second combined run is `/tmp/fowf-kafka-engine-combined-02-20261008.log` (interrupted at the
user's request so they can rebuild and deploy; no final suite result).

The operation adapter is reached by the real workflow. Test actor/tenant propagation and a live
PDP denial that prevents the external operation. Keep deterministic adapter authorization unit tests
for exact request construction and fail-closed ordering.

### Overflow acceptance matrix

Shared-library HTTP/MinIO tests establish bounded reads, offload and digest behavior only. They
cannot stand in for Kafka, Pekko/PostgreSQL or Pekko/Cassandra runtime acceptance. For each of
those engines, exercise the production worker/actor transport with shared real object storage and
the real durable backend. A handwritten `StorageClient` fake is useful only as a focused component
fixture and must be labelled accordingly.

For each Quarkus/Spring/Micronaut deployment, submit an authenticated workflow through its real
API and prove that the selected engine actually ran it. Cover all nine framework/engine cells;
changing a parameter label while invoking one shared fake runtime does not create a matrix.
Require observable artifact storage and downstream materialization, correct terminal history and
persisted query state, tenant isolation, oversized input rejection and object-storage failure.
Exercise fresh-process recovery on each durable backend, with the original artifact and execution
identity retained. Include a modified-object digest failure so a URI-only happy path cannot pass.

Use actual producer/consumer serializers in the transition-integrity regression: hashing an
in-memory JSON tree and reading the same tree back misses changes introduced on the wire, such
as sorted nested properties. Keep meaningful exhaustive boundary cases at the shared layer, and
representative end-to-end cases in every deployment cell; duplicating every pure algorithm input
nine times is not required. Record written, compiled and executed results separately per cell.

### FDS ingestion execution

Audit detail, 2026-10-08: FDS's `WorkflowBoundedMatrix{Quarkus,Spring,Micronaut}*SmokeTest`
classes do invoke launcher HTTP and real workers. However, their reused `RealFowfWorkflowFixture`
hardcodes Quarkus FOWF definition/execution/engine/adapter images, and its Pekko setup uses
PostgreSQL. A Spring launcher label therefore does not establish a Spring FOWF runtime cell,
and the existing two engine labels do not establish Cassandra. Extend the actual fixture/runtime
selection and image prerequisites; do not count renamed tests as additional deployed cells.
Preserve the existing direct-launcher contracts alongside the public launcher cases.

That older fixture set `QUARKUS_OIDC_TOKEN_ISSUER=any` to bridge host/container addresses.
A real HTTP regression reproduced acceptance of a same-key token from an unexpected issuer
(`/tmp/fds-fowf-issuer-baseline-20261008.log`). The repair pins the expected token issuer separately
from the container-reachable discovery/JWKS URL. Production/test compilation and both real HTTP
cases passed in `/tmp/fds-fowf-issuer-repair-20261008.log` (two cases, no failures/errors/skips).
The expected issuer works before and after rejection; the unknown-execution 404 contract also
passes. This is fixture authentication evidence, not engine-dispatch or ingestion acceptance.
New acceptance fixtures can pin a real issuer using the shared Keycloak network fixture and
configure host-reachable JWKS separately for framework test hosts. Keep signature, issuer and
audience validation enabled; prove rejected identities through the actual public endpoint.
Do not change production authorization to accommodate a test-network hostname mismatch.

The October 8 packaged three-framework extension found a real Micronaut deployment defect: its
JWT config selected trusted JWKS but omitted the issuer claims validator. A token signed by the
same real Keycloak key with a different `iss` was accepted. This is why a malformed-signature
negative case alone is insufficient. Require a real same-key wrong-issuer token to return 401,
and require a valid token to work again afterward. The shared `KeycloakTestContainer` now has
an alternate-loopback-issuer helper for local unpinned fixtures; pinned/network fixtures must
reject that helper rather than silently minting a token with the original issuer.

Keep the expected issuer distinct from internal JWKS/PDP routing. Micronaut's placeholder parser
does not implement Spring-style nested `${ENV:${property}}` fallback: the attempted repair left
a literal trailing brace in the expected issuer, and live tests caught rejection of valid tokens.
The verified FOWF setting is `${OPENWORKFLOW_KEYCLOAK_ISSUER:openworkflow.authorization.issuer:}`.
Do not install a test-only issuer validator: the test must exercise the production configuration.
The corrected actual Quarkus/Spring/Micronaut execution images pass six cases in
`/tmp/fds-fowf-three-framework-issuer-repair-03-20261008.log`. Three FDS launcher issuer cases
and nine FEI ingestion authorization cases subsequently passed across the three frameworks in
`/tmp/fei-fds-three-framework-issuer-regressions{,-02}-20261008.log` (see the handover for each
module's result; neither combined log is wholly green). FEI screening/resolution checks remain
pending at that checkpoint. The later persistent recovery run passed 15 screening HTTP cases
and three resolution issuer cases across all three frameworks. These are HTTP results, not
completed worker dispatch.

The screening run also exposed stale test-only SQL that omitted newly required population
contracts. The correct response is to create valid fixture metadata through managed services and
retain the production constraint. Do not relax the constraint, edit deployed migrations, or label
an application that failed during fixture setup as passing an HTTP negative case. Screening tests
may explicitly seed canonical OpenSearch documents to isolate recall/scoring; ingestion acceptance
must separately verify that actual workers create those documents from the supplied source files.

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
2. The four FEI production-managed service contracts now pass across all three frameworks
   (57 managed-service cases plus 18 existing HTTP cases). Preserve their meaningful assertions;
   independently review any proposed new service API for test-only motivation. This does not
   make the remaining domain inventory or public ingestion acceptance complete.
3. Execute the real WorldCheck and simple/correlated/resolution API paths, current-image FOWF
   dispatch, lifecycle/recovery and Studio acceptance. Build only the required image closure.
4. Triage the remaining scan candidates by the layers above. Retain legitimate storage/runtime/unit
   contracts, replace fabricated application graphs, and add missing authenticated HTTP cases.
5. Measure aggregate handwritten coverage and close behavioral gaps. Report exact remaining matrix
   cells and blockers. Finish with the affected-image build commands and deployment steps.

The full repair is not complete. Earlier successful worker/index tests do not close public API,
framework matrix or Studio acceptance. At the original October 7 checkpoint the FEI WorldCheck API case had only compiled. Later
three-row and full-export passes are recorded in the matrix ledger; the small fixture alone is
never full-file scale evidence.

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

## 9. FDE deployment acceptance supplement (original diagnosis; see current ledger)

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

Implementation update, 2026-10-08: the packaged conformance fixture now uses real Keycloak tokens,
audience mappers, scoped grants and shared PostgreSQL, and `full-suite` enables it. It runs each
framework independently and checks evaluation-only callers cannot replace rules or clear caches.
The audience helper passed six real-Keycloak fixture cases; all 26 FDE modules compiled production
and test sources. The first packaged run exposed the fixture's wrong resource-server decision
strategy and Micronaut readiness using a transaction-bound datasource without a transaction.
An overlapping-role regression reproduced the fixture failure; aligning it with production's
AFFIRMATIVE strategy restored all six fixture cases. Unwrapping the physical control-plane pool
repaired Micronaut readiness. All three packaged gRPC cases subsequently passed in
`/tmp/fde-authenticated-conformance-03-20261008.log`, including cross-tenant Valkey-window isolation.
Valkey now uses the shared authenticated fixture too; its three component tests and the subsequent
three-framework packaged rerun passed (`/tmp/fde-authenticated-shared-fixtures-conformance-20261008.log`).
Embedded/external deployment-shape proof and actual FOWF adapter acceptance remain open.
These passing cases do not establish those other acceptance layers.

## 10. Reusable test contracts and newly confirmed fixture gap

Keep reusable JUnit contracts in `src/test/java`, attach a test JAR at `test-compile`, and select
it with `<type>test-jar</type><scope>test</scope>` from each framework leaf. This permits reactor
`test-compile`/`test` consumers without packaging assertions as production code or introducing a
coverage waiver. Declare fixture dependencies explicitly or deliberately preserve transitive
fixture dependencies in the artifact POM; never let them enter deployment runtime dependencies.
The shared Testcontainers implementation itself remains a real library requiring its own tests.
Move fixture resources into `src/test/resources` at the same time: the attached test JAR must carry
realm JSON and other classpath inputs alongside its classes. A move between Maven modules also
requires moving resources to their actual consumer; an ordinary dependency does not expose another
module's test resources. After relocating classes, clean only those fixture modules to remove stale
`target/classes` entries, then compile their dependency closure and all framework consumers. Do not
repeatedly clean upstream modules and discard their coverage evidence. Audit all sibling contract
modules for the same layout instead of waiting for sequential build failures.

The event-ingress contracts now use this packaging and the shared PostgreSQL container; all
seven HTTP cases pass on each of the three frameworks after a clean build. Their scope is
validation and unadmitted-execution rejection, not successful workflow dispatch.

Additional confirmed repair candidate: `WorkflowExecutionManagementFixture.seedPublishedRevision`
uses SQL to insert actors, workflows, definitions and publication rows. The owning definition API
exists. Replace that business setup with authenticated definition creation/publication through the
current packaged service, keeping schema/tenant provisioning through migrators. Its execution API
suite is useful diagnostic coverage but must not be called end-to-end public workflow-creation
acceptance. The same fixture uses an HTTP engine double: that isolates execution-management
behavior and does not establish real FOWF engine/operation-adapter delivery.

Latest execution contract verification: 107 execution API cases plus seven event-ingress cases
pass on each framework (342 total), `/tmp/fowf-execution-api-frameworks-02-20261007.log`.
The SQL definition seed and HTTP engine double limitations above still apply. Durable admission
is committed before dispatch: do not resurrect old assertions expecting its deletion after an
engine/network error. Verify retained identity, stable retry, replayed permanent rejection and
unchanged acknowledged state; distinguish retryable unknown outcomes from definite rejection.

Organization-based authorization fixtures must use organization-role policies. Direct realm/user
role grants can make tests pass while bypassing the membership boundary. This was reproduced in
FOWF's former `KeycloakOrganizationFixture`: an actor's human-task grant in one organization
incorrectly permitted the action in its other, ungranted organization. The fixture now delegates
to shared AuthzenKeycloakFixture. Five real-PDP regressions pass, including the previously red
isolation case; six framework consumer leaves compile. This direct PDP evidence complements,
and does not replace, inbound JWT validation and authenticated framework HTTP tests.

## October 8 recovery verification supplement

The original `/tmp` logs were lost when the laptop rebooted. Surviving Surefire reports confirm
94 Kafka engine cases and ten Quarkus/Spring screening cases passed before reboot. FEI's
`docs/regression-evidence-recovered-2026-10-08.json` preserves case names, counts and report hashes;
its additional post-reboot section records 276 selected cases (268 FOWF and eight FEI).
Persistent new logs are under `/home/pn/.local/state/forwardmeasure/validation/20261008-recovery/`.

FOWF human-task HTTP fixtures now use shared PostgreSQL/Kafka containers on all three frameworks,
with six cases passing per framework. This infrastructure repair does not remove the separately
identified repository-based setup boundary. Micronaut definition-management required an explicit
test-scoped Commons Logging dependency for Rest Assured's HttpClient transport; after repair,
116 API, nine authorization and eleven publisher cases passed. The dependency uses the platform
version and is not added to production packaging. Micronaut execution HTTP contracts passed 114.

A Spring test JVM separately aborted inside the native C2 compiler. The successful Spring and
Micronaut human-task retry applied `-XX:TieredStopAtLevel=1` through `JAVA_TOOL_OPTIONS` as well as
`MAVEN_OPTS`; Maven-only flags do not reach Surefire JVMs. Report this execution condition and
preserve crash evidence. Do not weaken assertions or count the aborted run's zero cases as a pass.
Full FOWF production/test compilation then passed; test execution and coverage checks were not
part of that compile-only invocation.

### Packaged ingestion tests must prove dispatch, not just admission

The FEI Kubernetes component fixture exposed a concrete counterexample: both starts returned a
real execution ID and `RUNNING`, but the fixture deployed no operation adapter. No worker Job
could be dispatched. Preserve separate assertions for admission identity, terminal execution state,
actual Job outcome and persisted/indexed business output. `RUNNING` is a START acknowledgement,
not evidence of ingestion. Match the current API contract instead of retaining a stale `NEW`
assertion or accepting any returned state.

An engine plus its Pekko operation adapter must join the same cluster. Static discovery targets
and each node's advertised identity must agree; a fixture using service-name targets and pod-IP
identities stalled before admission with `Self contact point ... not found in targets`. A listening
HTTP port alone does not establish cluster membership. Use the shared Kubernetes container,
checked image imports and immutable image references accepted by the real tenant policy. Provision
scoped RBAC and real OPERATION_EXECUTE permission; do not bypass either to make Jobs launch.

Keep token endpoints reachable for the full test lifecycle, including token renewal while polling.
Capture scoped cluster/service/worker diagnostics before teardown. Do not dump credential-bearing
pod environment descriptions. Use persistent logs outside `/tmp` when the host is unstable, and
label deliberately interrupted diagnosis separately from completed test runs.

The same inspection found missing explicit authentication on FEI's final OpenAPI callbacks.
FOWF compiles a null authentication plan when `with.authentication` is absent; a security scheme
in the referenced OpenAPI document alone does not supply a token. Compiler/admission acceptance
therefore cannot establish a protected callback succeeded. Test the callback against the real
protected FEI API and verify its state transition, including denied/wrong-tenant cases.

A second production counterexample is durable event identity: FEI's publisher uses the catalogue
name `fei-correlated-sources-ingestion`, while the DSL document is named
`correlated-sources-ingestion`. Revision-UUID admission succeeds, but the old pinned-publication
lookup compares DSL coordinates with the catalogue name and rejects actual engine STARTED facts
with HTTP 404. Execution HTTP tests posting empty event data missed this path. Shared framework
contracts must post the real durable-start definition metadata (coordinates and all digests), then
verify completion and replay through HTTP with intentionally different catalogue/document names.
A renamed catalogue entry must not invalidate its immutable published document identity.

### October 8 callback body regression

The real FEI correlated Job test exposed another engine discrepancy: Pekko's
`HttpOperationMaterializer` sent `parameters.body` as `{"body": {...}}`, while Kafka's
`OpenApiCallAdapter` unwraps `body`/`requestBody`. Both worker Jobs succeeded, but FEI's final
HTTP callback deserialized no root tenant ID and returned 500. Do not infer protocol parity
from compilation or a passing Kafka adapter test. Test the actual wire payload through each
engine's production request construction, including declared path/header parameters and
explicit body aliases. A transparent fixture observer may record synthetic request bodies;
it must forward to the real authenticated business endpoint and never log bearer credentials.
The focused regression is `HttpOperationMaterializerTest.explicitOpenApiRequestBodiesAreNotWrappedInTheParameterName`;
the end-to-end component assertion remains actual terminal completion and OpenSearch persistence.

### October 9: inject storage faults before the payload is consumed

A downstream wait is not a portable overflow recovery barrier. Kafka retained an artifact reference
through the wait, but Pekko resolved and journaled the payload before entering it. Corrupting the
old object at that point did not exercise a storage read: completion was legitimate, and demanding
failure was a fixture error. Observe the actual boundary being tested.

The public overflow fixture now forwards real GCS protocol requests and holds media reads until
the engine process is stopped. It verifies the durable ARTIFACT state through public history,
then mutates/deletes the actual emulator object and restarts the engine. The proxy supplies no
business result, synthetic checksum verdict or replacement worker. Untampered reads must complete;
corruption/missing data must fail before the downstream effect. Count a deployment pass only after
all six overflow scenarios complete, rather than counting its successful first scenarios as a pass.

This exposed a real recovery defect: a persisted DataResolutionStarted outlived its process's
read future, leaving the recovered workflow WAITING forever. FOWF `b755aac2` resumes that read
through the normal storage transport using its persisted resolution identity. The focused test
failed before the fix; all eight PostgreSQL/Cassandra journal/storage cases passed afterward.
The focused test deliberately delays a read, while the packaged acceptance gates actual storage
HTTP. These are complementary proofs with explicitly different boundaries.

The packaged test then exposed incomplete public failure reporting. A STATE_OBSERVED terminal
snapshot can precede the FAILED journal fact; the projection published FAILED without its error.
FOWF `e90a5536` saves the snapshot's structured failure details. A new shared REST contract passed
on Quarkus, Spring and Micronaut, checking the first terminal response, later fact delivery and
idempotent observation replay. Do not poll only for a terminal enum and silently ignore a missing
result/error. Assert the externally usable outcome as part of the same acceptance requirement.

### October 9: verify aggregate control size and actual command serialization

The FEI Kafka acceptance run exposed two failures that isolated mapping tests could not catch:

- Several individually small payload fields produced an operation control envelope above its
  32,768-byte limit. Test the combined serialized envelope with the production storage transport,
  including a configured offload threshold larger than the aggregate. Preserve routing/security
  metadata inline, enforce storage limits, and verify the adapter resolves the stored operation.
  Test both combined-small fields and an individually large field. Do not raise limits just to pass.
- A computed transition's fractional protocol deadline changed numeric representation after the
  production Kafka JSON serializer round trip, invalidating its digest. Constructing and checking
  an in-memory transition missed this. Round-trip the actual command through `JsonSerde`, verify
  acceptance, and separately verify that mutation is still rejected. Preserve wire compatibility.

The focused regressions passed (15 control-envelope/storage/adapter cases and 21 wire/runtime
cases); the subsequent FEI API run passed WorldCheck ingestion/screening and configured pipelines.
Those passing families do not make the entire runtime cell pass. Keep the complete-cell assertion
and preserve failure reports for later families.

Tenant setup must provision every participating identity, including the worker's service account,
for each tenant. The second-tenant fixture initially provisioned its caller and outbound identity
but omitted the worker, which was correctly denied. Add the real organization membership and
scoped grant; never replace authorization with a permit-all service or reuse the first tenant's
asserted identity to bypass the denial.


### October 9: preserve evidence across correlation and exercise typed worker boundaries

Successful Spark and delivery Jobs did not prove correct correlation: FEI's default scalar TRUST
rule discarded the lower-trust source's whole assertion list. The public entity API assertion
caught the missing enrichment. The focused Spark/Kafka regression must read the raw handoff JSON,
not a readback mapping that drops assertion fields itself. Verify both input orders, retention of
distinct lower-trust assertions, removal of exact duplicates and unchanged scalar trust precedence.

State Street's public API scenario then found a production LocalDate conversion failure that older
direct tests concealed by installing JavaTimeModule in their own mapper. Never assume framework
mapper defaults when a deployment supplies a custom producer: inspect the actual producer and
worker bootstrap. The shared resolution sink now owns a configured copy; the wire regression uses
both ISO strings and LocalDate values with an otherwise bare mapper and the real generated HTTP
client. It asserts the emitted date string and that the caller's mapper is unchanged. This HTTP
protocol fixture does not claim authorization or persistence; the complete State Street API test
must also verify stored typed dates, identifiers, addresses and all provider fields across frameworks.


### October 9: populate response collections and verify engine producers separately

An execution-detail API can pass empty-array tests while every populated response is broken.
Timer/effect tests must persist real activity facts and read the resulting typed collections over
HTTP on all three frameworks. Check native activity identity, duplicate/replayed facts, fork
coordinates, completion/cancellation and exact timestamps. Include fractional RFC 3339 engine
timestamps: SimpleDateFormat can silently parse nine fraction digits as milliseconds instead of
fractional seconds. An HTTP 200 or a nonempty array alone will not catch that corruption.

Posting authenticated canonical engine events tests the API projection contract. It does not
establish that real engines emit those events. The packaged workflow tests must also exercise an
actual wait and external operation, then poll public execution details for the timer/effect outcome.
Reuse the existing runtime combinations; keep earlier image evidence separate from new image tags.
Do not backfill a passing matrix result from component tests or source compilation.


### October 9: contract HTTP fixtures must serve the declared media type

Do not reuse a YAML/AsyncAPI fixture server for proto source without changing Content-Type.
The FDE production-workflow test was rejected at publication because its helper served
`evaluation.proto` as `application/yaml`; that incorrectly admitted proto source to the JSON Schema
registry. Serve proto as `text/x-protobuf`, keep YAML contracts as YAML, and exercise the real
publication API. The production resource classification was already correct; do not weaken it
because a fixture mislabeled the bytes.

Docker services do not inherit the test JVM's JAVA_TOOL_OPTIONS. The acceptance fixture supports
an explicit `forwardmeasure.acceptance.java-tool-options` setting while retaining framework-specific
issuer options. The current laptop validation uses `-Xmx1g -XX:TieredStopAtLevel=1` after a captured
Temurin C2 SIGSEGV. Record that option with runtime evidence; it is not production-default JIT or
performance evidence. Keep the failed startup report rather than overwriting it on retry.


### October 9: protocol completion markers are not response payloads

The first actual FOWF-to-FDE invocation completed successfully but returned null: the Kafka
adapter forwarded gRPC's response message only as progress and then treated its null stream-end
marker as the result. Assert the business result as well as workflow state and activity state.
A focused transport-boundary test must reproduce response-before-terminal ordering, use both an
inline and an external data reference, and verify that completion retains the reference without
fetching a large payload again. Also test that a terminal failure overrides an earlier response.
This regression failed before the production repair and passed afterward. Keep unary behavior
separate from streaming aggregation and do not turn arbitrary protocol progress into a result.

Fixture egress allowlists must use the production parser's host separator (`|`) and scoped tenant
identity. A test that gets past a denial by making authorization or network policy permissive
invalidates its acceptance evidence. Correct the exact configured host list and retain denial tests.

### Tenant-member callers and deployment namespaces

An OAuth client used to evaluate AuthZEN policy is not automatically an application caller with
organization membership. FDS workflow acceptance exposed this distinction through the actual
HTTP admission path: its PDP credentials could obtain a token, but tenant-aware FOWF calls failed
because that token had no organizations. Give the launcher its own service-account client,
organization membership and scoped role. Keep the PDP identity separate; do not bypass tenant
selection or change production authorization to make a fixture pass.

When applying rendered Helmfile output in a disposable cluster, use the intended namespace on
the apply command. A Helm release namespace does not guarantee every rendered resource contains
`metadata.namespace`. Verify the deployed service and its real backing pods in that namespace.
For Docker restarts, refresh container inspection before recreating host clients: published host
ports may change, and cached Testcontainers port mappings can target a dead socket.
