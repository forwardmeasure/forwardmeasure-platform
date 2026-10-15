# Population contracts and monitored-population membership: implementation plan

Date: October 12, 2026. Schedule references use America/New_York (EDT on this date).

Status: proposed implementation plan, incorporating the user's architectural decisions. This document
does not assert that the proposed model is implemented. Implementation ownership is undecided; the
user will choose whether Codex or Claude performs it. No production changes, tenant reset, test run
or deployment is authorised merely by this planning document.

## 1. Objective and agreed model

Give a population a stable destination contract while permitting multiple provider input formats.

| Symbol | Meaning | Ownership and lifecycle |
|---|---|---|
| S1 | Target document schema | Fixed for the lifetime of the population |
| M1 | Destination configuration; for reference populations, OpenSearch settings/mappings | Fixed alongside S1; not uniquely derivable from S1 |
| S2 | Provider source-record schema, with parsing metadata | Part of an immutable version of an input definition |
| M2 | Transformation from S2 records into S1 records | Versioned together with S2 and explicitly bound to S1 |

One reference population has one immutable S1/M1 pair and can register multiple S2/M2 pairs. Different
reference populations may share the same target contract. WorldCheck, OFAC and proprietary inputs
can target the same screening shape without being combined into one dataset.

An ingestion selects one registered input-definition version, inherits the population's target, and
pins the effective four-part contract. Users configure the target at population level, not separately
for every revision. Revision snapshots establish provenance and deterministic replay; they do not
permit arbitrary changes to the population target.

For monitored populations, use the same fixed-destination principle. Their destination is FEI's
resolution model. M1 is a platform-owned resolution/persistence binding, **not a user-defined
OpenSearch index mapping**. Do not move customer-master persistence into a reference index to make
the terminology symmetrical.

### Architectural qualifiers and scope boundaries

These qualifiers are part of the agreed direction, not optional implementation suggestions:

1. **Share principles, not necessarily identical machinery.** Reference populations are indexed
   datasets with revision activation and index lifecycle. Monitored populations are memberships over
   candidates/resolved entities, backed by source records and assertion provenance. Reuse contract
   parsing, validation and immutable snapshot utilities where appropriate, but do not force both into
   one persistence hierarchy, revision model or deletion lifecycle. The monitored resolution binding
   can remain platform code identified by its contract version; do not invent a configurable mapping
   language simply to create a literal counterpart to the OpenSearch M1 document.
2. **Keep implementation proportional.** Deliver immutable contract records, explicit input bindings,
   membership/provenance and their API/UI behaviour. A general-purpose schema registry, arbitrary
   schema-conversion platform, user-programmable persistence layer and universal compatibility
   theorem prover are outside scope. Prefer existing services and supported contract constructs.
3. **Treat lifecycle rules as essential.** Full refresh, delta, retry, removal, resolution and merge
   semantics must preserve ownership and provenance. Immutability alone does not prevent one feed
   from withdrawing another feed's membership or evidence. Sections 5, 6 and 10 are completion
   requirements, not optional work deferred to achieve the requested cutoff.
4. **Keep population identity, target contract and ingestion definitions distinct.** Population
   identity denotes the dataset/group; its target contract defines the stable consumer-facing shape;
   input definitions describe how sources produce that shape. For monitored data, membership answers
   whom we monitor, resolution identifies the entity, and provenance identifies evidence supporting
   assertions. None of these relationships substitutes for the others.
5. **Use a new population for a changed S1/M1.** An explicit target-migration capability may be
   considered later if operational experience justifies it. It is not part of this implementation,
   and must not be introduced implicitly through ordinary population edits or data revisions.

## 2. Current evidence and its limits

The following were inspected during the preceding design discussion:

- `ReferencePopulation` has editable `defaultSchemaUri`, `defaultMappingUri` and
  `defaultIndexMappingUri`, plus a single optional `SourceFeedProfile` relationship.
- `ReferencePopulationRevision` stores a contract snapshot and revision/ingestion state.
- Reference ingestion currently allows per-request source/mapping/index URIs. `IngestionContract`
  supplies the bundled canonical screening schema and embeds it in the admitted snapshot.
- Custom index admission checks preservation of standard property definitions and analysis
  configuration. It is not a complete schema/index compatibility check.
