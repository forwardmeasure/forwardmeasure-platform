# Java build platform

`forwardmeasure-platform` is the Maven parent and version authority for ForwardMeasure Java
repositories, including proprietary FEI. Each product retains its own reactor and release identity.
The platform root builds its own modules; the separate `reactor.xml` is the wider compatibility
reactor, not a parent POM. FEI remains outside that open-source compatibility reactor.

## Managed artifacts

The platform parent owns shared dependency and plugin versions. `forwardmeasure-platform-core-bom`
exports that management to consumers using another parent. The Quarkus, Spring and Micronaut BOMs
provide framework overlays; `forwardmeasure-platform-bom` records compatible component releases.

`forwardmeasure-platform-policy-maven-plugin` runs at `validate` for inheriting consumers. It derives
ownership from the actual platform POM and inspects local declarations in both ordinary and inactive
profile configuration. There is no separately maintained Java list of owned versions.

Repository roots inherit `com.forwardmeasure.platform:forwardmeasure-platform:1.1.0`, normally with
`../forwardmeasure-platform/pom.xml` as the parent relative path. Omit local library/plugin version
declarations and use the managed versions. A local `revision` names the product being built; it does
not authorize overriding sibling product dependency versions.

Actual third-party compatibility exceptions require an exact key and a reason in the local policy
plugin configuration. First-party dependency-version exceptions are forbidden. See
[the consolidation handover](version-consolidation-2026-10-07.md) for examples, current exceptions,
bootstrap commands and per-repository compile evidence.

## Building from a fresh checkout

Install the platform parent and policy plugin first using the bounded bootstrap commands in that
handover. The explicit bootstrap switch is for those initial artifacts only. Normal downstream
builds inherit the policy execution. Install required component artifacts in dependency order;
there is no need to rebuild unrelated container images.

Use the bounded wrapper with `test-compile -DskipTests -DskipITs -Dmaven.test.skip=false` when checking
production and test sources without test execution. Tests, image builds/publication and deployment
remain separate operations requiring the user's instruction in this session.

## Coverage policy

The platform owns `jacoco.minimum.line.coverage=0.85` and
`jacoco.minimum.branch.coverage=0.85`. The user requested both minimums across the stack on
2026-10-07. `build.sh --run-tests` enables the shared `coverage` profile automatically;
direct Maven regression builds must include `-Pcoverage`. This is the Java coverage policy;
it does not measure browser, Python or other non-Java code.

The shared profile instruments tests, produces reports and checks handwritten Java code. Generated
code is exempt by the user's 2026-10-07 instruction. The policy identifies compiled classes through
generated-source provenance or retained Generated annotations and writes exact class exclusions to
`target/coverage-generated-excludes.txt`; it does not exempt an entire package containing both kinds
of code. Unknown provenance remains subject to coverage. Generated-only and resource-only modules
need no Java execution data; their configured tests still execute. Missing or empty execution data
for handwritten classes fails through the policy plugin's `require-coverage-data` goal.
Explicit test-skip flags permit compilation/bootstrap without claiming coverage evidence.

JPA, OpenWorkflow and FEI retain product aggregate checks because their framework/contract tests
exercise sibling production modules. They declare a reasoned policy exception for
`jacoco.module.check.skip=true`. The policy honors this request only when a selected reactor
aggregate directly includes the module at the same version, stages its classes and configures both
aggregate reporting and a coverage check. Modules omitted from the aggregate retain their local
gate. Exact generated exclusions propagate to aggregate reports and checks. Their
aggregate checks inherit the same 85%/85% thresholds and retain zero permitted missed classes;
the aggregate modules also reject missing merged execution data. Thresholds must not be lowered
to get a build through. Passing compilation is not evidence of passing these gates.
