# ForwardMeasure Platform

This repository owns the shared Java build parent and BOMs, shared deployment
umbrella, and tested compatibility catalogue for independently released
ForwardMeasure components. Platform Operations owns the cross-component
operational API, dashboard source and Keycloak theme.

The Java parent and BOM adoption pattern is documented in
[`docs/java-build-platform.md`](docs/java-build-platform.md).

## Artifact compatibility

`reactor.xml` builds the compatible Apache-licensed ForwardMeasure stack from
sibling source repositories - a standalone aggregator, not this repo's own
`pom.xml` and not a parent of anything, so that Quarkus's own bootstrap
resolver (used by `quarkus:dev` and Quarkus's test-mode code generation) never
discovers the sibling repos while building any individual component locally.
The proprietary Entity Intelligence source is not included. ForwardMeasure
Agents is not part of this compatibility train until it consumes the current
OpenWorkflow APIs instead of retired OKS artifacts.

```bash
mvn -f reactor.xml -B clean install
```

Individual component repositories remain independently buildable against the
published Platform parent and BOMs.

The `forwardmeasure-platform-compatibility-tests` module compiles and tests
against the selected public contracts. Unified OpenWorkflow is represented by
`com.forwardmeasure.openworkflow:openworkflow-bom`; the retired standalone
Kafka Streams parent is not part of the train.

Component Git revisions are deliberately not duplicated in a platform-owned
source manifest. Each released artifact supplies SCM metadata and should be
accompanied by its repository's SBOM and provenance attestation. The platform
catalogue records the compatible released versions, which are its actual build
inputs.

## Compatibility BOM

Consumers import
`com.forwardmeasure.platform:forwardmeasure-platform-bom:1.0.0`. The BOM
composes approved component BOMs while preserving independent component release
cycles. Product parent POMs are temporary compatibility debt and must be
replaced as the remaining products publish consumer BOMs.

## Platform Operations and shared deployment

The operational API, dashboard and Keycloak theme are built by
`forwardmeasure-platform-operations`. The shared Kubernetes release under
[`deploy/helmfile`](deploy/helmfile/README.md) deploys their immutable images
alongside common infrastructure, including Keycloak. Product charts consume
those services by URL and credentials; they do not deploy competing copies.

```bash
./deploy/helmfile/validate.sh gcp-greenfield-example
./deploy/helmfile/install.sh gcp-production
```
