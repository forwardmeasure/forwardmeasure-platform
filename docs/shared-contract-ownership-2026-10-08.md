# Shared contract ownership — October 8, 2026

The user approved one authoritative owner per contract across FDS, FEI and FOWF, with explicit
versions. Shared provider data definitions belong in a dependency-neutral artifact; product
workflow/API contracts remain product-owned. Generated Java/TypeScript and admitted snapshots
are derived artifacts, not independent sources to edit.

| Contract family | Authoritative owner | Consumers / boundary |
|---|---|---|
| Shared RFC 9457 `Problem` and `Violation` | `forwardmeasure-platform-api-contracts/src/main/resources/META-INF/forwardmeasure/openapi/problem-v1.json` | Platform error handlers; FEI, FDS and FOWF published API snapshots and generated bindings |
| WorldCheck and State Street source definitions, mapping definitions, canonical screening destination and indexes | `forwardmeasure-platform-data-contracts/src/main/resources`; `contracts/catalog.json` | FEI via worker-common; FDS source-compatibility tests directly; generic runtime engines consume admitted mapping inputs |
| FDS IngestionSpec, launcher API, worker AsyncAPI, merge policy and ingestion/stop workflows | `forwardmeasure-data-streaming-api/src/main/resources` | FDS bindings and FOWF operation/workflow publication; no vendor-specific replacement schema |
| FEI public API, domain value types and worker AsyncAPI | `forwardmeasure-entity-intelligence-api-specifications/src/main/resources` | Generated clients/models and three framework endpoints |
| FOWF definition/execution/engine/human-task/tenant APIs | `openworkflow-api-specifications/src/main/resources/META-INF/openapi` | Generated bindings and workflow publication |
| FEI application workflow definitions | `forwardmeasure-entity-intelligence-workflows/src/main/resources/definitions` | Published FOWF bundle; references API/worker contracts |
| Historical flat-target vendor examples | FDS `test-fixtures`; FEI worker `src/test/resources/legacy` | Explicit compatibility tests; not alternatives to the canonical destination |

## Implemented dependency direction

`FEI worker-common -> platform-data-contracts` and
`FDS shared-fixture tests -> platform-data-contracts`. The resource-only artifact has no FEI,
FDS or FOWF dependencies. The platform parent/core BOM manages its version. Existing classpath
resource URIs are retained; the interim FEI ingestion-contracts module is removed. Original
resource licensing is preserved explicitly; moving files does not relicense them.

FOWF owns no active WorldCheck/customer-master schema files in the inspected main resources;
historical documentation references are not new schema owners. Adding a provider-JAR dependency
to FOWF merely to imply participation would create unnecessary coupling. Its engines receive
workflow arguments and operation contract references through the existing publication path.

K3s HTTP-fixture assembly reads the authoritative platform checkout in this multi-repository
workspace. The packaging regression compares those bytes with the actual dependency artifact,
so stale installed JARs or source drift fail the check. Build/install the platform contracts/core
BOM before a standalone FEI build; the normal platform reactor already includes the new module.

## Shared API ownership repair — October 9

`forwardmeasure-platform-api-contracts` now generates `Problem` and `Violation` once, with no
product dependency. The established `com.forwardmeasure.openworkflow.common.model` package is
retained for Java compatibility; the Maven artifact, schema ownership and generated helper package
are platform-owned. FOWF's common-models module generates only its own `Paging` and
`ActorReference` models and depends on the platform artifact. Platform JAX-RS error handlers,
FEI common-models and FDS launcher JAX-RS also depend directly on that artifact.

Product OpenAPI documents contain **generated inline snapshots**, because the current generators
need local components to preserve existing cross-file names and client mappings. Edit only the
platform JSON, then run `python3 scripts/sync-shared-api-components.py` from this repository.
`--check` detects stale snapshots without writing. The script converts nullable type unions to
`nullable: true` for FDS's OpenAPI 3.0 document; FEI and FOWF retain OpenAPI 3.1 unions. Product
JUnit contract checks compare the published resource with the authoritative dependency artifact,
so separate-repository builds also reject drift. Generated Java must never be patched manually.

