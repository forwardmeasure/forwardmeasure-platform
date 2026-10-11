# Request to a new Claude or Codex agent: independent parity and architecture audit

**Requested by the product owner. Prepared October 10, 2026.**

This is a standalone assignment for a fresh agent, not a claim that an audit has been performed. Give the same request to Claude and Codex if two independent reviews are desired. Their agreement is not a substitute for code and execution evidence.

## 1. Assignment and reason

Analyze the actual ForwardMeasure code, contracts, framework bindings, tests, packaging and deployment configuration to determine whether the supported product behavior has **full parity**. The primary focus is FOWF across Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra, each on Quarkus, Spring Boot and Micronaut. Include the FEI, FDS and FDE integration paths and the architecture requirements below. Do not limit the audit to the latest defect or the tests that already exist.

A previous agent claimed “100% parity” after selected matrices passed. That claim was unsupported. A concrete counterexample then appeared: Kafka workflow start input above 32,768 serialized JSON bytes was rejected by `DataReferences.inline(start.input())`, while Pekko accepted it. Protocol-response overflow tests had passed without establishing oversized **start-input** behavior. This is precisely the class of omission this audit must find.

Do not inherit “done,” “complete,” “all fixtures written,” “100% parity,” or a green matrix label as a fact. Independently establish the denominator: **which requirements, operations, variants, boundaries and runtime combinations exist, and which have actually been checked?**

The objective is full parity of the required observable contract, not identical internal implementations. Finite tests cannot prove correctness for every possible input, fault or schedule. That limitation is not permission to quietly narrow the contract. Enumerate the full known required surface, inspect every implementation path, expose unverified cases, and attach any eventual full-parity statement to an explicit contract revision and evidence baseline. “No known difference in these tested cases” is not an unqualified 100% claim.

## 2. Authority, scope and coordination

For this assignment, start with **read-only investigation and audit deliverables**. Write the reports and a concrete missing-fixture/repair plan. Do not change production code, tests, POMs, contracts or deployed configuration merely because this document discusses a repair. Do not execute tests, start builds, build/publish images, deploy, commit or push unless the user authorizes that phase. Reading existing source, POMs, manifests and logs does not require another permission question.

This assignment is to audit and report. It does not itself authorize implementation changes or test execution, and it does not cancel or pause the broader engineering work. Follow the user's current instructions for subsequent work and coordination with other agents. Honor authorization already given in your session without asking for it again.

Workspace root: `/home/pn/Documents/code/forwardmeasure`.

In-scope repositories:

- `forwardmeasure-openworkflow`: primary engine/common-plane/framework audit.
- `forwardmeasure-entity-intelligence`, `forwardmeasure-data-streaming`, `forwardmeasure-decision-engine`: business contracts and integration paths using those runtimes.
- `forwardmeasure-platform`: shared API/data contracts, policy/BOM/build configuration, common handlers, framework support and deployment selection.
- `forwardmeasure-jpa`, `forwardmeasure-database-migrations`, `forwardmeasure-object-storage`, `forwardmeasure-testcontainers`, `forwardmeasure-authzen`: shared boundaries used by the above.
- `helm-charts` and `forwardmeasure-platform-operations`: relevant packaging/deployment configuration and existing verification infrastructure. Read non-secret files only.
- Other sibling libraries only when a traced production dependency requires them; identify why they are relevant.

Preserve all existing uncommitted changes. Record repository HEAD, branch and dirty **path names** before analysis, then distinguish changes by other sessions from this assignment. Never reset, stash, format, stage or commit another agent's work. Do not read credentials, secret values, Terraform state/tfvars, crash-dump credentials or unrelated customer data.

The user previously authorized a narrow read-only legacy ingestion comparison despite a local blanket prohibition on `data-fabric`. That authorization is scoped to the explicitly cited implementation and supporting schema/mapping files in the original FEI handover, starting at:

`data-fabric/data-fabric-components/java/data-fabric-services/entity-intelligence-framework/entity-intelligence-ingestion-service`

Use it only when checking FEI/FDS ingestion semantics. Do not scan the rest of `data-fabric`, edit it, or introduce it as a production dependency. Do not copy the legacy compiled target-model limitation back into the repaired contract-driven ingestion.

If two agents work concurrently: produce independent reports first, using `claude-` and `codex-` prefixes; do not overwrite each other's ledgers. Reconcile findings afterward. One owner coordinates any subsequently authorized source changes and the shared build lock. Do not spawn additional agents merely because this brief mentions two reviewers.

## 3. Read these sources before drawing conclusions

Read applicable `AGENTS.md` files first. Paths below are relative to the workspace root. Inspect the implementation cited by a requirement rather than stopping at its prose.

| Source | Use |
|---|---|
| `forwardmeasure-platform/docs/forwardmeasure-engineering-manifesto.md` | Shared engineering principles; apply later explicit decisions where they supersede defaults |
| `forwardmeasure-openworkflow/PROJECT_MANIFESTO.md` and `docs/architecture-design.md` | Full requirement inventory, including rule IDs ARCH, DSL, DEF, RES, PUB, TEN, SEC, API, FW, EXE, ENG, PEK, KS, ADP, DATA, HT, PER, MIG, WIRE, STU, DEP, TEST and decision log |
| `forwardmeasure-openworkflow/docs/engine-framework-parity-ledger.md` and `docs/engine-construct-gap-audit.md` | Historical hypotheses, known sibling paths and audit leads; not current proof |
| `forwardmeasure-openworkflow/docs/request-contract-and-api-security-parity-handoff-2026-10-01.md` | Request validation, generated interfaces, binding, authentication, errors and startup history |
| `forwardmeasure-openworkflow/docs/human-task-design.md` | Human-task lifecycle, concurrency and maker-checker semantics |
| `forwardmeasure-openworkflow/docs/rehabilitation/claude-testing-instructions-2026-10-06.md` | Recovery, fault injection, fixtures and migration exceptions |
| `forwardmeasure-platform/docs/testing-guide-for-claude-2026-10-07.md` | Settled layered testing policy, real infrastructure and evidence rules |
| `forwardmeasure-platform/docs/shared-contract-ownership-2026-10-08.md` | Canonical owner of each contract family and generated snapshots |
| `forwardmeasure-platform/forwardmeasure-platform-data-contracts/src/main/resources/contracts/catalog.json` | Exact canonical provider source/mapping/destination/index resource paths; its status strings are dated evidence, not current verification |
| `forwardmeasure-platform/forwardmeasure-platform-data-contracts/provider-field-semantics-2026-10-08.md` and `forwardmeasure-platform/forwardmeasure-platform-data-contracts/README.md` | Field-by-field WorldCheck/State Street dispositions, raw preservation, duplicate headers, matching limits and admitted-contract compatibility; recheck dated execution-status prose |
| `forwardmeasure-entity-intelligence/docs/architecture-design.md` | FEI architecture requirements |
| `forwardmeasure-entity-intelligence/docs/codex-reference-population-ingestion-repair-handover-2026-10-06.md` | Original confirmed defects, approved behavior, scoped legacy reference and acceptance requirements; distinguish chronological updates |
| FEI `docs/reference-population-data-contracts-handover-2026-10-06.md`, `docs/reference-population-ingestion-hard-wired-target-model-2026-10-06.md`, `docs/worldcheck-contract-fixture-discrepancy-test-handover-2026-10-08.md`, `docs/full-file-ingestion-repair-2026-10-08.md` | Source/output/index separation, actual provider semantics, streaming and request overrides |
| `forwardmeasure-data-streaming/docs/architecture-design.md` | FDS model, planning, mapping, correlation, durability and invocation requirements |
| `forwardmeasure-data-streaming/docs/rfc9457-and-security-parity-2026-10-01.md` | FDS-specific request/error/security behavior across framework bindings |
| `forwardmeasure-decision-engine/docs/IMPLEMENTATION-SPEC.md` **and** `docs/rehabilitation/fde-rehabilitation-handover-2026-10-06.md` | FDE gRPC/core requirements and later security/version/cache/window amendments |
| `forwardmeasure-jpa/docs/architecture.md` and current JPA code | Shared service/repository/metamodel/transaction infrastructure; reconcile dated tenancy descriptions |
| `forwardmeasure-platform/docs/runtime-matrix-status-2026-10-08.md` and its linked JSON evidence | Exact historical tested cells, images, limitations and reports |
| `forwardmeasure-platform/docs/claude-testing-completion-handover-2026-10-09.md` | Known unfinished work, last observed failures, uncommitted changes and explicit unknowns |
| `forwardmeasure-openworkflow/docs/kafka-start-input-overflow-repair-2026-10-10.md` and `docs/kafka-start-input-overflow-compile-evidence-2026-10-10.json` | Latest narrow start-input repair and compilation-only evidence |