- Monitored ingestion currently accepts file/schema/mapping/source-key values and creates or reuses
  an ingestion pipeline derived from the source key. The inspected domain has no dedicated
  `MonitoredPopulation` entity. Some comments mentioning one describe intent rather than an
  implemented entity.
- Candidates already carry source metadata and an optional resolved-entity association. These are
  useful foundations, but do not prove complete population membership or assertion provenance.
- The Customer Master provider contract embeds its resolution `output_schema`. A provider-owned file
  currently supplies that destination definition.
- Studio exposes independent schema/mapping URI inputs and an optional reference index URI. It does
  not expose the proposed immutable target and versioned input-definition model.

This is enough to identify the architectural change, not enough to estimate all implementation work.
Merge, deletion, assertion deduplication, screening selection and correlated-resolution provenance
still require a complete call-path audit before estimating their changes.

### Existing working-tree changes to preserve

Canonical platform resources now use these names:

- `contracts/worldcheck-source-schema-v1.yaml`
- `contracts/worldcheck-opensearch-mapping-v1.yaml`
- `contracts/screening-document-schema-v1.yaml`
- `contracts/customer-master-source-schema-v1.yaml`
- `contracts/customer-master-resolved-entity-mapping-v1.yaml`

The WorldCheck transformation filename contains `opensearch`, but its content is an FDS TransformSpec,
not an OpenSearch index creation body. The standard index body remains
`opensearch/reference-population-index-mapping.json`.

The YAML conversions, consumer renames and loader change have already been compiled in bounded
builds. Tests for those edits were not executed. These results do not validate this proposed redesign.
Historical documents may use retired names; current catalogue entries govern current resources.
Preserve all existing uncommitted changes and identify concurrent agent ownership before editing.

## 3. Target contracts and input definitions

### Immutable target contract

Persist the resolved S1/M1 definitions, schema identity/version, sink kind, origin URIs and content
digests. A URI alone is insufficient: its content can change. Include referenced schema resources in
the immutable admitted resource bundle; do not depend on mutable remote references during replay.

Reference-population creation supports two explicit alternatives:

1. Standard: resolve and freeze the platform screening schema/index pair.
2. Custom compatible: require both target schema and index mapping; validate and freeze them together.

Freeze the pair on successful creation, including populations in DRAFT status. Editing metadata does
not change it. A changed target requires a new population. No hidden override through update, trigger,
configured pipeline, worker environment or fallback to a newer classpath default is allowed.

Persist immutable contract records once and reference them from populations and admitted runs. Keep
the portable worker snapshot as a transport representation, not a second independently editable owner.
System catalogue resources can be reused; population ownership, access and contract lookup remain
tenant-scoped. Do not introduce cross-tenant mutable shared records.

### Source input definition

A population has named input definitions with immutable versions. Each version binds S2/M2, the
target identity/digest, source identity namespace and parsing defaults. One definition may be selected
as the population default. New versions can be registered or activated without changing old runs.

SourceFeedProfile continues to describe source identity/resolution policy. It must not double as the
population identity. Replace the assumption that one population-level profile describes every possible
source with an explicit binding at the input-definition level. Preserve profile policy provenance where
it influences resolution and replay.

Changing a delimiter or mapper version must not accidentally create a new logical source record.
Provider record identity is namespaced by a stable source/feed identity; input version and run identify
how that record was processed, not a different business key.

S2/M2 creation and updates are paired operations. Do not independently inherit half of a pair from one
version and half from another. The convenience ingestion form can register a new pair, but it must
still use the same backend registration/admission rules.

## 4. Validation and admission

Use one admission service across simple reference ingestion, configured pipelines, correlated
ingestion and monitored resolution ingestion. No alternate path may bypass target ownership.

Before dispatch:

1. Authorise the tenant, population and selected input definition.
2. Resolve the immutable population target and exact input version.
3. Validate schemas, declared mapping targets, supported transforms, parsing and identity rules.
4. Validate S1/M1 compatibility for the supported contract vocabulary.
5. Persist the run's immutable contract references/snapshot and idempotency state atomically.
6. Dispatch using the admitted data. Retries recover it instead of fetching mutable documents again.

