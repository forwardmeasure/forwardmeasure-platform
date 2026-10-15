# Architecture

`forwardmeasure-platform` has five responsibilities:

1. Publish the shared Java parent, core dependency BOM and framework overlay BOMs.
2. Publish a compatibility BOM representing one tested set of released artifacts.
3. Validate shared build behavior and cross-component public contracts.
4. Aggregate the Apache-licensed component repositories into one source build.
5. Own shared deployment infrastructure.

It does not own product source. Each component keeps its own repository,
modules, version, product BOM, tests and release. The Platform root aggregates
those sibling repositories for a single complete build. Open-source Java
components inherit `com.forwardmeasure.platform:forwardmeasure-platform` and
can also be built independently against the published parent and applicable
Platform BOMs.

```text
forwardmeasure-platform parent + core/framework BOMs
        |
        v
single open-source source reactor
        |
        v
independently released component BOMs and artifacts
        |
        v
forwardmeasure-platform-bom + build/artifact compatibility tests
        |
        +-- Platform Operations dashboard deployment
        `-- shared infrastructure deployment
```

OpenWorkflow's Pekko and Kafka engines are one unified product entry. The
platform imports `openworkflow-bom` and never imports the OpenWorkflow parent or
lists the retired standalone engines as parallel products.

The compatibility train records released artifact versions because those are
the inputs it resolves and tests. Git revision provenance remains with each
component release through Maven SCM metadata, signed tags, SBOMs, and build
attestations; a second hand-maintained `platform-sources.json` is intentionally
not used.

API specifications and generated clients remain owned and published by their
service repositories. The platform verifies consumers against those artifacts;
it does not copy their contracts.

The shared deployment owns common infrastructure such as Keycloak, messaging,
search, and routing. Product deployments supply product configuration and
consume shared endpoints without deploying duplicate infrastructure.

The cross-product UI composition and ownership boundaries are defined in
[`ui-architecture.md`](ui-architecture.md).
