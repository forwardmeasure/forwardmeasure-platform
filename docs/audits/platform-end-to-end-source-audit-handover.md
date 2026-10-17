# Platform end to end source audit and review handover

Prepared for a new Codex session on **2026-10-14, America/New_York**. Reusable for subsequent audits.

## Repair checkpoint — 2026-10-15

The owner separately authorized fixing the reported FEI failures and then committing and pushing
the current work across repositories. The targeted repairs passed **549 FEI tests**, with zero
failures, errors or skips across sequential focused runs. All **nine FOWF start-input runtime
configurations** passed, including persisted validation errors and engine restart recovery.
The full source audit commissioned below remains outstanding.

The screening export defect was a lazy monitored-population association mapped after its
transaction closed. Other repairs cover canonical contract resources for deletion fixtures,
managed fixture services, Spring lifecycle request ports, empty-query validation and the FARCH-07
service summary boundary. Population creation retains its documented optional initial input
definition; a supplied incomplete input definition returns 400. FOWF's durable FAILED
acknowledgement can precede asynchronous error/history projection, so acceptance now requires
bounded convergence and matching persisted failure details.

See [repair validation](fei-fowf-repair-validation-2026-10-15.json) for counts, source hashes,
runtime image IDs and scope, and [repository checkpoints](source-checkpoint-2026-10-15.json)
for the coordinated source commits. These results do not certify all pre-existing work included
in the checkpoints. Fresh release images, a test deployment and the live FEI public-ingestion
acceptance matrix remain required before production promotion.

**Assignment:** independently analyze FOWF, FEI, FDS, FDE, forwardmeasure-platform, and their complete first-party dependency and deployment closure. Read the actual code. Find latent correctness defects, incomplete integration, architectural violations, engine/framework divergence, dead code, and misleading verification claims. Deliver an actionable, evidence-backed report and a finite repair/verification backlog. This document commissions the audit; it does not certify the implementation.

**Current operating mode: source review and audit documents only.** The owner is building and deploying; Claude is repairing findings concurrently. Do not edit implementation, contracts, tests, POMs, charts or configuration, run Maven/tests, build/push images, change the cluster, commit or push as part of this assignment. Read existing source and evidence and write audit reports. Later explicit instructions can authorize those other activities. Preserve all other sessions' changes.

This is the audit procedure for the [platform technical vision](../technical-vision.md), not a replacement architecture specification. Keep intended requirements, observed implementation, and verification evidence separate. Canonical status remains in [implementation status](../implementation-status.md) and [testing status](../multi-framework-testing-status.md), with FOWF detail in its own three governing documents. Report contradictions and proposed corrections; do not silently rewrite those documents during the audit.

## 1. Prompt to give the new agent

> Read `forwardmeasure-platform/docs/audits/platform-end-to-end-source-audit-handover.md` in full, then perform the complete source audit it specifies. Workspace: `/home/pn/Documents/code/forwardmeasure`. Start with applicable AGENTS.md files and the current governing requirements. Review all five products and the first-party modules, generated contracts, runtime resources, packaging and deployment paths they actually use. Establish the complete denominator before claiming completeness. Trace real entry points to persisted effects and back to API/Studio observations. Require full contract parity across Quarkus, Spring Boot and Micronaut, and Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra wherever applicable. Audit JPA/metamodel boundaries, contract-first generation, tenant isolation, recovery, migrations, integration and test/coverage gates. Include dead code and contradictory decisions. Claude is making fixes concurrently: record and recheck the source baseline and do not treat old findings as still present without reading the current code. Produce the reports and ledgers specified here. Do not change implementation or run builds, tests or deployment commands. Continue through the whole inventory; a review of the latest diff or a list of already known bugs is not the assignment.

## 2. Why this audit is necessary

This platform has repeatedly compiled, packaged and deployed while important behavior remained broken. Examples that must inform the audit method include:

- Workflow dispatch depended on an async-task table that test migrations created but the shipped production migrator did not include.
- A Kafka Streams start-input size boundary differed from Pekko even though other large-payload scenarios had passed.
- Framework factories, interceptors, serialization and health checks compiled but did not behave equivalently in packaged applications.
- Workers carried an engine they did not use, or bypassed the installation's engine selection.
- Source schemas, mappings, output models and OpenSearch mappings disagreed while small fixtures validated only their common subset.
- Installer ordering, topic provisioning, image admission, tenant routes and secret composition prevented otherwise packaged services from working.
- Recovery claims accounted for one crash interval but omitted another; an idempotent sink was mistaken for idempotent state evolution.
- A test or handover said a capability was complete even when the real trigger, production resource or framework wiring was missing.

These are historical failure patterns, not assertions that each remains unfixed. Trace their siblings and verify current fixes. Do not attribute blame to an author; report the defect, the missed boundary and the gate that should have caught it.

The required posture is **one coherent platform with real behavior**, not a collection of plausible modules. Compilation, a populated module tree, a successful installer, Ready pods and a Studio page establish different, limited facts. None establishes end-to-end correctness.

## 3. Authority and source precedence

Use this order when requirements conflict:

1. Current explicit owner instructions, including this audit-only scope.
2. Later, clearly recorded owner-approved decisions for the affected capability.
3. Current consolidated platform and product visions, with their dated amendments.
4. Product plans, architecture documents and contract specifications, reconciled against the above.
5. Historical reports, comments, tests and handovers as leads and evidence of a particular baseline.

Source code establishes what happens; it does not automatically redefine what ought to happen. A failing test may be wrong, a comment may be stale, and two engines may share the same defect. Check **contract conformance, parity and architecture compliance independently**.

Read these sources, using workspace-relative paths. Locate moved files rather than assuming an archived path remains current.