Compatibility validation must have a documented supported subset. Do not claim to prove arbitrary
JSON Schema/OpenSearch equivalence or arbitrary transformation correctness. Preserve required
canonical screening fields and their query semantics. Check supported scalar/array/object shapes,
nested mappings, numeric/date constraints, analyzers, indexing flags and dynamic-field policies.
Reject unsupported custom constructs with an actionable explanation rather than treating them as
compatible. Test the supported cases against real OpenSearch.

Vendor data stays under `source_data` by default. A custom searchable field needs schema constraints
consistent with its index type; otherwise source-schema validation may pass values that indexing
rejects. Preserve raw values rather than silently coercing or discarding them to satisfy an index.

Record processing still validates each source record against S2 and transformed output against S1.
The normal worker error/progress accounting must report rejected records and indexing errors honestly.

Do not permit unrelated complete target rewrites: screening currently depends on canonical fields.
Supporting arbitrary target semantics would be a separate query architecture project.

## 5. Monitored membership, provenance and lifecycle

Introduce a first-class monitored population with a platform-fixed resolution target contract and
versioned input definitions. Extract the provider-embedded Customer Master output schema into a shared
resolution target resource; make the source provider-independent with respect to destination ownership.
Keep the destination record shape compatible with the existing resolution service contract.

The required logical relationships are:

```text
Monitored population -> population/source-record associations -> ingested record versions
Ingested record version -> ingestion run + input version + source provenance
Ingested record version -> assertion support/provenance
Source record/candidate -> resolved entity (optional until resolved)
Monitored population <-> resolved entity (membership supported by source records)
```

Decide the exact JPA association structure after inspecting current candidate and assertion lifecycle
code. These invariants are mandatory regardless of table naming:

- A resolved entity can belong to multiple monitored populations within the tenant.
- An investigation-derived candidate/entity/assertion can exist without monitored membership.
- Do not add a mandatory population foreign key to every resolved entity or assertion.
- Preserve each contributing record's assertion provenance. If assertions are deduplicated, retain
  multiple support relationships rather than overwriting their provenance.
- Membership is backed by contributing source records. Removing one supporting record does not
  remove membership while another active support remains.
- Source IDs from different feeds or tenants cannot collide. A new parser/mapping version cannot
  silently change the identity namespace of an existing feed.
- Resolution, re-resolution, merges and unmerges/splits where supported preserve or recalculate
  membership and assertion support transactionally. Do not create a new split feature as part of this
  change if none exists; document the existing lifecycle and cover every supported operation.
- Population retirement/removal withdraws only that population's associations. It must not cascade
  delete shared resolved entities or another source's assertions/evidence.
- Historical provenance survives operational membership changes, subject to existing retention policy.

### Screening scope

Select monitored members by explicit population identity, not only by source key. Deduplicate resolved
entities within a run while retaining supporting memberships and provenance. Do not silently omit
unresolved members: use the existing supported candidate path or report them explicitly as unresolved.

Specify which assertions build each screening input: preserve the existing authorised entity-view
semantics unless explicitly changed, and make the population selection independent from assertion
ownership. Population membership is not blanket permission to expose unrelated investigation data.
Carry current authorisation and tenant restrictions through selection, detail views and exports.

## 6. Full/delta and multiple-source semantics

Reference full revisions inherit the same S1/M1 and create their own physical index. Delta revisions
must match the population target and compatible identity semantics of their baseline. Compare and
apply operations must reject mismatches explicitly.

Several input definitions may describe alternative formats of the same dataset. This does not
automatically implement combined-provider dataset management. For reference populations, retain
population-wide full replacement semantics unless the API explicitly declares a different supported
scope. A one-provider full load must never be advertised as preserving other providers by accident.

For monitored refreshes, define reconciliation scope explicitly by population and stable feed identity.
Do not infer deletion from absence until a supported full refresh has completed successfully. Failure,
partial ingestion or retry must not withdraw unseen memberships. A delta only changes the records
explicitly identified within its scope. If a refresh/delete mode does not exist today, reject that mode
until its lifecycle is implemented; do not silently substitute upsert behaviour for replacement.

## 7. API, database and Studio impact

### Contract-first API

