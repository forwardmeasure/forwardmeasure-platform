# Shared data contract catalogue

This resource-only platform artifact owns the shared provider data contracts. It has no product
dependencies and does not own product API or workflow schemas. Services and workers receive it
through `worker-common`; acceptance packaging copies these exact resources during Maven
`process-test-resources`. There must be no separately edited production/test copies.

All paths below are relative to `src/main/resources`. `contracts/catalog.json` is the machine-readable
inventory. These are ingestion metadata plus JSON Schema 2020-12 and FDS TransformSpec mappings;
YAML serialization does not make a document an OpenAPI specification.

`worldcheck-opensearch-mapping-v1.yaml` transforms provider rows into screening documents destined
for OpenSearch. It is an FDS TransformSpec; index field types/settings are defined separately under
`opensearch/`. The source and mapping filenames below are authoritative, without aliases for the
superseded filenames. Update configured contract URIs when adopting these names.

| Purpose | Source schema | Mapping | Destination / index |
|---|---|---|---|
| WorldCheck screening | `contracts/worldcheck-source-schema-v1.yaml` | `contracts/worldcheck-opensearch-mapping-v1.yaml` | `contracts/screening-document-schema-v1.yaml`; `opensearch/reference-population-index-mapping.json` |
| State Street customer master, resolution assertions | `contracts/state-street-customer-master-source-schema-v1.yaml` | `contracts/state-street-customer-master-resolved-entity-mapping-v1.yaml` | `contracts/resolved-entity-record-schema-v1.yaml` (the standard resolution target); FEI resolution service persistence |
| WorldCheck explicit vendor-field search override | Same source and mapping | Same mapping | `opensearch/worldcheck-index-v1.json`: additionally indexes `source_data.UPDATE CATEGORY` as keyword |

The default index stores unmapped `source_data` fields without indexing them. The override is an
explicit capability, not the default and not a complete premium-feed index specification.

## Current correctness limits

The WorldCheck and customer-master semantics are specified field by field in
[the provider ledger](provider-field-semantics-2026-10-08.md). WorldCheck now maps all 35 columns,
with explicit date precision, richer locations, classifications, identifiers and provenance. State
Street maps all 42 logical named columns into resolution fields/assertions, including the canonical
keys actually consumed by resolution matching. Raw source preservation is retained in both paths.
The ledger records blank/invalid handling, duplicate headers, unknown entity types, identifier domains,
matching limits and snapshot compatibility. Current execution evidence lives in the FEI repair handover.

Full-file streaming/size limits and the complete framework/engine matrix remain open. The current
State Street destination is resolution, not a second screening schema. Future screening mappings
must reuse this source contract and explicitly declare their different destination.

FDS's `forwardmeasure-data-streaming-test-fixtures` retains historical flat-target engine fixtures.
They exercise the generic FDS engine and are not selectable alternatives to FEI's shared screening
contract. FDS Pekko consumes those shared fixtures instead of maintaining duplicate copies. FEI's
old WorldCheck flat-target compatibility mapping is isolated under ingestion-worker
`src/test/resources/legacy`; synthetic worker mechanics use `mapping/synthetic-simple-ingestion.yaml`.

## Consumption and changes

WorldCheck uses `classpath:/contracts/worldcheck-source-schema-v1.yaml` and
`classpath:/contracts/worldcheck-opensearch-mapping-v1.yaml`. The superseded WorldCheck source/mapping JSON
files were removed at the user's request on October 12, 2026; there are no compatibility aliases.
Update configured population/pipeline contract URIs before admitting a new run. Existing admitted
snapshots retain their own contract bytes. For live testing on already deployed services, host the
new YAML files at service-accessible HTTP/object-storage URIs and select those URIs in Studio;
new classpath resource names require rebuilding the consuming images.