If a path has moved, locate its replacement and record it. Missing documentation is an audit finding, not grounds to invent a requirement or implementation.

### Precedence and known stale statements

Latest explicit product-owner decisions govern. The architecture documents record approved requirements but their dated BUG/PASS/NOT ASSESSED labels require current verification. Code establishes actual behavior; it does not silently redefine the intended contract.

Carry forward these resolved decisions:

- All **three** frameworks are required. An earlier “two frameworks” phrase does not narrow the matrix.
- Both direct focused tests **and** real API tests are required. Do not delete legitimate algorithm/storage tests under the superseded REST-only interpretation.
- Coverage is **85% lines and 85% branches**, with generated code exempt. Old 75% branch text is superseded.
- FEI schema-driven ingestion is active scope; older statements deferring it are superseded.
- The shared, versioned screening shape was selected. Do not reopen that decision. All unmapped provider fields are stored under `source_data`; only explicitly mapped extra fields are searchable.
- Software **has been deployed**. “Nothing is deployed” is false and never authorizes changing historical migrations, destructive resets or dismissing wire compatibility.
- Application query requirements are Criteria API plus generated JPA static metamodel, superseding the older general JPQL preference. Catalogued justified low-level exceptions remain possible.
- The shared manifesto prohibits repository dependencies in application/use-case orchestration, and FEI FARCH-07 agrees. FOWF PER-03's broader wording permits concrete application services to inject repositories. Apply the stricter orchestration boundary for this audit, explicitly record the source conflict, and distinguish domain/persistence services from orchestration by responsibility rather than class name.
- FDE's verified organization JWT/AuthZEN requirements supersede the original unsigned internal-caller trust model. FDE remains gRPC-only.
- Database-per-tenant with per-product functional schemas is the newer product tenancy requirement recorded in FOWF. Older shared-library documents describe schema-per-tenant infrastructure. Establish where each mechanism is used today; do not assert either topology platform-wide without tracing provisioning, registry resolution and connections.
- Framework-selective Maven profiles were deferred until testing is complete. They are not an excuse to omit a framework or a prerequisite for this audit.
- The October 8–9 shared-contract ownership decision supersedes older documents placing provider contracts in FEI or shared Problem/Violation Java generation in FOWF. Canonical provider resources and shared error types are platform-owned; product API/workflow contracts retain their respective owners.

Separate **approved requirements**, **documented implementation decisions**, **observed code behavior**, and **audit-derived scenarios** in the register. Boundary and fault scenarios below are checks of existing contracts, not authorization to invent new endpoints or features. Record a suspected missing capability without promoting it to an agreed requirement. Conversely, absence from the current spec does not erase a capability that an approved requirement demands: that may be a spec defect.

Record remaining contradictions with source/date/rule IDs, the implementation consequence and a focused decision question. Continue independent audit work while a genuine product decision is pending.

## 4. Required runtime matrix and scope denominator

FOWF has **two engine implementations and three runtime/persistence configurations**:

| Cell | Framework | Engine | Engine durability |
|---|---|---|---|
| Q-K | Quarkus | Kafka Streams | Kafka topics, state stores, changelogs, offsets |
| Q-PG | Quarkus | Pekko | PostgreSQL journal/snapshots/projections |
| Q-PC | Quarkus | Pekko | Cassandra journal/snapshots/projections |
| S-K | Spring Boot | Kafka Streams | Kafka topics, state stores, changelogs, offsets |
| S-PG | Spring Boot | Pekko | PostgreSQL journal/snapshots/projections |
| S-PC | Spring Boot | Pekko | Cassandra journal/snapshots/projections |
| M-K | Micronaut | Kafka Streams | Kafka topics, state stores, changelogs, offsets |
| M-PG | Micronaut | Pekko | PostgreSQL journal/snapshots/projections |
| M-PC | Micronaut | Pekko | Cassandra journal/snapshots/projections |

PostgreSQL in a historical `kafka-streams/postgresql` fixture label serves the relational product/common plane; it is not a fourth engine or Kafka's native workflow journal. Pekko/Cassandra still needs the product's relational tenant registry/common-plane services. Do not route those services to the Cassandra journal endpoint.

For each cell inspect the actual definition, execution, human-task, tenant, engine, operation-adapter and Studio distribution where relevant. A Spring test that starts Quarkus images is not a Spring runtime pass. A parameter name is not proof of the selected backend. List actual bindings, image coordinates/digests, runtime configuration and native persistence implementations.

The nine cells describe the supported coherent platform installations. Record the framework of every participating FEI/FDS/FDE service as well as FOWF; a Spring FOWF run against Quarkus-only product services does not establish an all-Spring installation. Do not silently substitute pairwise sampling for required combinations. Do not multiply the denominator by arbitrary mixed-framework installations that were never specified either: identify a contract basis before adding such combinations. Pure algorithm checks need not be relabeled as nine runtime executions; their shared evidence complements the applicable public runtime cases.

Separate three dimensions in every conclusion:

1. **Contract conformance:** does each implementation satisfy the intended behavior? Identical defects are not correctness.
2. **Behavioral parity:** do all applicable cells expose equivalent accepted input, result, error, security, persistence, recovery and operational behavior?
3. **Architecture compliance:** are layering, shared ownership, native engine separation, JPA usage and deployment construction correct even if a happy path works?

Build the capability inventory from specs, compiler plans, command/event types, actual runtime handlers, adapter registries, service APIs, configuration, framework startup and fixtures. Preserve declared-but-unimplemented capabilities as gaps. Unsupported submission must fail explicitly rather than hanging or silently dropping work, but rejecting a required feature does not satisfy the requirement to implement it.

## 5. Architecture requirements to audit