- Add explicit target-contract selection at population creation and read-only target identity in views.
- Remove mutable/per-run target and index overrides from reference population updates/triggers.
- Add population-scoped input-definition registration/version selection and defaults.
- Introduce monitored population management and ingestion selection using its ID.
- Expose effective target/input identities on ingestion and revision views for diagnostics.
- Specify missing-pair, incompatibility, immutable-field, cross-tenant and idempotency-conflict errors.
- Regenerate server interfaces/models and Java/TypeScript bindings; never hand-edit generated Java.

### Persistence and clean baseline

Use JPA services/repositories and generated metamodels according to repository architecture. Keep
constraints, uniqueness, locking and transaction boundaries aligned with the new associations.
REST behaviour tests must not bypass the application through hand-built repositories or SQL.

The user explicitly authorises resetting all tenants after implementation. Update the initial FEI
Liquibase table/constraint definitions and fixtures directly; an ALTER/backfill compatibility chain is
unnecessary for these disposable tenant databases. Do not reset anything during implementation.
Identify exactly which databases and baseline checksums are affected; do not rewrite unrelated
platform/FOWF migration histories merely because FEI tenants will be recreated.

Old API payloads, persisted FEI data and old images are not a supported mixed-version rollout target.
Seed/bootstrap scripts and Studio fixtures must use the new baseline consistently.

### Studio

- Separate Target contract and Source definitions in reference population setup.
- Default to the managed screening target; require both documents for a custom compatible target.
- Display target identity/version read-only after creation, with clear validation errors.
- Manage named source definitions and choose one for ingestion; preserve supported parsing overrides.
- Add/select monitored populations rather than treating source key as the population.
- Show population membership and provenance where users inspect ingestion results/entities/assertions.
- Use product language in ordinary flows; implementation hashes may live in advanced diagnostics.

## 8. Repositories and code paths

Paths below are relative to `/home/pn/Documents/code/forwardmeasure/`.

| Area | Primary locations and responsibilities |
|---|---|
| Shared resources | `forwardmeasure-platform/forwardmeasure-platform-data-contracts/`: target/source contracts, catalogue, documentation |
| OpenAPI | `forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-api-specifications/src/main/resources/META-INF/openapi/entity-intelligence-api.yaml` |
| Domain | FEI `forwardmeasure-entity-intelligence-domain/`: ReferencePopulation, revisions, new monitored ownership, input definitions, assertion provenance and membership |
| Admission | FEI `forwardmeasure-entity-intelligence-ingestion-service/forwardmeasure-entity-intelligence-ingestion-service-application/`: ReferencePopulationApplicationService, ReferencePopulationIngestionApplicationService, IngestionPipelineTriggerApplicationService |
| Contract/worker | FEI `forwardmeasure-entity-intelligence-worker-common/`: IngestionContract, DocumentSchema, resolution records and sinks; ingestion/delivery workers for every selected runtime |
| Resolution lifecycle | FEI resolution application/domain services: merge, deletion, assertion deduplication and candidate resolution; identify exact affected methods in the initial audit |
| Screening | FEI screening application/worker: population selection, candidate/resolved input, export and authorisation |
| Migrations | FEI `forwardmeasure-entity-intelligence-migrations/src/main/resources/db/changelog/`: initial definitions and test fixtures |
| Studio | FEI `forwardmeasure-entity-intelligence-studio/webapp/`: ReferencePopulations, WorkflowPanel, API clients and membership/provenance views |
| Acceptance | FEI worker contract tests, all three framework deployment REST suites and `forwardmeasure-entity-intelligence-ingestion-k3s-verification/` |

FDS supplies transformation/runtime components; FOWF transports and executes workflows. Trace the
new admitted payload through them and update only affected boundaries. No engine redesign is planned.
No FDE production change is currently indicated. Any newly discovered cross-repository production
change must be reported explicitly with its reason, not folded into an undefined wider matrix.

## 9. Implementation sequence and checkpoints

Do not start independent conflicting edits across the same API, entity or migration files. If the user
assigns multiple agents, agree ownership and one integrating owner first.

Track two implementation workstreams separately:

- **Reference contracts:** fixed S1/M1, versioned S2/M2, admission and full/delta enforcement,
  reference-population Studio changes, and their focused regressions.
- **Monitored membership:** the fixed resolution destination, input definitions, explicit population
  membership, assertion support/provenance, resolution lifecycle, population-scoped screening,
  monitored Studio changes, and their focused regressions.