| Source | Required use |
|---|---|
| `forwardmeasure-platform/docs/technical-vision.md` | Governing product and engineering rules; especially §§5–9, 11–13, 15–26 and recorded conflicts. Read the whole document, not just the summaries here. |
| Platform `docs/implementation-status.md`, `docs/multi-framework-testing-status.md` | Current inventories and evidence leads; verify every relevant claim. |
| `forwardmeasure-openworkflow/docs/architecture-design.md`, `docs/implementation-status.md`, `docs/multi-framework-testing.md` | Full FOWF functional requirements, retained requirement IDs, remaining work, and per-cell evidence. |
| `forwardmeasure-entity-intelligence/docs/architecture-design.md` | FEI domain and service boundaries. |
| `forwardmeasure-data-streaming/docs/architecture-design.md` | FDS capability inventory; reconcile with the October 14 decisions below. |
| FDS `docs/fds-generic-engine-plan-2026-10-14.md`, `docs/fds-generic-engine-review-handover-2026-10-14.md` | Approved generic-engine, transform, engine-selection and journal work; implementation claims and concurrent changes. |
| FEI `docs/subject-and-monitored-population-deletion-plan-2026-10-14.md`, `docs/reference-population-deletion-plan-2026-10-14.md`, `docs/deletion-implementation-handover-2026-10-14.md` | Membership, provenance, lifecycle, concurrent deletion, archive/rehydrate/purge and any specifically approved reset scope. Distinguish implemented behavior from planned behavior. |
| FEI `docs/customer-master-entity-identifier-handover-2026-10-14.md` | Identifier domains and the consequences of conflating source/customer IDs with resolved entity identity. |
| FEI `docs/fei-lifecycle-identity-and-entity-directory-implementation-plan-2026-10-15.md` | Combined lifecycle, identity, ingestion aliases and directory scope; seven directory review findings; release and audit evidence requirements. The owner requested implementation/deployment before the next audit. Review actual delivered behavior and keep this audit's source-review-only operating mode. |
| Platform `forwardmeasure-platform-data-contracts/src/main/resources/contracts/catalog.json`, its README and `provider-field-semantics-2026-10-08.md` | Canonical provider assets, field dispositions, source/mapping/target/index ownership and pinned artifacts. |
| `forwardmeasure-decision-engine/docs/IMPLEMENTATION-SPEC.md`, `docs/rehabilitation/fde-rehabilitation-handover-2026-10-06.md` | FDE behavior and later security, versioning, cache, health and FOWF integration changes. Later sections/updates may supersede earlier status. |
| `forwardmeasure-jpa/docs/architecture.md` and actual adapters | Foundation contracts; its older schema-per-tenant description must not override current product database-per-tenant rules. |
| Platform `docs/archive/independent-parity-audit-request-2026-10-10.md`, `docs/archive/testing-guide-for-claude-2026-10-07.md`, `docs/archive/claude-testing-completion-handover-2026-10-09.md` | Historical audit/testing requirements and evidence gaps. Old stop instructions and old status are historical, not a new authorization or a permanent stop. |
| FOWF `docs/rehabilitation/requirements.csv` and current architecture checks | Existing requirement IDs to preserve and extend; reconcile with current visions. |
| Applicable `AGENTS.md`, parent/BOM POMs, `reactor.xml`, build wrappers, policy plugin and CI | Actual build, review, verification and enforcement rules. |

### 3.1 Known conflicts to resolve explicitly

- **FDS engine choice:** October 14 D4 says the installation selects the engine; the planner selects an optional Spark stage. Older platform §12 text says stateful transforms select Kafka Streams. Do not restore that older restriction.
- **FDS Pekko state:** October 14 D6 specifies Pekko Persistence using the selected PostgreSQL/Cassandra journal. Older text specifies Kafka-compacted correlation state. Audit the transition and recovery compatibility, not just the new class names.
- **Persistence queries:** the current default is JPA Criteria over the generated static metamodel. The earlier blanket preference for JPQL is superseded. Inventory JPQL/HQL/native/JDBC exceptions and their reasons.
- **Layering:** platform P7 requires application services to use domain services, with repositories underneath. Older FOWF PER-03 permits repository access from application services. Use the stricter working boundary for evaluation, identify the conflict, and do not pretend the older allowance never existed.
- **Tests:** complementary focused algorithm/storage tests and real API acceptance are both required. Do not delete useful direct coverage under an old REST-only interpretation. Do not replace managed business-service integration with hand-constructed repositories/services.
- **Framework default versus parity:** the agreed installation default is Quarkus; that gives Quarkus no exclusive capability or privileged implementation. Resolve wording that appears to prohibit the agreed default without weakening parity.
- **Migration history:** preservation is the normal rule. The October 14 FEI lifecycle plan records a coordinated fresh-tenant changelog fold/reset. Establish its exact approved release and data scope; do not condemn an explicitly approved fold as an accidental checksum change, or generalize it into permission to reset other databases. This audit authorizes no reset.
- **Reactors and licensing:** inspect current `reactor.xml`; older README statements exclude FEI while a later decision includes it. Inclusion in a source build does not make proprietary artifacts Apache-licensed or acceptable dependencies of open components.
- **Namespaces and display name:** standard names, existing legacy deployments and runtime `kriyagentic` branding are separate concerns. Verify current selection and recorded cutover decisions; do not infer that a namespace migration occurred.

Keep a decision ledger containing the conflicting passages, dates, implementation paths, working rule, impact and the precise owner question if one remains. Continue independent review while decisions are pending.

## 4. Scope and dependency closure

The audit is of the full current implementation, including old code reached by new paths. Reviewing only changed files is insufficient.

| Repository or area | Minimum audit scope |
|---|---|
| `forwardmeasure-openworkflow` (FOWF) | Specifications, compiler, shared runtime, both engines and both Pekko backends, adapters, definition/execution/query/human-task/tenant services, generated clients, Studio, migration and deployable leaves. |
| `forwardmeasure-entity-intelligence` (FEI) | Every reactor capability: reference and monitored populations, ingestion, contracts, screening, resolution, entities/assertions/membership, investigations/evidence, lifecycle operations, workers, generated clients, Studio and deployment. Do not omit modules absent from recent handovers. |
| `forwardmeasure-data-streaming` (FDS) | API and planner, transform SPI/catalog, mappers/readers/connectors, bounded/continuous engines, correlation/merge/state, loader/Spark handoffs, direct/workflow launchers, journals, migrations and deployment. |
| `forwardmeasure-decision-engine` (FDE) | Canonical protobuf and language clients, rule management/evaluation/admin, JPA persistence, compiled KIE cache, Valkey state, all framework servers, migration and FOWF adapter/bundle integration. |
| `forwardmeasure-platform` (FWP) | Parent/BOM/version and policy tooling, all shared API/data/request/error/configuration modules, generators/templates, tests/coverage infrastructure, deployment selection, installer, identity/secrets/topic/bootstrap/bundle wiring. |
| `forwardmeasure-jpa` | Entire used foundation: entities, repositories/services, metamodel, tenant routing/scopes/pools, transactions, locks, async tasks, framework adapters, Liquibase and contract-test infrastructure. |
| `forwardmeasure-database-migrations` | Migration orchestration, ownership/permissions, classpath resources, startup ordering and product adapters. |
| `forwardmeasure-authzen` | Authentication-derived organizations, token acquisition/cache, client audiences, resource/action identity, PDP denial/outage/cache/revocation behavior and framework wiring. |
| `forwardmeasure-object-storage` | Shared clients, provider routing, streaming and bounds, pinned objects/checksums, URI handling, credentials and lifecycle/cleanup. |
| `forwardmeasure-testcontainers` | Fixture fidelity, real authentication, schema/runtime roles, health, K3s/image/coverage collection, lifecycle/cleanup and whether fixtures conceal production omissions. |
| `forwardmeasure-entity-matching` | Used algorithms and evidence semantics, entity-kind handling, normalization, candidate recall, scoring/calibration, explainability and domain/licensing boundary. |
| `forwardmeasure-presentation-system` | Shared design system, frontend contracts/branding/build configuration and actual Studio consumption. |
| `helm-charts`, `openworkflow-k8s-setup` | Non-secret chart and infrastructure source used by products; production Makefile/OpenTofu flow, secret names, IAM, namespaces, migration/startup dependencies. Do not read credentials, tfvars or state. |
| Other first-party repositories | Follow all actual dependencies, generator/plugin extensions, worker/image references and runtime calls: e.g. Agent OS, NLP, document intelligence, events or platform operations. Record inclusion reason and exact modules. Do not silently exclude a shared runtime boundary because it lives outside the five products. |

