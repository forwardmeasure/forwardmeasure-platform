# Platform version consolidation — 2026-10-07

## Testing policy, guide and focused repairs — 2026-10-07 18:06 EDT

The user agreed to complementary direct and authenticated REST tests across Quarkus, Spring and
Micronaut. Codex owns the current FEI/FOWF/FDS test repairs. See
[the detailed guide for Claude](testing-guide-for-claude-2026-10-07.md) and its linked static inventory;
the older FEI REST-only handover is explicitly superseded.

Three real Maven cache fixtures pass in `/tmp/platform-cache-regressions-openapi-20261007.log`.
OpenAPI generation and dependency input materialization are now always run on cache hits to retain
source registration and inputs. Cache remains enabled. The full product reproduction is being
recompiled; the initial isolated warm-cache fixture itself passed before the policy change.

`/tmp/fei-managed-services-fowf-regressions-03-20261007.log` has passed the selected **12 FOWF tests**
(4 upgrade/recovery, 8 execution-JPA). The new production fix reads only canonical error fields from
native-start event data while retaining full history; commit/reload assertions pass. This selected
run proceeds toward the three FEI managed-service/HTTP framework leaves. Full run56 had passed
25 migrations, 24 execution application and 10 definition application tests before the now-fixed
execution-JPA failures. Remaining full-suite gates, current-image dispatch, FEI API matrix and Studio
acceptance are not complete. No production deployment, image push, commit or publication occurred.


## WorldCheck canonical persistence regression passes; API case compiled — 2026-10-07 17:25 EDT

`/tmp/fei-worldcheck-canonical-index-regression-jdk2503-20261007.log` succeeds across the required
192-module production/test compile closure. Exactly **one selected test** executed and passed
(12.64s): `CanonicalScreeningIngestionIntegrationTest#premiumWorldCheckExportUsesCanonicalSchemaAndExplicitIndexJson`.
It loads the unchanged three-row premium sample through the production canonical source/mapping
contract and explicit index JSON, then checks all three acknowledged documents, 35 retained raw
columns, the `source_data.UPDATE CATEGORY=C1` keyword query returning UID3695, normalized date/country,
and a positive organization screening match despite blank FIRST NAME. It uses real OpenSearch.
This is worker/indexing evidence, not public API dispatch evidence.

The enhanced `ReferencePopulationPublicApiAcceptanceIT` **compiled**, including explicit input URIs,
workflow completion, actual index/metadata/raw-field assertions and public screening. It remains
**unexecuted**. Source/mapping fixture copies match the production contracts. Premium CSV byte-copy
SHA256: `c53107a245d24749b03bc431d3e8071d64147c200b5b87b9efa1c96908cc565e`.

This successful run used process-local Temurin25.0.3 with normal compiler mode, cache enabled and
bounded memory. Earlier Temurin25.0.4.1 native crashes (including under C1) remain unexplained.
Full stack now resumes at the first missing FOWF prerequisite, `:openworkflow-migration`, in
`/tmp/forwardmeasure-takeover-54-jdk2503.log`; current FOWF images must build before the corrected
FDS dispatch fixture runs. Other module gates, full framework/API matrix and live Studio remain
pending. No publication, production deployment, commit or push occurred.


## FDS fixture image ordering and WorldCheck persistence acceptance — 2026-10-07 17:20 EDT

Run53 was stopped during real FOWF dispatch after identifying stale-image evidence: Kafka execution
remained RUNNING and history projection retried; engine/execution images were from October 6, and
FDS still selected the retired combined operation-adapter image. The launcher's reactor dependencies
omitted its seven FOWF image prerequisites. Written/compiled FDS fixture changes choose engine-specific
adapters and platform-managed tags, add POM-only image build-order dependencies, and use current
runtime/control-plane configuration. Renewed tests must follow local image builds; no pass is claimed.

FEI's new public API WorldCheck case supplies the actual checked-in 3-row/35-column premium sample,
versioned source/mapping and explicit canonical index JSON. It checks physical persistence, all raw
fields, ownership, normalization, custom vendor-field indexing, no dynamic raw-field indexing, and
positive/negative public screening including the organization whose FIRST NAME is blank. Keeping the
population's standard default while passing the custom index URI proves the request override works.
This public API case is not executed. A separate canonical worker/OpenSearch regression uses those
same data/index fixtures and is now running through the bounded reactor in
`/tmp/fei-worldcheck-canonical-index-regression-20261007.log` (one explicitly selected test, not a full
suite). Both tests' current production/test compilation is included; status is running. The preceding
192-module compile stopped only on a missing new test-helper overload, now fixed.

After this focused check, resume the full stack at `:openworkflow-migration`, the first required FOWF
module absent from the clean local repository. The changed reactor ordering then builds current
FOWF service images before FDS launcher tests. Do not resume at launcher and skip those prerequisites.
C1 workaround/cache caveats remain. No publication, production deployment, commit or push occurred.


## Pekko final gate and FOWF fixture admission pass — 2026-10-07 16:58 EDT

`/tmp/fds-pekko-launcher-contracts-c1-03.log` passes 15 focused tests: five Pekko source contracts,
ten launcher migration/identity, planner, shipped-workflow compilation and HTTP contracts.
`/tmp/fds-pekko-final-key-policy-c1-01.log` is a selected **Pekko-only** clean install/image build:
all **64 tests** pass, as do both gates (761/813 lines, 223/253 branches). New local image ID is
`sha256:b1d557aaff466de116679d6d9afcd41775199decf264b290d5edf549da3a9c3f`
for `forwardmeasure/data-streaming-executor-pekko:1.1.0`; not pushed.

`/tmp/fds-launcher-fowf-admission-c1-01.log` passes all three real FOWF admission checks with the
updated fixture settings and provisioned runtime database role. Stage4 was strengthened from
"anything except 500/502/504" to an exact 404 with a revision-related problem detail. Its claim is
now correctly scoped to admission rejection with the engine running: an unknown publication is
rejected before engine dispatch, so this is not proof of a dispatched workflow.

Full stack resumes only at launcher application in `/tmp/forwardmeasure-takeover-53-c1.log`.
Remaining real workflow dispatch, launcher gate, downstream modules and FEI API/Studio acceptance
are still pending. The configured production selection (read-only, not a live-cluster assertion)
is Quarkus/Kafka Streams with FDE enabled; `/tmp/fei-repair-selected-image-plan-20261007.json`
lists 26 configured image leaves across FOWF/FDS/FEI/FDE. This is a draft deployment inventory,
not yet the finalized affected-image closure or evidence those images have built.

Maven/test JVM C1 workaround and cache remain enabled. No production deployment, publication,
commit or push occurred.


## Real two-Job ingestion passes; launcher fixture repair and Pekko parity — 2026-10-07 16:52 EDT

Run52 passes both real K3s Spark→Kafka delivery tests: single source (59s) and correlated sources
(64s), using the locally built executor images. The separate private-registry Pekko/Kafka pull
checks skip without credentials. Launcher application overall fails: 38 tests, one failure,
14 errors, two skips. Ten errors are the shared FOWF fixture supplying obsolete database settings
to the migration image; remaining failures are old /v1/executions protocol paths, an options-only
bounded Spark source reused for continuous dispatch, and an unreferenced job document supplied
when compiling the stop workflow. These are fixture/contract inputs, not passing acceptance.

Written fixture corrections use the migration entrypoint's CONTROL_PLANE/ADMIN settings, runtime
service credentials and current tenant host/port settings; clean up acquired disposable resources
on failed startup; target /v1/workflow-executions; give continuous Spark its required Kafka URI;
and compile stop-ingestion with only its actually referenced deployment document. Production
migration history and generated clients are untouched. Focused validation is running in
`/tmp/fds-pekko-launcher-contracts-c1-03.log`.

Pekko bounded correlation also silently dropped missing keys despite FAIL: reproduced by
`/tmp/fds-pekko-missing-key-c1-01.log`. Both bounded and continuous Pekko now apply the selected
malformed-record policy to missing/blank keys, matching Spark and the repaired Kafka path.
`/tmp/fds-pekko-missing-key-c1-02.log` passes all six continuous integration tests, including real
missing/blank-key failures with no input offset commits. Its bounded regression initially reused
a file already consumed by Camel and timed out on the second case; unique input files correct
that test setup. Current bounded rerun, clean Pekko gate/rebuilt image and downstream validation
remain pending. The earlier Pekko image is not acceptance of these latest source changes.

Maven and test JVMs still use process-local C1; container image runtime settings are unchanged.
FEI public API nine-cell matrix, live Studio and remaining stack/framework gates remain open.
No production deployment, publication, commit or push occurred.