Agree shared API/resource conventions first, then give each workstream its own progress, blockers,
estimate and evidence. Shared files need an owner. The workstreams may proceed concurrently only
when people/agents are assigned and dependencies permit it; this document does not launch agents.
Completing the reference workstream is a milestone, not completion of the combined request. Splitting
implementation ownership does not authorise an uncoordinated database/API/image rollout.

| Step | Deliverable | Dependency / completion checkpoint |
|---|---|---|
| A. Focused audit and design freeze | Exact affected classes, membership/provenance mapping, refresh semantics, supported compatibility vocabulary, concrete scope estimate | Required before a credible implementation ETA; report surprises immediately |
| B. Contract-first model | OpenAPI, baseline migrations, shared resolution target, JPA associations and services, regenerated bindings | Agreed model and error cases; no generated files edited by hand |
| C. Reference admission | Immutable S1/M1, versioned S2/M2, all trigger paths, full/delta checks, portable snapshots | B; workers accept the admitted payload and cannot override target |
| D. Monitored lifecycle | Explicit population, source-record membership, assertion support, resolution/merge/removal handling and screening selection | B; every supported mutation path preserves invariants |
| E. Studio integration | Target setup, input-definition selection, monitored population selection and diagnostics | B's API frozen; can proceed alongside C/D under separate ownership |
| F. Regression fixtures | Focused contract and REST scenarios below, wired for all three frameworks | Developed with C/D/E, not deferred until their end |
| G. Integration and compile | Bounded production/test compilation, frontend build/type checking, packaging checks, fresh-baseline fixture compilation | C–F; one coordinated Maven build at a time |
| H. Execution and handoff | Focused runtime evidence, remaining failures, affected-image instructions and fresh-tenant rollout procedure | Begins after the user-authorised test window; no claim of readiness from compilation alone |

## 10. Required regression scenarios

Use `forwardmeasure-testcontainers` infrastructure. Retain useful direct contract/algorithm tests;
exercise domain/application behaviour additionally through real HTTP, framework wiring, authorisation
and persistence. Use synthetic or approved bounded fixtures with independent expected results.

| ID | Scenario and observable result |
|---|---|
| R1 | Create a standard reference population; inspect its fixed target contract |
| R2 | Supply only one custom target document; fail before any ingestion/index side effect |
| R3 | Supply compatible and incompatible custom pairs, including vendor field type mismatch; accept/reject correctly |
| R4 | Attempt target changes through update, trigger and configured pipeline; reject every bypass |
| R5 | Register two different S2/M2 versions targeting the same S1/M1; ingest and verify real indexed documents |
| R6 | Change hosted files/default input after admission; retry/replay retains original effective definitions |
| R7 | Full activation and delta application preserve target identity; incompatible identity/baseline is rejected |
| R8 | WorldCheck canonical YAML source/mapping through REST produces expected OpenSearch fields and preserved raw data |
| R9 | Customer Master through monitored REST ingestion produces expected resolution records/assertions and membership |
| R10 | Two monitored populations resolve to the same entity; memberships and assertion supports remain distinguishable |
| R11 | Repeat ingestion with the same idempotency key; no duplicate membership/assertion support or new target |
| R12 | Remove one support/population or complete a scoped refresh; another support/population/shared entity survives |
| R13 | Failed/partial refresh and retry do not withdraw unrelated or unseen memberships |
| R14 | Merge/re-resolution and supported reversal paths retain valid membership/provenance without duplicates |
| R15 | Investigation-only entities/assertions require no monitored membership and retain authorisation boundaries |
| R16 | Population screening selects only members, deduplicates appropriately, reports unresolved members and preserves provenance |
| R17 | Cross-tenant IDs, source-key collisions and unauthorised contract/membership operations are rejected |
| R18 | Studio default/custom target forms and input selection send the intended API requests and display errors |

Run public API scenarios on Quarkus, Spring and Micronaut. Cover simple and correlated resolution/
reference paths where supported, and each affected worker runtime. Identify selected runtime cells
explicitly in the handoff; do not claim full FOWF/FDS/FDE parity from this focused change. The 5.8 GB
WorldCheck live run remains separate evidence from bounded acceptance fixtures.

## 11. Definition of implementation completion

Implementation complete, ready to begin testing, means:

- API, domain, migrations, services, workers and Studio implement the agreed invariants consistently.
- All necessary regression scenarios are written and connected to the framework suites.
- Affected production and test sources compile using the bounded wrapper, and frontend checks pass.
- No unresolved placeholder, silent unsupported path or unreported blocker is represented as finished.
- The handoff names exact affected images, build commands and fresh-tenant deployment ordering.

Keep an evidence table with columns: scenario/module, written, compiled, executed, result, log path.
Never equate compiled with passed. Coverage shortfalls already accepted by the user must be stated;
do not add perfunctory tests or weaken behaviour checks to meet a coverage number.

Do not launch unrelated full-stack rebuilds. Use the shared bounded wrapper, coordinate its build lock
with other agents/user builds, and build dependencies before consumers. Generated Java is produced
through its generator. This plan creates no permission to run tests, delete tenants, publish images,
deploy or commit; follow the owner's explicit execution instructions for this implementation.

## 12. Schedule and realistic estimation

The user supplied a planning start of **4:00 PM on October 12** and a desired implementation cutoff of
**7:00 PM**, with testing to begin afterward. This is a three-hour requested window, not an evidenced
estimate or a completion guarantee.

**There is currently no defensible elapsed-time estimate for the complete scope.** Giving a numeric
range now would imply effort measurements that have not been performed. The scope includes a new
monitored-population model, provenance and lifecycle behaviour, not just a field or Studio change.
The prior resource-renaming compile durations are not implementation-effort estimates for this work.

The implementation owner must complete Step A and produce a bottom-up estimate covering:

| Workstream | Evidence needed before estimating |
|---|---|
| Contract/API/persistence | Exact new entities/endpoints, generators and affected migration/fixture definitions |
| Reference ingestion | Every admission path and target override; existing full/delta compatibility code |
| Monitored provenance | Existing assertion support semantics, all merge/delete/re-resolution mutation paths, membership selection |
| Studio | Reusable forms/client generation, new population management and provenance views |
| Regression development | Existing fixture reuse versus new HTTP/setup/oracles, framework wiring |
| Compilation/integration | Selected module graph, available local dependencies, competing builds and actual first-build failures |

Report estimated active work and build/wait time separately, the evidence for each estimate, key
unknowns and confidence. Parallelism is only credited where ownership is assigned and dependencies
permit it. Do not sum parallel work as elapsed time or assume extra agents reduce time automatically.

The 7 PM target must be reassessed after that audit. If it is not supportable, report the specific
unfinished work immediately and offer a clearly labelled staged delivery; do not silently remove
monitored lifecycle requirements or call a reference-only implementation the full change. At the
cutoff, report actual written/compiled status and what remains before execution can begin.

This document deliberately does not promise hours or days without supporting measurements. A
focused implementation may fit an hours-long effort, but the three-hour deadline cannot presently
be committed to honestly.

## 13. Instructions for the receiving agent

1. Read this plan and inspect the exact implementation paths before editing. Separate current code
   evidence from proposed behaviour; comments and older handovers are not implementation proof.
2. Confirm the user has selected you as implementation owner. Inventory current changes and other
   active work so the YAML changes and concurrent edits are preserved.
3. Complete the focused audit and publish the evidence-based estimate before promising a finish time.
4. Execute the integrated scope; report concrete blockers promptly and maintain progress/evidence.
5. Do not reset the user's running test environment. The clean-baseline rollout happens later under
   explicit instructions, with coherent API, worker and Studio versions.
6. End with exact completed scope, remaining work, test evidence and affected-image rollout guidance.

## 14. Step A outcome and design freeze (October 12, 2026)

### Audit findings that changed the plan's assumptions

1. Monitored ingestion never matches across sources: every unseen source record creates a new
   resolved entity (`SourceOfTruthResolutionSink`). Entities become shared only through merge or
   investigation resolution.
2. Population screening ignores population identity: POPULATION scope screens every resolved
   entity in the tenant; unresolved candidates are never considered.
3. Merge is absorb-and-hard-delete (`EntityMergeServiceImpl`); no unmerge or split exists. Entity
   deletion hard-deletes the entity's assertions and candidates.