The dashboard redesign was explicitly deferred. Inspect platform-operations code needed for tenant context, identity, shared routes or live dependencies; inventory dashboard gaps separately without expanding this audit into a dashboard rewrite. Do not activate Agent OS or invent its unresolved product role.

**Legacy Data Fabric is excluded from general searches.** A previous conversation allowed a narrow ingestion comparison at a named path; that is not blanket permission for a new platform-wide legacy scan. Use current contracts and approved field semantics. If a particular legacy comparison remains necessary, identify the narrow purpose and check the applicable authorization first.

### 4.1 Establish the denominator

Create a machine-readable module inventory, not a prose list of familiar repositories. Discover recursively from POM modules, parent/BOM imports, dependencies, active profiles, build extensions, generation/unpack tasks, ServiceLoader files, framework registration, packaged manifests and installer image/chart references.

For each module record: repository/path/GAV, license, role, incoming and outgoing dependencies, framework/engine applicability, generated inputs/outputs, public entry points, production callers, packaged/deployed inclusion, test ownership, coverage ownership and review status. Distinguish reactor membership, dependency availability, packaged inclusion and actual activation.

Include modules outside reactors, stale directories and modules activated only by profiles. A POM-only BOM, parent, specification, dependency wrapper or resource bundle is not automatically dead. Conversely, a class with tests but no production path is not automatically useful. All unreviewed or excluded rows require a reason.

Review first-party dependencies transitively. For third-party libraries audit pinned versions, API use, configuration, compatibility and the boundary assumptions the product relies on; a review of every line of third-party source is not implied. Consult version-matched primary documentation/source when behavior is uncertain.

## 5. Baseline and concurrent-work discipline

At audit start record repository HEAD, branch, dirty path names, UTC observation time, and hashes of relevant non-secret source files. A HEAD hash alone does not describe a dirty working tree. Include untracked files that participate in the implementation. Never print secret-bearing diffs or dump pod environments.

Use explicit repository paths with `git -C` and scope `rg` searches. Avoid an unscoped workspace scan that enters Data Fabric or credential trees. Do not stash, reset, checkout, format, stage or fetch merely to establish a baseline. Read existing build and test logs without taking ownership of the running builds.

Before finalizing each finding, re-read its source and hash it. If Claude changed the path, re-trace the caller and callee, mark the previous observation superseded, and report the new state. Do not combine a caller from one revision with a callee from another into a definitive finding. Preserve the original observation and explain what changed.

Record whether the review describes current dirty source, committed source, a packaged image or a deployment. A freshly edited source file cannot inherit validation from an older class file/image. If concurrent work prevents a stable conclusion, label that narrow area unstable and continue elsewhere.

## 6. Required structure and engineering posture

### 6.1 Ownership and dependency direction

The intended path is:

`canonical contract → generated server/client/models → transport adapter → application service → domain service → repository → forwardmeasure-jpa → tenant persistence`

Framework bindings provide composition, transactions and host integration. Deployment leaves provide the real process, startup, shutdown, configuration and packaging. Portable domain/application behavior must not be copied into three frameworks. Examine actual responsibilities, not package names alone.

FOWF is the durable workflow orchestrator. Workers execute bounded external work and return observations; they do not independently advance the workflow graph. Product domain lifecycle state is legitimate, but a second scheduler/orchestrator duplicating FOWF needs explicit justification. Trace timers, recovery loops and outboxes to determine whether they are necessary adapters or competing execution systems.

Shared capabilities have one canonical owner. Inventory duplicate HTTP/storage/security clients, token caches, retry loops, schema validators, DTOs, enums, mappers, resource resolvers and deployment helpers. Recommend a concrete owner and migration direction, not merely "deduplicate".

### 6.2 Full engine and framework parity

**The desired scope is 100% of the required observable contract on every supported engine and framework.** Do not reduce a working engine to the lowest common denominator, disable a difficult feature, or call one framework "later". Internal storage formats and algorithms may differ. Timing, memory and throughput are measured properties, not assumed identical; correctness, supported capabilities, identity, failure semantics and configuration meaning must match.

FOWF has nine installation cells:

| Framework | Kafka Streams | Pekko/PostgreSQL | Pekko/Cassandra |
|---|---|---|---|
| Quarkus | Q-K | Q-PG | Q-PC |
| Spring Boot | S-K | S-PG | S-PC |
| Micronaut | M-K | M-PG | M-PC |

Record the framework of **every participating service and worker**. Spring FOWF calling Quarkus FEI/FDE is not an all-Spring installation. Test each framework's real dependency injection, transactions, route generation, serializers, security filters, lifecycle and executable packaging.

For FDS retain the eight base capabilities: single/correlated × bounded/continuous × Pekko/Kafka Streams. Expand into direct/FOWF admission, applicable frameworks, Pekko journal backends, connectors, loader and Spark stages. Do not replace this with an old passing-cell total. State why a combination is inapplicable; unsupported required behavior is a gap, not N/A.

FEI's APIs, worker choices and journeys follow the installation selection and must have matching effects in all nine cells. FDE is a gRPC rule service, not a new Pekko/Kafka engine: its own three hosts and embedded/external Valkey modes require coverage, plus its FOWF invocation across nine cells. Deployment selection covers all 18 framework × engine-profile × FDE-enabled/disabled combinations.

**Independence:** no Pekko implementation/runtime in Kafka Streams or Spark artifacts; no Kafka Streams implementation in Pekko artifacts; neither engine in shared neutral modules. Ordinary `kafka-clients` transport use is not automatically a Kafka Streams leak. Check POM closure, imports, generated sources, shaded jars, manifests, actual image libraries and launched subprocesses. Compilation across both engines does not justify packaging both into one selected runtime.