The canonical document schema is now `classpath:/contracts/screening-document-schema-v1.yaml`;
its parsed validation rules and schema ID are unchanged by the YAML conversion. The index mapping
remains `classpath:/opensearch/reference-population-index-mapping.json`. For State Street,
use the source and resolution mapping paths from the table with `classpath:/` for bundled resources,
or publish those same files to an approved HTTP/object-storage location and use the resulting URIs.
The API freezes admitted contract bytes; do not mutate historical admitted snapshots or migrations.

For reference-population ingestion, Studio's Schema URI selects the provider source contract;
Mapping URI selects the transformation into the shared screening document. FEI automatically loads
the bundled canonical document schema and pins it in the admitted snapshot. Studio does not expose
a separate canonical-schema selector. The optional OpenSearch Index Mapping URI selects index
configuration, defaulting to the standard index mapping. Admission checks that an override preserves
the standard field definitions and analysis configuration. This is not general schema/index equivalence
validation: adding an index field does not authorize a new canonical root field. Vendor extensions
belong under `source_data`, which permits arbitrary values; new canonical fields require coordinated
changes to the shared document schema, transforms and index/query consumers.

For HTTP/Studio acceptance, build K3s verification with `test-compile` and host its generated
`target/test-classes/reference-ingestion-acceptance` directory, not its source directory. The build
includes the authoritative contracts and the independent test inputs/oracles. The packaging test
checks byte equality; contract tests detect duplicate classpath owners and undeclared mapping inputs.
Expected semantic output must remain independent of the transformation being tested.

The WorldCheck YAML restores descriptions for all 35 raw columns and translates legacy nullable
fields into JSON Schema `type: [string, "null"]`. Optional fields may be absent or blank; UID and
CATEGORY remain required nonempty strings. E/I accepts E, M, F, U and I plus blank/null for an
unspecified value; unknown nonempty codes now fail source validation. No restrictive CATEGORY enum
or single-URI constraint is imposed on provider vocabulary or multi-URL cells. The mapping's parsed
rule structure is unchanged by the YAML conversion. Both State Street resources already use YAML:
the source contract embeds JSON Schema 2020-12, while the resolution mapping is FDS TransformSpec.

When changing destinations, update the destination schema, provider mapping, index/query consumers
and independent semantic regressions together. Keep intentionally different target versions explicit.
Do not infer all-framework or full-export readiness from compilation or the packaging test.

## October 12 YAML conversion verification

The supplied ten-record WorldCheck sample passes the revised source JSON Schema. The YAML mapping
parses to exactly the previous JSON mapping structure; both State Street documents retain their
parsed content. The packaged contracts and generated FEI acceptance resources match the canonical
YAML bytes, and the contracts JAR contains neither superseded WorldCheck JSON resource.

The contracts artifact installed successfully with RAT enabled. FEI production/test-source compilation
(including the Studio frontend build) and FDS test-fixture dependency-closure compilation succeeded
through `scripts/build-bounded.sh`. A source-validation regression covering nullable/blank cells,
multiple URLs, additional fields, E/I codes and required fields was written and compiled. JUnit,
integration and live ingestion tests were not executed for this conversion. Local build logs are in
`~/.local/state/forwardmeasure/validation/20261012-worldcheck-yaml/`.

Following the same-day filename renames reflected in the catalogue above, the contracts artifact
was rebuilt and the affected FEI modules (with their dependencies) and FDS test-fixture sources
compiled again successfully. Canonical parsed content is unchanged. Generated acceptance resources
match the renamed files, with superseded copies removed. Tests were not executed. Rename-validation
logs are in `~/.local/state/forwardmeasure/validation/20261012-contract-renames/`.

The subsequent canonical screening-schema conversion preserves every parsed rule and the schema ID.
The FEI admission loader now reads the bundled schema as YAML; serialized admitted snapshots remain
JSON. The regression for raw vendor values, undeclared root fields and invalid dates now exercises
both production admission and snapshot replay. The contracts artifact installed, and worker-common,
K3s acceptance and their dependency closure compiled successfully; tests were not executed. Packaged
acceptance resources match the YAML and omit the old JSON. Logs are in
`~/.local/state/forwardmeasure/validation/20261012-screening-schema-yaml/`.
