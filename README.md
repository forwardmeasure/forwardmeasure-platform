# ForwardMeasure Platform

This repository defines the tested compatibility release train for the
ForwardMeasure foundations, OpenWorkflow Kafka Streams (OKS), and Entity
Intelligence. It does not own their source code and is not their Maven parent.

## Full development build

All component repositories must be checked out as siblings of this repository.

```bash
./scripts/verify-source-manifest.sh development
mvn -B clean verify
```

The reactor builds Testcontainers, database migrations, JPA, OKS, and Entity
Intelligence in dependency order. Maven resolves matching component coordinates
from the reactor instead of relying on stale artifacts in the local repository.

## Release validation

`platform-sources.json` must contain immutable Git commit IDs before release.
`WORKTREE` is deliberately accepted only by development validation.

```bash
./scripts/verify-source-manifest.sh release
```

Release validation rejects dirty repositories, moving revisions, mismatched
branches, mismatched Maven versions, and snapshot versions.

## Compatibility BOM

Consumers import `com.forwardmeasure.platform:forwardmeasure-platform-bom:1.0.0`.
The BOM composes the approved Testcontainers, migration, JPA, OKS, and Entity
Intelligence dependency-management surfaces. Component repositories remain
independently releasable.