Parity must use an independent contract oracle. Identical wrong results do not pass. Do not claim mathematical proof of all possible executions; tie acceptance to an explicit requirement denominator, contract revision, baseline and executed evidence.

### 6.3 Contract first and mapping

- HTTP capabilities have canonical OpenAPI; worker/message boundaries have AsyncAPI; data shapes have JSON Schema/OpenAPI; FDE uses canonical protobuf. Inventory every operation/RPC/channel and implementation/consumer.
- Generated Java, TypeScript and Python clients/models/stubs come from the same contracts. No handwritten replacement client, duplicate wire model or edit to generated output. Fix schema/templates/configuration when generation is wrong.
- Verify generator mappings actually take effect, generated packages have one owning jar, and classpath shadows cannot select an obsolete model. Check resource classifiers and clean/cache-hit generation, not only existing `target` files.
- Authentication declarations must reach generated clients. Verify optional/null/absent fields, enum values, numeric types, nested validation, discriminators, unknown fields, content types, errors and pagination on all hosts.
- Shared Problem/Violation and EntityKind types must retain one canonical owner. Preserve concrete entity kinds across APIs, contracts, persistence and mapping. An algorithm's deliberate fallback must not silently rewrite stored domain identity.
- Use MapStruct at fixed typed boundaries. FDS/FEI runtime provider mapping is an intentional metadata-driven exception, not a reason to handwrite API/entity mappers.
- Publication/admission pins the schema, mapping, output/index contract, source identity and executable revision needed for reproducibility. Mutable URI contents must not change an admitted run.

### 6.4 Persistence, transactions and tenancy

- Use `forwardmeasure-jpa` repository/service foundations, appropriate shared entity bases, Lombok conventions and generated static metamodel Criteria queries. No silent Panache, Spring Data or Micronaut Data substitute.
- Only repositories and justified persistence/transaction infrastructure access EntityManager/Session/SessionFactory. Inventory every exception with necessity, transaction boundary, tenant behavior and tests. Do not ban the infrastructure that legitimately constructs these objects.
- Carry verified `TenantId` across application/worker boundaries; physical database resolution belongs to persistence. Current products use a database per tenant and functional schemas per product. Trace and classify remaining legacy schema-per-tenant code.
- Unknown/inactive tenants fail closed. The owner requires rejecting new database connections after deactivation; check warmed caches, reconnection, pools and long-lived journal clients, not just the first request.
- Trace actor provisioning and ownership. An unknown principal must not silently acquire an actor or privileges on first login under the current policy.
- Commit precedes acknowledged success. Check proxy/self-invocation, mandatory transactions, exceptions, rollback, optimistic versions, lost updates, concurrent allocation, lock ordering, paging stability, materialization of lazy data, resource closure and pool reset.
- Async/reactive/event threads establish fresh tenant and transaction scopes. Do not propagate an open EntityManager or assume thread-local identity survives scheduling.
- Pekko journal/snapshot/projection plugins are legitimate backend-specific persistence. Their ownership, tenant isolation, leases, fencing and migrations remain reviewable; they are not business JPA repositories.
- Runtime roles are restricted. Test/admin roles must not hide missing grants or schema/search-path mistakes. No runtime ORM schema creation or open-in-view.

### 6.5 Authentication and authorization

Trace browser/user and machine identities separately through ingress, resource, application service, workflow, adapter, worker and callback. Verify issuer, audience, expiry, signature, organization membership, tenant conflicts, resource/action authorization and least privilege. Unsigned tenant/actor headers or workflow fields are never authoritative.

Cover two tenants with colliding business IDs and a caller entitled in only one; concurrent tenant-specific client tokens; token/PDP caches; warm-cache revocation/deactivation; key/secret rotation; interrupted reconciliation; denial and outage behavior. Distinguish established revocation latency from an unimplemented immediate-revocation promise. Preserve the platform's documented HTTP security/error policy rather than improvising different framework defaults.

Check SSRF/egress resolution, redirects, URI schemes, local-file access, object paths, command/argument injection, image digests, Kubernetes namespaces/service accounts, workload identity and secret references. Verify admitted workers cannot choose another tenant through environment or payload overrides. Report actual reachable paths and prerequisites, not speculative security labels.

### 6.6 Durability and uncertain outcomes

For every side effect draw the order of: admission transaction, durable intent, claim/lease, external request, external acceptance, result persistence, projection and acknowledgement/offset commit. Examine a crash or lost response **between each adjacent pair**, plus restart, redelivery, rebalance and overlapping workers.

The owner-approved rule is: if a remote action may have succeeded and there is no safe deduplication or outcome lookup, expose `OPERATION_OUTCOME_UNKNOWN`, abort the flow, and retain useful redacted exception/correlation information. Do not blindly retry or send ambiguity through ordinary DSL catch/retry handling. Known pre-dispatch rejection, confirmed failure and uncertain acceptance are distinct.

Do not ban all retries. Proven idempotent command delivery, durable receipts and safe outcome lookup may support retries under their contract. Verify command identity survives retry, lease expiry cannot let stale senders overwrite current state, attempt policies do not abandon admitted work accidentally, and retries are bounded in time/resources where required. A timeout does not prove the remote action stopped.

An idempotent final write is not proof that replaying an aggregate, join, counter, ledger append or cache mutation is safe. Check input receipts, state and output atomicity separately. A single replica/Recreate strategy is not automatically storage-level fencing under every failure.

## 7. Product journeys to trace completely

For every journey identify the initiating API/UI/event, authentication, contract validation, transaction, durable records, orchestration, worker identity/config, effect, result callback, projection, query and visible terminal outcome. Include timeout, cancellation, duplicate, replay and partial-failure paths.

### 7.1 FOWF

Use every requirement ID and normative construct from the current vision, not only these examples:

- Author draft → validate → publish immutable revision → resolve logical resources/contracts → start pinned revision → query history/output. Definition publication has no maker-checker requirement; maker-checker belongs to human tasks when policy requires it.
- All DSL task kinds and combinations: `do`, `set`, `call`, `emit`, `listen`, `wait`, `for`, `fork`, `switch`, `try`, `raise`, `run`; nested branches/loops; input/output/export transforms; schemas, jq, retries, timers, child and scheduled workflows, cancellation and compensation where contracted.
- Start receipts and dispatch outbox/async tasks; concurrent API/recovery claims; restart and duplicate commands; stale versions; tenant-fair backpressure and configurable limits.
- Identical canonical lifecycle events, publisher transport/configuration, error bodies/type URIs, start-input validation, public execution visibility and tenant data placement across engines. Query/projection lag must not masquerade as nonexistent accepted work.
- Internal children and scheduled executions remain visible and auditable. Projections survive duplicate/out-of-order events and reconstruction without querying an unrelated engine's private store.
- All protocol implementations actually exposed by contracts: HTTP/OpenAPI, gRPC, AsyncAPI transports, Kubernetes Jobs/Deployments, OCI, MCP, A2A and others discovered. Audit the full command/event/cancellation lifecycle, not just request formatting.
- OCI rejected launch versus real process exit; container ownership for cancel/timeout/cleanup; name collisions; worker completion versus observation loss.
- Large input, state, protocol results, fork results and final output: inline/spill boundaries, serialization, shared object storage, checksums, authorization, recovery and cleanup.
- Human-task eligibility, claim/lease, revisions, optimistic concurrency, maker-checker, disposition, expiry/cancel races and audit.
- Studio lossless editing/round-trip, validation/publication, live control/history, auth errors and tenant routes through generated clients.

### 7.2 FEI

- Reference population create/configure → source/contract admission → workflow → selected-engine load/delivery → OpenSearch → revision ready → activation → screening. Assert document contents and counts, not merely READY/202.
- Monitored population/customer-master ingestion → mapped resolution records → assertions → matching/merge → resolved entities → membership/support/provenance → screening. Preserve source identifiers and resolved entity identifiers as distinct domains.
- Source schema, mapping, output schema and OpenSearch index mapping are distinct artifacts with explicit ownership. Read every catalog entry and every alternative fixture; find stale duplicate schemas and hard-coded target models. Do not call an index mapping correct solely because JSON parses.
- WorldCheck and customer-master parsing: headers including duplicates/blanks, delimiter, quoting, multiline/large fields, charset, null/empty/missing values, nested source preservation, date and entity-kind semantics. Use approved bounded representative fixtures, never copy proprietary full datasets into reports/tests.
- Seven concrete entity kinds versus the `unknown` fallback; provider-specific category interpretation; organization records with no personal first name; explainable rejection with exact field/value policy and no sensitive record dumping.
- Full/delta ingestion, update/delete, baseline compatibility, checksum verification, immutable admitted contracts, partial batch failures, counts, retries, concurrency, cancellation, failed callback and recovery. Failed/partial refresh must not withdraw unrelated live supports.
- Multi-source correlation, trust/merge, cardinality errors, source-specific versions, re-resolution and reversal. Multiple populations supporting one entity must retain distinguishable provenance and memberships.
- Screening candidate retrieval and final scoring, person/organization/kind handling, effective request overrides, active population revision, quality gates and explained results. Verify real-data recall/precision requirements from the vision; do not invent thresholds or declare statistical quality from a toy fixture.
- Domain operations on assertions, candidates, entities, investigations, evidence and relationships; all actual CRUD/query/export operations and authorization, beyond ingestion.
- Deletion, suppression, retirement, reactivation, archive/offload/rehydrate/purge: ownership, reservations/leases, dependency snapshots, deterministic recompute, surviving supports, retained audit, external cleanup, interruption, concurrent screening/ingestion and idempotent replay. Distinguish removing membership, deleting an entity and erasing evidence.
- API-created tenants versus statically configured tenants: identity grants do not imply schema, index, topic, workflow or worker readiness. Document supported provisioning and exact gaps.
- FEI Studio at tenant-scoped `/ei/studio/`: forms submit intended values, errors are useful, progress is real, refresh/back navigation preserve tenant context, and lifecycle actions reflect server authorization.

### 7.3 FDS

- Generic code remains provider/domain-neutral. Proprietary entity matching, FEI schema resources and fictional production screening transforms must not leak into FDS. Test-only transforms use the public SPI and stay test-scoped.
- `NamedTransform` characteristics and catalog discovery agree in planner, mapper, Spark executors and worker image classpaths. Duplicate/unknown names fail clearly. Caller-supplied catalogs survive serialization and process boundaries where promised.
- Installation engine governs execution. Plain Kafka clients in loader/delivery stages are intentional on the Kafka installation; the class name need not imply a full Kafka Streams runtime for a bounded consumer.
- Source/mapping/graph/merge/sink semantics remain intact through direct admission, workflow admission, loader handoff and Spark handoff. Inspect recompiled specs, target fields, source identity, merge-policy pinning and planner recursion.
- Stateful graphs: keyed isolation, event-time parsing, watermark/grace policy, late/out-of-order data, dedup boundaries, join windows/cardinality, aggregation ordering, graph composition, malformed-record effects and EOF/drain behavior.
- Restart: input offsets, state, output and acknowledgements; committed and uncommitted changes; partial output; stale writer fencing; journal schema/credentials and deactivation. Test each backend's actual semantics.
- Loader topics: explicit ownership, replication/partitions/retention, run/attempt identities, row ordering, producer/session retries, partial load, completion summary/checksum, count validation before irreversible completion and cleanup.
- Bounded Kafka reads use a fixed read-committed frontier; account for aborted transactions/control records, sparse offsets and empty topics. Continuous work has real shutdown/cancel/drain, not just an HTTP-ready shell.
- Spark is optional compute and hands off to the selected delivery engine; it does not become an alternate business sink or silently omit graph nodes. Preserve all accepted combinations or identify unsupported required work.
- Correlation and merge include at least three sources, reversed orders, trust ties, unmatched/duplicate keys, conflicts, nested unions and provenance. Identical last-write-wins behavior is not automatically the required merge contract.
- Inspect complexity and resource bounds: all-key scans per record, single-partition bottlenecks, whole-file/RDD materialization, unbounded state/collect lists, per-row round trips and topic proliferation. Separate correctness defects from measured/unmeasured scale risk.

### 7.4 FDE

- Trace every canonical management/evaluation/admin RPC through generated clients and real framework registration. Do not invent a REST layer or separate Studio to make the audit easier.
- Create/version/activate/list/delete rulesets; concurrent monotonic allocation without reuse; first active version and active-delete rules; immutable pinned evaluations; populated-database upgrades and counters.
- Rule compilation, unique tenant-scoped KIE identity, cache limits/eviction/warm/unload, active-session reference ownership, disposal and shutdown. Process-local statistics must not be represented as cluster totals.
- Valkey fact windows: tenant/ruleset/version/session keys, collision-safe encoding, TTL, concurrent append, retry/response loss, script-cache eviction, auth/rotation, restart and embedded/external modes.
- Append plus evaluation is not a distributed exactly-once transaction. Confirm the FOWF adapter reports ambiguous post-dispatch outcomes safely.
- Drools consequences are executable code. Management privilege, firing limits, temporary storage, CPU/memory limits and any documented trust boundary must match the implementation; a firing limit is not a wall-clock sandbox.
- JWT/organization/audience and separate evaluate/manage/admin actions; warm caches must not bypass tenant deactivation. Numeric gRPC probe ports and health marshaller/interceptor behavior across all three frameworks.
- FOWF bundle → pinned `.proto` → bounded compiler/runtime resources → exact egress grant → tenant token → real evaluation → output/error projection. Cover generated language bindings and packaged resources, not only an in-process Java evaluator.

