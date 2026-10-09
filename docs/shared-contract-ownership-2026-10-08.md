# Shared contract ownership — October 8, 2026

The user approved one authoritative owner per contract across FDS, FEI and FOWF, with explicit
versions. Shared provider data definitions belong in a dependency-neutral artifact; product
workflow/API contracts remain product-owned. Generated Java/TypeScript and admitted snapshots
are derived artifacts, not independent sources to edit.

| Contract family | Authoritative owner | Consumers / boundary |
|---|---|---|
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

## Audit boundaries and remaining work

The adjacent JSON inventory records exact paths and SHA-256 for the main-source schema/API/mapping
search. No exact byte duplicates were found in that scope. This is not proof of semantic uniqueness.
The inventory separately records classpath resource-name collisions: FEI and FOWF both package
`META-INF/openapi/common-definitions.yaml` with different contents. Resolve shared components and
namespace product-specific remainders, verifying registry publication and client generation.
A resource-name collision is an audit finding, not a claimed reproduced runtime failure.
In particular RFC 9457 `Problem`/`Violation` components remain declared in multiple products;
centralizing their generation is separate work requiring generated-client and HTTP parity checks.
Do not move whole product-specific common-definitions files into the data-contract module: FEI's
file also owns domain value types and FOWF's owns workflow-facing API components.

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
framework/engine parity. Shared API-component deduplication remains separate from the completed
provider-contract centralization. Current runtime evidence is maintained in
[runtime-matrix-status-2026-10-08.md](runtime-matrix-status-2026-10-08.md).

## Verification

Platform parent/core BOM/resource artifact bounded install passed (2.248s); tests were skipped.
FDS focused source/fixture/mapping suite passed 18 tests (4.303s), including shared-vocabulary checks.
FEI runtime/packaging regressions passed 15/15 (1m12s); complete three-framework production/test
compilation passed (1m30s). Logs: `fei-platform-data-contracts-tests.log` and
`fei-platform-data-contracts-all-frameworks-compile.log`, alongside `fds-platform-data-contracts-tests.log`
and `platform-data-contracts-bootstrap.log`, under `~/.local/state/forwardmeasure/validation/20261008-recovery/`. No deployment or
container-image publication was performed.