## Kafka key-policy repair and AuthZEN clean gates — 2026-10-07 16:43 EDT

Run51 (`/tmp/forwardmeasure-takeover-51-c1.log`) accepts Kafka Streams with **16 tests**, coverage
379/401 lines and 104/114 branches. The missing/blank-key regression first reproduced silent
success under malformedRecord=fail in `/tmp/fds-kafka-missing-key-c1-02.log`; bounded correlation
now shares continuous correlation's mapping/key/error-policy validator. A failed source cannot
publish a partial correlated result. Current local executor image ID is
`sha256:559ea264752a3812cea91ee23f9ab9a7428730a6a0ac510a764124ec5ba1ed1b`
(`forwardmeasure/data-streaming-executor-kafka-streams:1.1.0`); not pushed.

That run also passes Kubernetes lifecycle's 15 tests with one private-registry skip, the generated
execution Apache client gate, and AuthZEN testkit's six tests plus both clean gates. It stopped at
AuthZEN client coverage. New token lifecycle and authorization protocol regressions cover cache
refresh/expiry/concurrent callers, encoding, cancellation, failed refresh, broken audit echoes,
malformed/incomplete batch decisions, decision-cache expiry/eviction and invalid runtime settings.
They supplement the real Keycloak issuance and tenant-isolation tests; HTTP protocol fixtures are
not substituted for real PDP integration evidence. No AuthZEN production authorization changes.

`/tmp/authzen-client-lifecycle-c1-01.log` passes 13 focused lifecycle/protocol tests.
Run52 (`/tmp/forwardmeasure-takeover-52-c1.log`) now passes **all 21 AuthZEN client tests** and both
clean gates (256/271 lines, 99/114 branches), plus the generated definition Apache client.
It is proceeding through FDS launcher real Kubernetes/image integration tests. Remaining modules,
all-framework FEI public API matrix and live Studio acceptance are still incomplete. C1/cache
caveats remain as documented. No production deployment, publication, commit or push occurred.


## AuthZEN fixture regressions pass; bounded correlation check — 2026-10-07 16:35 EDT

`/tmp/authzen-fixture-contract-c1-06.log` passes all six AuthZEN fixture tests. Diagnostic coverage
is 316/338 lines (including the response record) and 63/72 branches. The fixture production class
is unchanged. Added real Keycloak tests verify expired administrator-token refresh, disabled-admin
rejection, missing imported user/client/resource-server errors, and separate machine/user permissions.
An explicit HTTP response-fault proxy forwards to real Keycloak before removing token/discovery
fields: incomplete responses fail, retries recover, and the real PDP ultimately allows only the
granted action. This is fault-injection evidence, not a stubbed successful authorization service.
The clean module lifecycle/gate still awaits the next full-stack resume.

Source review found bounded Kafka correlation silently drops missing/blank correlation keys even
under malformedRecord=fail, unlike its continuous path. A real Kafka/OpenSearch regression is
written and compiling/executing in `/tmp/fds-kafka-missing-key-c1-01.log`; no outcome or production
repair is claimed yet. This newly identified concern warrants a selected executor rerun before
advancing downstream. The existing passing run50 remains evidence for the earlier source state.

Remaining whole-stack/framework tests, nine-cell FEI API matrix and live Studio acceptance are
still open. Temporary C1 JVM caveat remains. No publication or production deployment occurred.


## Kafka and Kubernetes final gates; Studio types pass — 2026-10-07 16:26 EDT

`/tmp/forwardmeasure-takeover-50-c1.log` accepts the transactional Kafka repair with all 15 tests
passing and coverage 394/418 lines, 108/122 branches. Current local Kafka executor image is
`docker.io/forwardmeasure/data-streaming-executor-kafka-streams:1.1.0`, digest
`sha256:1f715645b78c4fe4a87843cd88472730fabc7cd4a23c3a2dab9e8e619dc24882`.
Kubernetes job lifecycle passes 15 tests and both gates (257/264 lines, 98/107 branches); the
existing credential-dependent private-image pull test is skipped. No private-registry proof claimed.

Studio browser-test type compilation passes `/tmp/fei-studio-e2e-types-20261007-02.log` after
selected dependency-closure generation of the FOWF TypeScript client. The Studio webapp POM now
declares both imported generated TypeScript clients as provided build dependencies so a selected
reactor build orders them before the frontend. This POM change is written and reactor-model loaded;
the webapp Maven lifecycle has not yet rerun. Studio's 130 unit tests and production frontend build
pass as recorded below. Live browser acceptance remains unexecuted.

The sweep stops at AuthZEN testkit's missing coverage. Two new real Keycloak tests pass
`/tmp/authzen-fixture-contract-c1-03.log`, verifying organization claims, distinct user/reviewer/worker
identities, separated action permissions and host/network issuer behavior. Diagnostic coverage is
304/337 lines, 51/72 branches, below the branch gate. No authorization-policy production changes
were made. Additional real token-expiry and incomplete-realm-import tests are written and executing
in `/tmp/authzen-fixture-contract-c1-04.log`; their outcome is pending.

All JVM execution above uses the disclosed process-local C1 workaround with cache/JaCoCo enabled.
Remaining stack/framework gates, FEI public-API nine-cell matrix, live Studio acceptance and final
combined image/deployment instructions are outstanding. No production deployment, publication,
commit or push occurred.


## Transactional Kafka frontier repaired; job failure checks pass — 2026-10-07 16:09 EDT

Run49 accepts Kafka Streams with 14 tests and both gates, then stops at Kubernetes job lifecycle
branch coverage (62%). Kubernetes lifecycle's existing suite reports 11 passed, one credential-
dependent private-Docker-Hub pull test skipped; no private-registry proof is claimed.

A subsequent real transaction-boundary regression exposes a further Kafka defect, so run49 is not
final acceptance of the current executor. `/tmp/fds-kafka-transactions-c1-01.log` times out after
45 seconds: bounded readers used read_uncommitted and tracked only returned data offsets, ignoring
transaction control records. Both bounded single/correlated readers now use read_committed and
update frontier positions from the consumer after every poll. Single-source accounting excludes
records beyond the captured frontier. `/tmp/fds-transactions-job-failure-c1-02.log` passes the real
committed/aborted/aborted-only source regression, including correlated output exclusion.

That focused run also passes four new Kubernetes contracts: invalid secret/volume/scale admission
and defensive snapshots; real failed-pod terminal status, deletion-before-completion, unschedulable
pending/timeout and API manifest rejection. Cumulative job lifecycle diagnostic coverage is
256/264 lines, 97/107 branches. Full clean gates after these changes remain to be rerun.

Studio production `tsc --noEmit && vite build` passes `/tmp/fei-studio-build-20261007.log` (bundle-size
warning only). Browser-test type compilation initially fails because the generated FOWF definition
client is also missing. Its selected full-reactor dependency closure is generating in
`/tmp/fei-studio-fowf-client-generation-20261007.log`. The live acceptance revision-tab selector now
uses the actual counted label too; this is written evidence, not a browser execution pass.

Still outstanding: remaining stack/modules/frameworks, final affected-image verification, nine-cell
FEI public API and live Studio acceptance. All JVM checks here use the disclosed temporary C1 flag;
cache and JaCoCo remain enabled. No production deployment, publication, commit or push occurred.

## Studio unit suite passes; Kafka final gate rerun — 2026-10-07 16:01 EDT

Spark clean run48 passes 27 tests and both gates; local image digest is
`sha256:a6a29cf8084dd11e6208d4eb0f582d2a36030f7995126a60004b67c6d27aa594` for
`docker.io/forwardmeasure/data-streaming-executor-spark:1.1.0`.
Kafka run48 passes all 10 tests, including retries in all four mode/cardinality combinations,
fatal input surfacing without offset commits, and SIGTERM. It fails the branch gate (81/118; lines
353/412). Its local image built before the gate but is **not accepted**. New real process tests
pass `/tmp/fds-kafka-entrypoints-c1-01.log` (3 tests): mounted/CLI bounded single/correlated delivery,
fatal continuous exit and invalid dispatch/cluster rejection. Cumulative branches reach 95/118.
Additional bounded skip/dead-letter accounting and continuous missing-key checks are in clean run49:
`/tmp/forwardmeasure-takeover-49-c1.log` (active). These results retain the process-local C1 caveat.

Studio initial direct run failed to load eight files because its generated TypeScript client had
been cleaned; 64 tests passed, but that was not a suite pass. FEI-only generation could not resolve
its not-yet-installed FOWF BOM. The bounded full-reactor **selected client dependency closure**
regenerated it successfully: `/tmp/fei-studio-client-generation-reactor-20261007.log` (no Java files
edited by hand). The first complete UI run was 129/130; the revision-tab selector omitted the
visible count. Corrected selector preserves the full-only activation/payload assertions.
`/tmp/fei-studio-unit-20261007-03.log` now passes all **130 tests across 17 files**. Live browser/API
acceptance is still unexecuted. UI build and e2e type compilation are being checked separately.

