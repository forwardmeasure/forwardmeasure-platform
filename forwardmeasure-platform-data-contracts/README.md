# Shared data contract catalogue

This resource-only platform artifact owns the shared provider data contracts. It has no product
dependencies and does not own product API or workflow schemas. Services and workers receive it
through `worker-common`; acceptance packaging copies these exact resources during Maven
`process-test-resources`. There must be no separately edited production/test copies.

All paths below are relative to `src/main/resources`. `contracts/catalog.json` is the machine-readable
inventory. These are ingestion metadata plus JSON Schema 2020-12 and FDS TransformSpec mappings;
YAML serialization does not make a document an OpenAPI specification.

| Purpose | Source schema | Mapping | Destination / index |
|---|---|---|---|
| WorldCheck screening | `contracts/worldcheck-source-v1.json` | `contracts/worldcheck-mapping-v1.json` | `contracts/screening-document-v1.json`; `opensearch/reference-population-index-mapping.json` |
| State Street customer master, resolution assertions | `contracts/state-street-customer-master-source-v1.yaml` | `contracts/state-street-customer-master-resolution-mapping-v1.yaml` | `output_schema` inside the source contract; FEI resolution service persistence |
| WorldCheck explicit vendor-field search override | Same source and mapping | Same mapping | `opensearch/worldcheck-index-v1.json`: additionally indexes `source_data.UPDATE CATEGORY` as keyword |

The default index stores unmapped `source_data` fields without indexing them. The override is an
explicit capability, not the default and not a complete premium-feed index specification.

## Current correctness limits

Central ownership is not semantic acceptance. WorldCheck now declares all 35 header columns;
its normalized mapping still has the field-disposition, partial-date, identifier and classification
gaps listed in `../../forwardmeasure-entity-intelligence/docs/worldcheck-contract-fixture-discrepancy-test-handover-2026-10-08.md`.
The small premium fixture cannot prove full-feed support. Full-file streaming and limits remain open.

The State Street contract declares the 42 named source fields and the pipe/UTF-8 parser settings.
It replaces the old unused source-schema description and is actually consumed by the resolution
worker regression. Its existing assertion mapping is preserved, including documented unmapped
columns. It is not a newly certified complete customer-master mapping or a screening mapping.
The file reader's trailing duplicate-header handling needs separate validation against the external
source. Do not invent a second State Street source schema for a future screening mapping; add that
mapping as another explicit destination in the catalogue.

FDS's `forwardmeasure-data-streaming-test-fixtures` retains historical flat-target engine fixtures.
They exercise the generic FDS engine and are not selectable alternatives to FEI's shared screening
contract. FDS Pekko consumes those shared fixtures instead of maintaining duplicate copies. FEI's
old WorldCheck flat-target compatibility mapping is isolated under ingestion-worker
`src/test/resources/legacy`; synthetic worker mechanics use `mapping/synthetic-simple-ingestion.yaml`.

## Consumption and changes

Existing `classpath:/contracts/worldcheck-*.json`, `classpath:/contracts/screening-document-v1.json`
and `classpath:/opensearch/reference-population-index-mapping.json` remain stable. For State Street,
use the source and resolution mapping paths from the table with `classpath:/` for bundled resources,
or publish those same files to an approved HTTP/object-storage location and use the resulting URIs.
The API freezes admitted contract bytes; do not mutate historical admitted snapshots or migrations.

For HTTP/Studio acceptance, build K3s verification with `test-compile` and host its generated
`target/test-classes/reference-ingestion-acceptance` directory, not its source directory. The build
includes the authoritative contracts and the independent test inputs/oracles. The packaging test
checks byte equality; contract tests detect duplicate classpath owners and undeclared mapping inputs.
Expected semantic output must remain independent of the transformation being tested.

When changing destinations, update the destination schema, provider mapping, index/query consumers
and independent semantic regressions together. Keep intentionally different target versions explicit.
Do not infer all-framework or full-export readiness from compilation or the packaging test.