4. Reference population target URIs are editable after creation; contract contents are not read at
   creation; no digests are stored except inside OpenSearch index metadata; delta admission does not
   compare the delta's contract with its baseline.
5. Configured pipelines are fixture/database-defined, unlinked to populations, and the correlated
   path reads its target schema from the classpath at trigger time.
6. Already-resolved monitored records are skipped on re-ingestion, so changed values are never
   applied; there is no refresh or withdrawal mode.
7. FEI code, tests and fixtures already use the new YAML contract names; only historical docs use
   the old ones. The platform data-contracts renames are uncommitted.

### Decisions (user, October 12)

| # | Decision |
|---|---|
| 1 | Shared entities arise through merge or investigation resolution, not ingestion matching. R10 is tested through merge. Merge stays absorb-into-an-existing-entity with the absorbed entity hard-deleted; memberships and assertion supports are carried across, duplicate (population, entity) pairs folded with all supports kept. |
| 2 | Monitored re-ingestion updates a record; a full refresh withdraws supports absent from it, only after the refresh completes successfully. Failure, partial ingestion or retry never withdraws. |
| 3 | Retiring a monitored population withdraws only its memberships; entities and evidence stay. Explicit entity deletion remains an operator hard delete. |
| 4 | Configured pipelines come under the frozen-target model; no path bypasses target ownership. |
| 5 | Screening selects by membership, reports unresolved members, and a run targets a reference or a monitored population. |

### Membership model

Many-to-many from the start: `monitored_population_membership` (population, resolved entity),
unique on the pair; `membership_support` (membership, source record) with `added_by_run`,
`withdrawn_at`, `withdrawn_by_run`; a membership is active while any support is active.
`source_record` is one row per (stable feed identity, record id), with ingested versions beneath
it. No population column on resolved entities or assertions.

### Deferred (not in this implementation)

- Merge into a new entity C with the originals retired rather than deleted, and a choice of
  survivor.
- Unmerge/split.
- Automatic merge or cross-source matching at ingestion, with explicit matching rules.

### Revised estimate

Reference contracts about 6-6.5 hours active plus about 2 hours of compiles; the monitored,
Studio and regression rows to be re-estimated on the same basis as their work starts.

## 15. Implementation status (October 13)

Implemented, compiled and offline-tested; not yet deployed or run in the integration suites.

**Reference contracts (steps B and C).** A population's target (screening document schema and
index mapping) is chosen at creation, checked for compatibility when custom, and stored with its
digests in `target_contract`; it cannot change. Input definitions belong to a population and have
numbered, immutable, stored versions (source contract, mapping, parsing). Ingestion selects a
definition and version (or the defaults) and admits its contract from stored text; a delta must
keep its baseline's source identity namespace and identity rules. API: create takes
`custom_target`/`input_definition`; five input-definition operations; revisions name their target
and input version. Liquibase: forward changesets `2026-10-13-*`; rows from before stay readable,
NOT NULL rules are `NOT VALID`; tenants are reset after rollout.

**Configured pipelines (decision 4).** Every pipeline owns a fixed target; sources name stored
input versions; triggers copy them and cannot route past the target. Monitored-population pipelines
use the platform's standard resolution target, `contracts/resolved-entity-record-schema-v1.yaml`
(extracted from the customer-master contract, identical to it), and add an input version only when
submitted documents change. Correlated sources are now ordered by `sort_order`.

**Transport and integrity (user decisions, October 12).**
1. Workers fetch admitted contracts from the ingestion API by revision id and check the digest
   their Job was launched with; the 120K Job-environment snapshot is gone for all ingestion paths.
2. Sources are pinned at admission (object version, CRC32C/MD5, size) and streamed from object
   storage at exactly that version, verified at the end of the stream; Spark stages the pinned
   version. Unversioned overwrites are refused. forwardmeasure-object-storage gained version
   pinning and checksums on all three providers; forwardmeasure-data-streaming gained a
   caller-opened stream source.
3. Every FEI request body rejects properties its schema does not declare (400), in all frameworks.

**Studio.** Population creation chooses the target and an optional first input definition; edit
cannot touch the target; a Configuration-tab panel manages input definitions and versions;
ingestion selects an input definition and version.

**Remaining:** step D (monitored populations, membership, provenance, screening selection), the
regression scenarios R1-R18 in the integration suites, and deployment.