Assign stable IDs to every requirement and map the original design rule IDs into the ledger; do not replace the architecture designs with a shorter checklist that drops rules. Import all FOWF rule families listed above, all FEI families (`FARCH`, `FWF`, `FDOM`, `FSEC`, `FAPI`, `FPER`, `FDEP`), all FDS families (`SMOD`, `SINV`, `SMAP`, `SCON`, `SIND`, `SDUR`, `SSEC`, `STST`), and FDE/JPA requirements with section anchors where stable IDs do not exist. Record superseded clauses individually so that an outdated part does not discard valid neighboring requirements.

| Area | Required criteria |
|---|---|
| Dependency direction | Portable model/compiler/contracts and domain behavior remain framework-neutral. Bindings contain host integration, not duplicated domain logic. Identify cycles and engine/framework/JPA-implementation leaks. Follow declared ports and actual dependencies rather than only package names. |
| Engine separation | Kafka engine/adapter deployments must not carry or start Pekko; Pekko/shared common-plane modules must not depend on Kafka Streams engine internals. A neutral Kafka client for an approved transport/changelog is not automatically a Kafka Streams engine dependency. Inspect resolved runtime artifacts and startup paths. |
| Native durability | Kafka owns its topics/stores/changelogs and deterministic transitions. Pekko uses its sharded event-sourced model and supported journal/snapshot/projection plugins on both backends. Do not replace Pekko persistence with JPA or build a second scheduler/FSM/recovery subsystem to mimic Kafka. Shared public projections do not imply shared internal persistence formats. |
| One canonical implementation | One owner for compiler, identity, authorization, publication, common execution model, human tasks, Studio, schemas, clients and reusable connectors. No split packages/duplicate generated classes, parallel S3/GCS/OpenSearch clients or copied business implementations. Shared code must be truly wired into every consumer. |
| Contract-first HTTP | Public and internal HTTP capabilities have authoritative OpenAPI contracts, declared security, status/error shapes and generated server interfaces/models/clients. Resources implement those interfaces. Schemas, not Java patches, drive contract fixes. Differentiate a bad spec, bad generator/template/configuration and an implementation violation. |
| Generated code | Never edit generated Java/TypeScript directly. Regenerate from canonical contracts/templates; verify artifact/resource identity and client compatibility. Do not expose JPA entities or engine events as public DTOs. Use MapStruct for applicable typed edge mappings. |
| Legitimately dynamic ingestion | The approved ability to add providers without recompiling Java is an explicit exception to preferring generated fixed data models. Source parsing, mapping and destination validation remain runtime contract-driven. Do not reintroduce `TARGET_MODEL_CLASS`, provider-specific compiled target classes or reflection over generated `JSON_PROPERTY_*` fields. |
| Shared errors | Use platform-owned RFC 9457 Problem/Violation and common request/security/error handling with thin framework bindings. Audit 400/401/403/404/409/412/413/415/422/429/500/503 where applicable against the actual declared contract, not an invented blanket status policy. No service-specific duplicates or leaked exception/credential details. |
| Service/repository boundary | REST resources, consumers, workers, schedulers and orchestration/use-case services call domain service interfaces. Repository access belongs behind concrete domain/persistence service implementations. No hand-built repository graph in business orchestration. The stricter shared manifesto requirement applies to application services; document justified existing infrastructure exceptions explicitly. |
| JPA foundation | Application-owned relational persistence uses `forwardmeasure-jpa` service/repository/entity bases. No replacement Panache/Spring Data/Micronaut Data repository architecture. Do not apply this requirement to engine-native Kafka/Cassandra/Pekko plugin persistence or approved bulk connector internals. |
| Static metamodel | Fixed entity attributes and relationships use generated JPA metamodel classes and Criteria queries. Inspect string JPQL/HQL, native SQL/JDBC and string Criteria paths. Truly dynamic validated sort/filter paths are distinct from hardcoded entity field strings. Metamodel generation must work in actual builds. |
| Entity conventions | Follow the shared entity bases, `entity` package and Lombok conventions, auditing/ownership and optimistic-version semantics applicable to each entity. Verify generated metamodel attributes correspond to the actual mapped relationships. Do not impose an owner relationship on types whose approved base contract does not require one. |
| ORM access and transactions | EntityManager/Session/SessionFactory belong in repositories or specifically justified infrastructure factories. They must not leak into application orchestration. Production-managed services own transactional business behavior; commit precedes acknowledged success, rollback is effective, optimistic/pessimistic locking is correct, streams do not escape transactions, and scopes/connections close/reset. No open-in-view or runtime ORM schema generation. |
| Query exceptions | Provisioning DDL, migrations, backend plugins and proven JPA limitations may require SQL/native facilities. Record exact location, necessity, tenant/transaction behavior and test evidence. Do not ban legitimate migration observations, or use raw SQL to implement business behavior in acceptance tests. |
| Migrations | Preserve deployed Liquibase history/checksums. Use forward changesets for relational changes; real ordered migration Jobs rather than service startup `create-drop`. Cassandra CQL is versioned/idempotent and adapter-owned; Kafka topic ownership is explicit. No tests manually repairing Liquibase bookkeeping or pre-seeding production-owned topics to hide startup failures. |
| Identity/tenancy | Trusted active organization determines tenant; DID/TenantId derivation and registry lookup are consistent. Never accept caller-chosen database/schema names or let workflow input override tenant. Check database roles, schema ownership, reset on pooled connection return, missing/inactive tenant rejection and fresh scope on async threads. |
| Typed values | API DTOs, domain values, entities and durable events have explicit boundaries. Audit dates/timezones/durations, null/missing, enums, arrays/maps, numbers/precision and Jackson/Serde behavior on all frameworks. No silent truncation, coercion or loss. |
| Build/deployment ownership | Versions managed in platform BOM/properties; local overrides only for necessary, documented exceptions. Shared framework-neutral Helm charts and platform Helmfile values; Maven-generated Dockerfiles/images; one selected framework/engine per installation. No hidden requirement for a developer's installed JAR, hand-set property or unrelated service. |

Audit generated Java, TypeScript and Python clients against the same FOWF OpenAPI source; internal self-calls do not justify handwritten clients. FOWF's declared HTTP baseline is OpenAPI 3.1 and `/v1` paths, including the defined internal prefix. Preserve documented dialect differences such as FDS's existing OpenAPI 3.0 shared-error snapshot conversion; report inconsistencies against the owning product requirement rather than silently imposing a platform-wide dialect upgrade.

Check the declared YAML configuration convention, common property names and actual host-specific translation. FEI's `ENTITY_INTELLIGENCE_*` convention and the ingestion repair's documented `FEI_INGESTION_*` settings need explicit reconciliation; do not silently rename shipped settings. Check `platform.framework`, `platform.engine`, framework-independent release names, shared image pins, and role-specific startup. Inventory documented temporary image-tag overrides separately from the normal platform version/digest policy.

## 6. FOWF behavioral criteria — inspect every relevant path

### 6.1 DSL, compiler, definitions and publication

