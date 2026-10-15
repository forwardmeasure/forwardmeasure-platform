# Local Maven build cache

The platform owns `.mvn/extensions.xml` (Apache extension 1.3.0) and
`.mvn/maven-build-cache-config.xml`. FOWF, FDS, FEI and FDE link to those files, so direct product
builds and the umbrella reactor use one policy. These checkouts require their platform sibling.
No remote cache or credentials are configured. Up to three entries per artifact are retained locally.

Library compilation outputs can be restored. Compiler and build-helper goals still execute to
register reactor artifacts and generated source roots; incremental compilation can reuse restored
outputs. Skipping these lifecycle side effects broke downstream resolution and generated JPA
metamodel Javadoc in real focused FEI builds. Frontend modules and modules with the recognized Dockerfile
layouts opt out of reading/writing cache entries through explicit POM properties (on assembly parents where possible), so packaging those
assemblies still runs fresh. Other runtime packaging plugins are also explicitly always-run.
This initial scope deliberately does not promise full deployment assembly reuse.

The policy includes source/resources, schemas/protobuf, Liquibase SQL/XML, frontend lockfiles and
the JDK release file in inputs. Effective plugin configuration and dependencies are handled by the
extension. It restores classes, test classes, generated sources, generated TypeScript and Quarkus
application directories when those are present in eligible modules. Node modules, frontend build
outputs and browser test reports are excluded from inputs. Test result reports are not attached as
fresh execution evidence.

Surefire/Failsafe, frontend execution, validation, install/deploy and image publication plugins
always execute their configured goals, subject to their normal skip flags. Compiler skip/release
settings are reconciled before reuse. `skipTests` is now honored by both Studio npm test executions;
Java test sources still compile. The build scripts no longer loop over every locally present Docker
image: selected modules own publication. `build.sh` uses the bounded Maven wrapper.

```bash
./build.sh --module :openworkflow-studio-quarkus --skip-push
./build.sh --module :openworkflow-studio-quarkus --no-build-cache --skip-push
# Docker layer caching is independent:
./build.sh --module :openworkflow-studio-quarkus --no-cache --skip-push
```

The deployment-oriented builder now takes the same environment as the installer:

```bash
./deploy/scripts/build-product-images.sh gcp-openworkflow-prod --push
```

It resolves selection once, records the build plan and includes FDE when enabled. It builds every
selected product image; use `build.sh --module` for an incremental repair. The old positional
framework/engine arguments have been replaced by shared configuration and environment overrides.

For a direct Maven invocation, `-Dmaven.build.cache.enabled=false` disables reuse and saving.
Use that when gathering fresh compiler/verification evidence. No parallel Maven execution was
enabled by this change. Keep memory limits and the cross-repository build lock.

## Verification

The XML validates against the schema packaged in the pinned extension. A bounded parent `validate`
invocation loaded the policy, computed inputs, saved a local entry and completed successfully:
`/tmp/platform-build-cache-validation-2026-10-06.log`. This is configuration-loading evidence, not
evidence of speedup or artifact equivalence on cache hits.

`deploy/tests/test_build_cache_integration.py` now has two executed, passing real-Maven fixtures:
cold/warm restore after clean, restored classes/test classes, execution after a skipped-test build,
protobuf-resource invalidation, and a two-module test-to-package build proving reactor dependency
resolution plus generated-source compilation/Javadoc registration on cache hits. Together with the
mixed generated/handwritten coverage integration fixture, all three pass in
`/tmp/platform-cache-and-coverage-regressions-20261007.log`. Independent acceptance must also
compare cached/uncached product artifacts, generated clients, JDK/profile changes, image build/push
behavior and the actual selected-framework package layout. Do not count historic cached reports
as tests run in the current invocation. Disable cache if a generator's input/output contract is
not captured; extend the policy and regression before restoring reuse.

References: [Apache setup](https://maven.apache.org/extensions/maven-build-cache-extension/),
[execution and reconciliation guidance](https://maven.apache.org/extensions/maven-build-cache-extension/how-to.html),
[project-level cache controls](https://maven.apache.org/extensions/maven-build-cache-extension/parameters.html).

## OpenAPI source registration follow-up (2026-10-07)

A warm product compile failed to resolve generated engine-command models after the OpenAPI goal
was restored/skipped. The policy now always executes the OpenAPI generator (source-root registration)
and dependency goals (its unpack/copy inputs). The cache remains enabled. The three real Maven cache
fixtures pass in `/tmp/platform-cache-regressions-openapi-20261007.log`, including a new OpenAPI model
module consumed by newly compiled downstream test code on a warm cache. The initial isolated warm
fixture passed before the policy change; the failing product reactor is the reproduction, and its
rerun is pending in `/tmp/fei-managed-services-fowf-regressions-03-20261007.log`.