### 7.5 Platform and deployment

- Resolve selection once: explicit environment override → environment/shared configuration → default. Defaults are Quarkus, Kafka Streams, FDE enabled. Validate booleans/unknown/empty values, print effective selection and pass one frozen snapshot to all products.
- Images, digest inventories, allowlists, service addresses, migration/topic dependencies, worker choices and workflow bundles must follow that same selection. Test true/false distinctly and reject missing selected-image pins.
- Engine changes require a separate state migration design; never silently treat a selector change as a rolling upgrade. Namespace change also requires IAM/RBAC/Secrets/routes/storage/consumer review and coordinated drain, not just changing `metadata.namespace`.
- Trace install dependency order from infrastructure/identity/secrets and database migrations through topics, engines, APIs/adapters, product services, workers and bundles. Verify no readiness cycle depends on a resource scheduled later.
- Topic provisioning must cover source, repartition/changelog/internal stores and tenant lifecycle as actually used. Tests must not pre-create production-owned resources to conceal a missing release.
- Read shared chart defaults and composed product values together: selectors, labels, services, Jobs versus server pods, probes, ports, native sidecars, rollout strategy, termination, writable paths, resources, HPA, network policy and service accounts.
- Match source → generated Dockerfile/artifact → image digest → Helm pin → running role. New images must not be omitted by a digest synchronizer that only refreshes existing pins.
- Trace secret *names and keys* from OpenTofu/Makefile through Secret Manager references, ExternalSecret composition and workload config without reading values. Rotation/reconciliation must be restart-safe and idempotent.
- Verify tenant hostname/certificate SAN, ingress path rewrites, OIDC redirects, API prefixes and Studio asset paths. The dashboard can stay deferred; a shared routing defect cannot.
- Brand externally as configured `kriyagentic` while preserving internal `com.forwardmeasure` identity. Audit actual runtime settings and shared design-system consumption, not a cosmetic search-and-replace.
- Audit BOM/plugin centralization, profile wiring, Java/toolchain compatibility, generation, cache inputs and always-run side effects. A cached build must not skip publication/generation tasks incorrectly or supply stale classes as coverage evidence.

## 8. Latent defect, dead code and inconsistency searches

Use static searches as entry points, then inspect the actual call graph and registrations. Every positive match is a lead, not a finding by itself.

Look for unsupported operations/TODOs/stubs, `.unconfigured()`, empty success returns, fixed demo data, swallowed exceptions, catch-and-log success, unconditional retry, unbounded loops/maps/queues, hard-coded tenant/framework/engine choices, implicit first-tenant selection, mutable resource fetching at runtime, hand-built clients/DTOs, direct ORM outside repositories, string field names instead of metamodel, test-only dependencies in production, duplicate packages, obsolete configuration and unreachable factories.

Classify suspected dead code as one of: reachable; externally exposed library API; reflection/DI/ServiceLoader/generated registration; planned but unwired; compatibility support with named consumer; test-only; redundant implementation; obsolete candidate. Search framework metadata, configuration, chart commands, manifests, SPI entries, generated sources, scheduled startup and public consumers before concluding "unused". Explain the production trigger that is missing or the duplicate implementation that owns the behavior instead. Do not delete anything in this audit.

Inventory empty modules, directories outside reactors, unused templates/theme assets, deprecated namespace/OKS/schema conventions, old engine stores, dead flags, generated snapshots and alternative fixtures. Identify whether each is legitimate packaging/compatibility, intentional roadmap scope or avoidable debt. Record retirement risk, consumers and a proposed owner; avoid unsupported "safe to remove" claims.

Review comments, READMEs, status ledgers and tests that assert obsolete behavior. A stale comment is documentation debt; stale production selection or mismatched wire contracts are correctness defects. Keep these severities distinct.

## 9. Testing and coverage gates

This section specifies what evidence the platform needs. It does **not** authorize running suites during this review. Inventory existing tests and reports now; provide exact missing scenarios and proposed commands for a separately authorized execution phase.

### 9.1 Required test layers

| Layer | Required entry point | Evidence limit |
|---|---|---|
| Pure logic/value/algorithm | Actual component, independent expected output | Does not prove DI, identity or persistence. |
| Repository/storage contract | Actual adapter and migrated real backend with appropriate roles | Does not prove public API or service wiring. |
| Managed service integration | Production framework-managed service/transaction path | Does not establish HTTP/gRPC admission. |
| API conformance | Real authenticated HTTP/gRPC through each actual framework | Does not prove a dispatched worker unless it runs. |
| Engine/protocol recovery | Real engine and transport/backend; controlled fault boundary | Applies only to the exercised state transition and backend. |
| Packaged installation | Current images in disposable K3s using shared fixtures and production resource ownership | Does not certify other image digests or configurations. |
| Browser journey | Actual Studio → generated client → gateway/API → worker/effect | Screenshot or TypeScript compilation alone is insufficient. |
| Scale/quality | Explicit representative dataset, bounds, metrics and independent oracle | Small fixtures cannot certify 100 GB/TB or matching quality. |

Use `forwardmeasure-testcontainers` and shared real PostgreSQL, Kafka, Keycloak/AuthZEN, OpenSearch, object-storage, Valkey, Cassandra and K3s fixtures. No H2, Mockito or equivalent mocking framework. A narrow handwritten double needs an explicit reason and cannot stand in for backend/security acceptance. Do not weaken authorization or grant superuser access to make a scenario pass.

Business acceptance data enters through the owning API. Product-managed service tests resolve the real production bindings and interceptors. SQL is for justified infrastructure setup/catalog/permission/upgrade observations, not secretly implementing the behavior under test. Required non-API prerequisites use the established test-fixture migration mechanism. Never pre-create a production table/topic or repair Liquibase bookkeeping to rescue an acceptance test.

For every new test proposed, name the concrete defect it would detect, independent expected effects, entry point, real dependencies, framework/backend cells and failure assertion. Include successful authenticated calls and meaningful negative cases for **every** OpenAPI operation/RPC/channel. Derive the denominator from canonical specs, not a frozen count. A 401-only smoke test, a non-null result or HTTP 202 alone does not qualify.

### 9.2 Fault and concurrency requirements