- Inventory every normative Open Workflow 1.0.3 construct and required variant from the pinned schema/SDK and local approved extensions. Verify current pinned dependency/schema digests; do not upgrade versions as part of an audit.
- Trace `do`, `set`, switches/conditions, loops, forks/competing branches, try/catch/retry/raise, waits, listens, emits, calls and run/subworkflow constructs, including nested combinations, input/output/export transforms, timeouts, cancellation and termination directives.
- Require deterministic source/resource resolution and immutable plan/revision digests. A running execution must not change when a new definition is published.
- Definition lifecycle is DRAFT → PUBLISHED → DEPRECATED, validation before publication, one published revision per workflow. Definition authoring has no maker-checker; human-task approval does.
- Check CRUD/list/query/paging, invalid definition diagnostics, concurrent publication, mutation idempotency, ETags/version conflicts and immutable published revisions. Workflow selection uses the supported name/publication contract, not arbitrary fixed deployment IDs.
- Verify shared generic workflow-bundle publisher, entitled-tenant provisioning, API document registration before definitions, and product bundle compatibility. External resources are resolved/pinned during validation/publication; runtime must not silently fetch changed definitions.
- Verify approved DID bundle resolution, fragments/relative references, root/scheme restrictions, exact-host allowlists, redirects, media types, size/deadline bounds and digest provenance.

### 6.2 Admission, controls, concurrency and observable state

- Trace public API → authentication/authorization → application/domain service → durable admission/dispatch → engine gateway → actual handler → committed state → projections → query/history/Studio. Every handoff must be real.
- Test start, pause, resume, cancel, duplicate commands, command-ID conflicts, same ID across tenants, stale expected versions, concurrent controls, admission deadlines, lost acknowledgements and retry after service replacement.
- Engine and definition pinning persist across restart. HTTP 202, producer acknowledgement and an accepted Job are not terminal-success evidence.
- Compare public states, revisions, history ordering/identity, actor/correlation details, output, errors, timer/activity projections and callbacks. Expose failure reasons, including restored failures; no perpetually RUNNING execution after completed/failed engine work.
- Effects happen only after durable intent. Duplicate/redelivered observations must not apply twice. Preserve causation, command, event, execution, branch, child and correlation identities through all transports.
- **Approved unknown-outcome rule:** when remote acceptance is ambiguous and no trustworthy deduplication/outcome lookup exists, fail/abort with `OPERATION_OUTCOME_UNKNOWN`. Do not blindly repeat the effect, enter ordinary DSL retry/catch handling or report success. Propagate an awaited child's unknown outcome to its parent. Retain redacted diagnostic detail; a crash may leave genuinely unknown details.

### 6.3 Recovery, backend behavior and ordering

- Cover process replacement, actor reactivation, snapshot/journal replay, broker restart/rebalance/changelog restoration, lost callback acknowledgements, replay after commit and schema/wire upgrades. Test both Pekko backends distinctly.
- Inspect crash windows before/after intent persistence, effect dispatch, remote acceptance, outcome recording, projection update and acknowledgement. A normal graceful restart alone is insufficient.
- Timers, deadlines, retry backoffs, schedules and subscriptions retain original identity/deadline. Do not reset deadlines on restart. Use committed firing history when a due timer has already left the live store.
- Verify stale leases/fencing, ownership transfer, concurrent workers, ordering, duplicate/out-of-order events, bounded retry and poison-record behavior. Require single logical effects where the contract promises them; do not assert generic exactly-once external side effects without a deduplication mechanism.
- Inspect native PostgreSQL/Cassandra isolation, journal/snapshot consistency, offset stores, coordinator role placement, retention and cluster topology. “Same Java class” does not prove same backend recovery.
- Versioned command/event/snapshot manifests and golden fixtures cover actual producer/consumer serialization. Supported older data must recover correctly; unknown/incompatible versions fail explicitly. No silently changed durable meaning.

### 6.4 Payload and overflow audit — mandatory independent section

List **every** data-capture/materialization boundary, its default/configurable limits, size unit, serialization, failure behavior, storage owner and tests. Search all `DataReferences.inline`, capture/reference helpers, explicit constants, HTTP limits, Kafka record/state limits and whole-body materializations. Grep is the starting inventory, not the verdict.

Required path inventory includes:

1. Public and internal workflow start inputs, including FEI/FDS-generated inputs.
2. Timer/scheduled starts and CloudEvent-triggered starts/ingress.
3. Task inputs, outputs, workflow context, input/output/export transforms and execution result/history projections.
4. Root and nested fork branches; competing branches and joins.
5. HTTP/OpenAPI/gRPC/AsyncAPI and agent-protocol responses, stream frames and accumulated stream results.
6. Correlated-worker command, progress/event/result and cancellation payloads.
7. Subworkflow inputs, completion/error payloads and parent propagation.
8. Human-task descriptors/inputs/decisions.
9. Kafka routing/control descriptors versus offloadable user fields, durable snapshots and outbox records.
10. Restart/replay of each kind of stored reference.

For each relevant path cover below/at/above thresholds in **serialized bytes**, multibyte UTF-8, nested values, null/scalar/list/object forms, key ordering, size/checksum integrity, configured versus hard limits, and equality of returned user content. Test the 32,767/32,768/32,769 boundary explicitly and independently of the protocol-response threshold.

Separate inline cutoff, maximum accepted value, whole request/record/state limits and decompressed/materialized size. A 1 MB response threshold does not automatically make a 64 KiB start command legal. An accepted envelope may still fail at a later serialization boundary.

Overflow belongs in shared object storage, not Kafka's/Cassandra's/Pekko's durable store as an unbounded blob. Inspect tenant ownership, immutable/content-addressed keys, duplicate capture, limits before unsafe buffering, failure before publication, retry after failed upload, dangling reference prevention, retention/cleanup, missing/corrupt objects, and transparent materialization before validation/expressions.

Storage I/O must not block Kafka's deterministic reducer/processing thread or Pekko command handling. Trace the actual off-thread worker/coordinator wiring and readiness. Configuration alone is not proof of use.

The October 10 gateway handover records compilation without execution of its new fixtures. Check for newer evidence before assigning its current runtime status. The recorded repair leaves CloudEvent/scheduled-start inline paths outside its scope. Treat those siblings as mandatory audit targets rather than assuming the public-start repair fixes them.

### 6.5 Operation adapters, correlated workers and subworkflows

- Inventory every declared executor: HTTP/OpenAPI, gRPC, AsyncAPI transports (Kafka, AMQP, JMS/IBM MQ, MQTT, NATS, Pulsar, Redis, STOMP, SNS/SQS, Pub/Sub and any additional declared bindings), HTTP streaming/WebSocket/SSE, MCP, A2A, container/local process, Kubernetes Job and Deployment.
- Follow compile → materialize → select executor → authorize → dispatch → observe/correlate → cancel → persist/project. Existence of a shared executor is not proof that each engine constructs a descriptor that reaches it.
- Check root/nested/fork paths, correlation field preservation, command/events/cancellation legs, actual external cancellation, restart during pending work, protocol terminal markers, completion after failure, unknown outcomes, retries and output metadata.
- Require tenant-scoped egress policy, exact host/port/protocol checks, approved image digests, namespaces/service accounts/volumes/ports and secret lookup. Workflow data cannot choose credentials or tenant context.
- Subworkflows pin a published child revision; propagate controls and return results once; do not lose/duplicate parent continuation on restart.
- Test executors against real services/brokers/containers/Kubernetes for operational claims. Document any unsupported transport or missing real fixture explicitly; do not silently mark it N/A because only HTTP has been tested.

