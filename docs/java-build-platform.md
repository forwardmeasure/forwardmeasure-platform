# Java Build Platform

`forwardmeasure-platform` is the published Maven parent and dependency-policy
source for ForwardMeasure's open-source Java repositories. It replaces the
retired `forwardmeasure-java-reactor` and `forwardmeasure-java-parent` artifacts.

## Published artifacts

- `com.forwardmeasure.platform:forwardmeasure-platform` — Java 25 build parent,
  core dependency management, plugin management, formatting, coverage and SBOM
  conventions.
- `com.forwardmeasure.platform:forwardmeasure-platform-core-bom` — importable
  view of the parent's shared Java and protocol dependency management for
  consumers that inherit a different Maven parent.
- `com.forwardmeasure.platform:forwardmeasure-platform-quarkus-bom` — core BOM
  plus the supported Quarkus platform.
- `com.forwardmeasure.platform:forwardmeasure-platform-spring-bom` — core BOM
  plus the supported Spring Boot platform.
- `com.forwardmeasure.platform:forwardmeasure-platform-micronaut-bom` — core
  BOM plus the supported Micronaut platform.
- `com.forwardmeasure.platform:forwardmeasure-platform-policy-maven-plugin` —
  version-policy checks available to product builds.
- `com.forwardmeasure.platform:forwardmeasure-platform-bom` — compatible
  ForwardMeasure component releases; this is distinct from the core Java BOM.

## Consumer pattern

Repository roots inherit the Platform parent:

```xml
<parent>
  <groupId>com.forwardmeasure.platform</groupId>
  <artifactId>forwardmeasure-platform</artifactId>
  <version>1.0.0</version>
  <relativePath>../forwardmeasure-platform/pom.xml</relativePath>
</parent>
```

Product roots inheriting the Platform parent receive the core dependency
management directly. A project that must inherit another Maven parent imports
`forwardmeasure-platform-core-bom` instead. Framework binding modules import
exactly one framework overlay BOM. Products retain their own source reactor,
product versions, public BOM, tests and release lifecycle. The Platform reactor
also aggregates the open-source product roots from the standard sibling checkout
layout, allowing the complete open-source stack to be built with one command.

The Platform root references sibling source directories solely for aggregation.
Each component remains independently buildable: when the sibling Platform
checkout is absent, Maven resolves the published Platform parent normally.

The proprietary `forwardmeasure-entity-intelligence` repository is deliberately
excluded from the Platform reactor.

## Complete open-source build

Use the standard sibling checkout layout and run Maven once from the Platform
repository:

```bash
cd forwardmeasure-platform
mvn clean install
```

This builds the Platform foundation, Testcontainers support, database migration
support, JPA, object storage, NLP, entity matching, OpenWorkflow, Platform
Operations, the compatibility BOM and compatibility tests. ForwardMeasure
Agents remains outside the train until its dependency on retired OKS artifacts
has been migrated to the current OpenWorkflow contracts.

## Build order for an unpublished Platform version

1. Run the complete Platform reactor from the sibling checkout layout.
2. Publish the Platform parent, BOMs and policy plugin.
3. Publish independently versioned component artifacts as required.
4. Run Platform component compatibility tests against the selected releases.

The proprietary Entity Intelligence repository is not part of the open-source
source reactor. It may consume published Platform BOMs explicitly while keeping
its own parent and release policy.