No images were pushed; no production deployment, commit or push occurred. Whole-stack acceptance
and remaining FEI/framework gates remain outstanding; do not interpret these checkpoints as complete.

## Spark launch checks and Kafka policy repair — 2026-10-07 15:49 EDT

Spark subprocess regressions pass: five real single/correlated launches across all three mains
produce actual screening results on Kafka and honor the launcher handoff override; missing settings
and no-stage dispatch exit nonzero. Source-row regressions preserve Java/Scala nested structures,
null/false/zero and date/timestamp values. Evidence: `/tmp/fds-spark-entrypoints-c1-02.log` (5 tests).
Continuous Spark restart test passes `/tmp/fds-spark-recovery-c1-01.log`. Run47 passed 26/27 tests;
the new policy test initially expected an unnormalized key. Corrected uppercase-key assertion and
all three correlation tests pass `/tmp/fds-spark-kafka-policy-c1-01.log`. Cumulative diagnostic
coverage is 433/467 lines, 135/156 branches; final clean gate still requires run48.

That same focused run reproduced Kafka continuous ingestion terminating on malformed JSON despite
`malformedRecord: skip`. The continuous single/correlated paths now apply the ingestion policy to
source parsing/mapping, use the shared sink retry contract, and close their sink client on stop.
Both real Kafka-to-OpenSearch skip cases pass `/tmp/fds-kafka-policy-c1-02.log`. Shared changelog
serialization and bounded-frontier regression tests pass `/tmp/fds-kafka-retry-c1-02.log`.
The latter also reproduced bounded correlated delivery ignoring `sinkFailure: retry`; that path
now uses the shared retry handler. Written regression covers all four mode/cardinality combinations.
Written fatal-input tests require surfaced worker failure and no input offset acknowledgment.

`/tmp/forwardmeasure-takeover-48-c1.log` is the active clean stack resume at Spark, testing these
changes before advancing. Cache/JaCoCo stay enabled; process-local C1 workaround remains in use.
Kafka full gate, subsequent framework/product tests, FEI public-API matrix and live Studio acceptance
are not complete. Only the Pekko repair image has built in this sweep; no image publication or
production deployment, commit or push occurred.

## Pekko accepted; Spark validation underway — 2026-10-07 15:35 EDT

`/tmp/forwardmeasure-takeover-46-c1.log`: Pekko passes all 62 tests and both module gates
(761/813 lines, 223/253 branches). Local image `docker.io/forwardmeasure/data-streaming-executor-pekko:1.1.0`
built as `sha256:8356ed373ee759a4bce71b2d49f5c36a5d47d2dbcefc0655e743e63da5d1bc1c`; not pushed.
Execution used the temporary process-local C1 workaround described below, with JaCoCo/cache enabled.