### 6.6 Human tasks and Studio

- Human-task intent/creation/decision is durable and correlated to execution and branch; duplicates, restart and cancellation do not create extra tasks or revive cancelled work.
- Cover role/assignment visibility, claim/release, leases and expiry, concurrent claim/decision, optimistic conflicts, review sessions, escalation/deadline behavior and maker-checker separation. A maker cannot approve their own submitted work.
- FOWF Studio must losslessly author/round-trip supported definitions, validate, publish and display/control real executions, human tasks, history, timers, outputs and failures through generated clients and actual APIs.
- One Maven-built webapp with thin framework hosts; consistent routing/base paths/assets/authentication and API behavior across hosts. A TypeScript compile or screenshot is not browser/API acceptance.

## 7. Security, framework startup and API semantics

Apply to all exposed capabilities, not just workflow start:

- Real Keycloak Organizations and shared AuthZEN capabilities. Separate authenticated caller from authorized asserted subject; caller organization controls tenancy. Never use permit-all authorization as acceptance evidence.
- Use the `organization-role` policy provider with organization-scoped membership; merged top-level realm/client role claims do not authorize a tenant operation. Verify two organizations under one caller, a grant in only one, policy outage/ambiguous responses failing closed, and revocation/cache expiry. Shared environment clients/roles must not silently become a realm or client per tenant.
- Cover missing/invalid/expired tokens, wrong audience, real same-signing-key wrong issuer, valid but denied identities, inactive/missing organization, tenant spoofing and cross-tenant access. Valid requests must continue working after rejection.
- Expected issuer is distinct from container-reachable JWKS/PDP URLs. Do not use `issuer=any` or a test-only validator to hide host/container routing problems.
- Inventory all public/internal routes and per-framework filters. Check declared status/body/content-type/header behavior for missing/unknown/wrongly typed fields, enums, nulls, nested collections, dates, unsupported media, bad paths/queries, validation and errors. Authentication ordering and information disclosure must agree with the contract.
- Preserve the agreed HTTP security matrix: no token on any protected path, including an unknown path, gives 401 Problem with a Bearer challenge; invalid tokens give 401; a valid token on an unknown path gives 404 Problem; designated health endpoints are open. Audit legitimate open routes explicitly. Apply FDE's gRPC status contract separately.
- Verify JWT/organization/actor context survives HTTP, Kafka, actor messages, workers, callbacks and asynchronous computation without carrying EntityManager or open transactions across threads.
- Boot actual framework applications with shipped configuration. Lazy bean construction, missing providers/routes, Micronaut duration/placeholder interpretation, Jackson/Serde differences, Quarkus bean removal and Spring conditional/JPA auto-configuration are explicit audit targets.
- Readiness must represent the actual runtime and needed dependencies; healthy HTTP alone does not prove the engine/worker started. Kubernetes health paths/ports/protocols must match the real framework. Internal engine routes must have the intended cluster/network exposure and security boundary.

## 8. FEI criteria already agreed

Audit these on all three frameworks and the applicable nine FOWF runtime cells. Keep reference and monitored populations distinct; verify both.

- Import the complete FEI API operation inventory, not only ingestion triggers. Cover source-feed profiles, entities/assertions, merge/deletion where exposed, dossiers, investigations/evidence/candidates, LLM prompts, screening/export and indexing, as well as population lifecycle. Derive the current operation count from the canonical OpenAPI document instead of freezing the older 83-operation count. Every declared operation and its error cases require real HTTP evidence on all three frameworks; internal service-only behavior retains accurately labeled managed-service coverage.
- Long-running FEI work uses FOWF's generated execution-management client. FEI does not acquire its own Kubernetes Job orchestrator. **Dispatched FEI workers must not access tenant databases:** they use FEI REST APIs with scoped workload identities; no JDBC URLs, credentials, drivers or `forwardmeasure-jpa` dependency belong in those worker paths. This restriction does not ban legitimate database source connectors in FDS or direct OpenSearch indexing under the ingestion contract.
- Workflow publication uses the shared bundle publisher (`fei` moniker), bundled API documents and configured DID method. Resolve `fei-<name>` to its published revision on launch; no fixed `*_WORKFLOW_ID` configuration. End-user actions propagate `subjectActor` with the required assert-subject capability. Entity IDs follow `PlatformDids.entity` / `did:<method>:entity:<tenantId>:<uuid>`, not a hardcoded legacy DID method.
- Simple-source, correlated and resolution ingestion must start through the actual FEI API, dispatch the real workflow and packaged worker(s), report progress, persist/index results and support actual screening/resolution queries. Neither a spec-only nor simple-worker-only repair is enough.
- Canonical provider contracts are platform-owned under `forwardmeasure-platform/forwardmeasure-platform-data-contracts/src/main/resources/`, indexed by `contracts/catalog.json`. Source/mapping/destination resources are under `contracts/`; OpenSearch mappings are under `opensearch/`. State Street's destination schema is the source YAML's `#/output_schema`, as the catalog declares. Verify both source checkout and packaged dependency bytes; stale copies are not alternative authoritative contracts.
- Separate source parsing/validation, mapping, mapped-output JSON Schema and OpenSearch mapping. A CSV source schema is not the output schema. Declare actual supported schema dialects, references and constraints; Java DTO conversion/field-presence checks are not full validation.
- Preserve one versioned shared canonical screening document. Store **all** source-specific raw fields under `source_data`, including fields with no logical canonical mapping. Only explicitly mapped extra fields are searchable; stored-only fields round-trip through `_source` without uncontrolled dynamic index-field expansion.
- WorldCheck uses real representative premium data plus its actual source schema, mapping and index JSON. Verify provider UID/stable document IDs, names/aliases/categories, individuals and blank-FIRST-NAME organizations, source encoding/delimiter/header/date/null behavior, screening hits and unrelated-query negatives. Independent expected output must expose wrong/incomplete mappings.
- State Street customer master uses the centralized source/mapping semantics, typed dates/identifiers/addresses, all 42 fixture raw fields and persisted resolution/assertion relationships. Verify source-order independence, scalar trust precedence and union/deduplication of identifiers, aliases and assertions where specified.
- Population creation requires nonblank default schema and mapping URIs. Index mapping URI is optional, selecting the server-owned canonical default. Internal `classpath:` strings are not user-facing Studio placeholders/defaults.
- Population updates cannot clear those required contracts. Resolve index mapping in revision-override → population-default → canonical-server-default order and apply it before writing any row on both simple and correlated paths. Inspect the actual index settings/analyzers/nested fields after API-originated ingestion; a fixture-created index cannot prove production initialization.
- Full/delta runs inherit defaults unless explicit request overrides win. Snapshot resolved contracts and provenance per admitted run so later population changes cannot change the run.
- Existing reference trigger accepts optional `revision_kind: full | delta`, default full. Delta without an active base is rejected with 409. A staged READY delta owns its own query index and does not replace the active population until explicit apply/activation. Verify updates/deletes, rollback, retention/purge eligibility and concurrent lifecycle actions.
- Rollback reuses the superseded revision's own index. Retention/purge is manual with the declared keep-last-N policy; no invented scheduled deletion. Reference-population approval has no separate FEI maker-checker implementation: approved human review uses FOWF human tasks. Population screening uses a FOWF-dispatched Kubernetes Indexed Job, with per-subject OpenSearch recall and no Spark requirement.
- Index names, ownership, aliases and metadata are server-controlled and tenant/population/revision scoped. Reject arbitrary/unowned index takeover. Validate bulk item outcomes, refresh/query readiness and finalization before READY; do not infer successful indexing from submission counts or HTTP 202.
- Progress is cumulative, replay-safe and accurate for converted/submitted/acknowledged/rejected records. Rejected records are failures, not silently successful runs. Duplicate/out-of-order callbacks cannot regress totals or terminal state; retries/restart retain identity/checkpoints and expose actionable errors.
- The current parsing override contract is `source_parsing` with **delimiter, quote and encoding**, on reference and monitored ingestion. Empty quote disables quoting; omission inherits, and JSON null is not a disable instruction. Headers remain required; request overrides cannot alter schema/mapping or operator-owned memory/disk limits. Configured pipeline triggers use `source_parsing_overrides` keyed by exact configured source URI, reject unknown URIs and retain the no-body request form. Verify snapshots, retries, propagation through workers and Studio, and idempotency conflict when options change. Do not describe header control or parser resource-limit overrides as existing public options.
- Studio must supply contracts/options correctly, show real progress/failure/recovery, support full/delta lifecycle and allow screening/querying the actual output. Test browser → API → worker effects, not only UI mocks.
- Full-file ingestion is required: configurable acquisition limits and disk capacity, incremental CSV/TSV parsing, bounded in-flight records and backpressure, 64-bit file/progress counters, streaming downstream batches, cleanup/cancellation/recovery and meaningful disk-full/storage-error behavior. No whole-file byte array/String/record-list materialization or silent small-file cap.
- Design must support files in the hundreds of GB with bounded memory relative to file size and records bounded by configured parser/Java representation limits. A Java String's theoretical limit is not a safe operational record allocation. The immediate agreed dataset is the complete WorldCheck export, approximately 5.6 GB / 5.8 million rows; use the actual bytes/hash/row count in evidence. Earlier 5,611,953,182-byte / 5,818,856-row execution is historical evidence, not a 100 GB/TB execution claim.
- Preserve deployed migration history, real OIDC/AuthZEN and established worker dependency separation. Do not add caller-selected database names or a production fallback for required DID configuration.

