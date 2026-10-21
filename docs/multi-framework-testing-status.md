# Multi-framework testing status

**As of 2026-10-13.** For engineers and coding agents.

This document replaces the testing guide, runtime-matrix status, testing handover, parity-audit request and JSON evidence files that were in this folder. The originals move to `docs/archive/` (see [Sources](#19-sources)). Product purpose and architecture are in [technical-vision.md](technical-vision.md). Implementation state outside testing is in [implementation-status.md](implementation-status.md).

OpenWorkflow (FOWF) test evidence — per-cell matrices, suite results, measured coverage and the remaining verification rows T-01 to T-17 — is maintained only in [FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md) (ownership split agreed 2026-10-13). Section 7 here is a dated summary that links there; the FOWF evidence JSON files stay in this repository's `docs/archive/`.

## Summary

- **No current all-green matrix exists.** The last complete packaged-image matrices (FOWF basic 9/9, FOWF overflow 9/9, FEI 9/9, FDS 39/39, FDE 15/15) passed on 2026-10-09 on locally built images. Engine code changed substantially on 2026-10-11 to 2026-10-13, and none of those matrices has been re-run since. Treat them as history, not as current passes.
- **Newest results (2026-10-12/13):**
  - The overnight discovery build failed the FDS `FowfStartInputRuntimeMatrixAcceptanceTest` 9/9. Both engine causes have since been fixed; the test has not been re-run.
  - FDS launcher coverage rules failed.
  - `OksCloudEventIngressResourceTest` failed for a reason not yet explained.
  - The new cross-engine `LifecycleCloudEventParityTest` passed 8 of 15 scenarios. The engine bugs it exposed have been fixed; it has not been re-run.
  - The Pekko runtime suite (296 tests), the Kafka Streams runtime API and core suites, `LargeWorkflowDataTest` and the chart topic tests passed.
- **Written but never run:**
  - `OksCloudEventPublicationIntegrationTest`;
  - the FEI step D deep integration tests;
  - the updated Pekko lifecycle mapper tests and three Kafka engine binding tests;
  - the Kafka start-input overflow fixtures of 2026-10-10.
  - Because the FEI step D tests had not run, a startup bug reached a production deploy: two `@PrePersist` callbacks in `MonitoringScreeningHit`.
- **Coverage gates do not pass.**
  - The newest FOWF measurement is the 2026-10-11 run 34 final gate: 92.9% lines, 79.7% branches, 22 missed classes (fails branches and missed classes). The earlier 2026-10-09 retained-data check failed with branches 0.79 < 0.85 and 68 missed classes > 0.
  - The FDS launcher rules failed on 2026-10-12/13.
  - No product has a recorded passing gate.
- **No test evidence at all:** Agent OS and platform operations have test classes in their repositories, but no source records a run.
- **Production builds skip tests.** `build.sh` skips tests by default. Tests are run separately.

## Contents

1. [Scope, dating and status vocabulary](#1-scope-dating-and-status-vocabulary)
2. [Testing rules](#2-testing-rules)
3. [Kinds of test](#3-kinds-of-test)
4. [Test infrastructure and fixtures](#4-test-infrastructure-and-fixtures)
5. [Running tests](#5-running-tests)
6. [Coverage](#6-coverage)
7. [Matrix: OpenWorkflow (FOWF)](#7-matrix-openworkflow-fowf)
8. [Matrix: Entity Intelligence (FEI)](#8-matrix-entity-intelligence-fei)
9. [Matrix: Data Streaming (FDS)](#9-matrix-data-streaming-fds)
10. [Matrix: Decision Engine (FDE)](#10-matrix-decision-engine-fde)
11. [Agent OS](#11-agent-os)
12. [Platform operations](#12-platform-operations)
13. [Shared libraries and platform modules](#13-shared-libraries-and-platform-modules)
14. [Deployment, identity and build-tooling tests](#14-deployment-identity-and-build-tooling-tests)
15. [Required acceptance scenarios not yet evidenced](#15-required-acceptance-scenarios-not-yet-evidenced)
16. [Known failures, gaps and untested combinations](#16-known-failures-gaps-and-untested-combinations)
17. [Open testing work](#17-open-testing-work)
18. [Superseded and conflicting statements](#18-superseded-and-conflicting-statements)
19. [Sources](#19-sources)

---

## 1. Scope, dating and status vocabulary

### 1.1 Dating rule

- Every status has a date.
- Nothing in this document was re-executed to write it. Every result is as recorded by its source.
- An entry marked **(per <source>, <date>; not re-verified)** has not been re-run since that date. It must not be read as a current pass.
- Source keys (TG, RMS, HO, AUD and the `J-*` evidence files) are defined in [Sources](#19-sources).
- "Session record, 2026-10-13" means the testing changes recorded on 2026-10-13. These are newer than every archived source.

### 1.2 Status words

| Status | Meaning |
|---|---|
| PASSED | Executed, and every selected test passed |
| FAILED | Executed, with at least one failure or error |
| PARTIAL | Executed; some scenarios passed and some failed |
| SKIPPED | Selected, but the test framework skipped it. A skip is a gap, not a pass |
| NOT RUN | Written, and possibly compiled, but never executed |
| UNKNOWN | The run was stopped or interrupted before a result |
| NO RECORD | No source records any run |

### 1.3 Evidence labels and units

- **Evidence labels** (AUD, 2026-10-10): not inspected; statically traced; fixture missing; fixture written; compiled; executed pass; executed fail; skipped/blocked; packaged-image verified; deployed verified.
- **Units are not additive** (HO, 2026-10-09):
  - cells (deployment configurations);
  - scenario families or scenarios (the overflow matrix has 6 per cell);
  - test methods/cases;
  - classes.
  The older phrases "27 runs" and "27-case inventory" are superseded (RMS).
- **Cell notation:** Q = Quarkus, S = Spring Boot, M = Micronaut.
  - K = Kafka Streams engine. Its relational common plane is PostgreSQL. This "is not a fourth engine or Kafka's native workflow journal" (AUD).
  - PG = Pekko engine with a PostgreSQL journal.
  - PC = Pekko engine with a Cassandra journal. Pekko/Cassandra still uses the relational tenant registry.
- **The nine FOWF cells** (Q-K, Q-PG, Q-PC, S-K, S-PG, S-PC, M-K, M-PG, M-PC) are "the supported coherent platform installations" (AUD).
  - "a Spring FOWF run against Quarkus-only product services does not establish an all-Spring installation."
  - No pairwise sampling is allowed.
- **Image identity:** a matrix certifies its recorded image identities, "not every subsequent source change or whatever is currently deployed" (HO).
- **Deployment:** the 2026-10-09 matrices ran on local images in disposable Docker/K3s fixtures ("No registry publication or operator-cluster deployment was performed", RMS). Software has since been deployed (the FEI `@PrePersist` defect reached a production deploy, session record, 2026-10-13). However, no source records a test run against a deployed installation. The "deployed verified" label is unattained for every row.

---

## 2. Testing rules

The user's own rules, as recorded in memory, are quoted exactly. Where an older document conflicts with a newer rule, [section 18](#18-superseded-and-conflicting-statements) records the resolution.

### 2.1 Standing rules

**Real infrastructure; no mocks; no H2**
- "Mockito (or any mocking framework) must never be used in new test code, in any forwardmeasure/Kriyagentic repo." User, 2026-09-20: "Mockito is banned in this org."
  - Where a real instance is impractical: either drop the isolated unit test and rely on real end-to-end coverage, or use "a small, explicit, hand-written test double (a minimal inner/anonymous class implementing only the methods actually called, throwing `UnsupportedOperationException` for the rest) — never a general-purpose mocking framework."
- "Never write or propose H2 (in-memory/embedded) as a test or verification database, anywhere, in any repo". Every real test "runs against real Postgres, real Keycloak, real OpenSearch — via `forwardmeasure-testcontainers`" (2026-09-14).
- "'Never H2, always testcontainers' extends to auth providers, not just databases/search/queues." Before treating a test as verification, ask: "could this test fail if my own understanding of the external system's real behavior is wrong?" (2026-09-27/28).
- Manifesto P6: "Prefer real infrastructure — a real broker, a real database, a real actor system — over Mockito or an equivalent mocking framework, without exception." A hand-written double needs a comment stating why, "and why this isn't a mocking framework in disguise."
- AUD (2026-10-10): "No Mockito or H2. A small handwritten double is permitted only for a justified narrow condition and must never be presented as real integration evidence."

**Through the REST API, on every framework, with nothing narrowed**
- "tests must never construct repositories or service implementations directly, as in `new XRepository()`, `new XServiceImpl(...)` or a hand-built `EntityManager` harness. They invoke behaviour through the product's real REST API, in the deployment-leaf tests, on Quarkus, Spring and Micronaut, against real infrastructure." (2026-10-07)
  - Coverage: "meet it through REST-level tests."
  - Unreachable code: "if behaviour can't be reached through any API, surface it as possibly dead code; don't test it directly."
  - How this combines with the later agreement on direct tests: see 2.4.
- "A capability×framework combination is 'live-verified' only when every operation in its OpenAPI spec has had a successful authenticated call, plus its error cases, through a real running server (real Postgres/Keycloak). One answering endpoint, an auth-rejection (401/403) smoke test, or a test that constructs the resource/service class directly does not count, and must never be reported with the words 'verified', 'done' or 'all N combinations'." (2026-10-03)
- "Shrinking a test is a brutal violation of my ask." Asking first is not a way out.
  - "Never write a narrower test than full coverage, never drop operations or error cases, never substitute a 401-only smoke test or direct class construction for real HTTP calls, and never offer a narrower version as an option."
  - The agreed shape is "one shared contract per capability (human-task's abstract-contract + per-framework-subclass shape), every operation + error cases, plus a check that diffs spec operationIds against operations exercised so a missed endpoint fails the build."
- "Every real service/entrypoint in this ecosystem gets genuine Quarkus, Spring, AND Micronaut treatment — no exceptions". This includes bare Kubernetes Job workers and CLI tools.
  - Deployment values too: "Quarkus is never the privileged/production/default flavor."
  - "A framework difference I introduce is a parity bug to fix (or an explicit question to ask), never a side effect. Before any change, tabulate each framework's behavior for the edge cases (unknown path, anonymous, invalid token, health) before and after it."
- Multi-framework bindings as a bug detector: building all three at once is "a deliberate technique this user values for surfacing shared design flaws that single-framework work would hide".
- Manifesto P10: "build and verify all of them together, not one now and the others 'later.' … Treat 'it works on Quarkus' as an incomplete answer."
- FOWF parity: "verify EVERY applicable cell of the engine (Pekko/Kafka-Streams) x framework (Quarkus/Spring/Micronaut) matrix myself, in the same piece of work - not deferred, not assumed symmetric because one path worked."
  - "A cell not re-verified this session stays marked UNVERIFIED even if an older doc/plan file/memory claims it's fine." (2026-09-16)

**Proof**
- Compile success and "the framework's build-time validation passed" are "necessary, not sufficient" for interceptor registration, DI wiring and multi-tenant routing. Run real images against real testcontainers (2026-09-17).
- Manifesto P12: "Compiling, or passing an isolated unit test, is not proof of functional correctness."
- Never claim "ready" or "done" without live verification.

**Fixtures and seeding**
- When a deployment-leaf test needs data for an entity that the deployable cannot create through its own API, "seed it via a Liquibase changelog passed to the same `TenantSchemaMigrations.migrate(...)` call already in use — do not reach for `EntityManager`/`SessionFactory`/CDI transaction plumbing at all." Use fixed UUID literals (2026-09-07).
- Coverage: "Never lower the thresholds - that is shrinking tests." Each test "drives real behavior (real containers, no Mockito/H2) and any bug it finds is fixed, not asserted around" (2026-10-03).

### 2.2 Acceptance-evidence rules (TG, 2026-10-07 to 2026-10-09; AUD, 2026-10-10)

**Settled policy (TG §1):**
> "The user explicitly agreed to **both direct tests and REST tests**, after discussing the limits of REST-only coverage. Preserve useful focused coverage and add missing acceptance coverage. Quarkus, Spring and Micronaut are all required. The earlier FEI REST-only handover is superseded on that point."

**Layer table (TG; AUD repeats it with the same six kinds)**

| Layer | Correct entry point and infrastructure | What it proves | What it cannot prove |
|---|---|---|---|
| Pure algorithm/value object | Construct the component and use fixed inputs | Mapping, normalization, matching and invariants | DI, HTTP, authorization or persistence |
| Business service integration | Production service from its framework container; real migrated PostgreSQL and other services | Service rules, managed transactions, persistence, tenant routing, concurrency | HTTP binding or JWT admission |
| Repository/connector/storage contract | Real persistence adapter, real DB/schema, shared fixtures | Queries, locking, rollback, paging, backend parity, recovery | Application wiring or admission |
| HTTP contract/acceptance | Real framework app; real HTTP; real identity and AuthZEN | Routing, DTO binding, validation, authn, authz, DI, observable behaviour | Worker/image dispatch unless that path runs |
| Workflow/worker acceptance | Admit through owning API; dispatch current packaged workers; observe terminal state and effects | Cross-process ingestion and recovery | Cells not executed |
| Studio acceptance | Real browser and API stack | The usable flow and displayed state | Other frameworks/backends without runs |

**Quoted rules**
- "Do not describe a direct call to a `Resource`, launcher or service as REST acceptance. Do not use an in-memory repository or permit-all authorization in an acceptance fixture."
- "Production-managed service tests must resolve the actual production factories/producers and transaction interceptors. Moving `new Repository()` into a shared test helper does not meet that requirement. Never add production methods or public endpoints solely to make coverage convenient."
- "Before writing a test, record its concrete failure … Choose assertions that would fail with that defect present. … do not derive expected output with the mapper/normalizer under test."
- "Avoid assertions such as 'not 500', 'non-null', '202 returned', or 'a method was called' as the sole acceptance result. Retrying must have a bounded deadline and report the last observed state. Do not use broad catches, skipped failing tests, lower thresholds or extra exclusions to pass a gate."
- "More tests should mean more failure modes covered, not cloned assertions in many classes. … framework adapters must still boot separately. … Code with no REST caller may serve a worker, scheduler or recovery loop; trace those callers before labelling it unreachable."
- "Calling a binding's factory method directly can prove its method body calls `start()`, but cannot prove that the framework ever invokes it during startup. … require the real application to become ready and execute a public request before declaring the framework operational. Do not create production-owned topics in a fixture to hide this kind of startup defect."

**Matrix rules**
- "One Quarkus result does not establish Spring or Micronaut parity."
- "This is a nine-cell acceptance matrix, not three names attached to one runtime."
- "changing a parameter label while invoking one shared fake runtime does not create a matrix."
- "Do not infer protocol parity from compilation or a passing Kafka adapter test."
- "Posting authenticated canonical engine events tests the API projection contract. It does not establish that real engines emit those events. … Do not backfill a passing matrix result from component tests or source compilation."
- "`RUNNING` is a START acknowledgement, not evidence of ingestion."
- "HTTP 202, producer acknowledgement and an accepted Job are not terminal-success evidence." (AUD)

**Security and configuration**
- "Never assume framework mapper defaults when a deployment supplies a custom producer."
- "A test that gets past a denial by making authorization or network policy permissive invalidates its acceptance evidence."
- "Never use permit-all authorization as acceptance evidence." (AUD)
- Realm or user role grants "can make tests pass while bypassing the membership boundary". The platform guide "prohibits global role grants from substituting for organization policy tests".
- "Positive JWT fixtures must obtain a genuinely signed token … Negative cases should change one boundary at a time … A forged-claims case must retain the original signature".
- No `issuer=any` and no test-only validator. The expected issuer is pinned separately from container-reachable JWKS/PDP URLs.
- "HTTP protocol fixtures are not substituted for real PDP integration evidence." Fault injection through a proxy to real Keycloak is "not a stubbed successful authorization service".

**Recovery and wire fixtures**
- Recovery fixtures seed the journal: "Do not re-enable legacy snapshot recovery to rescue a fixture."
- "Append golden payloads for new wire types without replacing previously established bytes."
- For oversized responses, keep the public unknown-outcome classification and assert the precise limit in the structured cause chain. Compiler diagnostics "exclude random temporary paths".
- A Hibernate default schema does not set the PostgreSQL `search_path` for native statements. Native allocation/locking fixtures use the tenant-routed connection schema.
- Persistence-context regressions must keep the triggering state: do not clear the context before merge.

**Harness and coverage meaning**
- The e2e harness mirrors production. A container superuser "hides grant bugs", and a shared runtime role "would hide exactly the grant bugs this harness should catch".
- "Coverage reports identify missing scenarios; they are not the behavioral specification." "Coverage measures execution, not semantic parity. 100% coverage would not prove 100% correctness." "Do not conclude '100% parity' because the current tests pass." (AUD)
- "No known difference in these tested cases" is not an unqualified 100% claim. Any full-parity statement must name its contract revision and evidence baseline (AUD).

**Per-fixture accounting (AUD)**
- Every fixture states the defect it detects.
- There must be a mechanically checkable mapping from each operationId, gRPC method and error family to assertions, per framework.
- Behaviours outside the API are inventoried too.

**Other quoted rules**
- "A deterministic model-response fixture can prove validation/mapping, but cannot be presented as successful execution of the real agent/model" (information extraction handover, 2026-10-12).
- "A TypeScript compile or screenshot is not browser/API acceptance." (AUD)
- "rendered YAML is not runtime proof." "Offline Helm rendering and syntax checks are separate from behavioural proof."
- "Use synthetic or approved bounded fixtures with independent expected results" (population plan, 2026-10-12).
- "regression records must preserve missing fields as well as populated values".

### 2.3 Reporting rules

**Handover separation (TG)**

| Evidence | Required report |
|---|---|
| Written | File, scenario, entry point, real dependencies, remaining fixture prerequisites |
| Compiled | Exact command, module/framework scope, log, exit status, whether test sources compiled |
| Executed | Selected tests, count, pass/fail/skip, framework/backend, actual image identity and log |
| Accepted | Required matrix cells passed, persisted effects verified, known gaps disclosed |

- "A test written behind an unset environment property is not executed. A skipped matrix cell is a gap. A reactor's formatting phase reporting SUCCESS is not a passing regression suite."
- **What to record for every execution** (AUD; RMS):
  - commit and dirty state, and the contract hash;
  - fixture class/method and command;
  - JVM, framework and backend;
  - image tag **and digest**;
  - log hash, counts and persisted-effect assertions.
  Do not double-count reruns, and preserve failed attempts.
- "Never equate compiled with passed." "Compilation is not test execution." "Do not say '100% parity' or 'done' on compilation alone."
- "Cache hits never constitute fresh execution evidence." "Do not count historic cached reports as tests run in the current invocation."
- C1-only JIT runs (section 5.3) are "not production-default JIT/performance evidence". "Distinguish C1 diagnostic results from ordinary-JVM validation."
- Fix procedure: "Each fix should have a reproduction, regression code, compilation evidence, exact image rebuild scope and post-deploy verification."

### 2.4 How the REST-only rule and the direct-test agreement fit together

The memory rule of 2026-10-07 says to test only through the REST API. The testing guide of 2026-10-07 and the audit's product-owner decision 2 of 2026-10-10 require "both direct focused tests **and** real API tests". Under that decision, "Legitimate algorithm/storage tests must not be deleted under the superseded REST-only interpretation."

The newest statement, AUD 2026-10-10, governs. Read together:

1. Behaviour exposed by an API is proven through the real API on all three frameworks.
2. Direct tests are kept only for:
   - pure algorithms and value objects;
   - real storage, connector and engine contracts against real infrastructure.
   They never stand in for REST evidence.
3. No test constructs repositories or services by hand to raise coverage. Moving `new Repository()` into a helper does not satisfy the rule.

---

## 3. Kinds of test

| Kind | Mechanism | Where it is used (with latest recorded evidence) |
|---|---|---|
| Unit / pure | Direct construction, fixed inputs | FOWF wire goldens, `WorkflowEntityTest`, `ApiDateFormatTest`, `HttpOperationMaterializerTest`; FEI `HashingTextEmbeddingServiceTest`, architecture tests; FDE `ProtoStructMapperTest`; platform handlers |
| Managed-service integration | Production beans from each framework, migrated PostgreSQL | FEI 25 cases per framework (2026-10-07/08); `EntityReadIntegrationTest` 27/27/28 (2026-10-09) |
| Repository / storage / engine integration (testcontainers) | Real PostgreSQL, Cassandra, Kafka/Redpanda, MinIO, OpenSearch, Valkey, Keycloak | FOWF Pekko persistence contracts, Kafka engine restoration, `LargeWorkflowDataTest` (real MinIO, 2026-10-13), FDS `JpaPagingSourceTest`, FDE Valkey |
| Framework HTTP/gRPC contract | Real Quarkus/Spring/Micronaut app, real Keycloak/AuthZEN, shared containers | FOWF execution, definition, human-task and event-ingress APIs; FEI HTTP; FDS issuer/validation; FDE packaged gRPC conformance |
| Cross-engine parity (new 2026-10-13) | The same workflows run through the Pekko entity and the Kafka Streams engine; outputs compared | `LifecycleCloudEventParityTest` (15 scenarios), `CorrelatedWorkerStateParityTest` in `openworkflow-engine-cross-engine-tests` |
| Packaged-image acceptance in K3s | Shared `KubernetesTestContainer` (K3s), product images imported by digest; Helmfile for FDE deployment cells | FOWF basic/overflow, FEI complete, FDS 39 cells, FDE 15 cells (all 2026-10-09) |
| K3s image tests with JaCoCo | `KubernetesTestContainer#withJacocoCoverage` injects the agent through a MutatingAdmissionPolicy (built 2026-10-10) | FEI, FDS and FOWF image tests; see section 6.2 |
| Live runtime matrix runs | A matrix runner provisions one isolated cell at a time against packaged images | FEI `scripts/run-public-ingestion-matrix.sh`; FOWF/FDS/FDE matrix classes selected by `fowf.acceptance.runtime` / `decision.engine.acceptance.deployment` |
| Operator-cluster (deployed) verification | Tests against an installed environment | **No run recorded** |
| Studio browser | Real browser against the API | Playwright specs exist for FEI Studio. Recorded runs are old (FOWF testing §3.6 and section 8.6 here). The real-API browser acceptance gate has not been executed |
| Deployment/script tests (Python) | `deploy/tests`, `deploy/helmfile/scripts/tests` | Chart topic tests passed 2026-10-13; 18-combination selection rendering not executed (2026-10-06) |

---

## 4. Test infrastructure and fixtures

**`forwardmeasure-testcontainers` entry points**
- Containers: `PostgreSqlTestContainer`, `KafkaTestContainer`, `OpenSearchTestContainer`, `KeycloakTestContainer`, `KubernetesTestContainer` (K3s v1.36.4), `MinioTestContainer`.
- JUnit annotations: `WithPostgreSqlContainer`, `WithKafkaContainer`, `WithOpenSearchContainer`, `WithKubernetesContainer`.
- Also the GCS emulator `forwardmeasure-testcontainers-gcs` ("does not establish Google IAM behavior"), shared Valkey and Cassandra fixtures, and Jupiter and Quarkus adapters.

**Shared fixtures**
- FEI database fixtures: `EntityIntelligenceTenantProvisioner` / `WithMigratedEntityIntelligenceTenant`.
- Acceptance helpers: `PublicWorkflowAcceptanceClient`, `RealFowfWorkflowFixture`, `AuthzenKeycloakFixture`.
- `KeycloakTestContainer` has an alternate-loopback-issuer helper; pinned/network fixtures reject it.

**Recent fixture changes**
- 2026-10-13: the Cassandra, GCS, Keycloak and Kubernetes fixtures gained their own tests. Commits `e0df4be`, `6f657bf`, `1d0e48e` and `be2c6b5`, "each verified with its module's own build" (overnight findings).
- Earlier fixture test counts (VC, 2026-10-07):

  | Fixture | Tests |
  |---|---|
  | MinIO | 5 |
  | Keycloak | 3 |
  | PostgreSQL | 3 |
  | OpenSearch | 4, later a 5-test suite including real-node JVM-argument verification (RMS, 2026-10-09) |
  | Kafka | 5 |
  | K3s | 8 |
  | Jupiter adapters | 9 |
  | Quarkus adapters | 3 |
  | Valkey | 3 |
  | Keycloak audience helper | 6 |

- "The shared Testcontainers implementation itself remains a real library requiring its own tests" (TG).

**Fixture rules (TG §3)**
- Use isolated tenant databases for destructive tests.
- Create business data through the API under test.
- Use test-only Liquibase migration fixtures only for prerequisites that have no creation path.
- Use SQL only for catalogs, privileges, upgrade snapshots and limited observation. "Read-only SQL observations are not substitutes for API mutations" (HO).
- Read JDBC booleans with `getBoolean()` and check for NULL.
- No `hbm2ddl=create-drop`; run real ordered migration Jobs.
- Use distinct transactions for concurrency, and include a case with no outer transaction to prove the interceptor.
- Raw-container use is a consolidation item for `forwardmeasure-testcontainers`, "not a reason to remove their backend assertions". If a wrapper is missing, add it to that library.
- Open since 2026-09-06: OpenSearch tests in entity-matching and FEI hand-roll `OpenSearchContainer` plus `ImageFromDockerfile` (for `analysis-phonetic`) instead of the shared module:
  - `OpenSearchRecallIntegrationTest`;
  - `ScreeningMatchServiceIntegrationTest`;
  - `ScreeningResourceTest` × 3.

**Reusable test contracts (TG §10)**
- Keep contracts in `src/test/java` and attach a test JAR at `test-compile`.
- Consume them from each framework leaf with `<type>test-jar</type><scope>test</scope>`.
- Fixture resources go in `src/test/resources`. Fixture dependencies never enter deployment runtime dependencies.
- After relocating classes, clean only those fixture modules. "Do not repeatedly clean upstream modules and discard their coverage evidence."

---

## 5. Running tests

### 5.1 Builds: bounded wrapper, one build at a time

**The wrapper**
- All Maven runs go through `forwardmeasure-openworkflow/scripts/build-bounded.sh` (FEI has a copy).
- Always pass an absolute `-f`, "because the wrapper changes directory".
- It takes `$XDG_RUNTIME_DIR/openworkflow-maven.lock` non-blocking. If another bounded build holds the lock, it prints "Another bounded ForwardMeasure Maven build is running." and exits 1. The step then silently does nothing.
- To chain builds, wait for an explicit marker line written by the previous chain's last step (for example `until grep -q "<step> exit=" results.txt; do sleep 20; done`). Two queued scripts waiting on the same marker race, and the loser fails instantly.
- Editing a running script with `sed -i` does not affect the running process.
- Memory limits: defaults `MemoryHigh=32G`, `MemoryMax=40G`, `MemorySwapMax=4G`, overridable with `FWD_BUILD_MEMORY_HIGH`, `FWD_BUILD_MEMORY_MAX` and `FWD_BUILD_SWAP_MAX`. They were raised from 6G on 2026-10-11, because the full-reactor JVM needs about 6.4 GB.
  - If a step is inexplicably slow, read the scope's cgroup first (`memory.current` vs `memory.high`, and the `high` counter in `memory.events`), then reproduce the step outside the scope.

**Discipline**
- One build at a time. "No parallel Maven execution… Keep memory limits and the cross-repository build lock." Do not use `-T`.
- Do not wipe `.m2` or the build cache.
- Never edit the source of a repository while a Maven build of it is running. On 2026-10-11 a mid-build edit aborted run 74 and lost its images and K3s step.
- Scope verification runs to the touched modules:
  - Use `-am` only when upstream modules changed. Otherwise downstream tests run against stale `~/.m2` jars.
  - After a cross-module rename, use `clean` on downstream modules: a plain `install`/`test-compile` can skip recompiling them.
  - Compile-time constants inlined into downstream classes also need `mvn clean` downstream.
- Tee every handed-off command. A sequence of builds writes one file per repository (`/tmp/mvn-<repo>.out`), never one shared file.

### 5.2 Commands

**Production builds skip tests.** `build.sh` skips tests by default (session record, 2026-10-13).
- `build.sh --run-tests` enables the shared `coverage` profile.
- Direct Maven regression builds must add `-Pcoverage`.
- For fresh evidence, disable the build cache with `-Dmaven.build.cache.enabled=false` or `build.sh --no-build-cache`.

**Compile production and tests without running them** (TG; AUD; VC):
```
scripts/build-bounded.sh -B -ntp -Dmaven.build.cache.enabled=false -DskipTests -DskipITs -Dmaven.test.skip=false test-compile
```
FOWF changes that span engines or frameworks need the full production/test reactor, including both Pekko backends and all frameworks.

**Focused test run** (example from HO, the FEI Quarkus CRUD class):
```
-f /home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-intelligence/pom.xml -pl forwardmeasure-entity-intelligence-deployments/ingestion-service/quarkus -am -Dtest=ReferencePopulationCrudResourceTest -Dsurefire.failIfNoSpecifiedTests=false -Pcoverage -DskipTests=false -DskipITs=true -Dmaven.test.skip=false -Dcontainer-image.build=false -Dcontainer-image.push=false -Ddocker.skip.push=true test
```
- Never use `skipTests=true` and then call the skipped check a gate pass. Explicit test-skip flags allow compilation without claiming coverage.

**Cold repository** (VC):
1. Run nonrecursive `-N -f <pom> clean install` in this order: platform parent, core BOM, Quarkus BOM, Spring BOM, Micronaut BOM.
2. Then run `build.sh --resume-from :forwardmeasure-jpa-spring --skip-push --full-suite` over the complete `reactor.xml` (`--full-suite` runs unit and integration tests in every repo).
- `platform.policy.bootstrap` is for bootstrap installs only. Bootstrap goals "do not substitute for license or packaging gates".

### 5.3 Required JVM flags

On this Fedora host, container JVMs intermittently crash with SIGSEGV in the C2 JIT. This was seen in Keycloak 26.7.4, in OpenSearch (`PhaseIdealLoop`) and in Temurin test JVMs (25.0.4.1+1). A crashed Keycloak makes Quarkus mark the shared boot as failed, so every later `@QuarkusTest` class in that module is **skipped**, not failed. Every test run carries:

- `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1` **and** `MAVEN_OPTS=-XX:TieredStopAtLevel=1`. "Maven-only flags do not reach Surefire JVMs."
- `-Dforwardmeasure.testcontainers.opensearch.jvm-options=-XX:TieredStopAtLevel=1`. OpenSearch unsets `JAVA_TOOL_OPTIONS`; this property sets `OPENSEARCH_JAVA_OPTS`.
- `-Dforwardmeasure.testcontainers.keycloak.jvm-options=-XX:TieredStopAtLevel=1`. This property was added 2026-10-10 and sets `JAVA_OPTS_APPEND`.
- `-Dforwardmeasure.acceptance.java-tool-options=-Xmx1g -XX:TieredStopAtLevel=1` for Docker services, which "do not inherit the test JVM's JAVA_TOOL_OPTIONS".

The 2026-10-09 matrices all ran under these C1-only settings. Temurin 25.0.3 in normal mode later succeeded (VC). The Temurin 25.0.4.1 crashes remain unexplained, and no normal-JVM (C2) verification run is recorded.

### 5.4 Image and matrix selectors

| Product | Properties |
|---|---|
| FOWF | `-Dopenworkflow.acceptance.<component>.<framework>.image=…` for `definition-management`, `execution-management`, `engine-kafka-streams`, `engine-pekko`, `operation-adapter-kafka-streams`, `operation-adapter-pekko`; `openworkflow.image.tag` |
| FEI | `fei.image.tag`; `fei.acceptance.{ingestion-service,ingestion-worker,resolution-delivery-worker,resolution-service}.<framework>.image`; `fei.spark.worker.image`; `fds.delivery.image`; `fds.image.tag` |
| FDS | `fds.acceptance.pekko.image`, `fds.acceptance.kafka-streams.image` (a real digest is required; the curl fallback is removed); `-Dfds.acceptance.pekko.persistence=CASSANDRA` (PostgreSQL by default). Since 2026-10-11, launcher acceptance tests take their executor image names from the build (FDS `74b34e0`) |
| FDE | `decision.engine.image.{quarkus,spring,micronaut,migration,registry-migration}` |
| Matrix selection | `fowf.acceptance.runtime`, `decision.engine.acceptance.deployment`. These default to all combinations and reject unknown selections |
| Coverage in K3s | `forwardmeasure.testcontainers.jacoco.agent`, `forwardmeasure.testcontainers.jacoco.output` (set by the `coverage` profile) |

### 5.5 Matrix runners and retained scripts

- **FEI runner:** `scripts/run-public-ingestion-matrix.sh` with `ENTITY_INTELLIGENCE_IMAGE_TAG` and `FOWF_IMAGE_TAG`.
  - It provisions one isolated cell at a time, stops on failure, and writes per-cell logs plus a CSV.
  - `ENTITY_INTELLIGENCE_MATRIX_START_AT=framework/engine/persistence` skips earlier cells without counting them as passes.
- **Validation scripts and logs** live in `~/.local/state/forwardmeasure/validation/20261008-post-rollout/` ("LOGS" below). Earlier logs are in `…/20261008-recovery/`. Scripts there:
  - `verify-worker-and-wire-03.sh`;
  - `verify-fowf-retained-coverage-gate-01.sh`;
  - `verify-component-corrections-01.sh`;
  - `run-fei-matrix-12.sh`, `run-fei-matrix-13.sh`, `run-fei-matrix-15.sh`;
  - `verify-shared-api-consumers-01.sh`, `verify-shared-api-http-01.sh` to `-03.sh`.
- "Do not blindly rerun the earlier broad collection script: it selected legacy real-image tests with unsuitable default image assumptions."
- Keep persistent logs outside `/tmp`; a reboot lost the `/tmp` logs.

### 5.6 Fixture operating practice

- Capture diagnostics before teardown, without the credential-bearing pod environment.
- Refresh Docker port mappings after a restart.
- Use the intended namespace on Helmfile apply.
- Serve proto as `text/x-protobuf`. The egress host separator is `|`.
- Provision every participating identity per tenant, including worker service accounts.
- Pods do not inherit Docker aliases. Register storage aliases in CoreDNS.
- Sequencing (RMS; kept by AUD): "finish **all fixture implementation first**, then execute remaining matrix runs". Tests, image build/push and rollout are separate phases.

### 5.7 Before handing off a build

Check all three gates (memory, 2026-10-04):
1. **RAT offline, per module.**
   - Treat a run with no `Unapproved:` summary line as a failure, and prove the check by reproducing a known failure first.
   - `hs_err_pid*.log` and `replay_pid*.log` are excluded in the platform and FOWF.
   - Every new file gets the full 14-line ASF header. FEI files take the ForwardMeasure proprietary header instead.
2. **Versions:** every dependency without a `<version>` in a changed POM is managed by the platform POM, the FOWF root POM or an imported BOM.
3. **javac** for each changed module, especially files other agents touched.

Anything left half-done by an interrupted agent is checked before the build.

---

## 6. Coverage

### 6.1 Policy (user decision, 2026-10-07; JBP, VC, AUD)

- **Thresholds:** `jacoco.minimum.line.coverage=0.85` and `jacoco.minimum.branch.coverage=0.85` "across the stack", with zero permitted missed classes in aggregates. This supersedes the earlier per-product 85% lines / 75% branches / 0 classes (see [section 18](#18-superseded-and-conflicting-statements)).
- **Scope:** Java only; browser and Python code are not covered.
- **Generated code is exempt** by generated-source provenance or retained `Generated` annotations.
  - Exact class exclusions go to `target/coverage-generated-excludes.txt`. Whole packages are not exempted. "Unknown provenance remains subject to coverage."
  - "handwritten production code still requires 85% lines and 85% branches." Handwritten adapters are not generated code.
- **Missing data fails.** Missing or empty execution data for handwritten classes fails through the `require-coverage-data` goal. Policy classes: `CoverageOwnership.java`, `GeneratedCoverage.java`, `RequireCoverageDataMojo.java`.
- **Aggregates:**
  - JPA, OpenWorkflow and FEI use product aggregate checks. A module may declare `jacoco.module.check.skip=true` only when a selected reactor aggregate includes it at the same version, stages its classes, and reports and checks it.
  - FEI's aggregate owns domain coverage. FDS and FDE are checked "through their configured ownership rather than inventing a new aggregate".
  - Since 2026-10-11 the FOWF aggregate owns every production module (FOWF `76f82dcd`), and the FDS aggregate owns the launcher framework bindings (FDS `07440a7`).
- **Rules:**
  - "Thresholds must not be lowered to get a build through. Passing compilation is not evidence of passing these gates."
  - "do not add perfunctory tests or weaken behaviour checks to meet a coverage number".
  - The information-extraction handover (2026-10-12) refers to a temporary waiver: it "does not excuse functional failures, hidden skips or misleading test claims". The permanent target is unchanged.
  - The `coverage` profile is required on every run that should count; "Remote/container tests do not contribute automatically" (HO). Section 6.2 now provides the container path.

### 6.2 Coverage from packaged images in K3s (built 2026-10-10)

- `KubernetesTestContainer#withJacocoCoverage("<image prefix>")` must be called before start. It applies a MutatingAdmissionPolicy (GA `admissionregistration.k8s.io/v1` in K3s v1.36.4) to every pod whose image starts with the prefix (or `docker.io/<prefix>`). It:
  - mounts a node hostPath;
  - appends `-javaagent:…=destfile=…,append=true,includes=com.forwardmeasure.*` to `JAVA_TOOL_OPTIONS`, keeping any `-Xmx`;
  - labels the pod `forwardmeasure.io/jacoco=injected`.
  This also covers the Jobs that FOWF dispatches, with no product or workflow change.
- `close()` / `collectJacocoCoverage()`:
  - deletes the workloads (SIGTERM triggers the shutdown-hook dump);
  - copies the node exec file and appends it to `-Dforwardmeasure.testcontainers.jacoco.output`;
  - fails only if injected pods ran but nothing was recorded.
- JUnit form: `@WithKubernetesContainer(jacocoImagePrefixes = …)`.
- On Fedora with SELinux enforcing, coverage files stay inside the K3s node container, never on host bind mounts.
- **Images must be rebuilt from current code first.** Otherwise JaCoCo silently drops mismatched class IDs.
- FOWF's engine-pekko k3s-verification moved from raw `K3sContainer` v1.31.2 (which lacks MutatingAdmissionPolicy v1) onto this fixture.
- FDE's 2026-10-09 gRPC lifecycle run used the JaCoCo 0.8.15 agent in the service JVMs. Its "source/class-ID matching and 85% gate [were] not yet verified".

### 6.3 Recorded gate results

| Product / module | Result | Date | Detail |
|---|---|---|---|
| FOWF aggregate `openworkflow-coverage` | **FAILED** | 2026-10-11 | Newest: run 34 final gate, 92.9% lines, 79.7% branches, 22 missed classes (testing wrap-up of 2026-10-11). Earlier: 2026-10-09 retained-data check, branches 0.79 and 68 missed classes; 2026-10-03 failing baseline. Full record and class-level gaps: [FOWF testing §7](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#7-coverage-and-architecture-gate-state) |
| FDS `data-streaming-launcher-application` | **FAILED** rule | 2026-10-12/13 | Branches 0.65 < 0.85 (overnight discovery build; session record) |
| FDS `data-streaming-launcher-spring-service` | **FAILED** rule | 2026-10-12/13 | Lines 0.33 < 0.85 (overnight discovery build; session record) |
| FEI, FDE, JPA, others | NO RECORD of a passing gate | — | — |

Coverage-data collection gaps found 2026-10-12/13: `quarkus-jacoco` is missing in `forwardmeasure-jpa-quarkus`, the platform-operations Quarkus binding and the Agent OS Quarkus actor binding. Fixes are drafted and not applied (overnight findings).

### 6.4 Diagnostics (not gates)

| Product | Lines | Branches | File | Date | Limitation |
|---|---:|---:|---|---|---|
| FDS | 93.63% | 86.39% | `fds-component-partial-diagnostic.xml` | 2026-10-09 | No class-ID mismatch; not the per-module gate |
| FEI | 70.17% | 55.84% | `fei-after-api-diagnostic.xml` | 2026-10-09 | Predates the latest 24 worker API tests |
| FDE | 82.98% | 68.05% | `fde-after-rejections-diagnostic.xml` | 2026-10-09 | One enhanced Quarkus class mismatch (`DecisionEngineQuarkusBinding$TransactionalRulesets`); predates the latest 7 tests |
| FOWF | 88.84% | 77.83% | `fowf-after-components-diagnostic.xml` | 2026-10-09 | `$Cursor` mismatch; scope differs from the gate. Detail in [FOWF testing §7](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#7-coverage-and-architecture-gate-state) |
| JPA aggregate | 91.53% (1,286/1,405) | 87.00% (395/454) | — | 2026-10-07 | 69 classes |
| Platform JAX-RS | 97.31% | 89.67% | — | 2026-10-07 | 44 unit/contract tests, "not a claim of live HTTP acceptance on all three frameworks" |
| Micronaut validation | 101/101 lines | 66/68 branches | — | 2026-10-07 | 18 tests |
| Spring Security | 15/15 lines | — | — | 2026-10-07 | 401/403 regressions |
| Quarkus Security | 26/28 lines | — | — | 2026-10-07 | 3 tests, real Vert.x challenge |
| FDS Pekko executor | 761/813 lines | 223/253 | — | 2026-10-07 | 64 tests |
| FDS Kafka Streams executor | 379/401 lines | 104/114 | — | 2026-10-07 | 16 tests |
| FDS Spark | 433/467 lines | 135/156 | — | 2026-10-07 | 27 tests |
| entity-matching core | 889/913 lines | 496/578 | — | 2026-10-07 | 62 tests |
| AuthZEN client | 256/271 lines | 99/114 | — | 2026-10-07 | 21 tests |

These diagnostics "do not establish canonical module ownership, latest-source completeness, or proof that every uncovered branch lacks an existing test" (HO).

### 6.5 Diagnostic gap classes (HO, 2026-10-09, older snapshots)

These are observed diagnostic gaps, "**not a claim that every listed behavior has no existing test**". Refresh the data and inspect the existing assertions first.

| Product / class | Missed branches | Behaviours to reconcile with tests |
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
| FOWF (10 classes: `WorkflowEntity` 618 … `PekkoExecutionActivity` 41) | — | Moved to [FOWF testing §7](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#7-coverage-and-architecture-gate-state) on 2026-10-13 |
| FDE `DroolsRuleEvaluator` | 12 | Evaluation, unload/retirement and construction |
| FDE `RulesetMigrationsMain` | 10 | Required settings, URL selection and migration entry-point behavior |
| FDE `EvaluationServiceImpl` | 9 | Evaluation/error mapping; latest wire cases may reduce this |
| FDE `TenantExecution` / `MicronautTransactionalServiceProxy` | 8 each | Tenant execution, object methods, invocation and transaction boundaries |
| FDE `ProtoStructMapper` | 7 | Latest four unit cases address wire conversions; refresh before adding more |
| FDE tenant interceptors/resolvers, evaluator cache, management/admin, Valkey store | Multiple smaller gaps | Tenant rejection/context, cache lifecycle, rejected updates, fact-window behavior; inspect exact methods in XML |

---

## 7. Matrix: OpenWorkflow (FOWF)

**Summary as of 2026-10-13.** The full FOWF record moved to
[FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md) on
2026-10-13; nothing was dropped. Headlines:

| Area | Headline | FOWF section |
|---|---|---|
| Engine changes since the last matrices | `86d5fed2`, `1a7b5f93` (2026-10-11); `b9a2df5a`, `92a7b1c2`, `2b042da3`, `97f32483` (2026-10-13). No matrix covers the current code. | [FOWF status §8](../../forwardmeasure-openworkflow/docs/implementation-status.md#8-recent-changes-that-invalidate-older-blanket-completion-claims) |
| Basic public-API and recovery matrix | 9/9 cells passed on 2026-10-09 (local images; per-cell logs, durations, failure history and defects found are in FOWF). | [FOWF testing §2.1](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#21-basic-matrix-per-cell-fowfruntimematrixacceptancetest) |
| Overflow matrix | 9/9 cells × 6 scenarios passed on 2026-10-09; not start-input evidence. | [FOWF testing §2.2](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#22-overflow-matrix-per-cell-j-ovf-complete-about-1045-edt-on-2026-10-09) |
| Start input above 32 KiB | `FowfStartInputRuntimeMatrixAcceptanceTest` failed 9/9 in the 2026-10-12/13 overnight build; both causes fixed in `92a7b1c2`; not re-run. Scheduled and CloudEvent starts follow the placement rule since 2026-10-13 (uncommitted; `OksScheduledStartPlacementIntegrationTest` passes 3/3). | [FOWF testing §3.4](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#34-session-record-2026-10-1213) |
| Cross-engine parity | `LifecycleCloudEventParityTest` partial 8/15 before fixes, not re-run; `CorrelatedWorkerStateParityTest` no record. | [FOWF testing §3.4](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#34-session-record-2026-10-1213) |
| Kafka Streams and Pekko suites | Session record 2026-10-13: Kafka runtime API/core passed, `LargeWorkflowDataTest` passed, Pekko runtime 296 passed, chart topic tests passed; `OksRestorationIntegrationTest` unknown; `OksCloudEventPublicationIntegrationTest` and updated binding/mapper tests not run; `OksCloudEventIngressResourceTest` failed. The Start wire pin is **decided: drain before upgrade** (`92a7b1c2`). | [FOWF testing §3.1–§3.4](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#31-kafka-streams-engine-suites-2026-10-0809) |
| Framework HTTP contracts | Passes recorded 2026-10-07 to 2026-10-09 with SQL-seeded definitions and an HTTP engine double for execution; human-task parity tests build `JpaHumanTaskRepository` directly; tenant administration GET only. | [FOWF testing §3.3, §4](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#33-framework-http-contracts-by-capability-2026-10-07-to-2026-10-09-not-re-verified) |
| September 15/15 claim | Superseded (tests were narrowed). | [FOWF testing §3.5](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#35-september-capability--framework-claim-superseded) |
| Studio and untested combinations | No real-API browser run; executors other than HTTP, gRPC and Kubernetes Job have no runtime evidence; every AUD §6 criterion unassessed. | [FOWF testing §3.6](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#36-studio-and-untested-combinations) |
| Coverage | Newest: 92.9% lines, 79.7% branches, 22 missed classes (2026-10-11); gate not passed. | [FOWF testing §7](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#7-coverage-and-architecture-gate-state) |

---

## 8. Matrix: Entity Intelligence (FEI)

### 8.1 Complete public-API runtime matrix (J-feiC, recorded 2026-10-09 15:15 EDT)

**Per cell:** 1 test, 0 failures/errors/skips, covering five families:
1. WorldCheck ingestion and screening;
2. configured pipeline overrides (and request overrides, RMS);
3. two-tenant full/delta lifecycle;
4. simple/correlated resolution;
5. customer master: real persisted assertion union; typed dates, identifiers and addresses; all 42 provider fields.

| Cell | Status | Seconds | Run dir / runner | Fixture DB max_connections |
|---|---|---|---|---|
| Q-K | PASSED (per J-feiC, 2026-10-09; not re-verified) | 437.051 | `fei-complete-matrix-12/` / `run-fei-matrix-12.sh` | not recorded |
| Q-PG | PASSED (per J-feiC, 2026-10-09; not re-verified) | 452.928 | 12 | not recorded |
| Q-PC | PASSED (per J-feiC, 2026-10-09; not re-verified) | 618.003 | 12 | not recorded |
| S-K | PASSED (per J-feiC, 2026-10-09; not re-verified) | 452.123 | 12 | not recorded |
| S-PG | PASSED (per J-feiC, 2026-10-09; not re-verified) | 480.221 | `fei-complete-matrix-13/` | 200 |
| S-PC | PASSED (per J-feiC, 2026-10-09; not re-verified) | 620.811 | 13 | 200 |
| M-K | PASSED (per J-feiC, 2026-10-09; not re-verified) | 497.626 | `fei-complete-matrix-15/` | 200 |
| M-PG | PASSED (per J-feiC, 2026-10-09; not re-verified) | 475.096 | 15 | 200 |
| M-PC | PASSED (per J-feiC, 2026-10-09; not re-verified) | 635.871 | 15 | field absent |

**Images:**

| Component | Image or tag |
|---|---|
| FEI ingestion-service Q/S | `revision-response-20261009-1` |
| FEI ingestion-service M | `revision-response` in runs 12/13; `resolution-persistence-dates-20261009-1` in run 15 |
| Ingestion workers and resolution-delivery workers (Q/S/M) | `resolution-dates-20261009-1` |
| Spark worker | `entity-intelligence-ingestion-worker-spark:assertion-union-20261009-1` |
| FDS delivery | `data-streaming-executor-kafka-streams:runtime-validation-20261009-1` |
| FOWF Kafka engines / Kafka adapters | `decimal-transition-20261009-1` / `control-envelope-20261009-1` |
| FOWF Pekko engines / Pekko adapters | `pekko-read-recovery-20261009-1` / `pekko-recovery-20261009-1` |
| FOWF execution-management | `observed-failure-20261009-1` |
| FOWF definition-management M | `issuer-repair-20261008-1` |
| Tags | `openworkflow.image.tag=1.1.0`, `fei.image.tag=runtime-validation-20261009-1`, `fds.image.tag=1.1.0` (absent in M-K and M-PG) |
| Run 15 only | resolution-service M `authorization-duration-20261009-1` |

The Micronaut runs (attempts 14 and 15) and their two Micronaut-only image tags have no narrative in any prose source.

**Attempt history** (2026-10-09; failed attempts are "not a pass"):

| Attempt | Outcome |
|---|---|
| 01-02 | An operation of 49,639 bytes exceeded the 32,768-byte limit, with no object storage configured. The fixture switched to `forwardmeasure-testcontainers-gcs` on the K3s network |
| 03 | Stopped deliberately: pods do not inherit Docker aliases. The storage alias is now registered in CoreDNS |
| 04 | Control-envelope cutpoint failure |
| 05 | Digest mismatch; stalled, stopped with exit 143 |
| 06 | WorldCheck and configured pipelines passed. The tenant-B worker was correctly denied because the fixture omitted worker membership |
| 07 | Activation response schema failure |
| 08 | 3 of 5 families passed, including full/delta. The resolution worker required OpenSearch |
| 09 | Simple resolution passed. The correlated image was blank (fixture defect) |
| 10 | Correlated assertions dropped |
| 11 | 4 of 5 passed. Customer Master `LocalDate` failure |
| 12 | Q-K, Q-PG, Q-PC and S-K passed. S-PG failed after its families: the adapter rolling replacement exhausted PostgreSQL connections (97 JDBC clients, 65 on the FOWF database, reserved-superuser-slots exhaustion). Fix: 200 connections |
| 13 | Resumed at S-PG |
| 15 | Micronaut cells |

**Production defects found by the matrix:**
- delta-status HTTP 500;
- activation response nullability;
- resolution worker requiring OpenSearch;
- correlation dropping lower-trust assertions;
- Customer Master date mapping.

The classification-merge fix (checkpoint `4ba0435`) has component evidence only. Its image `classification-merge-20261009-1` is not image-verified.

### 8.2 Step D (monitored populations, membership, screening by membership)

| Item | Status | Date |
|---|---|---|
| Step D deep integration tests on Q/S/M over Liquibase fixtures (committed in FEI `d9b161b`) | Written, compile; **NOT RUN** | 2026-10-13 (session record; memory 2026-10-13) |
| Framework test classes, each in Q/S/M | — | — |
| Domain/application/worker tests | — | — |
| Liquibase test fixtures | — | — |
| Studio | — | — |
| Consequence | A startup defect, two `@PrePersist` callbacks in `MonitoringScreeningHit`, reached a production deploy. These tests would have caught it | 2026-10-13 |

Detail for the rows above (all compile, none run):
- **Framework test classes, each in Q/S/M:** `IngestionTriggerAdmissionResourceTest`, `IngestionTriggerReplayResourceTest`, `MonitoredPopulationMembershipResourceTest`, `MonitoredPopulationResourceTest`, `ReferencePopulationCrudResourceTest`, `ReferencePopulationRevisionRulesResourceTest`, `SourceFeedProfileCrudResourceTest`, `EntityDeletionResourceTest`, `AdHocScreeningResourceTest`, `MonitoredPopulationScreeningResourceTest`, `PopulationScreeningRunResourceTest`, `ScreeningResourceTest`, `ScreeningRunTransformAndExportResourceTest`.
- **Domain/application/worker tests:** `ClosedSchemaRequestBodyTest`, `GrantManifestTest`, `EntityApplicationServiceTest`, `PopulationScreeningWorkerIntegrationTest`, the ingestion-worker contract tests (`DocumentContractTest`, `IngestionContractTest`, `ResolutionTargetTest`, `SourcePinTest`), the K3s verification classes (`IngestionPipelineFowfK3sVerificationTest`, `ReferencePopulationPublicApiAcceptanceIT`, `AcceptanceContractPackagingTest`).
  - Entity merge has no REST operation, so its membership test is at the application layer (`EntityApplicationServiceTest`). It "must not be labeled REST proof".
- **Liquibase test fixtures:** `entity-deletion-membership.xml`, `ingestion-trigger-admission.xml`, `ingestion-trigger-replay.xml`, `ingestion-worker-operations.xml`, `monitored-population-admission.xml`, `monitored-population-membership.xml`, `reference-population-comparison.xml`, `reference-population-contracts.xml`, `reference-population-indexing.xml`, `reference-population-lifecycle.xml`, `reference-population-revision-rules.xml`, `screening-monitored-population.xml`.
- **Studio:** Vitest files (`MonitoredPopulations.test.tsx` and others) and the Playwright `monitored-population-live.spec.ts` with an `openView` navigation helper; no run recorded.

**Population-contract scenarios R1-R18** (population plan §10, 2026-10-12) are required on Quarkus, Spring and Micronaut, covering simple and correlated paths and each affected worker runtime. **None has been executed in the integration suites** (plan §15, 2026-10-13):

| ID | Scenario |
|---|---|
| R1 | Standard population; fixed target visible |
| R2 | A single custom document fails before side effects |
| R3 | Compatible and incompatible custom pairs, including vendor field type mismatch |
| R4 | A target change via update, trigger or pipeline is rejected |
| R5 | Two S2/M2 versions on the same target, with real indexed documents |
| R6 | Hosted files or the default changed after admission; replay keeps the originals |
| R7 | Full/delta keeps target identity; an incompatible baseline is rejected |
| R8 | WorldCheck YAML via REST produces the expected OpenSearch fields and raw data |
| R9 | Customer Master monitored REST produces resolution records, assertions and membership |
| R10 | Two populations resolve to the same entity with distinguishable supports (tested through merge) |
| R11 | Idempotent repeat |
| R12 | Removal or scoped refresh leaves other supports, populations and entities intact |
| R13 | A failed or partial refresh does not withdraw |
| R14 | Merge, re-resolution and reversal keep provenance without duplicates |
| R15 | Investigation-only entities need no membership and keep authorization |
| R16 | Screening selects members, deduplicates, reports unresolved and keeps provenance |
| R17 | Cross-tenant, collision and unauthorized operations are rejected |
| R18 | Studio forms send the intended requests and display errors |

- "Every FEI request body rejects properties its schema does not declare (400), in all frameworks" is implemented. No per-framework execution is recorded.
- "Do not claim full FOWF/FDS/FDE parity from this focused change. The 5.8 GB WorldCheck live run remains separate evidence from bounded acceptance fixtures."

### 8.3 FEI framework and component evidence (2026-10-07 to 2026-10-09)

| Suite | Q | S | M | Status and date |
|---|---|---|---|---|
| Managed-service replacements (57 managed-service + 18 HTTP) | 25 | 25 | 25 | Passed (per TG/VC, 2026-10-07/08; not re-verified). "These results do not establish complete ingestion acceptance" |
| `EntityReadIntegrationTest` | 27 | 27 | 28 | Passed, `fei-managed-merge-corrected-01.log` (per J-feiCorr, 2026-10-09; not re-verified) |
| `IngestionWorkerOperationsResourceTest` | 8 | 8 | 8 | 24/24 passed, `fei-worker-api-corrected-03.log`, 2m51s (per HO, 2026-10-09; not re-verified). Earlier: 104 selected, 90 passed, 3 errors (this class), 11 skips |
| `ReferencePopulationCrudResourceTest` | 4 PASSED (26.675 s) on 2026-10-09 ~18:17; then 4 **SKIPPED** in a later run | 4 PASSED | 4 PASSED | J-api and HO, 2026-10-09. The skips came from a Keycloak C2 crash that marked the Quarkus shared boot failed (memory, 2026-10-10). The class changed in `d9b161b` (2026-10-13) and has **not been run** since |
| Shared API HTTP (lifecycle/security/validation) | 4 | 4 | 4 | Passed, plus `SharedProblemSchemaTest` 1 (per J-api, 2026-10-09; not re-verified) |
| Ingestion authorization (issuer) | 9 cases across Q/S/M | | | `/tmp/fei-fds-three-framework-issuer-regressions{,-02}-20261008.log`. "neither combined log is wholly green" (per TG, 2026-10-08) |
| Screening HTTP / resolution issuer | 15 / 3 across Q/S/M | | | Passed (per TG, 2026-10-08; not re-verified). "HTTP results, not completed worker dispatch" |
| Packaged delta workers (real OpenSearch) | 2 | 2 | 2 | 6 subprocess ITs passed (per VC, 2026-10-07; not re-verified). Quarkus and Micronaut needed Commons Logging; Spring supplies it |

**Component corrections (J-feiCorr, 2026-10-09):**
- 114 tests; coverage gate "not verified"; source-level framework runs.
- `ReactorBoundaryArchitectureTest` 8, `PopulationScreeningWorkerIntegrationTest` 1, `EntityApplicationServiceTest` 15, `ReferencePopulationApplicationServiceTest` 8 (`fei-components-corrected-01.log`), plus the `EntityReadIntegrationTest` row above.
- The initial component run had 312 tests, 2 failures and 8 errors. Full FEI compile took 1m28s.

**Focused regressions (2026-10-09; not re-verified):**
- `ReferencePopulationRevisionResponseContractTest`: 3 statuses (INGESTING/READY/FAILED), failing before the fix and passing after.
- Studio: 4 component tests.
- Assertion union Spark/Kafka: 4 class cases, 2 of them new and failing before the fix; 17.665 s.
- Resolution date wire: 3 tests, 5.921 s, through the generated client with ISO text and `LocalDate`.

**2026-10-08 (per RMS; not re-verified):**
- Bounded contract/Spark 11/11 (9 contract plus 2 real Spark/Kafka).
- Studio serialization 3/3.
- Q/Pekko/PG public WorldCheck refresh 1/1 (3m57s) on `full-file-ingestion-20261008-1`.
- `IngestionPipelineFowfK3sVerificationTest#fullDeltaRollbackAndRejectedRowsUsePublicApi` 1/1, Q/Pekko/PG, 4m54s, single tenant.
- Configured-pipeline status regression passed.
- Evidence: FEI `docs/post-rollout-regression-evidence-2026-10-08.json`, `docs/delta-lifecycle-regression-evidence-2026-10-08.json`.
- Earlier the same day: FEI runtime/packaging regressions 15/15; FEI complete three-framework production/test compile (compile only).

**Older, 2026-10-07 (per VC; not re-verified):**
- FEI ingestion focused 17.
- WorldCheck canonical regression 1 test (12.64 s): "worker/indexing evidence, not public API dispatch evidence".

**Provider semantics:** 23 focused tests plus 2 decoder tests with real OpenSearch/PostgreSQL. The 10-record external sample was checked only for mapping.

**Superseded:** the historical 27-invocation table of 2026-10-08 is replaced by J-feiC. Its methods were `premiumWorldCheckFileSchemaMappingAndIndexAreInvokedThroughThePublicApi`, `fullDeltaRollbackIsolationAndRejectedRows` and `simpleAndCorrelatedResolutionUseRealApiAndRemainTenantScoped` in `ReferencePopulationPublicApiAcceptanceIT`.

**September history (memory; not re-verified):**
- 2026-09-06: all six resolution-service and ingestion-service leaves (Q/S/M) passed their real-PostgreSQL HTTP suites, user-run.
- 2026-09-16: investigation work closed with Q 26/26, S 22/22, M 22/22.

### 8.4 Full-file scale run

- 5,611,953,182 bytes, 5,818,856 records, Quarkus API → Pekko/PG → worker.
- READY, zero rejected, exact count. Admission to READY 14m08s.
- Worker working set about 555 MiB, VmHWM about 614 MiB, under a 1 GiB heap / 2 GiB Job. Run on `full-file-ingestion-20261008-1` (per RMS, 2026-10-08; not re-verified).
- Runs 03 (bad assertion) and 04 (editor crash) are not passes.
- "not an executed 100 GB or TB benchmark". It "does not establish hundreds-of-GB execution or complete framework/engine parity". It ran on one cell (Q/Pekko/PG).

### 8.5 Information extraction

No extraction tests have been executed (handover, 2026-10-12).
- Required matrix: Quarkus, Spring and Micronaut; for workflow integration, Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra.
- "Scope the affected regression cases explicitly; do not restart every unrelated matrix or claim one tested runtime proves them all."
- Upload bindings exist for all three frameworks; this is structural inspection only.
- "Historical ingestion/runtime passes do not certify new extraction code or current deployment images."

### 8.6 Screening regression gates and FEI Studio

**Screening gates.** The port mandate (2026-08-08) says: "The greenfield Entity Intelligence port must implement both gates. A small hand-written unit test is not an adequate substitute." The two gates are:
1. `RealDataScreeningRegressionTest`:
   - OpenSearch 3.2 with the phonetic plugin, the production mapping and real WorldCheck documents;
   - real Spark rows through the production mapper, recall client, ensemble matcher, scoring engine, override, gates and threshold;
   - asserts the exact accepted UID set;
   - corpus `screening-regression-expectations.json`, `screening-regression-fixture.json`, extractor `extract-real-screening-regression-fixtures.py`.
2. `FullExportScoringBacktestTest`: opt-in and streaming. It rescores exported `raw_hit` into `target/screening-backtest-summary.json`. It "cannot prove recall for candidates that the old run never exported."

The evidence recorded for these gates comes from the predecessor implementation, not from FEI: run 7, run 8, and run 9 (330 valid plus 1 miss, fixed in code, not re-run). No FEI execution of either gate is recorded. `TaskStatusHttpSemanticsContractTest` exists in FEI, with no framework breakdown recorded.

**FEI Studio:**
- Real-API browser acceptance gate: **NOT RUN** (HO, 2026-10-09; AUD).
- History (memory; not re-verified):
  - 2026-09-16: a testcontainers e2e passed Milestones 1+2 3/3, with a network-level assertion. Milestone 3 reached real business logic but had an intermittent AuthZEN issue at the final step.
  - Studio JVM tests 1/1 per framework plus `StudioApiProxyTest` 8/8.
  - Vitest: 116 tests in 14 files on 2026-09-16; 130 tests in 17 files on 2026-10-07 (VC).
- The 2026-09-29 e2e-harness fix plan has nine steps; its done criteria are healthy containers, Milestone 1 health, Milestone 2 browser login and the Milestone 3 trigger reaching a running execution. Its later status is unknown.
- Operator Studio testing of parsing overrides was left to the operator (2026-10-08); no result is recorded.

---

## 9. Matrix: Data Streaming (FDS)

### 9.1 Runtime matrices: 39 cells, 57 tests (J-fds, recorded 2026-10-09 17:42 EDT)

- Logs are `fds-<Suite>-<SELECTION>-01.log/.xml` in LOGS.
- Classes are `{Direct,WorkflowBounded,WorkflowContinuous}Matrix{Framework}{Engine}[TestCustomerMaster]SmokeTest`.
- `selection` is POSTGRESQL or CASSANDRA (the FOWF Pekko backend). Direct and Kafka cells carry POSTGRESQL; for direct cells this is only the selector default.
- Each cell is 1 test unless noted. The values are seconds.
- Every cell: **PASSED (per J-fds, 2026-10-09; not re-verified)**.

| Flow | Provider | Engine / backend | Q | S | M |
|---|---|---|---|---|---|
| Direct | WorldCheck | Kafka Streams (**7 tests**) | 55.707 | 64.711 | 61.572* |
| Direct | Customer Master | Kafka Streams | 57.202 | 60.587 | 56.43* |
| Direct | WorldCheck | Pekko | 57.685 | 56.249 | 52.32* |
| Direct | Customer Master | Pekko | 51.698 | 55.516 | 56.203* |
| Bounded (FOWF) | WorldCheck | Kafka / PG | 93.408 | 88.488 | 95.371* |
| Bounded | Customer Master | Kafka / PG | 86.293 | 90.248 | 94.456* |
| Bounded | WorldCheck | Pekko / PG | 94.369 | 102.397 | 102.012* |
| Bounded | WorldCheck | Pekko / Cassandra | 136.05 | 145.634* | 140.017* |
| Bounded | Customer Master | Pekko / PG | 96.434 | 112.435* | 97.554* |
| Bounded | Customer Master | Pekko / Cassandra | 133.296 | 152.043* | 142.329* |
| Continuous (FOWF; two-wave real data) | — | Kafka / PG | 84.17 | 86.909* | 97.831* |
| Continuous | — | Pekko / PG | 95.79 | 104.053* | 93.486* |
| Continuous | — | Pekko / Cassandra | 134.878 | 150.722* | 147.688* |

\* These 19 cells (all 13 Micronaut cells and 6 Spring cells) used `forwardmeasure.testcontainers.opensearch.jvm-options=-XX:TieredStopAtLevel=1` (checked against J-fds).

**Common images:**
- FDS executors `data-streaming-executor-{kafka-streams,pekko}:runtime-validation-20261009-1`;
- FOWF engines and execution-management `activity-projection-20261009-1`;
- Kafka adapters `grpc-unary-result-20261009-1`;
- Pekko adapters `pekko-recovery-20261009-1`;
- definition-management Micronaut `issuer-repair-20261008-1`;
- `forwardmeasure.acceptance.java-tool-options=-Xmx1g -XX:TieredStopAtLevel=1`.

**Archived failures** (not counted):
- one Keycloak provisioning EOF;
- one OpenSearch C2 `PhaseIdealLoop` SIGSEGV.

**Fixture repairs:**
- tenant-member launcher machine identity (`ce6f183`);
- explicit shared Cassandra dependency (`07017d7`);
- bounded retry of the shard-recovery 503 (`f6482d3`; "No final data assertion was removed");
- OpenSearch fixture (`9f951aa`).

**Earlier fixture repairs, all now in place:**
- the curl-image fallback removed from all 12 direct fixtures;
- `RealFowfWorkflowFixture` selects the framework for all four FOWF services and uses real Cassandra when selected;
- continuous fixtures dispatch a real executor and check two waves of OpenSearch data after Deployment readiness;
- the expected issuer is pinned (replacing `QUARKUS_OIDC_TOKEN_ISSUER=any`);
- public-API read barrier and explicit images (`d13dffd`);
- workflow name derived from a stable UUID (`7969ddc`);
- Kafka supplied to the Pekko human-task producer (`d95f60f`).

**Baselines:**
- "Historical 39-cell/57-test counts are evidence sets, not a complete behavior denominator" (AUD).
- The eight base capability cells (single/correlated × bounded/continuous × Pekko/Kafka) and the historical 18-cell baseline for WorldCheck and Customer Master must be preserved. The 18-cell baseline omits direct continuous invocation and does not discharge SINV-01.
- Historical class inventory (2026-10-08): 10 class patterns × 3 frameworks = 30 classes (12 direct, 12 bounded, 6 continuous), "not 30 test methods".

### 9.2 Launcher and component evidence

| Item | Status | Date |
|---|---|---|
| `FowfStartInputRuntimeMatrixAcceptanceTest` | **FAILED 9/9**; causes fixed; not re-run | 2026-10-12/13 (FOWF testing §3.4; section 7 here) |
| Launcher coverage rules | **FAILED** (application branches 0.65; Spring service lines 0.33) | 2026-10-12/13 |
| Drafted launcher tests (not applied) | `LaunchRequestsTest` (records, auth resources, tenant client cache) and planner Spark preconditions (launcher-application); `LauncherProblemMappersTest` (auth mappers, bad-gateway/unnamed status, unhandled NPE) (launcher-jaxrs); `LauncherSpringApplicationTest`, which boots the real `main()` with Keycloak and K3s and checks liveness UP | 2026-10-12/13 (overnight findings) |
| Drafted component tests (not applied) | `data-streaming-executor-pekko`: `StreamingTextRows` option validation. `data-streaming-api`: OpenSearch private-CA TLS end-to-end; `TransformCharacteristics` | 2026-10-12/13 |
| Shared API | `SharedProblemSchemaTest` 1, plus 1 missing-namespace HTTP case per framework via `DirectIngestionMatrix{F}KafkaStreamsSmokeTest` (Q 53.484 s, S 54.54 s, M 51.726 s). "FDS requires explicitly selected existing executor image even for the HTTP validation case" (per J-api, 2026-10-09; not re-verified) | 2026-10-09 |
| Component run | It accidentally selected packaged FOWF matrix tests with default images and was interrupted ("not another matrix result"). A second selection was interrupted after 24 ordinary launcher cases passed. The client/resource continuation had 12 passes and 3 errors from the old URL `/v1/executions`; after the fix to `/v1/workflow-executions`, 4 resource cases passed (per RMS, 2026-10-09) | 2026-10-09 |
| Issuer | 2 real HTTP cases (expected issuer before/after rejection; unknown-execution 404: "fixture authentication evidence, not engine-dispatch"); 3 launcher issuer cases across Q/S/M (per TG, 2026-10-08; not re-verified) | 2026-10-08 |
| `DirectIngestionLauncherKeycloakIntegrationTest` | Real Keycloak denial for an ungranted cancel. Keep it as a scoped security integration | TG, 2026-10-07 |
| Provider-data ownership | Focused source/fixture/mapping suite, 18 tests (per SCO, 2026-10-08; not re-verified) | 2026-10-08 |
| Executors (2026-10-07) | Pekko 64, Kafka Streams 16, Spark 27 (local images, not pushed). Real K3s Spark→Kafka: single source 59 s, correlated 64 s. Launcher 38 tests with 1 failure and 14 errors (fixture drift), then 15 focused tests passed. FOWF admission: 3 checks with an exact 404. Private-registry pull tests skip without credentials: "No private-registry proof claimed" (per VC; not re-verified) | 2026-10-07 |
| Modules (2026-10-07) | FDS API 24, transforms 28, mappers 29, core 20, fixtures 17, Kubernetes job lifecycle 15 (per VC; not re-verified) | 2026-10-07 |
| Spark two-Job real-image tests | `DirectIngestionLauncherSparkTwoJob{,Correlated}RealImageIntegrationTest`: older valid evidence of two-Job simple/correlated passes | before 2026-10-08 |

### 9.3 FDS gaps

- The full applicability table is not audited: source cardinality × bounded/continuous × engine × direct/workflow × framework × FOWF backend.
- Direct continuous invocation is untested (outside the 18-cell baseline); SINV-01 is not discharged.
- Spark staging is not a separate axis in the launcher matrices.
- No explicit evidence in these sources for:
  - malformed rows;
  - the read-committed frontier;
  - sink failure;
  - timeout/cancel and drain;
  - retry/idempotency.
- Real workflow dispatch evidence: "admission evidence is not proof of a dispatched workflow" (VC, 2026-10-07). The FOWF cells of the 2026-10-09 matrix later dispatched real workflows.
- Normal-JVM (non-C1) verification is not done.
- The coverage gate fails.

---

## 10. Matrix: Decision Engine (FDE)

### 10.1 Runtime matrices: 15/15 (J-fde, recorded 2026-10-09 16:19 EDT)

**Workflow cells** (`DecisionEngineWorkflowRuntimeAcceptanceTest`; logs `fde-workflow-<f>-<engine>-<backend>-01`).

Each cell verifies:
- timer/effect projection;
- business output (gRPC result);
- service restart;
- denied identity;
- tenant isolation.

| Cell | Status | Seconds |
|---|---|---|
| Q-K | PASSED (per J-fde, 2026-10-09; not re-verified) | 81.477 |
| Q-PG | PASSED (per J-fde, 2026-10-09; not re-verified) | 80.656 |
| Q-PC | PASSED (per J-fde, 2026-10-09; not re-verified) | 190.504 |
| S-K | PASSED (per J-fde, 2026-10-09; not re-verified) | 86.276 |
| S-PG | PASSED (per J-fde, 2026-10-09; not re-verified) | 86.715 |
| S-PC | PASSED (per J-fde, 2026-10-09; not re-verified) | 219.512 |
| M-K | PASSED (per J-fde, 2026-10-09; not re-verified) | 88.297 |
| M-PG | PASSED (per J-fde, 2026-10-09; not re-verified) | 102.823 |
| M-PC | PASSED (per J-fde, 2026-10-09; not re-verified) | 197.777 |

**Valkey deployment cells** (`DecisionEngineValkeyDeploymentAcceptanceTest`, 6 real Helmfile deployments; logs `fde-deployment-<f>-<mode>-01`). They verify authenticated Valkey and retained facts after FDE pod replacement.

| Framework | Embedded | External |
|---|---|---|
| Quarkus | PASSED, 120.802 s (per J-fde, 2026-10-09; not re-verified) | PASSED, 76.752 s (per J-fde, 2026-10-09; not re-verified) |
| Spring | PASSED, 95.624 s (per J-fde, 2026-10-09; not re-verified) | PASSED, 73.823 s (per J-fde, 2026-10-09; not re-verified) |
| Micronaut | PASSED, 88.351 s (per J-fde, 2026-10-09; not re-verified) | PASSED, 74.69 s (per J-fde, 2026-10-09; not re-verified) |

**Images:**
- FDE `docker.io/forwardmeasure/decision-engine-{quarkus,spring,micronaut}:1.1.0`, `decision-engine-database-migration-service:1.1.0`, registry migration `openworkflow-migrations:1.1.0`.
- FOWF engines and execution-management `activity-projection-20261009-1`, Kafka adapters `grpc-unary-result-20261009-1`, Pekko adapters `pekko-recovery-20261009-1`.
- JVM `-Xmx1g -XX:TieredStopAtLevel=1`.

**Notes:**
- The FDE service images are the released `1.1.0`. Whether they contain the 2026-10-08 Micronaut readiness and issuer fixes is not recorded.
- Archived failure suffixes: `*-jvm-failure`, `*-media-type-failure`, `*-egress-fixture-failure`, `*-unary-result-failure`, `*-restart-probe-failure`, `*-restart-port-failure`. A fixture namespace omission was fixed before the 6 deployment passes.

### 10.2 Packaged gRPC conformance and components

| Item | Q | S | M | Status and date |
|---|---|---|---|---|
| `DecisionEngineContainerConformanceTest.authenticatedFrameworkRoutesEachTenantToItsOwnPhysicalDatabase`: real Keycloak, PostgreSQL, Valkey; cross-tenant Valkey windows | 1 | 1 | 1 | Passed, `fde-authenticated-shared-fixtures-conformance-20261008.log` (per TG/RMS, 2026-10-08; not re-verified). The old opt-in, unsigned, raw-container version is superseded |
| Extended gRPC lifecycle (J-fdeG): see scenarios below | 1 | 1 | 1 | Passed, `fde-grpc-lifecycle-01.log`. Service image IDs Q `sha256:23bee666…`, S `sha256:bbc72a96…`, M `sha256:189d55c7…` (per J-fdeG, 2026-10-09; not re-verified) |
| Rejected updates: see scenarios below | 1 | 1 | 1 | Passed, `fde-rejected-updates-04.log`, 1m56s, checkpoint `85fd859` (per RMS, 2026-10-09; not re-verified) |
| `ProtoStructMapperTest` 4 + conformance 3 | — | — | — | Passed, `fde-evaluation-wire-01.log`, 1m52s (per HO, 2026-10-09; not re-verified) |

**Extended lifecycle scenarios:**
- two-tenant routing and authorization;
- active versus pinned evaluation;
- pagination and invalid cursors;
- activate, and reject deletion of the active version;
- deleting an inactive version → NOT_FOUND;
- a deleted version identity is never reused;
- 4 concurrent creations allocate distinct monotonic versions.

**Rejected-update scenarios:**
- invalid mode, globals, DRL, name and stateful bounds reject without changing the persisted active rules;
- the trusted identity overrides `createdBy`;
- the first version must be active, and rejection creates no rows;
- missing activate/delete/warm and invalid warm/unload parameters return the correct gRPC status.
- Fixture correction: compare persisted microsecond timestamps.

**Components, 2026-10-09** (per RMS; not re-verified):
- rule evaluator 9, real Valkey 6, schema-routed ruleset persistence 2, JWT 9;
- the initial component run was 18 tests with 3 errors.

**2026-10-08:**
- Audience helper: 6 real-Keycloak fixture cases.
- All 26 FDE modules compiled production and test sources.
- `VerifiedJwtTenantResolverTest` is a direct signature/claims contract. It does not prove server wiring.

**Uncommitted test changes listed in HO** (2026-10-09; "likely superseded", check git):
- `ProtoStructMapperTest`: nested values/nulls, invalid map keys, unsupported objects;
- the conformance test: wire output, invalid requests, missing rules, invalid result shapes, continued valid evaluation.

### 10.3 FDE gaps

- None of the 15 cells has run since 2026-10-09. FDE commit `8728a2b` (2026-10-11) made the conformance tests depend on the OpenWorkflow test kit.
- The rehabilitation acceptance list has no evidence. Each case needs its own evidence:
  - populated old-schema upgrade and counter seeding under restricted roles;
  - cache-warm tenant deactivation;
  - real authorization TTL;
  - Valkey script eviction and response loss;
  - read-only-root startup with writable compiler temp space;
  - secret rotation;
  - unavailable-dependency recovery;
  - proto compiler packaging without runtime Maven network access.
- Coverage: diagnostic 82.98% / 68.05%, plus the class-ID mismatch.

---

## 11. Agent OS

- **Recorded test evidence: none.** No source records any Agent OS test run, matrix cell or coverage result.
- **What exists in the repository** (read-only check, 2026-10-13):
  - 18 test classes.
  - Framework-specific tests are one per framework: `QuarkusAgentActorResolverPostgreSqlIntegrationTest`, `SpringAgentActorResolverPostgreSqlIntegrationTest`, `MicronautAgentActorResolverPostgreSqlIntegrationTest`.
  - Real-PostgreSQL integration tests: `AgentOsTenantMigratorPostgreSqlIntegrationTest`, `AgentPostgreSqlIntegrationTest`, `AgentExecutionServiceImplPostgreSqlIntegrationTest`, `AgentGovernanceServiceImplPostgreSqlIntegrationTest`.
  - Unit and contract tests: `AgentActorTest`, `AgentCoordinatesTest`, `AgentDefinitionContentSchemaParityTest`, `AgentDefinitionSchemaGenerationTest`, `AgentDefinitionTest`, `AgentStatusTest`, `DomainNeutralityArchitectureTest`, `DroolsPolicyEvaluatorTest`, `OpaPolicyEvaluatorTest`, `SharedSchemaInvariantTest`, `WorkflowReleaseBindingTest`.
- **Known gap:** `quarkus-jacoco` is missing in the Agent OS Quarkus actor binding. The fix is drafted and not applied (2026-10-12/13).
- **Not covered:** no HTTP contract tests per framework, no engine dimension and no K3s image tests exist in the list above.

---

## 12. Platform operations

- **Recorded test evidence: none.** No source records a test run.
- **What exists** (read-only check, 2026-10-13): 4 test classes.
  - `PlatformApplicationPathTest` and `PlatformTenantResolverTest` (application module);
  - `PlatformDashboardResourceTest` and `PlatformDashboardBrandingTest` (Quarkus binding only).
- **There are no Spring or Micronaut tests.** That is a three-framework parity gap under the standing rule.
- **Other recorded testing state (2026-10-06):**
  - The dashboard chart deploys only Quarkus, although bindings exist for all three frameworks.
  - Dashboard reactor compile, chart lint and a synthetic custom-name render passed. Real authentication and deployment were not exercised.
- **Known gap:** `quarkus-jacoco` is missing in the platform-operations Quarkus binding. The fix is drafted and not applied (2026-10-12/13).

---

## 13. Shared libraries and platform modules

### 13.1 Shared API ownership (J-api, 2026-10-09)

**140 tests, 0 failures/errors/skips** (per J-api, 2026-10-09; not re-verified):

| Group | Tests | Detail |
|---|---:|---|
| Platform | 64 | `SharedProblemContractTest` 2. `forwardmeasure-platform-server-jaxrs`: `ConstraintViolationExceptionMapperTest` 6, `JsonBindingProblemsTest` 13, `JsonFieldNamesTest` 3, `ProblemMapperRegistrationTest` 3, `QueryParameterConversionFilterTest` 6, `QueryParameterRequestFilterTest` 6, `RequestProblemsTest` 3, `UnhandledExceptionProblemMapperTest` 4. `forwardmeasure-platform-micronaut-validation`: `JacksonRequestBodyReaderTest` 7, `MicronautArgumentProblemTest` 4, `MicronautExceptionContractTest` 4, `MicronautSecurityProblemTest` 3 |
| Product schema tests | 4 | FOWF `SharedProblemSchemaTest` and `SharedProblemOwnershipTest`, FEI, FDS |
| HTTP | 72 | FOWF 57 (19 per framework), FEI 12 (4 per framework), FDS 3 (1 per framework). "Each product's HTTP cases boot Quarkus, Spring and Micronaut with real infrastructure" |

**Notes:**
- Logs: `shared-api-{platform-tests,fowf-contract-tests,fei-contract-tests,fds-contract-tests,fowf-full-compile,fei-full-compile,fds-frameworks-compile,fowf-http,fei-http,fds-http}.log`.
- C1 workaround; no publication.
- "This is source/framework verification after the recorded packaged matrices, not a rerun of those image cells." The engine dimension of these cases is not stated.
- "schema equality alone does not prove runtime wiring."

### 13.2 Library tests (VC/CB, 2026-10-07/08; not re-verified)

| Module | Tests |
|---|---|
| JPA | Spring and Micronaut 5 contract tests each; Micronaut 7 contracts; aggregate 91.53% lines / 87.00% branches |
| Platform JAX-RS | 44 |
| Micronaut validation / Spring Security / Quarkus Security | 18 / 401-403 regressions / 3 |
| Policy plugin | 41 |
| Database migrations: API, JDBC, Liquibase | 5, 5, 12 |
| Object storage: API, core, S3, GCS, Azure | 9, 9, 5, 7, 4 |
| NLP client | 4 contracts |
| entity-matching: core, opensearch, testkit | 62, 16, 2 |
| AuthZEN: API, fixture/testkit, client | 22, 6, 21 |
| Testcontainers | See section 4 |
| Full compile | 14 repositories, 482 modules; 14-reactor policy validate |

**Status notes:**
- The Keycloak token HTTP interruption branch is unverified (VC).
- AuthZEN real-Keycloak tests were executed on 2026-10-07 and 2026-10-08.
- **Drafted, not applied** (2026-10-12/13):
  - `jpa-datasource`: template validation and `close()` of plain/failing pools;
  - `jpa-testcontainers`: extension misuse via EngineTestKit (adds `junit-platform-testkit`);
  - `jpa-contract-tests`: contracts catch broken implementations ("lying real-backed doubles");
  - `quarkus-jacoco` in `jpa-quarkus`.

---

## 14. Deployment, identity and build-tooling tests

| Suite | Status | Date |
|---|---|---|
| Chart topic tests (`deploy/helmfile/scripts/tests`, with `OksTopicDeclarationDriftTest`, `OksTopicsTest`, `WorkflowCommandContractTest`) | PASSED after adding the cloud-events topic | 2026-10-13 (session record; not re-verified) |
| Deployment selection rendering, 18 combinations (3 frameworks × 3 engines × FDE on/off), `deploy/tests/test_deployment_selection_rendering.py` | Authored; **NOT RUN** | 2026-10-06 |
| Selection and transition regressions `deploy/tests/test_deployment_selection.py`; frontend branding tests beside each Studio's `branding.ts` | No run recorded | 2026-10-06 |
| Helmfile renders (five product/platform builds plus one non-default selection: Spring / Pekko Cassandra / FDE disabled); Keycloak chart lint and synthetic render | Passed: rendering only, "rendered YAML is not runtime proof" | 2026-10-06 |
| Build cache: `deploy/tests/test_build_cache_integration.py` (cold/warm restore, restored classes and test classes, execution after a skipped-test build, protobuf invalidation, two-module reactor, generated-source/Javadoc on hits, OpenAPI model consumed downstream on a warm cache), plus the mixed generated/handwritten coverage fixture | PASSED (`/tmp/platform-cache-and-coverage-regressions-20261007.log`, `/tmp/platform-cache-regressions-openapi-20261007.log`) | 2026-10-07 (not re-verified) |
| Independent cache acceptance: compare cached and uncached artifacts, generated clients, JDK/profile changes, image build/push and the selected-framework package layout | Not done | 2026-10-07 |
| Identity fixtures, see list below | Compile only; **NOT RUN** at that date. Later status unknown | 2026-10-05 |
| Identity acceptance cases ID-01..ID-10, see list below | NOT RUN | 2026-10-05 |
| Gateway request-ID hop | Missing from the K3s matrix ("direct Keycloak Testcontainers bypassed this hop"). `deploy/helmfile/scripts/tests/test_gateway_request_id.py` exists; no run recorded | 2026-10-05 |
| `TenantDatabaseOwnershipTest` and the migrations tests | Not yet run | 2026-09-29 |
| UI release gate "Final Closure Evidence" | Empty. Requires contract/client drift checks, Helm lint/schema/render for three tiers, one clean full regression, clean K3s deployment and recovery, and authenticated tenant-routed browser acceptance for FEI, FOWF Studio and the Dashboard | undated |
| Branding K3s two-tenant test; branding REST tests "with real HTTP, including the unknown-host and wrong-file cases" | Planned | 2026-10-12 |

**Identity fixtures (2026-10-05):**
- AuthZEN `TenantClientCredentials*`;
- FOWF `CapabilityPackLoaderTest`, `TenantOrganizationReconcilerTest`, `HttpKeycloakOrganizationAdminTest`, `TenantProvisioningMainTest`, `TenantAdministrationServiceRealPostgresqlKeycloakIntegrationTest`;
- FDS `DataStreamingCapabilityPackTest`;
- FEI `PublishedWorkflowRevisionResolverTest`;
- the Keycloak chart Python tests;
- the platform Python identity, sync and reload suites;
- ESO integration, which needs `IDENTITY_TEST_CONTEXT` and a disposable cluster.

**Identity acceptance cases (2026-10-05):**

| ID | Case |
|---|---|
| ID-01 | Empty realm, one complete install |
| ID-02 | Two registered tenants |
| ID-03 | Concurrent tenant-specific tokens |
| ID-04 | Non-member or mismatched tenant |
| ID-05 | Interrupted secret adoption/rotation |
| ID-06 | Repeated reconciliation |
| ID-07 | Composed Secrets |
| ID-08 | API-created FEI tenant |
| ID-09 | Configuration changes |
| ID-10 | Boolean and inventory validation |

---

## 15. Required acceptance scenarios not yet evidenced

These scenarios still govern. Where a scenario is covered by a 2026-10-09 matrix, that coverage predates the current code.

**FEI (TG §4):**
- an authenticated public request;
- premium CSV, schema, mapping and index JSON supplied separately, with a non-default value overridden to detect ignored fields;
- all record IDs and rejected counts;
- workflow terminal success, FEI revision status and OpenSearch documents ("READY or HTTP 202 alone is insufficient");
- index ownership, strict mappings, `source_data` and the `_source` round trip;
- screening of a person and of an organization (blank FIRST-NAME), plus a negative;
- simple, correlated and resolution ingestion, with correlation key/cardinality errors;
- full/delta, updates/deletes, activation, rollback, retention, idempotent retries, concurrent triggers, tenant isolation;
- malformed inputs, worker failure, callback retry, restart and partial progress;
- Studio through the real API;
- a nine-cell matrix.

**FEI operation inventory and provider ledger (AUD):**
- Every operation in the canonical FEI OpenAPI document (count derived from the document, not the old "83") needs real HTTP evidence, with its error cases, on all three frameworks.
- The provider ledger covers all 35 WorldCheck columns and 42 Customer Master fields.
- Latin-1 versus CP1252 is still to be confirmed by the supplier.
- Committed fixtures must be synthetic.

**FOWF lifecycle (TG):**
- real publication and admission;
- invalid publication, duplicates, pinned engine, stale versions;
- pause/resume/cancel;
- query, history and errors;
- human-task races;
- denial and cross-tenant cases.
- Recovery timestamps commands after fixture startup and verifies committed firing history: "Never silently reset a deadline on restart."
- The adapter is reached by the real workflow, with a live PDP denial.

**Overflow (TG; AUD):**
- In all 9 cells: real worker/actor transport plus shared object storage plus the durable backend.
- Cover artifact storage, materialization, history, tenant isolation, oversized rejection, storage failure, fresh-process recovery and modified-object digest failure. Use real serializers in the transition-integrity test.
- Test 32,767 / 32,768 / 32,769 bytes explicitly, in serialized bytes, including multibyte UTF-8. "A 1 MB response threshold does not automatically make a 64 KiB start command legal."
- "Record written, compiled and executed results separately per cell."

**HTTP security matrix (AUD), per framework:**
- no token on any protected path, including unknown paths → 401 Problem with a Bearer challenge;
- invalid token → 401;
- valid token on an unknown path → 404 Problem;
- designated health endpoints open.

Token negative cases: missing, invalid or expired tokens; wrong audience; the same signing key with the wrong issuer; valid-but-denied identities; inactive organization; tenant spoofing; cross-tenant access. Valid requests must keep working after a rejection.

**FDS (TG):**
- Start through the launcher HTTP API, both direct and via FOWF, bounded and continuous, on Pekko, Kafka and Spark.
- Spark staging plus delivery Jobs.
- Malformed rows, key policy, read-committed frontiers, sink failure, timeout/cancel, retry/idempotency and drain.
- "An unknown-revision 404 is not dispatch evidence."

**FDE (TG §9):**
- real gRPC on each packaged server, with shared PostgreSQL/Keycloak and frameworks run sequentially;
- tenant registry via provisioning, and the audience mapper;
- two actors in different tenants with the same ruleset names and keys;
- evaluate/manage/admin separation, with denials leaving state unchanged;
- golden evaluations, pinned versions, cache lifecycle and health;
- the FOWF adapter path tested separately, across the nine FOWF cells.

**Static boundary inventory** (INV, 2026-10-07: "Static candidates, not verdicts or execution evidence"):
- Candidates: FEI 33, FOWF 55, FDS 13, with signals for repository construction, service construction, manual persistence bootstrap, authorization double and raw container construction.
- The repository count field reads FEI 139, FOWF 384, FDS 128; its meaning is unstated.
- The required treatment per group (TG §2) is below. No triage status after 2026-10-07 is recorded.

| Product | Group | Required action |
|---|---|---|
| FEI | Four `domain/service/impl` tests | Keep useful business assertions. Run them through production-managed services on all three frameworks; add authenticated HTTP scenarios; verify interceptor-owned commits |
| FEI | Nineteen `domain/repository` tests, `DomainEntityGraphIntegrationTest`, `DomainTestSupport` | Retain narrowly identified storage contracts; consolidate infrastructure; do not use this harness for application-service acceptance; map duplicate scenarios to managed-service/HTTP contracts |
| FEI | `ReferencePopulationApplicationServiceTest`, `…IngestionApplicationServiceTest`, `…RetentionApplicationServiceTest`, `…IndexingAndComparisonIntegrationTest`, `IngestionPipelineFinderConcurrencyTest` | Preserve the concurrency and index-integrity assertions; add API admission, request validation, tenant/auth failure and full/delta lifecycle acceptance |
| FEI | `EntityApplicationServiceTest`, `AssertionApplicationServiceTest`, `ResolutionCascadeServiceTest` | Resolve real services through DI; exercise entity/assertion/investigation actions over REST; check commit/reload and dependent records |
| FEI | `PopulationScreeningExportApplicationServiceTest` | Keep the export-format tests; add authenticated HTTP export, status, content and denial checks |
| FEI | `CustomerMasterIngestionWorkerIntegrationTest`, `SimpleSourceIngestionWorkerProgressCheckpointingIntegrationTest`, `PopulationScreeningWorkerIntegrationTest` | Use packaged workers and real callback contracts; keep deterministic checkpoint/failure tests; add API-originated runs |
| FEI | `IngestionPipelineFowfK3sVerificationTest` | It uses a manually seeded revision and a permit-all direct launcher. Add/execute FEI trigger API acceptance with actual credentials, workflow completion and persisted results |
| FEI | `ScreeningMatchServiceIntegrationTest`, `CanonicalScreeningIngestionIntegrationTest`, `HashingTextEmbeddingServiceTest` | Keep. They do not replace API acceptance |
| FDS | `IngestionRunResourceTest`, `WorkflowRunResourceTest` | Label retained checks as unit tests; add real HTTP admission/denial contracts in all launcher deployment leaves |
| FDS | `DirectIngestionLauncherTest`, `WorkflowIngestionLauncherTest` | Keep focused contracts; obtain production-managed launchers; require REST acceptance separately |
| FDS | Four `DirectIngestionLauncher*RealImageIntegrationTest` classes | Preserve the dispatch assertions; make equivalent acceptance originate from the launcher REST API with real AuthZEN identity |
| FDS | `DirectIngestionLauncherKeycloakIntegrationTest` | Keep it as a scoped security integration; add HTTP JWT/claims/routing coverage |
| FDS | Launcher deployment matrix tests | Audit every cell for terminal completion, persisted documents, fixture authorization and current images |
| FDS | Pekko/Kafka/Spark engines, runners, field mapping, job launcher | Keep direct runtime contracts (retries, malformed rows, key policy, drain, commit/ack ordering); tie representative scenarios to API-originated Jobs |
| FDS | `JpaPagingSourceTest`, `PekkoJpaSourceIntegrationTest` | Keep as real adapter contracts with shared database fixtures |
| FOWF | `WorkflowDefinitionManagementServiceTest`, `WorkflowExecutionManagementServiceTest`, `HumanTaskApplicationServiceTest` | Keep pure decisions as named unit tests; add managed-service integration and authenticated REST lifecycle tests |
| FOWF | `WorkflowManagementResourceTest`, `HumanTaskResourceTest`, `AuthorizationSmokeSpringTest` | Add real framework HTTP tests; remove misleading acceptance claims |
| FOWF | `WorkflowDefinitionRepositoryTest`, `JpaWorkflowExecutionRepositoryTest`, `JpaWorkflowExecutionProjectionStoreTest` | Keep storage invariants; move business wiring to production DI; add REST admission/history/query verification |
| FOWF | `AsyncTaskUpgradeAndCommandRecoveryTest` | Keep the upgrade/recovery contract, historical schema fixture and restricted-role checks; prove API-admitted commands recover with unchanged identity; do not rewrite applied migrations |
| FOWF | `HumanTaskConcurrencyTenancyContractTest`, `JpaHumanTaskRepositoryContractTest` | Preserve the barrier-based storage tests; add API races |
| FOWF | `HumanTaskManagement*ApiParityTest` | API operations perform the business mutations; use managed services or migration fixtures for prerequisites |
| FOWF | `KafkaEngine*BindingTest`, `OksCloudEventIngressResourceTest`, `OksCloudEventIngressResourceKafkaIntegrationTest`, `CloudEventIngressResourceTest`, `CloudEventIngressResourceResponseTest` | Add authenticated real HTTP ingress/command acceptance for every framework |
| FOWF | `AuthzenAuthenticatedActorProviderTest`, `AuthorizedOperationExecutorsTest`, `OperationAuthorizationTest`, `AuthzenOperationSecurityResolverTest`, adapter runtime tests | Keep the deterministic contracts; add real Keycloak/AuthZEN workflow-to-adapter runs |
| FOWF | `CloudEventOutboxHandlerTest`, `SubscriptionProjectionHandlerTest` | Require durable outbox/subscription restart and duplicate-delivery contracts |
| FOWF | `HibernateSessionWorkflowExecutionQueryRepositoryTest`, `JpaSubworkflowPlanResolverTest` | Keep; add end-to-end workflow evidence |
| FOWF | Subscription repositories, `TenantAwareCloudEventSubscriptionRoutingTest`, `CloudEventSubscriptionRepositoryParityTest`, `RealCloudEventOutboxRecoveryTest`, `RealSubworkflowRecoveryTest` | Retain both backends; consolidate raw containers; add REST-admitted workflow recovery; do not invent REST endpoints for internal stores |

Raw-container FOWF candidates are a consolidation item: `OpenWorkflowCassandraMigrationTest`, `OpenWorkflowMigrationsJobTest`, `KafkaHumanTaskTransportTest`, the Kafka engine `Oks*IntegrationTest` classes, `DurableKafkaStreamsRestorationTest`, the Pekko persistence contract tests and the framework binding tests.

---

## 16. Known failures, gaps and untested combinations

| # | Area | Failure or gap | First recorded | What closes it |
|---|---|---|---|---|
| F1 | FDS / FOWF | `FowfStartInputRuntimeMatrixAcceptanceTest` failed 9/9 (causes fixed in `92a7b1c2`) | 2026-10-12/13 | FOWF T-02 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F2 | FOWF cross-engine | `LifecycleCloudEventParityTest` 8/15 | 2026-10-13 | FOWF T-13 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F3 | FOWF Kafka | `OksCloudEventIngressResourceTest`: 202 with 0 committed records | 2026-10-12/13 | FOWF T-14 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F4 | FOWF Kafka wire | `DurableWireCompatibilityTest`: Start command digest changed | 2026-10-12/13 | **Decided: drain before upgrade**; pin updated in `92a7b1c2`, no dual-read. Rule: FOWF F-03 in [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) |
| F5 | FOWF Kafka | `OksDefinitionAdmissionTopologyTest` fixture fix applied, not confirmed run | 2026-10-12/13 | FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F6 | FOWF Kafka | `OksRestorationIntegrationTest` stopped before running | 2026-10-13 | FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F7 | FOWF Kafka | `OksCloudEventPublicationIntegrationTest` never run | 2026-10-13 | FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F8 | FOWF | Updated Pekko lifecycle mapper tests and Kafka engine binding tests not run | 2026-10-13 | FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F9 | FDS | Launcher coverage rules: application branches 0.65 < 0.85; Spring service lines 0.33 < 0.85 | 2026-10-12/13 | Apply and run the drafted launcher tests; re-measure |
| F10 | FOWF | Aggregate gate not passed (newest 2026-10-11: 79.7% branches, 22 missed classes; `$Cursor` class-ID mismatch) | 2026-10-09/11 | FOWF T-12 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F11 | FEI | Step D deep integration tests never run; the `MonitoringScreeningHit` double `@PrePersist` reached production | 2026-10-13 | Run the suites on Q/S/M; fix what fails |
| F12 | FEI | R1-R18 population scenarios not executed on any framework | 2026-10-13 | Execute in the integration suites on Q/S/M and the affected worker runtimes |
| F13 | FEI | `ReferencePopulationCrudResourceTest` Quarkus: last recorded run skipped 4/4 (Keycloak C2 crash) | 2026-10-09 | Run with the Keycloak JVM flag (section 5.3); four real passes |
| F14 | All packaged matrices | FOWF basic and overflow, FEI, FDS and FDE matrices predate the 2026-10-11 to 2026-10-13 engine changes | 2026-10-09 | Rebuild images and re-run every cell |
| F15 | FOWF | Start-input fixtures never executed; scheduled and CloudEvent starts bypassed the placement rule (fixed 2026-10-13, uncommitted) | 2026-10-10; traced 2026-10-13 | FOWF T-02 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) and F-14 in [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) |
| F16 | FOWF | Nested-reply decoder (`1a60efd6`) never packaged or matrix-verified | 2026-10-09 | FOWF F-23 in [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) |
| F17 | FEI | Classification-merge image not verified | 2026-10-09 | Build the image and run the matrix |
| F18 | Image skew | No single image set certifies all fixes (FEI used `decimal-transition`/`control-envelope`; FDS/FDE used `activity-projection`/`grpc-unary-result`; FOWF overflow Q-K used pre-observed-failure execution management; FDE used released 1.1.0) | 2026-10-09 | One consistent image set for every matrix |
| F19 | FOWF | 5 of 14 skipped component modules have no recorded coverage | 2026-10-09 | FOWF T-16 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F20 | FOWF | Repository and SQL setup in human-task and execution HTTP suites | 2026-10-07/08 | FOWF T-17 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F21 | FOWF | Executors other than HTTP/gRPC/Kubernetes Job lack runtime evidence | 2026-10-03 | FOWF T-09 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F22 | FOWF | Tenant administration POST untested; engine HTTP per-host subclasses; operationId inventory | 2026-09-24 / 2026-10-03 | FOWF T-03 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) |
| F23 | Studio (FOWF, FEI, Dashboard) | No real-API browser acceptance | 2026-10-09 | Authenticated tenant-routed browser runs |
| F24 | FEI | No public merge REST endpoint; merge evidence is application-layer only | 2026-10-09 | Product decision; label accordingly |
| F25 | FEI | Hundreds-of-GB ingestion not executed | 2026-10-09 | Scale benchmark |
| F26 | FEI | Screening regression gates (`RealDataScreeningRegressionTest`, `FullExportScoringBacktestTest`) not implemented/executed in FEI | 2026-08-08 | Port and run both gates |
| F27 | FEI | Information extraction: no tests executed | 2026-10-12 | Tests on Q/S/M and the three FOWF runtimes |
| F28 | FDS | Applicability table, direct continuous invocation, Spark staging axis, malformed-row/frontier/sink-failure/drain evidence | 2026-10-09/10 | Audit and add cells |
| F29 | FDE | The rehabilitation acceptance list has no evidence | 2026-10-10 | One test per item |
| F30 | Agent OS | No recorded test run of any kind | — | Run the existing suites; add per-framework HTTP and engine coverage |
| F31 | Platform operations | No recorded test run; no Spring/Micronaut tests; chart deploys only Quarkus | 2026-10-06 | Add S/M tests; run them; central framework selection |
| F32 | Coverage collection | `quarkus-jacoco` missing in `jpa-quarkus`, platform-operations Quarkus binding, Agent OS Quarkus actor binding | 2026-10-12/13 | Apply the drafted changes |
| F33 | JVM | All runtime evidence is C1-only; no normal-JVM run; Temurin 25.0.4.1 crashes unexplained | 2026-10-07/09 | Normal-JVM verification run |
| F34 | Deployment | 18-combination selection rendering not executed; identity fixtures and ID-01..ID-10 not executed; gateway hop missing from K3s matrix | 2026-10-05/06 | Execute |
| F35 | Deployment | No test run against a deployed installation ("deployed verified" unattained) | 2026-10-13 | Post-deploy verification suite |
| F36 | OpenSearch tests | Ad hoc `OpenSearchContainer` in entity-matching and FEI screening tests | 2026-09-06 | Move to the shared OpenSearch module |
| F37 | FOWF / FEI | Callback authentication null plan when `with.authentication` is absent | 2026-10-08 | FOWF F-21 in [FOWF backlog](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog) |
| F38 | FDE | Uncommitted `ProtoStructMapperTest`/conformance changes listed in HO | 2026-10-09 | Check git; commit or discard |

---

## 17. Open testing work

| # | Work | Product | Depends on | Closes |
|---|---|---|---|---|
| W1 | Rebuild all product images from current code (required before any K3s coverage or matrix run) | All | — | F14, F16, F17, F18 |
| W2 | Cross-engine parity re-run: FOWF T-13 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F2 |
| W3 | Stopped or unrun Kafka and Pekko suites: FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F6, F7, F8 |
| W4 | `OksCloudEventIngressResourceTest`: FOWF T-14 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F3 |
| W5 | Start command migration: **decided, drain before upgrade** (`92a7b1c2`); `DurableWireCompatibilityTest` pin updated | FOWF | — | F4 (closed) |
| W6 | `OksDefinitionAdmissionTopologyTest` re-run: FOWF T-15 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F5 |
| W7 | Start-input matrix and fixtures; scheduled and CloudEvent start paths: FOWF T-02 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) (with FOWF F-14) | FOWF / FDS | W1 | F1, F15 |
| W8 | FOWF basic and overflow nine-cell matrices: FOWF T-01 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | W1 | F14 |
| W9 | Run the FEI step D deep integration tests on Q/S/M; fix failures | FEI | — | F11 |
| W10 | Execute population scenarios R1-R18 | FEI | W9 | F12 |
| W11 | Run `ReferencePopulationCrudResourceTest` on Quarkus with the Keycloak JVM flag | FEI | — | F13 |
| W12 | Re-run the FEI complete nine-cell matrix | FEI | W1 | F14, F17 |
| W13 | Apply and run the drafted FDS launcher tests (`LaunchRequestsTest`, `LauncherProblemMappersTest`, `LauncherSpringApplicationTest`, planner Spark preconditions) and executor/API tests | FDS | — | F9 |
| W14 | Re-run the FDS 39-cell matrix; audit the applicability table; add direct-continuous and failure-mode cells | FDS | W1 | F14, F28 |
| W15 | Re-run the FDE 15-cell matrix on current images; add the rehabilitation acceptance tests | FDE | W1 | F14, F29 |
| W16 | Apply the drafted `quarkus-jacoco`, JPA datasource/testcontainers/contract tests | Shared, Agent OS, platform ops | — | F32 |
| W17 | Write the finite, behaviour-based regression plan (HO R3) and the missing fixtures (R4) from the gap classes in 6.5 (FOWF part: FOWF T-12) | FOWF, FEI, FDE | — | F10 |
| W18 | Pass the 85/85/0 gates with fresh reactor and K3s coverage data; align class IDs (`$Cursor`, `DecisionEngineQuarkusBinding$TransactionalRulesets`) | FOWF, FEI, FDS, FDE | W1, W17 | F9, F10 |
| W19 | Full operation × error-case contracts per framework with the operationId diff check, including tenant administration POST (FOWF part: FOWF T-03) | FOWF, FEI | — | F22 |
| W20 | Repository/SQL setup in FOWF HTTP suites: FOWF T-17 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F20 |
| W21 | Real-system executor tests: FOWF T-09 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F21 |
| W22 | Real-API browser acceptance for FOWF Studio (FOWF T-10), FEI Studio and the Dashboard | Studio | W1 | F23 |
| W23 | Port and run the two screening regression gates in FEI | FEI | — | F26 |
| W24 | Information-extraction tests on Q/S/M and the three FOWF runtimes | FEI | — | F27 |
| W25 | Run Agent OS suites; add per-framework HTTP and engine coverage | Agent OS | — | F30 |
| W26 | Add Spring and Micronaut tests for platform operations; run all | Platform ops | — | F31 |
| W27 | Normal-JVM (C2) verification run | All | — | F33 |
| W28 | Execute the 18-combination rendering, selection regressions, identity fixtures, ID-01..ID-10 and the gateway hop | Deployment | — | F34 |
| W29 | Post-deploy verification against an installed environment | All | Deploy | F35 |
| W30 | Hundreds-of-GB ingestion benchmark | FEI | — | F25 |
| W31 | Move ad hoc OpenSearch containers to the shared module | entity-matching, FEI | — | F36 |
| W32 | Triage the static boundary inventory groups (section 15) | FEI, FDS, FOWF | — | — |
| W33 | Unaccounted skipped FOWF component modules: FOWF T-16 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF | — | F19 |
| W34 | Callback-authentication closure: FOWF F-21 and T-09 in [FOWF testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work) | FOWF / FEI | — | F37 |
| W35 | Independent build-cache acceptance (cached vs uncached artifacts and images) | Platform | — | — |

---

## 18. Superseded and conflicting statements

| # | Statement | Superseded by / resolution |
|---|---|---|
| 1 | FOWF gate "85% line / 75% branch / 0 uncovered classes" (memory, 2026-10-03; old per-product POMs) | 85% lines / 85% branches / zero missed classes, platform-wide (user decision 2026-10-07; JBP; AUD 2026-10-10: "Old 75% branch text is superseded") |
| 2 | "Tests only through REST APIs" (memory, 2026-10-07) vs "both direct tests and REST tests" (TG 2026-10-07; AUD 2026-10-10) | AUD is newest; reconciled in section 2.4 |
| 3 | FEI Quarkus `ReferencePopulationCrudResourceTest` 4/4 passed (J-api, 2026-10-09 18:17) vs 4 skipped (HO, 2026-10-09) | Both happened. The later run skipped because a Keycloak C2 crash marked the Quarkus boot failed (memory, 2026-10-10). Last recorded state: skipped. Class changed 2026-10-13 and not run |
| 4 | "All 15 of 15 capability×framework combinations … live-verified" (memory, 2026-09-24) | Overclaimed per memory 2026-10-03 (narrowed tests; Spring definition-management 500s behind a 401-only smoke test). Treated as history |
| 5 | TG: second combined Kafka run interrupted, "no final suite result" | RMS: third combined run 94/94 (2026-10-08) |
| 6 | TG §9: "No FDE full-framework pass is claimed"; RMS (2026-10-08): overflow and FDE fixtures "Incomplete" | J-fde 15/15 and J-ovf 9/9 (2026-10-09) |
| 7 | RMS: "shared API component deduplication" outstanding | J-api (2026-10-09) |
| 8 | RMS "current status" prose ("rerun is queued", "Attempt N is running") | HO (2026-10-09) final state |
| 9 | Skipped FOWF modules: 14 (RMS) vs 9 (RMS later; J-unb) | Both recorded; 5 unaccounted (F19) |
| 10 | FOWF HTTP 342 (2026-10-07) vs 345 (2026-10-08) | Different runs; not merged |
| 11 | Historical FEI 27-invocation table (2026-10-08) | J-feiC nine-cell matrix (2026-10-09) |
| 12 | Population plan §15: "Remaining: step D" (2026-10-13); memory: step D done but uncommitted (2026-10-13 02:23Z) | Step D committed in FEI `d9b161b` (2026-10-13 01:16 local). Its commit message says the deep tests "run on Quarkus, Spring and Micronaut", meaning where they run; they have **not** been executed (session record) |
| 13 | Overnight findings heading "Drafted, applied after the discovery pass" | Only the three OpenWorkflow test fixes were applied (`92a7b1c2`, 2026-10-13); the other drafts (JPA, testcontainers-JPA, `quarkus-jacoco`, FDS launcher and component tests) have not been applied (session record, 2026-10-13) |
| 14 | "Nothing is deployed" in older documents | Software has been deployed (AUD decision 6; production deploy 2026-10-13). No test evidence against a deployed installation |
| 15 | H2-backed tests and `MockConsumer`/`MockProducer` in the first FEI rebuild (memory, 2026-08-27; that memory file is marked SUPERSEDED) | Never H2 (2026-09-14); Mockito/mocking banned (2026-09-20) |
| 16 | Human-task HTTP 5/5/5 (memory, 2026-09-06) | 6/6/6 (TG, 2026-10-08) |
| 17 | "Do not run any mvn commands" (memory, 2026-09-30) and "compile tests without running them until explicitly authorized" (2026-10-06) | The user authorized test runs for the testing program (VC 2026-10-07; memory 2026-10-10). The one-build-at-a-time and lock rules still apply. Who runs a given build is agreed per session |
| 18 | FEI REST-only handover | Superseded by TG §1 (2026-10-07) |
| 19 | "Earlier 26/26 restoration evidence" | Used the old broker fixture; "not interchangeable" (CB, 2026-10-08) |
| 20 | Start command wire pin "needs the user's decision" between drain and dual-read (overnight findings, 2026-10-13) | Decided: drain before every engine upgrade; pin updated in FOWF `92a7b1c2` (2026-10-13), no dual-read reader |
| 21 | FOWF coverage "failed on 2026-10-09: branches 0.79, 68 missed classes" as the latest figure | Newer: 2026-10-11 run 34, 92.9% lines / 79.7% branches / 22 missed classes (still failing) |
| 22 | FOWF matrix detail in section 7 of this document | Moved to [FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md) on 2026-10-13 |

---

## 19. Sources

**Testing documents and evidence files** (moving to `docs/archive/`):

| Key | File | Date |
|---|---|---|
| TG | `testing-guide-for-claude-2026-10-07.md` | 2026-10-07, supplements to 2026-10-09 |
| RMS | `runtime-matrix-status-2026-10-08.md` | 2026-10-08 to 2026-10-09 22:31 EDT |
| HO | `claude-testing-completion-handover-2026-10-09.md` | 2026-10-09, addendum 2026-10-10 |
| AUD | `independent-parity-audit-request-2026-10-10.md` | 2026-10-10 (a request; no audit recorded) |
| INV | `testing-boundary-inventory-2026-10-07.json` | 2026-10-07 |
| J-basic | `fowf-basic-runtime-evidence-2026-10-09.json` | 2026-10-09 |
| J-ovf | `fowf-overflow-runtime-evidence-2026-10-09.json` | 2026-10-09 |
| J-act | `fowf-activity-projection-evidence-2026-10-09.json` | 2026-10-09 |
| J-wire | `fowf-wire-recovery-evidence-2026-10-09.json` | 2026-10-09 |
| J-tail | `fowf-tail-corrections-evidence-2026-10-09.json` | 2026-10-09 |
| J-unb | `fowf-unblocked-components-evidence-2026-10-09.json` | 2026-10-09 |
| J-feiC | `fei-complete-runtime-evidence-2026-10-09.json` | 2026-10-09 |
| J-feiCorr | `fei-component-corrections-evidence-2026-10-09.json` | 2026-10-09 |
| J-fde | `fde-runtime-evidence-2026-10-09.json` | 2026-10-09 |
| J-fdeG | `fde-grpc-lifecycle-evidence-2026-10-09.json` | 2026-10-09 |
| J-fds | `fds-runtime-evidence-2026-10-09.json` | 2026-10-09 |
| J-api | `shared-api-ownership-evidence-2026-10-09.json` | 2026-10-09 |

The JSON files hold the SHA-256 hashes of every log and report. They are not copied here.

**Other documents whose testing content is incorporated:**

| Key | File |
|---|---|
| VC | `version-consolidation-2026-10-07.md` |
| CB | `checkpoint-backup-2026-10-07.md` |
| ID | `identity-implementation-handover-2026-10-05.md` |
| XR | `cross-repo-handover-2026-09-29.md` |
| SCO | `shared-contract-ownership-2026-10-08.md` / `.json` |
| JBP | `java-build-platform.md` |
| MBC | `maven-build-cache.md` |
| DEP | `deployment-configuration-and-cutover.md` |
| IDN | `identity-service-clients-and-tenant-membership-design-2026-10-05.md` |
| UIAUD | `production-ui-delivery-architecture-audit.md` |
| MAN | `forwardmeasure-engineering-manifesto.md` |
| BRAND | `tenant-branding-design-2026-10-12.md` |
| PC | `population-contract-and-membership-implementation-plan-2026-10-12.md` |
| IE | `information-extraction-agent-studio-handover-2026-10-12.md` |
| SO | `entity-intelligence-screening-optimisations-handoff.md` |

**Newer records:**
- The overnight tested-build findings of 2026-10-12/13.
- The session record of the 2026-10-13 testing changes.
- The user's standing rules in project memory (2026-09-06 to 2026-10-13).
- Read-only checks of test classes and git history in the `forwardmeasure-*` repositories on 2026-10-13.