New regressions reproduced a production delivery defect: Camel asyncSend can complete with an
Exchange containing an exception. Ignoring that Exchange acknowledged rejected OpenSearch/SQL
writes. Pekko now propagates Exchange failures in HTTP, single SQL and batched SQL delivery.
HTTP rejection/retry and PostgreSQL constraint tests pass. A real write-blocked OpenSearch index
now proves the worker exits nonzero and leaves its Kafka offset uncommitted; SIGTERM drain/commit
also passes. Reproduction: `/tmp/fds-pekko-http-contracts-c1-01.log`; focused SQL validation:
`/tmp/fds-pekko-http-sql-contracts-c1-03.log` (SQL passed; HTTP request-count fixture was subsequently
corrected to allow the HTTP client's automatic retries, then all tests passed in run46).

Run46 advanced to Spark: 21/22 tests passed; its continuous recovery fixture constructed an empty
Spark transform plan now rejected by admission. The fixture now supplies and verifies a real date
transform across restart and separate source handoffs. Focused rerun is in progress at
`/tmp/fds-spark-recovery-c1-01.log`. Remaining executors/frameworks, full-stack gates and FEI
public-API/Studio acceptance remain open. No production deployment, publication, commit or push.

## Pekko boundary coverage and JVM validation issue — 2026-10-07 15:25 EDT

Full Pekko run45 passes all 44 tests, but its local coverage remains below 85% (74% lines,
52% branches): `/tmp/forwardmeasure-takeover-45.log`. Additional regression development continues;
this is not a module acceptance pass.

New source/storage tests pass (5 tests, `/tmp/fds-pekko-source-storage-contracts-02.log`): a real
1,003-object MinIO stream crosses pagination and preserves every payload; real S3 protocol parsing
rejects missing/repeated continuation and accepts empty advancing pages; JSON/NDJSON/CSV data,
malformed inputs and correlation error policies are checked. The PostgreSQL-backed JPA stream
regression passes in `/tmp/fds-pekko-jpa-entrypoint-contracts-01.log`. Four Kafka changelog tests
pass in `/tmp/fds-pekko-recovery-entrypoints-02.log`, including offset/topic/partition replacement,
serialization failure recovery, invalid retention and corrupt persisted state.

A focused entrypoint test reproduced bounded Pekko bypassing a still-planned Spark stage
(`/tmp/fds-pekko-spark-stage-reproduction.log`). The Spark-stage guard now applies to both modes.
Entrypoint and continuous-correlation boundary verification is in progress.

The entrypoint rerun hit a JVM SIGSEGV (exit134), not an assertion failure in production logic:
`forwardmeasure-data-streaming-executor-pekko/hs_err_pid1100117.log`, C2 register allocation while
compiling JaCoCo ASM Handle.hashCode. An existing September14 crash report
`hs_err_pid1394504.log` shows C2 register allocation while compiling JaCoCo ASM ClassReader.readCode.
Both use Temurin25.0.4.1+1. Reports are preserved. This identifies the crash location, not a proven
root cause. Current bounded diagnostic execution uses process-local
`JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -XX:TieredStopAtLevel=1"`, retaining JaCoCo and cache:
`/tmp/fds-pekko-boundary-contracts-c1-01.log`. No production JVM/build configuration was changed.
Distinguish C1 diagnostic results from ordinary-JVM validation; the intermittent native crash remains
an environment limitation until normal-JVM verification succeeds reliably.

Full-stack, current images and FEI public-API/Studio acceptance remain incomplete. No deployment,
publication, commit or push occurred.


## Executor shutdown regression reproduced and repaired — 2026-10-07 15:15 EDT

FDS fixtures, Camel, JDBC, object-storage bridge and streaming API now pass their tests and local
gates (`/tmp/forwardmeasure-takeover-42.log` through `44.log`). The JDBC test adds explicit
pre-scheduling rejection of invalid bounds; the streaming API test checks terminal observation
and repeated cleanup of completed jobs.

Pekko's full suite initially ran 43 tests: 42 passed, one SIGTERM subprocess failed
(`/tmp/forwardmeasure-takeover-44.log`). The main thread could classify a requested shutdown as an
unexpected stream failure, while Pekko's own concurrent JVM shutdown hook could stop the consumer
before the application's drain completed. The standalone entrypoint now owns draining, disables
the competing Pekko JVM hook and marks a requested stop before draining. Completion is logged
inside the hook, where JVM exit cannot race past it. The analogous requested-stop/logging fix is
written in Kafka Streams. Both subprocess harnesses forward the active JaCoCo agent.

Focused Pekko SIGTERM verification passes (`/tmp/fds-pekko-shutdown-01.log`), including an actual
Kafka offset check after shutdown. A new failed-delivery subprocess test asserts nonzero exit and
no commit of the failed input. Full Pekko rerun and remaining stack are ongoing in
`/tmp/forwardmeasure-takeover-45.log`; Kafka Streams changes have not yet been executed.
Pekko's space/plus-ID real OpenSearch regression passed in run44.

Public API/Studio acceptance will use local disposable fixtures unless an existing test environment
is supplied. Whole-stack acceptance, image completion and the nine-cell public API matrix remain
open. No production deployment, publication, commit or push has occurred.


## Mapper and core regressions pass — 2026-10-07 15:09 EDT

FDS mappers passes 29 tests and both gates (174/180 lines, 74/80 branches),
`/tmp/forwardmeasure-takeover-40.log`. New real OpenSearch replay tests reproduced ID corruption:
form encoding changed spaces to plus signs in document paths, colliding with actual plus-sign IDs.
The shared writer now preserves path identity, restores interruption on cancelled writes and closes
its HTTP client. Mapping regression also reproduced substitution of placeholder-like source data;
template expansion now operates on the original template with quoted literal replacements.
Unused legacy payload-snapshot logging code was removed; sampled logs contain counts only.
Reproduction logs: `/tmp/fds-mappers-contract-reproduction-02.log` and
`/tmp/fds-mappers-contract-03.log`. The latter used the explicit raw source_data index mapping.
The same path-encoding fix is written in Pekko's separate Camel writer, and its real sink test now
uses distinct space/plus IDs; execution of that adapter test remains pending.

FDS core passes 20 tests and both gates (239/249 lines, 116/127 branches),
`/tmp/forwardmeasure-takeover-41.log`. New tests exercise bounded retry exhaustion/recovery,
interrupted backoff, fail-fast writes, sparse correlated evidence, real heavy-screening planning,
stable handoff identity and continuous per-source handoff. FDS fixtures passes 17 tests including
an isolated broken-JAR packaging contract, and Camel connector passes its test in the ongoing
`/tmp/forwardmeasure-takeover-42.log`. The run is proceeding through the remaining connectors.

Whole-stack and FEI public-API/Studio acceptance are not yet complete. An asynchronous question
asks for an existing disposable acceptance configuration location, or local disposable fixtures;
independent work continues. No deployment/publication/commit/push has occurred.


## FDS API and transforms pass; mapper regressions underway — 2026-10-07

FDS API passes 24 tests and both gates (321/363 lines, 193/204 branches),
`/tmp/forwardmeasure-takeover-38.log`. FDS transforms passes 28 tests and both gates
(491/497 lines, 298/327 branches), `/tmp/forwardmeasure-takeover-39.log`.
The transform regressions reproduced two production defects: impossible full dates were silently
clamped (20250229 became 2025-02-28), and city-only provider locations were discarded.
Full-date parsing is now strict; sparse location parsing retains valid cities. Valid leap dates,
identifier schemes, domain lists, missing inputs and existing provider fixtures pass.

The current full-stack stop is FDS mappers: its 22 existing tests pass, but handwritten coverage
is 60% lines/57% branches. Additional writer and mapping contracts are being developed and run.
No threshold has been reduced. Full-stack acceptance and local image completion remain pending.
The live public-API/Studio matrix still lacks disposable endpoints and configured tenant/login
identities. No deployment, publication, commit or push has occurred.


## Recall gates pass; FDS boundary regression continuation — 2026-10-07 14:58 EDT

Entity-matching-opensearch passes 16 tests and both gates (321/324 lines, 120/126 branches),
`/tmp/forwardmeasure-takeover-36.log`. Its decoder now rejects blank textual name hits, preserving
trimmed usable names. The matching testkit passes two consumer-contract tests and its gates in
`/tmp/forwardmeasure-takeover-37.log`, including builder snapshot isolation and real recall/scoring.

The next stop is FDS API: 14 original tests pass but module coverage was 44% lines/42% branches.
Six execution/admission contracts now pass, covering stable tenant/revision consumer identity,
cardinality, graph rejection, source uniqueness, Kafka URI decoding and merge-policy boundaries.
Four HTTP contracts reproduced swallowed interruption during index initialization
(`/tmp/fds-index-http-reproduction.log`). HEAD/PUT now restore interruption before surfacing the
failure; all three HTTP-stage cancellation checks pass in the current full-stack resume
`/tmp/forwardmeasure-takeover-38.log`. Authenticated create/reuse, rejected ownership/settings,
HTTP failures and pre-network validation are included. Full module gate result is pending.

No threshold was reduced. Full-stack acceptance, live public-API/Studio prerequisites and local
image completion remain open; no production deployment/publication/commit/push has occurred.


## Matching core gates pass; recall validation underway — 2026-10-07 14:52 EDT

Core now passes 62 tests and both unchanged 85% gates: 889/913 lines and 496/578 branches.
Evidence: `/tmp/entity-matching-evidence-03.log`, repeated successfully in full-stack resume
`/tmp/forwardmeasure-takeover-35.log`. New contracts cover missing names, unknown kinds,
geography strengths/conflicts, strict date exclusions, independent corroboration, token overlap,
partial policy isolation, invalid admission thresholds and conservative recall normalization.

The next gate is entity-matching-opensearch (11 existing tests pass, 61% branch coverage).
Five new boundary regressions reproduced blank textual names being admitted as usable hits;
`/tmp/entity-matching-recall-01.log` fails that assertion. The decoder now strips textual names
and discards blanks consistently with object-shaped names. Full-stack resume is running in
`/tmp/forwardmeasure-takeover-36.log`. This adds the recall adapter to the shared image impact.

The live public-API/Studio suite currently lacks supplied disposable endpoints/tenant identities;
the FEI_ACCEPTANCE token/login variables are unset. Existing self-orchestrated k3s tests can be
run after their required local images are built; they do not replace the nine-cell public API matrix.
No production deployment or publication is authorized or performed.


## Matching alias regression fixed; handwritten gate remains open — 2026-10-07 14:16 EDT

Added policy transport/partial-override/isolation/threshold regressions and an identifier recall versus
scoring contract. The latter reproduced a real bug: US_TIN was emitted as a recall alias for US-TIN,
but canonical scoring treated it as a different scheme. Reproduction:
`/tmp/entity-matching-contract-reproduction-20261007.log`. Canonicalization now includes US_TIN.
All 50 entity-matching-core tests compile and execute successfully in
`/tmp/entity-matching-core-regressions-20261007.log`, including the six new tests. The build correctly
FAILS the unchanged handwritten branch gate at 66% (previously 60%); this is not a coverage pass.
The new shared matching change also affects its runtime consumers; include those in the final
combined image-impact audit, alongside storage and CSV changes. Full-stack acceptance and image
builds remain incomplete. Generated-code policy verification remains complete as recorded below.


## All three packaged delta workers pass — 2026-10-07 14:15 EDT

`/tmp/fei-delta-packaged-frameworks-02-20261007.log` is BUILD SUCCESS: six subprocess integration
tests executed (two each in Quarkus, Spring and Micronaut) against real OpenSearch. They verify
private delta staging, retained active baseline, merged document contents, replay idempotence,
and nonzero failure without APPLICATION_ID. The first run exposed missing Commons Logging in
the minimal Quarkus runtime; explicit runtime dependencies now cover Quarkus and Micronaut.
Spring already supplies it. The subprocess harness now enforces its timeout without blocking on
stdout first and forwards the active JaCoCo agent to collect actual application execution.

Combined with the preceding 17 selected ingestion/FDS tests, these provide executed evidence for
simple, correlated, resolution and packaged delta paths. These are focused runs, not complete
full/delta lifecycle, Studio, all product modules, public API matrix or coverage acceptance.
Generated-code coverage exemption is verified. Remaining handwritten entity-matching coverage
and full-stack regression work is still open; no new image build/deployment/publication is claimed.


## Focused ingestion package and regression pass — 2026-10-07 14:12 EDT

`/tmp/fei-ingestion-focused-reactor-08-20261007.log` is BUILD SUCCESS. Executed evidence:
three physical CSV width tests, one REST progress replay/outage test, one OpenSearch delta test,
eleven simple/canonical/customer-master/progress/WorldCheck tests, and one real Spark/Kafka
correlated ingestion test (17 total). The customer-master regression now passes against PostgreSQL;
repeated CSV headers no longer falsely reject valid rows. Missing/extra physical cells still fail.
All three framework delta-worker production/test sources compiled and their jars packaged.

The framework subprocess ITs are now running separately after packaging in
`/tmp/fei-delta-packaged-frameworks-20261007.log`; they were not executed by the package checkpoint.
Neither checkpoint is a complete product regression or coverage-gate pass. The generated-code
exemption is complete and verified; the entity-matching handwritten coverage gap and remaining
full-stack acceptance are still open. No images were published and no deployment/commit/push ran.


## Generated exclusions and aggregate ownership verified — 2026-10-07 14:10 EDT

Generated code is exempt from coverage by explicit user instruction; handwritten production code
still requires 85% lines and 85% branches. The policy plugin now also verifies that a requested
aggregate exemption actually belongs to an aggregate that reports, stages and checks the module.
Modules omitted from their aggregate retain their local gate. All 41 policy tests and the plugin's
coverage gates pass: `/tmp/platform-generated-coverage-plugin-02.log`.

Executed integration evidence: both real Maven cache fixtures and the mixed generated/handwritten
coverage fixture pass (3 tests, `/tmp/platform-cache-and-coverage-regressions-20261007.log`).
The mixed fixture verifies both a successful generated exclusion and failure below 85% handwritten
branches. Compiler and build-helper goals execute on cache hits to restore Maven's reactor artifact
and generated-source-root registrations; the cache remains enabled.

FEI focused run 06 executed and passed: six canonical screening tests, two simple-source tests,
one database progress test, one REST replay/outage test and one real OpenSearch delta test.
It failed customer-master resolution because CSV validation used the unique header count for an
export with repeated column names. Physical-width validation and three focused CSV regressions
are now written. WorldCheck's legacy assertion also expected a generated Java model's default
empty category array; the generic-document expectation now correctly requires absence when no
category maps. Verification is running in `/tmp/fei-ingestion-focused-reactor-07-20261007.log`.
This selected package run is not a full-suite or coverage-gate pass. Spark correlated execution
and all three packaged framework delta-worker IT executions are still pending.

The full-stack gate remains stopped at entity-matching-core: 44 tests pass but handwritten branch
coverage is 60%. Full product/framework acceptance and new local images remain pending. No
publication, deployment, commit or push has occurred.


## Generated-code policy accepted and verified — 2026-10-07 13:55 EDT

The user explicitly exempted generated code from coverage. The platform now classifies compiled
classes using generated-source provenance and retained Generated markers; mixed modules retain
handwritten classes. It writes target/coverage-generated-excludes.txt and supplies exact class
exclusions to JaCoCo. Generated Java was not edited. All 37 policy tests pass, including eight new
classification/gate regressions (/tmp/platform-generated-coverage-plugin.log). A real Maven
mixed-module fixture passes with generated output excluded and deliberately fails at 50%
handwritten branches against the unchanged 85% threshold:
 /tmp/platform-generated-coverage-integration-03.log, PASS.

The NLP client now passes all four HTTP contracts and its generated-code exemption in
/tmp/forwardmeasure-takeover-34.log. The next full-stack stop is entity-matching-core: all 44
tests pass, but handwritten branch coverage is 60%. This is a real remaining coverage gap.

Focused FEI execution proved REST cumulative progress/replay and terminal outage behavior after
correcting the HTTP fixture to hold a persistent outage across Apache's automatic retries.
Delta-worker execution then exposed missing JCL on the plain library test classpath. The same
dependency is missing from the Micronaut delta-worker runtime; it has been added there at runtime
scope, and separately to the plain library at test scope. New POM changes are awaiting verification.

Cached compile-only builds exposed two lifecycle side-effect failures: reactor dependency
resolution after restoring classes, and JPA Javadoc losing its generated metamodel source root.
The compiler plugin now runs its incremental goals even on cache hits to restore those Maven
registrations. The extension remains enabled. A two-module cold/warm/test-to-package cache fixture
has been written but not yet executed. Logs of the failures:
 /tmp/fei-ingestion-focused-reactor-03-20261007.log and ...-04-20261007.log.

All three framework delta-worker ITs had stale baseline-deletion expectations and missing owner
metadata. Their assertions now require baseline retention, a private ready alias and idempotent
replay. They are written, not yet executed. The selected worker and all three delta-framework
packages are compiling/running focused tests in
/tmp/fei-ingestion-focused-reactor-05-20261007.log; this is not a full-suite/gate pass.

A further audit found blanket aggregate-coverage bypasses covering modules absent from their
aggregate: FEI's aggregate explicitly includes only domain. FOWF/JPA also have uncovered module
ownership gaps. Correcting aggregate ownership validation remains required before claiming the
85% policy applies everywhere. Full product/framework acceptance and new local images remain
pending. Nothing was published, deployed, committed or pushed.

## Storage verified; generated-client gate pending — 2026-10-07 13:29 EDT

Azure passes all four regressions and both 85% gates (238/248 lines, 103/114 branches) in
`/tmp/forwardmeasure-takeover-31.log`. Together with the GCS/S3 checkpoints above, all three
storage providers now pass their module regression and coverage checks.

The full reactor currently stops at `forwardmeasure-nlp-named-entity-recognition-client-java`,
a module containing only generated OpenAPI Java. Four handwritten HTTP contract tests now compile
and pass, covering all seven API operations, Unicode/large offsets, nanosecond timestamps, gzip,
headers, required arguments, error responses and interruption. Evidence:
`/tmp/nlp-client-contracts-final.log` and `/tmp/forwardmeasure-takeover-32.log`.
The latter still fails the unchanged generated-code coverage gate (25% lines, 11% branches).
The user has been asked whether generated clients should retain the numeric gate or use protocol
contract tests with the 85% gate applying to handwritten production code. No exemption or policy
change has been made while awaiting the answer. Generated Java was never edited.

Independent focused FEI worker/progress regressions are running in
`/tmp/fei-ingestion-focused-regressions-20261007.log` through the bounded wrapper, with selected
tests and their reactor dependencies. This is not a full-suite or coverage-gate pass. Full product
regressions, public-API/framework acceptance and current local image builds remain pending.

## GCS passes; Azure listing repair — 2026-10-07 13:20 EDT

GCS passes all 7 tests and both gates (305/320 lines, 160/176 branches) in
`/tmp/forwardmeasure-takeover-28.log`. The tests include actual SDK HTTP error classification,
sparse metadata, paged deletion order, provider discovery and seek-failure cleanup. Additional
production fixes preserve InputStream's zero-length read contract, avoid inclusive-range overflow,
and close a read channel when positioning fails while retaining suppressed cleanup errors.
The zero-length read failure was reproduced in `/tmp/gcs-range-reproduction.log` before the fix.

Azure's previous listing implementation discarded continuation tokens and ignored delimiters.
The real Azurite regression reproduces the lost continuation in
`/tmp/azure-pagination-reproduction.log`; production now returns a provider page and continuation,
uses hierarchical listing for delimiters, and avoids inclusive-range overflow. Both Azure
integration tests pass in `/tmp/forwardmeasure-takeover-29.log` but coverage still fails.
Two more HTTP-error/provider-discovery contracts are being verified in
`/tmp/forwardmeasure-takeover-31.log`. The preceding run exposed an incorrect test assumption:
Azure uses bucket paths on custom domains, without the emulator's account-name path prefix.

Full product regressions and current local image builds remain pending. No thresholds or
exclusions were weakened; all new provider tests use real SDKs, local HTTP fault servers or
storage emulators. No publication, deployment, commit or push has occurred.

## Storage regression continuation — 2026-10-07 13:09 EDT

Liquibase now passes 12 regressions and its coverage gates (144/157 lines, 35/38 branches).
The subsequent JPA reactor, including Quarkus, Spring and Micronaut, passes again in
`/tmp/forwardmeasure-takeover-18.log`. Object Storage API passes 9 tests (176/177 lines,
162/164 branches); core passes 9 (174/174 lines, 104/112 branches), logs 19 and 20.
S3 passes all 5 tests and both 85% gates in `/tmp/forwardmeasure-takeover-22.log`.
The S3 regressions exposed reversed application/presigning endpoint selection; production now
uses `endpoint` for SDK operations and `publicEndpoint` (falling back to `endpoint`) for signing.

GCS signing previously rewrote the URL after signing, changing the signed host and double-encoding
object paths/query values. `/tmp/gcs-signature-reproduction.log` reproduces the defect;
`/tmp/gcs-signature-fix.log` passes after signing with the public origin directly. The regression
independently verifies GET/PUT RSA signatures using an ephemeral test key. Public GCS endpoints
must be HTTP(S) origins without path prefixes, userinfo, query or fragments.
Three additional GCS behavior/failure tests are written; the latest compile attempts exposed
missing test imports (logs 23/24), now corrected. Their execution and coverage remain pending in
`/tmp/forwardmeasure-takeover-25.log`. Full FOWF/FDS/FDE/FEI regression completion and new local
images are still pending. Nothing has been published, deployed, committed or pushed.

## Database regression continuation — 2026-10-07 12:48 EDT

Jupiter fixture adapters pass 9 tests and coverage (`/tmp/forwardmeasure-takeover-15.log`),
including real Jupiter contexts verifying default registration, parameter selection, cleanup,
repeated cleanup and unavailable-container errors. Quarkus fixture adapters pass 3 tests and
coverage (`/tmp/forwardmeasure-takeover-16.log`), including Dev Services network URLs and
caller-owned network survival. The module-local coverage gates now pass for all shared fixtures.
Migration API and JDBC modules each pass five new regressions and their gates
(`/tmp/forwardmeasure-takeover-17.log`). They protect immutable migration selections, repeatable
change counts, validation consistency, real PostgreSQL schema restoration and JDBC cleanup
failure/suppression behavior. Liquibase's existing six tests pass but coverage fails; six new
real-Liquibase/PostgreSQL fault and checksum regressions are running in
`/tmp/forwardmeasure-takeover-18.log`. No deployed changelog or migration history was edited.


## Image import and remaining adapters — 2026-10-07 12:42 EDT

OpenSearch passes 4 tests, 75/83 lines and 29/34 branches (`/tmp/forwardmeasure-takeover-10.log`).
Kafka passes 5 tests, 71/76 lines and 21/22 branches (`/tmp/forwardmeasure-takeover-11.log`),
including a real isolated client using the host-gateway listener and broker metadata.
K3s passes all 8 tests and coverage (`/tmp/forwardmeasure-takeover-14.log`). A real pod ran a
locally imported image by digest with imagePullPolicy Never. Command-boundary tests verify
registry ports, digest-qualified inputs, malformed/missing digests, list/tag failures, I/O
failures and preserved interruption. Production fixture image-loading/pinning catches now
restore the thread interrupt flag; pinning exposes a package-private command boundary, and
blank listing lines replace an unreachable zero-length split check.
The Jupiter fixture adapter's existing 5 tests pass but branch coverage fails (46%). New real
JUnit lifecycle tests are running in `/tmp/forwardmeasure-takeover-15.log`. Full product tests
and image builds remain pending; no publication/deployment/commit/push has occurred.


## Shared service fixtures — executed 2026-10-07 12:34 EDT

MinIO passes 5 tests, 86/91 lines and 32/36 branches (`/tmp/forwardmeasure-takeover-06.log`).
Keycloak passes 3 tests, 76/85 lines and 22/24 branches; PostgreSQL passes 3 tests,
104/111 lines and 30/34 branches (`/tmp/forwardmeasure-takeover-09.log`). These use real
containers, including sibling-network access and fixture lifecycle checks. Keycloak verifies
host-issued tokens carry the network issuer; rejected credentials produce OAuth invalid_grant.
A proposed pre-interrupted-thread test was removed because it exercised Docker inspection,
not interruption of the token HTTP call; that HTTP interruption branch remains unverified.
PostgreSQL now verifies SQL/schema operations, identifier rejection and caller network ownership.
OpenSearch's existing two tests pass but coverage fails; index/network lifecycle regressions
are written and currently running in `/tmp/forwardmeasure-takeover-10.log`.


## Executed checkpoints during takeover — 2026-10-07 12:28 EDT

- Micronaut validation: 18 passing tests, 101/101 lines, 66/68 branches (user's last run).
- Spring Security: two new servlet 401/403 response regressions pass; 15/15 lines (no branches).
  Log `/tmp/forwardmeasure-takeover-02.log`; that run subsequently stopped on a corrected
  Quarkus test compilation error (ChallengeData now exposes a header map).
- Quarkus Security: three new tests pass, including an actual Vert.x HTTP challenge response;
  26/28 lines (no branches). All five platform compatibility tests pass.
  Log `/tmp/forwardmeasure-takeover-03.log`.
- AuthZEN API: 22 tests pass, including six new malformed-identity / immutable-decision /
  fail-closed enforcement regressions; both coverage gates pass.
  Log `/tmp/forwardmeasure-takeover-05.log`. The reactor is continuing from there.

These are executed results. Full-stack completion, FEI ingestion acceptance and local image
builds are still pending. No coverage thresholds or exclusions were relaxed.

## Assistant-run regression continuation — 2026-10-07

The user explicitly authorized the assistant to run bounded builds, regression tests and local
image builds, with ongoing progress reports. Deployment, publication, commits and pushes remain
unauthorized. Keep the Maven/Docker caches and resume failures rather than restarting the stack.
The latest user run, `/tmp/forwardmeasure-resume-20261007-121824.log`, passes all 18 Micronaut
validation tests and coverage. It stops at Spring Security because there are no tests/coverage
execution data. Real servlet response-contract regressions are being added for 401/403 security
handlers. FEI runtime acceptance remains pending; historical compile-only restrictions below
refer to earlier checkpoints.

The user authorized completion of the version-centralization sweep described in FEI's
`docs/claude-session-changes-2026-10-07.md`, section 8.1. Existing uncommitted changes are retained.
No tests, image builds/publication, deployment, commits or pushes are authorized by this work.

## Ownership and exceptions

The platform parent owns shared first-party dependency, Java library and Maven plugin versions.
The sweep covers the 14 Maven repositories present under the workspace's `forwardmeasure-*`
directories, without reading or changing the legacy `data-fabric` implementation. Property aliases
have been normalized, including `forwardmeasure.datastreaming.version`,
`forwardmeasure.entitymatching.version` and `forwardmeasure.authzen.version`. Shared ArchUnit,
duplicate-finder, OpenSearch, JSON-P/Parsson and animal-sniffer pins now live in the platform.
Other explicitly versioned Maven libraries/plugins used by these consumers have also been moved
into the platform rather than left as ungoverned local pins.

The policy plugin derives owned properties and managed dependency/plugin coordinates from the
actual platform ancestor model. It runs at `validate` through an inherited profile and checks
original consumer POM declarations, including inactive profiles. Dependencies are distinguished
by type and classifier; a test jar is not interchangeable with its production jar. Maven imports
and managed entries retaining exclusions may reference the platform's version expression.
Ordinary dependencies and plugins inherit managed versions without repeating them.

`revision` identifies the product being built, not a sibling dependency. It remains local for
Maven CI-friendly parent discovery and independently released products. Maven parent coordinates
also remain explicit, since parent resolution precedes inherited property evaluation. Python
package constraints and product data-format versions are outside this Maven library sweep.

An actual compatibility override must be declared in that consuming POM's build/plugins:

```xml
<plugin>
  <groupId>com.forwardmeasure.platform</groupId>
  <artifactId>forwardmeasure-platform-policy-maven-plugin</artifactId>
  <configuration>
    <overrides>
      <override>
        <key>property:netty.version</key>
        <reason>Quarkus 3 requires the platform-pinned Netty 4.1 line.</reason>
      </override>
    </overrides>
  </configuration>
</plugin>
```

Keys identify exactly one `property:NAME`, `dependency:GROUP:ARTIFACT` (with type/classifier when
applicable), or `plugin:GROUP:ARTIFACT`. Reasons must be nonblank; wildcard exceptions and
exceptions for first-party dependency versions are rejected. Parent exception configurations do
not silently authorize declarations in children. Existing Quarkus Netty, Lettuce and Rest Assured
compatibility selections remain explicit; their actual version values are pinned centrally.

The existing Camel 4.22.1 change is retained. Kubernetes client uses 7.8.0, preserving the existing
OpenWorkflow compatibility fix instead of the older Testcontainers-local 7.3.1 pin. HK2 aligns
with the existing Jersey 4.0.2 parent's 4.0.0-M3 dependency after convergence exposed Spark's
separate HK2 declaration. These are dependency changes; compilation is not runtime acceptance.

## Bootstrap and bounded verification

A fresh local repository must install the parent and policy plugin before downstream builds can
run the inherited check. `platform.policy.bootstrap` disables only this check's inherited profile
for those bootstrap invocations. Do not use it for normal consumer validation.

```bash
FM_ROOT=/home/pn/Documents/code/forwardmeasure
FEI="$FM_ROOT/forwardmeasure-entity-intelligence"
PLATFORM="$FM_ROOT/forwardmeasure-platform"
"$FEI/scripts/build-bounded.sh" -f "$PLATFORM/pom.xml" -N \
  -Dplatform.policy.bootstrap -B -ntp -Dmaven.build.cache.enabled=false \
  -DskipTests -DskipITs -Dmaven.test.skip=false test-compile install:install
"$FEI/scripts/build-bounded.sh" -f "$PLATFORM/pom.xml" \
  -pl forwardmeasure-platform-policy-maven-plugin -Dplatform.policy.bootstrap \
  -B -ntp -Dmaven.build.cache.enabled=false -DskipTests -DskipITs -Dmaven.test.skip=false \
  test-compile plugin:descriptor jar:jar install:install
```

These direct goals install local bootstrap artifacts without executing tests or reaching verify.
They do not substitute for license or packaging gates. The initial normal platform `install`
failed RAT on deployment files (`/tmp/version-policy-parent-01.log`). The user subsequently reported
that Claude fixed the platform RAT issues and explicitly asked not to verify them. Those fixes are
preserved; RAT was not rerun. The earlier failure is historical, and no RAT pass is claimed here.

Normal source verification uses the bounded wrapper with
`-B -ntp -Dmaven.build.cache.enabled=false -DskipTests -DskipITs -Dmaven.test.skip=false test-compile`.
One build runs at a time under the shared Maven lock. Production and test compilation are required;
no test execution is implied. Final per-repository evidence will be recorded below when complete.

## Current verification evidence

### Micronaut validation contract coverage — 2026-10-07 12:14 EDT

The user-run `/tmp/forwardmeasure-resume-20261007-120751.log` confirms JAX-RS passes all 44 tests
and its gate: 326/335 lines (97.31%), 269/300 branches (89.67%). The next module,
`forwardmeasure-platform-micronaut-validation`, passed six tests but failed at 26/101 lines
(25.74%) and 18/68 branches (26.47%); most exception/security handlers had no regression coverage.

Twelve additional behavioral regressions now cover invalid bearer-token rejection on public,
internal and protected paths; valid/missing/non-bearer credentials deferring to later rules;
401 challenges versus authenticated 403 responses; real generated Micronaut/JAX-RS binding
metadata for missing arguments and query/path conversion; scalar-only rejected-value echoes;
wrapped Jackson body errors; real Jackson 3 parse failures; bean-validation wire names; safe
fallback responses; routing-error normalization; and leaving streaming/reactive/framework-owned
bodies to their dedicated readers. Micronaut-native inputs use generated record introspection,
and JAX-RS inputs use an actual annotated resource method (constructor-bound JAX-RS parameters
are unsupported by its processor). The tests use the existing Micronaut JAX-RS runtime delegate;
no additional dependency or production change was needed.

Bounded production/test compilation with caching enabled and tests skipped passed:
`/tmp/platform-micronaut-validation-compile-final-20261007.log`, 2.196 seconds. All four test-source
files and their generated metadata compiled. Whitespace checks pass. New regressions are
**written and compiled, not executed**; no coverage pass is claimed. Resume the complete reactor
at `:forwardmeasure-platform-micronaut-validation`, retaining tests/coverage/cache/image builds
and disabling publication. No thresholds or exclusions were changed.

### JAX-RS contract regressions — 2026-10-07 12:05 EDT

The user explicitly requires meaningful regressions protecting real behavior, not perfunctory
tests added merely to clear coverage gates. Preserve this requirement for subsequent repairs.
Coverage reports identify missing scenarios; they are not the behavioral specification.

`/tmp/forwardmeasure-resume-20261007-115654.log` confirms the policy plugin now passes all 29
tests and its gate (152/154 lines, 140/142 branches). The reactor then stopped at
`forwardmeasure-platform-server-jaxrs`: all 28 existing tests passed, but coverage was 241/335
lines (71.94%) and 180/300 branches (60%). The largest gaps were actual request filtering and
JSON-name/type discovery rather than scalar conversion alone.

Sixteen new regressions now exercise malformed/repeated/comma-separated query values and inherited
interface/superclass annotations; leaving unrelated inputs to the runtime; JSON property discovery;
bounded scalar echoes without serializing request objects; specific Jackson mappers and server
error redaction; and provider registration completeness/uniqueness without relying on incidental
registration order. A real Hibernate Validator/Jackson contract regression independently checks
that errors within maps, lists and arrays identify the same JSON paths clients see on the wire.
Request-context proxies provide only JAX-RS boundary plumbing; validation, JSON parsing, mapping
and response objects use the real implementations. These are unit/contract tests, not a claim of
live HTTP acceptance on all three frameworks.

Production and all nine test-source files compiled with the bounded wrapper, tests skipped and
cache enabled: `/tmp/platform-jaxrs-contract-compile-final-20261007.log`, PASS. Whitespace checks
pass. New regressions are **written and compiled, not executed**. No production behavior,
coverage thresholds or exclusions were altered. Resume the full reactor at
`:forwardmeasure-platform-server-jaxrs` with tests/coverage/image builds enabled, pushing disabled.
JAX-RS coverage acceptance and the remaining full-stack/FEI runtime acceptance remain pending.

### Full-stack policy-plugin coverage failure — 2026-10-07 11:51 EDT

The user-run full stack build at `/tmp/forwardmeasure-full-20261007-115121/full-build-and-tests.log`
stopped at `forwardmeasure-platform-policy-maven-plugin`. All 13 existing tests passed, but JaCoCo
recorded 126/154 lines (81.82%) and 90/142 branches (63.38%), below the shared 85% minimums.
The plugin's Maven entry point had no unit coverage; ownership and exception validation also
had uncovered cases. Sixteen additional regression tests now exercise authority traversal,
missing/false authority, original consumer declarations versus inherited exception configuration,
direct plugin ownership, classifier identity, local and cross-product version expressions,
scope/exclusion handling, duplicate/malformed/first-party exceptions, profile-local exceptions,
and absent/module-descriptor/valid coverage-data paths. No production code or coverage limits
were changed for this failure.

Production and all four test-source files compiled using the bounded wrapper with cache enabled
and tests skipped: `/tmp/platform-policy-coverage-tests-compile-20261007.log`, PASS, 2.074 seconds.
The added tests are written/compiled, **not executed**. Whitespace checks passed. Resume the full
reactor at `:forwardmeasure-platform-policy-maven-plugin` with tests, coverage, full-suite and
container-image builds enabled, pushing disabled. Preserve `.m2`; no bootstrap repeat is needed.

The completed user JPA run is now verified:
`/tmp/jpa-regression-20261007-114632.log`, BUILD SUCCESS, 2m53s, finished 11:49:26 EDT.
Its aggregate report records **1,286/1,405 lines (91.53%), 395/454 branches (87.00%), and all 69
classes covered**. JaCoCo explicitly reports all checks met. This supersedes the earlier JPA
coverage-pending notes; it does not establish FEI or the remaining stack's runtime acceptance.

### Coverage policy raised by user — 2026-10-07

**Latest coverage repair (11:45 EDT):** `/tmp/jpa-resume-20261007-113840.log` confirms Micronaut
passes after fixture cleanup; the aggregate still fails at 339/454 branches and two missed
classes. Merged-data analysis is recorded in `/tmp/jpa-coverage-gaps-20261007.txt`; no class-ID
mismatches were reported. The remaining missed classes are `RegistryTenantDataSource` and
`QuarkusRepositoryContextProducer`. Their previously added regressions are absent from the
Surefire execution reports because recent runs resumed at Micronaut rather than rerunning these
earlier modules.

Added further regressions for async-task lifecycle classifications, retry-budget/scheduling
boundaries, resource-type derivation, missing downstream counts, null payloads, lease ownership
and expiry, partial error/status projections, task-type storage bounds, page immutability and
validation, entity identity, DID reference resolution and tenant-schema validation. These target
the concrete uncovered branches; no production exclusions or coverage limits were changed.
All 14 JPA source/BOM modules compiled production/test sources with the bounded wrapper, tests
skipped, cache enabled: `/tmp/jpa-coverage-gaps-compile-20261007.log`, PASS, 6.582 seconds.
The coverage-only packaging module was excluded from compilation as before. Whitespace checks pass.
These new tests are **written and compiled, not executed**. The next user run must run the complete
JPA `clean install -Pcoverage` with tests enabled and without `-rf`, so the new tests in earlier
modules execute and contribute fresh coverage. Passing 85%/85%/zero missed classes is still unverified.

**Subsequent Micronaut regression failure (11:35 EDT):** the user's Surefire report at
`forwardmeasure-jpa/forwardmeasure-jpa-micronaut/target/surefire-reports/TEST-com.forwardmeasure.jpa.micronaut.MicronautJpaContractTest.xml`
records seven tests, one failure: `executesTheSameRepositoriesAndServicesThroughMicronaut`
failed `Actor type lookup failed`. The newly added independent-transaction regression committed
a HUMAN actor and did not remove it, contaminating the shared fixture for the existing exact-count
contract. This was a defect in the assistant's new test, not evidence of broken transaction semantics.
The test now deletes only its committed actor by UUID in a `finally` block, in a fresh transaction,
and verifies removal. The existing contract assertion and coverage thresholds are unchanged.
Bounded Micronaut plus dependency production/test compilation passed with tests skipped:
`/tmp/jpa-micronaut-isolation-compile-20261007.log`, 8.233 seconds. The cleanup fix has not been
executed; resume JPA at `:forwardmeasure-jpa-micronaut` with coverage and tests enabled. The 85%
coverage gate remains outstanding after this test failure is resolved.

The user requested **85% for both line and branch coverage across the stack**. Previously JPA,
OpenWorkflow and FEI each declared 85% lines, 75% branches and zero missed classes locally.
Those thresholds now live in the platform parent at 85%/85%/zero. Product aggregate checks
inherit them; other Java modules receive checks from the shared coverage profile.
`build.sh --run-tests` now enables that profile. The new policy goal `require-coverage-data`
rejects missing/empty execution data when production classes exist, including staged aggregate
classes. Compilation/test-skip invocations do not claim coverage. See `java-build-platform.md`
for aggregate ownership and the Java-only scope.

The user's resumed log `/tmp/forwardmeasure-full-20261007-105559/07-resumed-build-20261007-111536.log`
confirms the Logback fix: Spring and Micronaut each executed five contract tests, all passing.
It then failed JPA's original coverage gate: 337/454 branches (74.22%) and three missed classes.
Read-only analysis of the merged execution data identified `RegistryTenantDataSource`,
`QuarkusRepositoryContextProducer` and `MicronautRepositoryTransactions`. There were no class-ID
mismatches in that analysis. The new 85% branch policy requires at least 386/454 covered branches
for that production snapshot; this is an outstanding test gap, not an accepted pass.

New JPA regression sources cover cached tenant routing across deactivation/reactivation,
credentialed connection borrowing and unwrap restrictions, Quarkus repository-context wiring,
and independent Micronaut transaction commit despite outer rollback. New policy regression
sources cover missing/empty execution data, resource-only modules and explicit skip/aggregate
ownership. These tests are **written; execution remains with the user**. Policy production/test
compilation and local plugin installation passed in `/tmp/coverage-policy-plugin-compile-20261007.log`.
The updated parent was installed locally in `/tmp/coverage-policy-parent-final-20261007.log`.
No images were built or published and no deployment/commit/push was performed by the assistant.

JPA production/test compilation passed across all 14 source/BOM modules (including Quarkus,
Spring and Micronaut), excluding only the packaging-dependent coverage aggregate:
`/tmp/jpa-coverage-regression-compile-final2-20261007.log`, 5.245 seconds. The bounded command used
`-pl '!forwardmeasure-jpa-coverage' -Dmaven.build.cache.enabled=true -DskipTests -DskipITs
-Dmaven.test.skip=false spotless:apply test-compile`. The complete umbrella reactor then passed
`-Pcoverage,full-suite validate` with tests skipped and the cache enabled:
`/tmp/coverage-policy-reactor-validate-20261007.log`, 58.861 seconds. These are compilation/model
checks, not execution of the new coverage gates or regression tests. Shell syntax and whitespace
checks passed. Next runtime measurement must rerun the changed JPA tests, not resume only at the
coverage module using old execution data. The stricter 85% branch target is not yet achieved evidence.

### User-run cold-cache regression build — 2026-10-07

The user ran the cache-enabled full build/tests with logs in
`/tmp/forwardmeasure-full-20261007-105559`. Parent and policy-plugin bootstrap both passed.
The full reactor stopped at `forwardmeasure-jpa-spring`: all five
`SpringJpaContractTest` tests errored during logging initialization. Its Surefire XML records
`logback-classic:1.6.1` alongside `logback-core:1.5.37`, causing `AbstractMethodError` for
`PatternLayoutBase.getDefaultConverterMap()`. The consolidation had centrally pinned Classic
without managing Core at the matching release. This was a consolidation defect.

The platform source now manages both artifacts with one `logback.version` property at 1.6.1,
matching Classic's own parent dependency management. XML parsing and source checks passed;
the corrected build/tests have **not been rerun by the assistant**. The existing Spring JPA
contract suite is the runtime regression gate. Refresh the platform parent/framework BOMs,
then resume the full reactor at `:forwardmeasure-jpa-spring`, retaining Maven build caching,
tests and container-image builds, with publication disabled. Do not empty `.m2` again.
The earlier compilation evidence below predates this POM correction. FEI runtime acceptance
is still pending; the user's reactor stopped before FEI.

The first suggested refresh command (`-f pom.xml -pl <framework BOMs> -am`) also failed:
Maven builds the aggregate platform BOM's model before applying the project selection, and
Object Storage/OpenWorkflow/NLP BOMs were not yet installed in this partial cold repository.
Use individual **nonrecursive** (`-N -f <specific pom.xml>`) `clean install` invocations in
this order: platform parent, core BOM, Quarkus BOM, Spring BOM, Micronaut BOM. These have no
imports of the missing product BOMs. Then use `build.sh --resume-from :forwardmeasure-jpa-spring
--skip-push --run-tests` with `MAVEN_ARGS` including `-DskipITs=false -Pfull-suite`.
The resume uses the complete `reactor.xml` to resolve sibling models from source. The assistant
inspected these imports but has not executed the corrected commands.

### Compilation checkpoint before the user-run regression build

Production and test sources compiled successfully across **14 repositories / 482 reactor modules**.
Tests were written/updated and compiled, **not executed**. No images were built/published, deployments
performed, or changes committed/pushed. These results validate compilation, not runtime behavior.

| Repository (`forwardmeasure-` prefix) | Modules | Production + test compile | Log |
|---|---:|---|---|
| platform | 18 | PASS | `/tmp/version-policy-compile-platform-01.log` |
| testcontainers | 10 | PASS | `/tmp/version-policy-compile-testcontainers-01.log` |
| database-migrations | 5 | PASS | `/tmp/version-policy-compile-database-migrations-01.log` |
| jpa | 14 | PASS | `/tmp/version-policy-compile-jpa-02.log` |
| object-storage | 7 | PASS | `/tmp/version-policy-compile-object-storage-01.log` |
| authzen | 4 | PASS | `/tmp/version-policy-compile-authzen-01.log` |
| entity-matching | 4 | PASS | `/tmp/version-policy-compile-entity-matching-01.log` |
| data-streaming | 25 | PASS | `/tmp/version-policy-compile-data-streaming-01.log` |
| openworkflow | 189 | PASS | `/tmp/version-policy-compile-openworkflow-01.log` |
| decision-engine | 26 | PASS | `/tmp/version-policy-compile-decision-engine-01.log` |
| entity-intelligence | 79 | PASS | `/tmp/version-policy-compile-entity-intelligence-final.log` |
| agent-os | 79 | PASS | `/tmp/version-policy-compile-agent-os-03.log` |
| nlp | 6 | PASS | `/tmp/version-policy-compile-nlp-01.log` |
| platform-operations | 16 | PASS | `/tmp/version-policy-compile-platform-operations-02.log` |

All commands use the bounded wrapper and explicit `-DskipTests -DskipITs -Dmaven.test.skip=false`.
JPA's successful command additionally uses `-pl '!forwardmeasure-jpa-coverage'`: its 14 source/BOM
modules compile, while the coverage-only aggregation requires packaged artifacts (MDEP-98) and is
not a source-compilation gate. No JPA source module was omitted.

Agent OS originally unpacked its specification jar before `package`. Its specification module now
attaches an `early` resource jar at `process-resources`, and code generators unpack that classifier,
matching the existing FEI approach. This enables its complete 79-module `test-compile` without
executing tests. Platform Operations retains its own 1.0.0 release: explicit local-group coordinates
in its managed reactor dependencies prevent the shared 1.1.0 catalogue entry shadowing them.

Final policy production/test compile and local plugin installation:
`/tmp/version-policy-bootstrap-09.log`, PASS. This includes regression sources for dynamically added
ownership, inactive profiles, exact exceptions/blank reasons/wildcards, forbidden first-party
overrides, BOM precedence, exclusions, classifiers, local exception scope and product release identity.
Final parent installation with policy enabled: `/tmp/version-policy-parent-final.log`, PASS.
The policy implementation reads the current parent model, so adding the final GCS dependency pin
required no plugin-code rebuild. FEI's final 79-module compile includes that pin and the final policy.

Policy-only final validation of all 14 reactors is recorded in
`/tmp/version-policy-validation-summary-final.txt`, with per-repository logs
`/tmp/version-policy-validate-forwardmeasure-<name>.log`. This runs Maven's normal `validate` gates
(including configured convergence checks), not tests. The final summary contains zero exit codes for all 14 repositories: PASS.

A bounded dependency-tree check of FEI's simple and Spark ingestion workers also passed:
`/tmp/version-policy-httpcomponents-final.log`. Both resolve `httpcore5` and `httpcore5-h2` at
5.4.3 and `httpclient5` at 5.6.3, with no FEI-local HTTP Core pin. Whitespace checks (`git diff
--check`) pass in all affected repositories.

The initial working POM contents were saved under `/tmp/version-consolidation-originals-20261007`
for comparison; these are not clean checkouts and must not be used to discard the user's earlier
changes. Generated Java was regenerated by normal Maven plugins, never edited directly.

## Image and rollout implications

No umbrella image rebuild is needed to review or compile these changes. FEI's targeted ingestion
image/runbook scope remains in `forwardmeasure-entity-intelligence/docs/ingestion-repair-rollout-2026-10-06.md`.
The platform parent/BOM/plugin are local Maven artifacts, not container images. Build future consumer
images from their selected modules with the new platform artifacts installed first. Dependency
changes outside FEI also need their own runtime acceptance before those products are released;
this sweep has not deployed or certified them at runtime.