Across applicable journeys cover: two tenants with equal business identifiers; duplicate commands/events; reordered messages; stale versions; two concurrent callers/workers; lease expiration; deactivation with warm caches; failure before and after commit/remote acceptance/offset commit; process death without graceful shutdown; lost responses; rebalance; journal restore; sink partial write; callback retry; clock/deadline edges; cleanup after cancellation; source/contract mutation after admission; repeated install/migration; interrupted secret rotation; inactive/missing dependencies and recovery.

Use deterministic coordination around the relevant boundary, not sleeps that merely make a race unlikely. Distinguish restart tests that process the next input from tests that redeliver the previously committed input. Wire fixtures must originate from the real serializer and preserve supported old manifests; rewriting bytes with a side utility is not authentic serializer evidence.

### 9.3 Numeric coverage policy and enforcement

The current platform gate is **85% line, 85% branch, zero missed production classes** for handwritten Java under the configured module/aggregate ownership. This is not the obsolete 75% branch threshold and not a demand for arbitrary 100% line coverage. Full behavioral parity and numeric code coverage are separate obligations.

Audit the actual JaCoCo/policy-plugin/CI wiring. Find modules missing from any gate, profile-dependent bypasses, disabled tests, skip properties, broad exclusions and empty execution data. Generated classes are exempt only through actual generated provenance/retained annotations and exact exclusions; handwritten code in a generated-adjacent package is not exempt. Verify `target/coverage-generated-excludes.txt` and the configured ownership mechanism.

JPA/FOWF/FEI aggregate ownership and FDS/FDE configured ownership must include the actual current classes/versions. A module-level skip is valid only if its designated aggregate really measures and checks it. Remote/packaged tests require coverage collection and class-ID alignment; passing container tests do not automatically cover local Maven classes. Preserve failed reports and distinguish no data, stale data and genuinely uncovered behavior.

Do not lower thresholds, add meaningless getter tests, delete hard assertions, exclude difficult classes or modify production APIs purely for coverage. Coverage cannot prove semantic correctness, isolation, durability or parity, even at 100%.

### 9.4 Separate completion gates

| Gate | Required result |
|---|---|
| G0 Scope | Every repository/module/contract/capability and dependency edge inventoried; omissions explicit. |
| G1 Architecture | Ownership/layering/engine separation/contracts/tenancy compliant, or deviations individually recorded with owner decisions. |
| G2 Static correctness | Every production journey traced, high-risk branches reviewed, findings supported and rechecked at final baseline. |
| G3 Build/package | Current production and test source compile; formatting/license/policy/generation checks and actual package dependency boundaries verified. Not satisfied by old logs. |
| G4 Behavioral conformance | Complete known operation/scenario denominator exercised through real boundaries with effects and negative cases. |
| G5 Parity/recovery | Applicable nine-cell and product-specific matrices complete; independent oracle; failure/concurrency/upgrade cases covered. |
| G6 Coverage | Current aligned 85/85/zero-missed-class checks pass with valid collection and ownership. |
| G7 Deployment/UX | Selected current images, migrations/topics/secrets/IAM/routes/startup and actual Studio journeys verified. |

A source audit may complete G0–G2 while reporting the others unverified. Do not call the platform complete because the audit completed. Record any explicit owner risk acceptance separately; it is not a passing test or a waiver of the architecture by inference.

## 10. Audit execution method

1. **Inventory and authority.** Record baselines, read requirements, enumerate modules/contracts and contradictions. Reconcile the module list with packaging and installer references. Preserve existing requirement IDs and add stable IDs for newly discovered capabilities.
2. **Shared foundations.** Trace identity, errors/contracts, JPA/transactions/tenancy, migrations, object storage and build/test infrastructure first; shared defects multiply across products.
3. **Vertical journeys.** Follow each journey in §7 from the real trigger through durable effects and public observations. Read the implementation and configuration at every boundary, including negative/recovery paths. Record exact call-path files and symbols.
4. **Systematic sibling sweep.** For each defect, inspect equivalent framework hosts, engines, backends, worker types and products using that foundation. Do not stop at the first matching file or repeat the same root cause as many unrelated findings.
5. **Independent parity comparison.** Build a behavior table per requirement across cells, including normal, malformed, denied, duplicate, timeout, cancel, restart and large-input outcomes. Compare to the contract as well as to each other.
6. **Deployment/package review.** Follow selected configuration to resolved dependencies, images/resources and real startup triggers. Static YAML or a factory method alone cannot prove runtime activation.
7. **Tests and evidence.** Read test assertions, setup and selection filters. Map each claim to exact scenario and baseline; identify omitted cases, fixture contamination and coverage ownership holes. Do not execute tests without authorization.
8. **Final source reconciliation.** Recheck changed files and findings against Claude's latest work. Mark fixed-in-source separately from verified. Produce the reports, remaining-scope ledger and ordered repair/verification plan.

These are review passes, not permission to narrow delivery to an initial slice. Continue until every inventory row has a disposition. If context/time requires continuation, leave a precise resume ledger: completed rows, unreviewed paths, current hypotheses and next concrete reads. Do not silently redefine "complete" as "reviewed the riskiest files".

Read-only examples (run individually from explicit locations, excluding credential paths):

```bash
git -C /home/pn/Documents/code/forwardmeasure/forwardmeasure-data-streaming rev-parse HEAD
git -C /home/pn/Documents/code/forwardmeasure/forwardmeasure-data-streaming status --short
rg --files /home/pn/Documents/code/forwardmeasure/forwardmeasure-data-streaming -g pom.xml
rg -n 'UnsupportedOperationException|TODO|not yet implemented|unconfigured' \
  /home/pn/Documents/code/forwardmeasure/forwardmeasure-data-streaming/forwardmeasure-data-streaming-core/src
```

Do not run a root-wide `cat`, `git diff` or environment dump that can expose credentials. A document-generation script or link checker is not a product test, but it must remain scoped to audit artifacts.

For a later authorized Java implementation phase, applicable `AGENTS.md` requires the bounded wrapper and compilation of production **and test** sources. The compile-only lifecycle is `test-compile -DskipTests -DskipITs -Dmaven.test.skip=false`; `-Dmaven.test.skip=true` cannot prove test compilation. Do not advance to test/verify/install merely to claim a compile check. Derive exact module/build order from the current closure and coordinate the shared memory/build lock. Never launch an hour-long whole-stack rebuild as a speculative audit probe.

## 11. Deliverables and finding format

Write new audit artifacts under `forwardmeasure-platform/docs/audits/<audit-date>-codex/`, without overwriting another reviewer's reports. Use a suffix for multiple audits on one day. Keep raw logs and large evidence outside Git in a durable validation directory; do not rely only on `/tmp`.