The provider field ledger is mandatory input, not optional background. Audit all 35 WorldCheck columns and all 42 logical State Street fields against independent expected outputs and actual screening/resolution consumers. Include WorldCheck alias quality, DOB plus DOBS precision/invalid dates, E/I classification precedence, brace-qualified/masked identifiers, country/location resolution and descriptive update categories (not inferred deletion commands). For State Street include duplicate trailing headers/blank last-column-wins behavior, provider-scoped identifier keys, person versus organization dates, unknown-type rejection, address roles and typed raw assertions. Raw named-field retention is not physical preservation of duplicate column positions; record that limitation explicitly.

Do not assert that Latin-1 is vendor-confirmed: the current ledger records ISO-8859-1 with Latin-1-versus-CP1252 supplier confirmation outstanding. Check for newer authoritative evidence. Committed fixtures must be synthetic; keep the authorized real sample/export external and logs redacted. Preserve admitted bytes/transform versions, and audit the documented requirement for a fresh full revision when the destination/index shape changes; old-shape delta compatibility must not be assumed.

## 9. FDS criteria already agreed

- One canonical IngestionSpec with `sources: []`; single-source and correlated flows do not acquire competing schemas. Source, transform, identity, merge and sink contracts have declared owners.
- Two permanent delivery engines: Kafka Streams and Pekko Streams, with bounded and continuous modes as supported. Spark is optional computation/staging, **not** a third delivery engine or a synonym for correlation. Correlated continuous ingestion can run without Spark.
- The author specifies execution mode, not delivery engine or internal ExecutionPlan/ExecutionProfile. The planner selects from declared transform characteristics (cardinality, state, cost, order sensitivity, determinism); audit every selection rule. Camel is connector plumbing, not another delivery engine. Runtime vendor mapping uses the shared metadata-driven `FieldMappingEngine` / `NamedTransformRegistry`; generated typed DTOs do not replace that approved dynamic boundary.
- Direct REST and FOWF-managed admission use the same planner and produce equivalent effective plans. Spark-staged runs must actually launch Spark and delivery Jobs and verify the final sink, not stop after staging.
- Bounded execution dispatches a Job; continuous execution dispatches a Deployment. Both must be reachable through direct and FOWF-managed invocation. Reuse the shared Kubernetes lifecycle library for launch/poll/cancel; verify image/namespace policy and cancellation effects rather than copying an orchestration implementation.
- Preserve the approved Spark staging boundary: Kafka or the supported in-process handoff feeds delivery; Spark must not bypass the delivery engine by writing directly to a non-Kafka final sink. Trace the selected handoff and its lifecycle rather than assuming every correlated flow requires staging.
- Audit the real applicability table of source cardinality × bounded/continuous × delivery engine × direct/workflow invocation × framework × relevant FOWF backend. Enumerate supported and unsupported combinations with contract reasons. Historical 39-cell/57-test counts are evidence sets, not a complete behavior denominator.
- Preserve the architecture's eight base capability cells (single/correlated × bounded/continuous × Pekko/Kafka) and its historical 18-cell acceptance baseline for **both** WorldCheck and Customer Master. The latter omits direct continuous invocation and does not discharge the broader SINV-01 requirement or the later all-combinations request. Map the old baseline into the full ledger explicitly; do not use its smaller denominator to omit supported paths.
- Cover source/connector/mapping/type semantics, missing/blank correlation keys, join cardinality, unmatched records, duplicates and deterministic trust/union policy. Preserve normalized identifiers/aliases/assertions where the FEI contract requires them; no hardcoded provider-specific merge semantics hidden in a generic engine.
- Verify durable correlation state. The approved FDS Pekko correlation design uses a Kafka compacted changelog; do not confuse this with FOWF Pekko's PostgreSQL/Cassandra journals or incorrectly ban the neutral Kafka client.
- Merge policy is a URI-referenced versioned artifact, not hardcoded winner-takes-all logic. Audit at least three sources and every runner: highest-trust resolved scalar wins, lower-trust evidence/aliases remain, identifiers and applicable assertions union/deduplicate. Verify both source orders, ties and the policy's actual configured semantics.
- Bounded Kafka ingestion captures a fixed read-committed frontier, accounts for aborted transactions/control records, drains and terminates correctly. Continuous ingestion retains state and cancels/drains cleanly. Check commit/ack ordering, sink failure, retries, stable IDs and duplicate-free observable effects.
- All acceptance uses real workers/executors, input records and OpenSearch effects. No curl-image fallback or fixture writing the expected output directly into OpenSearch. Direct component tests remain useful but are not public REST acceptance.
- JDBC/JPA connector contracts may directly exercise real repositories because that is their boundary; application business code still observes service-layer rules. Bulk Spark JDBC extraction is an explicit different mechanism, not a reason to rewrite it as per-row JPA.
- FDS API and worker contracts are generated/centralized; exception handling/security is shared. Confirm declared classpath separation and supported resources for each executor image.
- Framework parity applies to the launcher hosts; delivery runners are framework-neutral `main()` programs. Do not invent three framework implementations of each runner. Shared planning/mapping/connector modules remain engine-neutral; Kafka/Spark images contain no Pekko, and Pekko images contain no Kafka Streams engine implementation.