The unused FEI `META-INF/openapi/common-definitions.yaml` is removed. Its active domain schemas
are in `entity-intelligence-api.yaml`; no runtime or generator referenced the removed file. FOWF
retains its product-specific common definitions, eliminating the classpath resource collision.
FDS now publishes the same `correlation_id` and structured `violations` fields its Java error
handlers already emit. Nullable optional evidence is allowed without weakening required fields
or status/type validation. No endpoint, migration or provider data contract changed.

For an existing checkout, install the platform parent/core BOM and the new artifact before the
product consumers. Normal platform reactor builds include the new module. The test-support
classifier is produced at `test-compile`, so reactor builds can run product contract checks.

## Audit boundaries and remaining work

The adjacent JSON preserves the October 8 path/hash inventory and records the October 9
remediation separately. The original collision was an audit finding, not a claimed reproduced
runtime failure. Current shared-component ownership is enforced by the checks above; keeping
derived inline snapshots does not create additional authoritative schema owners.

FDS legacy transport fixture templates retain explicitly historical flat-target mappings. They
must not become newly published FEI canonical mappings. Where several transport templates repeat
the same field rules, a later fixture composition change should remove that repetition while
preserving independently specified expected outputs.

At this initial ownership audit, WorldCheck and State Street semantics and large-file ingestion
were incomplete. Subsequent repairs centralized the 35-field WorldCheck and 42-field State Street
dispositions and mappings; see FEI's repair handover for their exact semantic evidence. The later
full WorldCheck export acceptance processed 5,611,953,182 bytes and 5,818,856 records through the
real API and worker into OpenSearch. That full-file result supersedes the old acquisition and
whole-file-materialization blocker. It does not establish hundreds-of-GB execution or complete
framework/engine parity. The complete runtime matrices subsequently passed on their recorded
images, before the shared API ownership migration. Current runtime evidence is maintained in
[runtime-matrix-status-2026-10-08.md](runtime-matrix-status-2026-10-08.md).

## Verification

**October 9 API ownership repair:** 140 tests passed, zero failures/errors/skips in the retained
passing reports. This includes 68 schema/model/handler cases, FOWF's 57 real HTTP validation/query/
not-found cases, FEI's 12 population lifecycle/authorization/validation cases, and FDS's three
missing-namespace HTTP cases. Each product's HTTP cases boot Quarkus, Spring and Micronaut with
real infrastructure. The FEI fixtures now supply the canonical WorldCheck source/mapping/index
URIs and preserve an explicit missing-contract rejection. Earlier stale FEI request and missing
FDS executor-image-selection attempts are retained separately, not counted as passes.

Full FOWF and FEI production/test compilation passed; FEI's compile includes Studio's npm build
and type checks. FDS's three launcher dependency closures also compiled. The clean FEI API JAR
contains no conflicting common-definitions resource. FOWF's class-ownership test and duplicate
finder confirmed one owner for the relocated classes. The public Java model method signatures
are preserved. No generated Java was hand-edited and no migration changed.

Exact reports, counts and SHA-256 values:
[shared-api-ownership-evidence-2026-10-09.json](shared-api-ownership-evidence-2026-10-09.json).
Commands are retained as `verify-shared-api-consumers-01.sh` and `verify-shared-api-http-01.sh`
through `-03.sh` under `~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.
All Maven invocations used the bounded wrapper; actual test execution was authorized. No image
was published or operator cluster deployed. The earlier packaged matrix reports retain their
recorded image identities; these checks validate the later source/library ownership migration.
The handwritten 85% coverage gate is a separate verification step and is not claimed here.

**Earlier provider-data ownership repair:**

Platform parent/core BOM/resource artifact bounded install passed (2.248s); tests were skipped.
FDS focused source/fixture/mapping suite passed 18 tests (4.303s), including shared-vocabulary checks.
FEI runtime/packaging regressions passed 15/15 (1m12s); complete three-framework production/test
compilation passed (1m30s). Logs: `fei-platform-data-contracts-tests.log` and
`fei-platform-data-contracts-all-frameworks-compile.log`, alongside `fds-platform-data-contracts-tests.log`
and `platform-data-contracts-bootstrap.log`, under `~/.local/state/forwardmeasure/validation/20261008-recovery/`. No deployment or
container-image publication was performed.