Required outputs:

1. `report.md`: prioritized findings, practical effect, scope limits, gate status and precise conclusions per product/shared layer.
2. `baseline.json`: repositories, commits/branches, dirty paths and source fingerprints; observed image/evidence identities only where actually inspected.
3. `module-inventory.csv`: full closure, role/ownership, production callers, package/deployment inclusion, review and coverage disposition.
4. `requirements-and-parity.csv`: requirement ID/source, operation/journey, implementation paths, each applicable cell, test evidence and gaps. Missing cells remain visible.
5. `findings.md`: complete reproducible records, including sibling searches and current-source rechecks.
6. `dead-code-and-deviations.md`: reachability classifications, duplicate implementations, undocumented exceptions, stale docs/contracts/config and unresolved decisions.
7. `verification-plan.md`: concrete missing tests and coverage collection fixes, exact intended scope/commands/prerequisites when known, and why existing tests cannot prove the claim.
8. `repair-plan.md`: root-cause order, impacted modules/frameworks/engines/contracts, migration/wire risks, regression requirements and specific image/chart rebuild scope. No implementation performed under this assignment.

Each finding must contain:

```text
ID / severity / title
Classification: correctness | isolation/security | parity | architecture |
                deployment | verification gap | documentation | dead-code candidate
Baseline: repository, commit, dirty source hash, observation and recheck time
Requirement: canonical rule/contract and any conflicting approved decision
Evidence: exact file, line, symbol, relevant caller and registration/configuration
Trigger: concrete reachable input, state or failure schedule
Expected behavior / actual source behavior / consequence
Affected cells and products; explicitly unaffected or unreviewed scope
Proof level: statically traced | reproduced (with existing valid evidence) | hypothesis
Sibling paths checked and their disposition
Existing test gap and proposed regression with independent assertions
Recommended repair direction and compatibility/rollout implications
Status: open | fixed in source, unverified | verified at named baseline |
        superseded by concurrent edit | decision required | rejected with evidence
```

Use P0 for demonstrated catastrophic/immediate exposure or loss, P1 for serious reachable corruption/isolation/lifecycle or required-capability failure, P2 for bounded defects and meaningful operational/architectural gaps, and P3 for lower-impact clarity/maintenance debt. Severity requires impact and reachability, not alarming wording. A suspected defect is a hypothesis until its call path and prerequisites are established. A missing test is an evidence gap, not automatically a production defect.

Report evidence using separate dimensions: statically traced; fixture missing/written; compiled; executed pass/fail/skipped; packaged-image verified; deployed verified; browser exercised. Record actual contract hashes, test methods, framework/backend, image digests, command/log/exit status and persisted-effect assertions. Do not add scenario counts, test methods and matrix cells together or count reruns as additional coverage.

## 12. October 14 review leads to recheck

Claude is fixing these concurrently. They are **historical source findings from the preceding review**, not assertions about the next agent's starting tree. Locate the current implementation, assess the fix and its siblings, and retain exact evidence.

| Lead | Original path/symbol in FDS unless stated otherwise | What the recheck must establish |
|---|---|---|
| Graph planning null mapper | `ExecutionPlanCompiler.allFieldRules` | Deduplicate/aggregate/join nodes with no mapper compile on both engines; characteristics only inspect applicable rules; real parity fixtures traverse the planner. |
| Pekko state/offset crash gap | `ContinuousPekkoStreamsConsumerRunner.sendThroughGraph`, correlation runner, `JournalGraph.commit` | Crash after journal commit before Kafka offset commit cannot reapply input to aggregate/join state. Include replay of the same input, not only orderly restart. |
| HEAVY graph handoff | `SparkHandoffSpecs.deliveryStageSpec`, compiler, Spark and delivery runners | Accepted graph transforms are executed once at the intended stage; delivery planning does not request an unhandled second Spark stage; joins preserve source identity. |
| Malformed graph output and state | `GraphRecords.process`, `StatefulGraph.closeWindows`, journal staging | One rejected downstream result cannot silently remove valid closed-window outputs or commit unintended partial state; compare per-node Kafka handling and whole-chain Pekko handling. |
| Loader reuse | `KafkaRowWriter.ensureTopic/write`, `SourceLoader`, FEI `SimpleSourceLoadStage`/`SimpleSourceDeliveryStage` | Repeated/partial load on the same run identity cannot append an unsafe duplicate prefix or incompatible summaries; count checking after sink writes is not atomic rejection. |
| Window timestamp ordering | `StatefulGraph.sortableTime` | Signed-long overflow cannot reverse chronological key ordering; cover pre/post-epoch and multiple windows flushed together. |

Also recheck the transform/catalog ownership change, shared EntityKind generation/import mappings, removed tests and repinned fixture hashes. The old review handover explicitly mentioned a deleted `SharedSourceContractTest` and incomplete transfer of its WorldCheck coverage: establish whether that gap has since been repaired rather than asserting a lost-test count.

Other priority historical leads: shipped async-task migrations; actual serializer-produced wire fixtures; worker/container ownership; tenant registry ACTIVE checks on new connections; engine/framework dependency leakage; authentication at the real boundary; startup factories actually invoked; source-schema/mapping/index drift; missing topic releases; image digest allowlists; projection/query visibility; namespace/routing/secret composition. None is a substitute for the complete inventory.

## 13. Periodic use and the completion statement

For a later audit, retain this procedure and create a new dated output directory. Rebuild the module/contract denominator; do not merely reopen old findings. Compare additions/removals, dependency/ownership changes, contract and serialization changes, new configuration surfaces, test skips/exclusions and coverage ownership. Invalidate old evidence wherever shared code or packaging changed, with explicit rationale for evidence that remains applicable.

Run both the full inventory pass and the risk-driven journey pass. A healthy regression suite is not proof that newly introduced capabilities are in its denominator. Sample-based investigation may prioritize work but cannot be reported as the requested complete source audit.

The final audit answer must say:

- Which repositories/modules/capabilities were actually reviewed and which remain unreviewed.
- The current baselines and whether concurrent edits changed conclusions.
- Confirmed defects, architectural deviations, decision conflicts and verification gaps separately.
- Which parity/coverage/deployment gates are satisfied by admissible evidence, failed or unverified.
- What must be repaired, what must be tested, what needs an owner decision, and the next concrete actions.

Do not say "code complete," "100% parity," "production ready" or "only tests remain" when a required code path, capability, integration, migration or parity cell is absent or unresolved. Equally, do not portray a statically plausible defect as an executed failure. The purpose of this audit is to make the remaining work finite, traceable and difficult to hide—not to produce another reassuring handover.