## 10. FDE criteria already agreed

- FDE remains framework-neutral Drools core with framework-native **gRPC** bindings for Quarkus, Spring and Micronaut. Do not invent REST or a new Studio to satisfy an HTTP checklist.
- Canonical `.proto` ownership and generated language bindings; actual wire-level tests through generated clients. Check request/status/metadata behavior, nested Struct/list/null/number mapping, correlation IDs, fired-rule/result semantics, malformed requests and unsupported output types.
- Inventory the actual protobuf RPCs. At this review the management API declares `CreateRulesetVersion`, `GetActiveRulesetVersion`, `ListRulesetVersions`, `ActivateRulesetVersion` and `DeleteRulesetVersion`; evaluation declares `Evaluate`; administration declares `GetStatistics`, `GetCacheStatus`, `UnloadRuleset`, `ClearCompiledRulesCache` and `WarmRuleset`. Validate compilation/rejected creation and first-version/activation semantics through those real operations. Do not invent update, validate or publish RPCs. Preserve durable version identity and commit ordering, including no version reuse after deleting the highest version, concurrent allocation/activation/deletion and pinned evaluation.
- Verify tenant-scoped compiled-rule cache and unique KIE identities, disposal/retirement during active evaluations, invalidation, bounded tenant/rules/firing limits, shutdown and failures. A firing limit is not a wall-clock sandbox for arbitrary executable Java/DRL.
- Stateful fact windows use properly tenant/ruleset/version-scoped Valkey keys with unambiguous encoding. Cover append, TTL/window behavior, concurrent calls, restart, configured authentication and embedded/external Valkey deployment modes.
- Verified JWT organization/issuer/audience/expiry plus distinct AuthZEN evaluate/manage/admin capabilities are mandatory. Unsigned `tenant-id` is not production identity. Conflicting metadata is rejected; evaluate-only callers cannot upload executable rules or administer caches.
- gRPC liveness/readiness, real health marshaller compatibility, numeric probe ports and framework registration must work in actual images. Health cannot expose tenant data or falsely certify unavailable required dependencies.
- FOWF calls FDE through its real gRPC adapter, registered/pinned proto, approved exact egress endpoint and tenant-scoped credentials. Prove actual result and failure propagation across the nine FOWF cells; retain separate three-framework × embedded/external Valkey deployment evidence.
- Stateful append and evaluation are not a distributed exactly-once transaction. Lost response after append may be outcome-unknown; do not hide this with blind workflow retries. Process-local admin/cache statistics must not be presented as cluster-wide operations.
- Verify migration/provisioning prerequisites for newly added tenants. Membership grants alone do not prove the tenant's FDE schema exists. Audit current code against the rehabilitation amendments rather than obsolete initial-spec assumptions.
- Include the rehabilitation handover's complete acceptance list: populated old-schema upgrade and counter seeding under restricted roles, cache-warm tenant deactivation, actual authorization TTL, Valkey script eviction/response loss, read-only-root startup and writable compiler temp space, secret rotation, and unavailable-dependency recovery. Prove proto compiler packaging without network access to Maven at runtime or reliance on a host-installed compiler. Keep these as separately evidenced cases, not implied consequences of a successful Evaluate call.

## 11. Testing and evidence requirements

### Test boundaries

Retain both layers; more coverage means more independently meaningful failure modes, not copied assertions.

| Test kind | Required entry and proof |
|---|---|
| Pure algorithm/value contract | Direct fixed inputs with independent expected results; no claim about DI/HTTP |
| Managed business service | Resolve actual production services/interceptors through each framework; real migrated storage; commit/reload/rollback and concurrency |
| Repository/connector/native storage | Real adapter and backend, deliberately testing query/locking/paging/recovery semantics; no claim about public admission |
| Public API | Real HTTP or FDE gRPC, actual identity/AuthZEN, generated production binding and observable committed state |
| Workflow/worker | Public admission, actual current engine/adapter/worker images, durable completion and actual downstream effects |
| Studio | Real browser against the real API stack; distinguish from UI unit tests and frontend compilation |

Use `forwardmeasure-testcontainers` containers/extensions and existing shared cross-product fixture infrastructure. Add missing reusable support there when subsequently authorized; do not duplicate raw container lifecycle throughout products. No Mockito or H2. A small handwritten double is permitted only for a justified narrow condition and must never be presented as real integration evidence.

Seed source records and legitimate prerequisites, not the expected ingestion output. Business mutations under test go through the owning API/service boundary. Test-only Liquibase fixtures may supply prerequisites with no public creation API. Approved migration/upgrade cases may seed rows with SQL after genuine historical schema creation; do not replace schemas or alter checksums/bookkeeping. Limited read-only SQL for privileges/catalogs/independent persistence observation is distinct from implementing domain behavior with SQL.

Do not add production methods/endpoints purely for convenient coverage. Do not move `new Repository()` into a helper and call it managed-service testing. Do not remove focused persistence tests to satisfy a superficial “all tests must be REST” rule that the user explicitly superseded.

Every proposed fixture must state the concrete defect it detects and why its assertions would fail without the fix. Include exact identities/counts/content, rejected-row/item outcomes, terminal state, persisted rereads, denial ordering and downstream effects as appropriate. Avoid “not 500,” “nonnull,” “202 received,” fake echoes, method-call counts or output computed by the same mapper as the only assertions.

Require a mechanically checkable mapping from each current HTTP operationId or gRPC method and declared error family to concrete assertions on each applicable framework. Missing mappings fail completeness review. Also inventory non-API engine/worker/compiler behaviors; an operation inventory alone cannot cover recovery or internal alternate paths. Preserve independent expected provider outputs and specific historical defect triggers, including populated timer/effect responses, real definition metadata, explicit OpenAPI callback bodies/authentication, lost lower-trust assertions and worker date serialization.

Fault injection must be deterministic and target real boundaries; polls have bounded deadlines and report last observed state. Skips, ignored failures, broad catches, narrowed test selection or lower gates cannot turn a failing suite into a pass. Do not silently use obsolete/default images when prerequisites are missing.

### Coverage and compilation

- Minimum **85% handwritten lines and 85% handwritten branches** across the stack. Inventory gate ownership for all production modules; a missing or incomplete gate is a finding, not an exemption. Preserve existing zero-uncovered-class rules; do not weaken them without explicit authorization.
- Generated code is exempt by provenance, including mixed packages. Handwritten adapters are not generated. Fix class-ID mismatch, framework enhancement and cross-module/remote-service collection before interpreting uncovered lines as missing tests.
- Coverage measures execution, not semantic parity. 100% coverage would not prove 100% correctness, and 85% gates do not replace missing operation/error-path acceptance.
- Report actual gate ownership and results. An aggregate-only build, stale JaCoCo files, a diagnostic percentage, empty report or skipped check is not an actual passing configured gate.
- When implementation is authorized, compile production **and test** sources using the bounded wrapper with an absolute `-f` and `test-compile -DskipTests=true -DskipITs=true -Dmaven.test.skip=false`. Engine/framework-spanning FOWF changes require the full production/test reactor including both Pekko backends and all frameworks.
- The shared wrapper is `/home/pn/Documents/code/forwardmeasure/forwardmeasure-openworkflow/scripts/build-bounded.sh`; inspect its current options and locking before preparing commands. Do not substitute an unbounded direct Maven invocation.
- Coordinate with the shared Maven lock and running builds. No parallel Maven/`-T`, speculative full rebuilds, cache wiping or deleting `.m2`. Keep Maven build cache enabled; distinguish restored outputs from fresh execution. Dockerfile generation/packaging and required Spotless/RAT/dependency/architecture checks remain separate evidence.
- Tests, image build/push and rollout are separate phases requiring the current session's authorization. Prepare exact affected-module commands and image/configuration prerequisites; do not call acceptance ready to run if it needs images that have not been built.
- Preserve the user's fixture-first sequencing: inventory and write the agreed missing fixtures before beginning the wider runtime execution phase, when that implementation phase is authorized. State exactly which fixtures remain unwritten; compilation of a subset is not completion of fixture development. Any change to that sequencing needs a current instruction, not an assumption.

### Evidence labels — never collapse them

For every requirement/cell track separately: **not inspected; statically traced; fixture missing; fixture written; compiled; executed pass; executed fail; skipped/blocked; packaged-image verified; deployed verified**. A finding can have more than one dimension, e.g. statically fixed and compiled but runtime unverified.

Record commit/dirty-source hash, contract version/hash, fixture class/method, command, JVM/framework/backend configuration, image tag **and digest**, report/log path/hash, tests/failures/errors/skips and persisted-effect assertions. Explain C1-only/JVM or environment workarounds and what they limit. Do not double-count original runs and targeted reruns as unique cases. Preserve failed attempts rather than overwriting history.

## 12. Required audit procedure and deliverables

Complete this analysis phase rather than stopping after a plan or grep results. Do not start repair/testing silently.

1. **Baseline:** record HEAD/dirty paths, applicable instructions, active build state and available evidence. Distinguish source under review from deployed images and historical checkpoints.
2. **Requirements register:** import every architecture rule ID and all criteria in this request; reconcile newer decisions. For each capability enumerate semantic variants, boundary/failure/recovery cases and applicable runtime cells before discussing a completion percentage.
3. **Production trace:** independently follow each engine and framework path from a real trigger to durable effects and public observation. Inspect constructors/factories/startup/configuration, not only common helpers. Trace alternate callers such as schedules, callbacks and recovery.
4. **Sibling analysis:** for every gap search the pattern in other boundaries, engines, frameworks and product consumers. Distinguish same symptom from independently confirmed same root cause.
5. **Fixture audit:** inspect what each existing test actually executes, asserts, seeds and substitutes. Map assertions to requirements. Identify missing fixtures separately from existing-but-unexecuted fixtures, stale/broken fixtures, and coverage-collection gaps.
6. **Findings and counterexamples:** give minimal input/sequence/configuration, expected versus observed or statically derived behavior, exact file:line/branch and affected cells. Label inference as inference; do not claim a reproduction was run when only source was read.
7. **Finite repair/fixture plan:** each known gap gets a stable ID, exact module/class/test owner, scenario/assertion, dependency, acceptance condition and necessary runtime cells. State unresolved discovery explicitly instead of inventing a fixture count or ETA.
8. **Independent reconciliation:** if Claude and Codex both review, compare by requirement ID. Resolve disagreement using source/spec/evidence. Agreement without evidence does not change a cell to pass.

Write these deliverables under `forwardmeasure-platform/docs/parity-audit-2026-10-10/`, using an agent prefix when reviews run independently:

- `<agent>-audit.md`: clear verdict, critical defects, scope examined, evidence limits, contradictions and exact remaining work.
- `<agent>-requirements.json`: stable IDs, authoritative source and decision precedence, expected semantics, applicable cells, implementation paths, status, findings and evidence links.
- `<agent>-matrix.csv`: one row per requirement/scenario/applicable runtime cell; no green summary hiding unassessed rows.
- `<agent>-findings.md`: severity, minimal counterexample, affected paths/cells, architectural versus behavioral defect, fix direction and required regression.
- `<agent>-fixtures.md`: existing tests and missing/incorrect/unexecuted fixtures, with a concrete failure-catching plan and execution prerequisites.
- `<agent>-execution-plan.md`: smallest justified compile/test/image sequence, exact commands, resource/lock coordination, and what needs additional authorization. Do not execute it in the analysis phase.

Suggested ledger fields:

```text
requirement_id, source_rule_ids, contract_version, capability, scenario_id,
framework, engine, persistence, entry_point, implementation_paths,
expected_result, static_finding, fixture_class_method, fixture_status,
compile_evidence, execution_evidence, source_hash, image_digest,
test_count, failures, errors, skips, architecture_status, parity_status,
blocker, next_action, acceptance_condition
```

Only use N/A when the contract makes a cell inapplicable, with a reason. A missing implementation, fixture or environment is a gap, not N/A. Record legitimate differences without demanding identical physical storage, private class names or deployment tuning. Publicly observable acceptance/result/security/recovery differences require an explicit contract basis, not convenience.

Include a source-to-requirement crosswalk showing every imported rule or acceptance clause, its retained/superseded/disputed status and the corresponding ledger IDs. This is the completeness check on the audit itself. The summaries in this request do not replace the referenced full requirements.

## 13. Completion/reporting standard

An **analysis-complete** report means every inventoried requirement has been traced or explicitly marked blocked/unassessed with a specific reason, every known defect has a fixture/repair plan, and the report gives exact counts with denominators. It does not mean the product has passed.

An eventual **parity-verified** claim requires the complete agreed requirement/cell ledger, successful meaningful execution on applicable actual runtimes, architecture checks, no hidden skips/unassessed applicable rows, correct source/image provenance and explicit residual limitations. A user-approved exception is recorded as an exception, not silently erased to make “100%.” Do not equate the historical 9/9, 39/39 or 15/15 image selections with every behavior being covered.

Reports must answer directly:

1. What concrete parity or architecture defects exist, and where?
2. What has been checked only statically, and what has been executed on which exact images?
3. Which fixtures are missing, written, compiled, failed, skipped or not run?
4. What exactly remains, by stable ID—not “remaining meaningful coverage/API work”?
5. Which claims in earlier handovers are now contradicted, stale or narrower than originally stated?
6. What can proceed independently, what is actually blocked, and what user decision/action is genuinely needed?

Provide concise progress reports during sustained work, including the actual running process/log or explicit statement that you are doing static analysis. Do not imply background tests/builds are running when they are not. Do not stop after an update unless the requested phase is complete or a concrete blocker prevents useful progress. Avoid speculative ETAs; first establish the inventory and measured execution costs.

**Do not conclude “100% parity” because the current tests pass. Establish whether the tests and the production traces cover the complete required contract first.**
