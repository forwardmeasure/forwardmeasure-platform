# Runtime matrix accounting — October 8, 2026

## October 9, 11:39 EDT: FEI/Kafka WorldCheck and configured pipelines passed

Attempt 05 cleared the envelope-size failure but remained waiting without a worker Job.
The engine rejected its computed transition with `Workflow computation transition digest does not match`.
The serialized command and engine diagnosis are retained under `fei-complete-matrix-05/`;
the stalled run was deliberately stopped (exit 143), not counted as an executed passing cell.
The actual transition contained a fractional numeric protocol subscription deadline. Jackson's
in-memory decimal tree and Kafka's deserialized double tree render the same value differently,
so hashing the original tree was not stable across the command wire.

`DurableWireCompatibilityTest#computationDigestSurvivesDecimalProtocolDeadlineAcrossTheCommandWire`
reproduced that exact rejection using the production command serializer
(`fowf-decimal-transition-before-fix-02.log/.xml`, one error, no skips). The first test attempt
used a human actor for a system-only observation and was corrected; it is not defect evidence.
The repair hashes a JSON-round-tripped tree before dispatch, preserving the existing command
reader's representation and retaining digest/tamper verification. Full production/test compilation
passed in `fowf-decimal-transition-full-compile.log` (2m06s). All **21** focused tests passed,
zero failures/errors/skips, in `fowf-decimal-transition-after-fix.log` (44.7s): eleven wire cases,
five Kafka/storage cases, four computation-state cases and one aggregate record-limit case.
Separate XML reports are retained. Source commit `db04fa65` is pushed. The three Kafka engine
images built locally in 40s (`fowf-decimal-transition-local-images.log`), tagged
`decimal-transition-20261009-1`; adapters retain `control-envelope-20261009-1`.
FEI attempt 06 has **passed WorldCheck ingestion/screening and configured-pipeline overrides**
through the real API/Kafka/worker/OpenSearch path. Both Jobs completed. It is now executing
two-tenant full/delta; simple/correlated resolution and State Street follow. This is two of five
scenario families in the first cell, not a completed 9-cell matrix. Logs: `fei-complete-matrix-06/`.
No new matrix axis or unrelated feature is being added.


## October 9, 11:16 EDT: FEI/Kafka control-envelope defect reproduced

Attempt 04 passed fixture startup with real overflow storage but failed before worker dispatch:
`Workflow computation reached another data cutpoint`. Individually small contract/operation
payload fields totaled 49,639 bytes in an inline-only control envelope. The blocking computation
path still used the same 32,768-byte envelope constructor. The default response offload threshold
(1 MB) did not reduce those fields. A focused real-MinIO regression reproduced the failure with
48,158 bytes (`fowf-control-envelope-before-fix.log/.xml`: one executed error, no skips).

The production repair preserves inline routing/security metadata and stores the largest eligible
payload fields until the control envelope fits. Storage maximums, tenant-scoped keys and digest
validation remain enforced. Kafka's protocol adapter now accepts/resolves `protocolOperationReference`
as well as the existing inline field. Full FOWF production/test compilation passed in
`fowf-control-envelope-full-compile.log` (2m12s). All **15** focused tests passed with zero failures/errors/skips in
`fowf-control-envelope-after-fix-02.log`: six real-MinIO storage cases, five computation/storage
cases (including both combined-small and individually-large control payloads), and four adapter
cases. Separate XML reports are retained with `fowf-control-envelope-*-passed.xml` names. The first after-fix
attempt stopped before tests because the formatter selection needed a regex, not a glob.

This is a correction within the existing FEI Kafka cell, not a new matrix axis. The completed FOWF
basic/overflow reports remain evidence for their recorded images. FOWF source commit `75eb3a4a` is pushed to the backup branch. Six Kafka engine/adapter
images built locally in 1m11s (`fowf-control-envelope-local-images.log`), tagged
`control-envelope-20261009-1`. FEI attempt 05 is running with those images; its integrated result
is still pending. Subsequent FDE/FDS runners select the same new Kafka images. For operator rollout, deploy
new Kafka adapters before new engines: new adapters accept old inline envelopes, while old adapters
do not understand the new referenced protocol field. No image has been published or operator
cluster modified by this repair.


## October 9, 11:01 EDT: FEI execution resumed with required overflow storage

The completed FOWF counts remain **9/9 basic and 9/9 overflow (54 scenarios)**. No additional
matrix axes have been added. FEI's first complete cell (Quarkus/Kafka/PostgreSQL) failed before
worker dispatch: the resolved operation was 49,639 bytes, above Kafka's 32,768-byte inline limit,
and the fixture had not configured object storage. Attempt 02 retained the public failure body,
which identified the disabled off-thread computation path; this was not an ingestion-worker crash.

The fixture now uses `forwardmeasure-testcontainers-gcs` on the disposable K3s network, creates a
real emulator bucket over HTTP, and supplies the existing production storage settings to engine
and adapter. Inline/maximum limits remain unchanged. Production/test compilation passed in
`fei-overflow-fixture-compile.log` (3.171s). Attempt 03 was deliberately stopped during setup
after a DNS preflight showed that pods do not inherit Docker aliases. The fixture now registers
its storage alias in the disposable cluster's CoreDNS, preserving existing host entries. That
correction compiled in `fei-overflow-dns-compile.log` (2.896s); attempt 04 is running under
`fei-complete-matrix-04/`. The stopped attempt is not a test pass. Current FEI images (19) and FDS executors (2) already built successfully;
no image rebuild was needed for this fixture change. FEI complete cells remain **0/9 passed** until
an entire five-family cell finishes. FDE's 15 and FDS's 39 configurations have not started.


## October 9, 10:45 EDT: both FOWF runtime matrices complete

**FOWF basic API/recovery: 9/9 passed. FOWF overflow: 9/9 passed, all 54 scenarios.**
The final Micronaut passes were Kafka Streams (345.9s), Pekko/PostgreSQL (97.3s), and
Pekko/Cassandra (218.7s), each with zero failures/errors/skips. The
[complete overflow evidence](fowf-overflow-runtime-evidence-2026-10-09.json) records actual images,
scenario names and SHA-256 hashes for each retained log/XML report. The basic matrix has its
[separate evidence](fowf-basic-runtime-evidence-2026-10-09.json). Earlier failed attempts remain
historical diagnosis, not unfinished matrix cells.

The sequential continuation has advanced automatically. Current FDS Pekko/Kafka executor images
built locally in 12s (`fds-current-executor-images.log`, tag `runtime-validation-20261009-1`).
The selected FEI API/worker images are building for the nine complete public-API cells; the
standalone FDS Spark image is not rebuilt because these cells use FEI's own Spark worker.
No FEI complete-cell execution pass is claimed by this image build. The continuation then runs
FEI, FDE and FDS matrices, stopping for diagnosis on failure. Logs/stage are retained under
`~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.

## October 9, 10:34 EDT: Quarkus and Spring overflow matrices complete

Overflow is **6/9 complete combinations passed**, all six scenarios in every Quarkus and Spring
runtime. The new Spring passes are Kafka Streams (348.6s), Pekko/PostgreSQL (120.8s), and
Pekko/Cassandra (241.5s), each with zero failures/errors/skips. The three Micronaut combinations
are running next. The [evidence JSON](fowf-overflow-runtime-evidence-2026-10-09.json) now contains
six verified reports and 36 successful scenarios. The basic FOWF matrix remains 9/9 passed.

## October 9, 10:22 EDT: all three Quarkus overflow combinations passed

Overflow is **3/9 complete combinations passed**: Quarkus/Kafka Streams (357.5s),
Quarkus/Pekko/PostgreSQL (101.9s), and Quarkus/Pekko/Cassandra (240.3s). Each passed all six
scenarios and has one executed test with zero failures/errors/skips. The remaining Spring and
Micronaut combinations continue in the same sequential runner, beginning with Spring/Kafka.
[Per-combination evidence](fowf-overflow-runtime-evidence-2026-10-09.json) records actual image tags,
scenario names and log/report hashes. The earlier failed attempts remain in the local result history.

## October 9, 10:16 EDT: recovered failure details verified across all three REST hosts

Pekko public attempt 05 passed INLINE and RECOVER. CORRUPT then correctly reached FAILED without
the downstream effect, but the API exposed `error: null`. A STATE_OBSERVED snapshot can report
terminal failure before the FAILED journal fact is projected; the store handled completed snapshot
output but omitted failed snapshot errors. FOWF `e90a5536` repairs that projection symmetry.

Full FOWF production/test compilation passed (`fowf-observed-failure-full-compile.log`, 2m06s).
The new real HTTP regression passed **1/1 on each of Quarkus, Spring and Micronaut**, zero
failures/errors/skips (`fowf-observed-failure-http-regression.log`, separate XML reports retained).
It asserts that the first FAILED response includes the cause and that later fact/replay delivery
preserves the error, version and two-entry history. The fixture uses actual framework APIs,
Keycloak and PostgreSQL; the separate packaged overflow test supplies the real workflow engine.

The three execution-management API images built locally in 34s, tagged
`observed-failure-20261009-1`. Attempt 06 is running with those images and the repaired Pekko
engines. Overflow remains **1/9 complete cells passed** until a full six-scenario invocation passes;
the previous Quarkus/Kafka pass is retained. No operator cluster or published image was changed.

## October 9, 10:05 EDT: interrupted-read defect reproduced and repaired

The new focused regression failed against the previous engine with the recovered execution stuck
in WAITING (`pekko-interrupted-read-before-fix.log`: one failure, one fail-fast skip). FOWF
`b755aac2` reissues pending object-storage reads from the journaled reference and resolution ID
when recovery completes. It uses the existing digest-checking transport and completion path.

Full FOWF production/test compilation passed (`pekko-interrupted-read-fixed-full-compile.log`,
2m06s). All **eight** real PostgreSQL/Cassandra journal/storage regressions then passed, with zero
failures/errors/skips (`pekko-interrupted-read-after-fix.log`, 50.2s test time). The three local
Pekko engine images built successfully in 49.6s with tag `pekko-read-recovery-20261009-1`.
Adapters retain `pekko-recovery-20261009-1`; no image was published. The source checkpoint is
committed and pushed to the existing backup branch.

FDS `d13dffd` contains the revised public-API read barrier and twelve explicit-image bounded
fixtures. Production/test compilation passed for the shared fixture and all three launcher leaves
(`fds-overflow-read-barrier-compile.log`, 12.7s). Attempt 05 started at 10:06 EDT, selecting the eight
outstanding cells and retaining the completed Quarkus/Kafka pass. Public-API proof of the
new Pekko recovery hook is still required; the focused eight-case pass is not a matrix pass.

## October 9, 09:55 EDT: first overflow cell passed; Pekko read boundary corrected

Quarkus/Kafka Streams passed all six overflow scenarios in 357.5 seconds, with one test,
zero failures/errors/skips (`fowf-overflow-quarkus-kafka-streams-postgresql-04.log` and retained XML).
Overflow is **1/9 complete cells passed**; the basic FOWF matrix remains **9/9 passed**.

Quarkus/Pekko/PostgreSQL attempt 04 passed inline and restart recovery, then failed the corruption
assertion because the workflow completed. Source inspection explains the fixture error: Pekko
resolves and journals the response before a subsequent wait task, so mutating that old artifact
at the timer does not exercise integrity validation. The revised fixture gates actual GCS media
reads before stopping the engine, then mutates/deletes the real object before restarting it.
The actual capture, download, digest check and downstream HTTP effect remain production behavior.

That boundary also exposes a missing recovery hook: a journaled `DataResolutionStarted` has no
in-flight future after restart, and recovery currently resumes captures/running workflows but not
pending reads. Focused PostgreSQL and Cassandra regressions are written; full production/test
compilation is running. No repaired Pekko overflow result is claimed yet. This is work within the
existing restart/integrity acceptance requirement, not an additional matrix axis.

Twelve FDS bounded workflow fixtures also now require the explicit current Pekko executor image
instead of silently using the old `1.1.0` tag. Those edits are written and await compilation; the
running FOWF compilation owns the bounded-build lock. Direct/continuous fixtures already accept
the explicit image selection. No FDS matrix pass is inferred from this fixture correction.

## October 9, 09:41 EDT: overflow run resumed after a fixture-name rejection

Attempt 03 ended at **02:10 EDT**. No tests were still running when checked at 09:39 EDT.
It passed INLINE, RECOVER, CORRUPT, MISSING and MAXIMUM in Quarkus/Kafka, then the production
workflow-definition API correctly rejected the sixth scenario's generated name (underscores and
excess length violate the published DSL contract). This is a fixture failure, not a passed cell.

FDS `7969ddc` derives the workflow name from a stable UUID while retaining the full marker in the
payload. Production/test compilation passed (`fowf-overflow-name-compile.log`, 2.7s). Attempt 04
is now running in `fowf-overflow-quarkus-kafka-streams-postgresql-04.log`; its sequential runner
continues to the other eight runtime cells only after all six scenarios pass. The basic FOWF
matrix remains **9/9 passed**. The earlier full WorldCheck export remains passed; the complete
FEI/FDS/FDE matrices, final coverage and recorded API projection work are not complete.


## October 9, 02:04 EDT: three overflow scenarios passed; emulator deletion check repaired

Quarkus/Kafka attempt 02 passed INLINE, RECOVER and CORRUPT. Recovery executed a real engine
stop/start after persisted history; corruption changed an object without changing its length and
correctly produced FAILED without the downstream effect. MISSING stopped during fault setup because
the emulator returned HTTP 200 for a successful delete while the fixture demanded 204.

The fixture accepts the two successful deletion statuses and independently requires a subsequent
GET to return 404, for both object and bucket deletion. Production/test compilation passed
(`fowf-overflow-gcs-delete-compile.log`, 2s); attempt 03 is running. No complete overflow cell has yet
passed. All nine basic API/recovery cells remain passed. The public timer-detail projection gap
below remains recorded separately.


## October 9, 01:58 EDT: overflow fixture barrier corrected; API timer-detail gap recorded

The first Quarkus/Kafka overflow attempt passed INLINE but failed RECOVER **before restart**:
`awaitDurableWait` assumed the public `timers` collection was populated. Inspection shows
`JpaWorkflowExecutionProjectionStore` initializes that collection empty and does not update it.
The workflow completed after its real timer while the fixture kept polling the empty collection.
This is not successful restart/overflow recovery evidence.

The fixture now synchronizes through persisted public history: Kafka's `TIMER_SCHEDULED` entry for
`restoreWindow`, or Pekko's `STATE_OBSERVED` WAITING entry containing the received marker. The earlier
HTTP-operation WAITING state is insufficient. Compile and rerun are required. Separately, populating
public timer/effect details remains an API projection gap; this fixture change does not repair it.


## October 9, 01:53 EDT: basic matrix complete on the repaired runtime images

All three changed-image confirmations passed: Quarkus/Pekko/PostgreSQL (110.6s),
Quarkus/Pekko/Cassandra (200.5s), and Spring/Pekko/PostgreSQL (125.9s). The basic FOWF matrix is
**9/9 passed** with no outstanding image rechecks. The retained Quarkus/Kafka invocation is explicitly
identified as a passing method from a subsequently failed multi-runtime run; it is not described as a
passing whole run. [Per-runtime evidence with source log hashes](fowf-basic-runtime-evidence-2026-10-09.json)
records the result of each combination.

Overflow execution has begun with Quarkus/Kafka Streams. It remains a separate nine-deployment,
54-scenario requirement. Each scenario now logs its start and successful completion; the runner
records a cell pass only after all six scenarios pass. Production and test sources compiled with
this logging change before execution. Broader FDE/FDS/FEI matrices and final coverage are still pending.


## October 9, 01:45 EDT: all nine basic FOWF combinations have passing runs

Micronaut/Pekko/Cassandra passed 1/1 with no failures/errors/skips in 3m20s
(`fowf-basic-micronaut-pekko-cassandra-03.log`). Every framework/engine/backend combination now has
passing public publication/admission, real adapter effects, restart recovery, persisted output/history,
idempotency, authentication/authorization and tenant-isolation evidence. Three earlier Pekko passes
are being rechecked with `pekko-relational-config-20261009-1`: Quarkus/PostgreSQL, Quarkus/Cassandra,
and Spring/PostgreSQL. This is a changed-image confirmation, not three missing fixture implementations.
The same sequential runner proceeds into the nine overflow deployments after those confirmations.
No overflow, FDE, FDS or FEI complete-matrix pass is inferred from the basic FOWF result.

FDE matrix fixtures now support exact selection (`fowf.acceptance.runtime` and
`decision.engine.acceptance.deployment`) while defaulting to all combinations and rejecting unknown
selections. Production/test compilation passed (`fde-matrix-selectors-compile.log`, 2s). This allows
failed cells to be retried without discarding completed evidence; FDE execution is still pending.


## October 9, 01:36 EDT: Cassandra repair passed; Micronaut Kafka lazy startup found

Spring/Pekko/Cassandra attempt 02 passed 1/1 in 3m03s with the relational repair, including engine
restart and cross-tenant checks (`fowf-basic-spring-pekko-cassandra-02.log`, XML retained beside it).
**Six distinct basic combinations have passed.** Micronaut/Kafka attempt 02 failed: generic HTTP
health returned ready while the lazy Kafka engine bean remained uninitialized; the adapter then
failed because the engine-owned checkpoint topic did not exist. The production repair (`6f304638`) eagerly starts
the Micronaut Kafka runtime, closes it at shutdown, and exposes runtime-backed readiness. Targeted
production/test compilation and the local image build passed in 29s (`micronaut-kafka-startup-image.log`,
image tag `micronaut-kafka-startup-20261009-1`). The packaged rerun passed 1/1 at 01:39 EDT in 2m01s
(`fowf-basic-micronaut-kafka-streams-postgresql-03.log`). Micronaut/Pekko/PostgreSQL then passed 1/1
at 01:41 EDT in 2m13s. Micronaut/Pekko/Cassandra is running: eight distinct combinations have passed. No fixture-created
topics are being added to mask the missing production startup.


## October 9, 01:29 EDT: Cassandra startup repair compiled and packaged

FOWF `fdd62a39` separates relational tenant database configuration from the workflow journal in
all three frameworks. Full production/test compilation passed (`pekko-relational-config-full-compile.log`,
2m07s). The three affected engine images built locally (`pekko-relational-config-local-images.log`,
48s), tagged `pekko-relational-config-20261009-1`. Adapters retain `pekko-recovery-20261009-1`.
FDS `d95f60f` supplies Kafka to the Pekko human-task producer and detects dead cluster members;
production/test compile and shared fixture install passed (`fowf-pekko-bootstrap-fixture-compile.log`).
Spring/Pekko/Cassandra attempt 02 is running, followed by the three Micronaut combinations. Previously
passing Pekko cells must be rechecked with the latest configuration repair before final sign-off.
Source checkpoints are pushed to `backup/full-file-ingestion-20261008`; images remain local.

## October 9, 01:24 EDT: five passes; Cassandra Spring startup defect

Spring/Pekko/PostgreSQL passed 1/1 (`fowf-basic-spring-pekko-postgresql-01.log`), bringing the
basic API/restart matrix to **5/9 passed**. Spring/Pekko/Cassandra attempt 01 exposed a production
configuration defect: `tenantDataSourceRegistry` derives relational tenant URLs from the workflow
journal endpoint, which is `cassandra:9042` for this backend. Spring eagerly constructs the bean and
fails startup. The same coupling exists in all three framework bindings. The repair uses the existing
`forwardmeasure.jpa.tenant-database.*` settings independently of journal configuration. Compilation
and a rerun are required; this failure is not a pass. The shared fixture also supplies its real Kafka
endpoint to Pekko's human-task producer and fails promptly if a cluster member exits during bootstrap.


## October 9, 01:19 EDT: four basic runtime cells passed

The Quarkus/Pekko/Cassandra cell passed 1/1 (`fowf-basic-quarkus-pekko-cassandra-01.log`, 199.5s
in the test). All three Quarkus combinations now pass. Spring/Kafka attempt 01 exposed a production
startup defect: the Kafka-only deployment activated JPA auto-configuration from the shared binding,
whose unused repository types were absent. FOWF commit `11147492` disables database auto-configuration
for that deployment and provides the tenant scope needed by its authenticated HTTP filter.
Production/test compilation and the targeted image build passed (`spring-kafka-bootstrap-image-01.log`,
26s). The local image is `openworkflow-engine-kafka-streams-spring:spring-kafka-bootstrap-20261009-1`.

Spring/Kafka attempt 02 passed 1/1 in 2m02s through real API admission, adapter effects, engine restart,
completion and tenant isolation (`fowf-basic-spring-kafka-streams-postgresql-02.log`). The shared
fixture now waits for application readiness for Kafka engine/adapter services, catching startup
failures before admission. **4/9 basic cells passed**; the remaining five are running sequentially.
No new images were pushed and no operator cluster was changed.

## October 9, 01:07 EDT: packaged Pekko recovery passed

`fowf-public-runtime-matrix-05-q-pekko-pg.log` **PASSED 1/1**, no failures/errors/skips, in 2m18s.
This verifies Quarkus/Pekko/PostgreSQL public admission, real adapter effects, engine restart,
terminal persisted output, history, stable idempotency, wrong-issuer/denied authorization and
cross-tenant isolation with the production role fix and corrected fixture callback URL.
Together with the retained Quarkus/Kafka Streams pass, **2/9 basic FOWF runtime cells have passed**.
The remaining seven basic cells are running sequentially, beginning with Quarkus/Pekko/Cassandra;
`fowf-basic-results.csv` records each result and stops on failure. Overflow and FDE/FDS/FEI matrices
remain separate, unexecuted requirements; this does not mark them passed.

## October 9 runtime execution and recovery defect

`fowf-public-runtime-matrix-03.log` executed the public API/restart matrix: **Quarkus/Kafka Streams
passed; Quarkus/Pekko/PostgreSQL failed; seven cells skipped** after failure. Method results are
retained in `fowf-public-runtime-matrix-03-results.json` beside the log. Attempts 01/02 exposed
fixture header mistakes (missing correlation ID, then changing it during idempotent replay);
these were corrected, along with the required If-Match on cross-tenant cancellation.

The Pekko failure is different: the engine rejoins after restart, but lifecycle projections seek
sharding coordinators on the older adapter member, where those projection types are not initialized.
The public execution remains RUNNING. Engine logs are retained separately in
`fowf-public-runtime-03-pekko-engine.log`. A production fix scopes default sharding and daemon-process
coordinators to the configured runtime role, preserving explicit cross-role workflow proxies.
A focused regression starts the adapter first and requires a real engine-only daemon to start.
The fix is **partially validated**: full FOWF production/test compilation passed at 00:55 EDT
(`pekko-role-full-reactor-compile.log`, 2m06s), and all four bootstrap regressions passed at 00:56
(`pekko-role-bootstrap-regression-01.log`), including the new mixed-role cluster reproduction.
The packaged recovery rerun is still required. Six local Pekko engine/adapter images are building
with tag `pekko-recovery-20261009-1` in `pekko-recovery-local-images-01.log`.
This expands the affected local image set to Pekko engines **and Pekko adapters**, all three frameworks.
The nine overflow endpoint images alone do not contain this subsequent recovery fix.

The six recovery images built successfully at 00:57 EDT (1m18s). Packaged attempt 04 then exposed
an independent fixture endpoint error: Pekko lifecycle events used the default hostname instead of
the test network's real execution API. It failed with the public execution still RUNNING. The
shared fixture now explicitly configures that callback; compile/install passed in
`fowf-public-runtime-client-compile-04.log`. Targeted attempt 05 is running with both corrections.
FEI per-component/framework image selection and the final FDE deployment image selection also
compiled (`fei-current-image-selection-compile.log`, `fde-deployment-image-selection-compile.log`).

## October 9 fixture completion and runtime preparation

The previously missing matrix fixtures are now written and compile. **No new matrix execution
passes are claimed by this checkpoint.** Historical results below remain separate evidence.
The old 27-case count is superseded: count deployment cells and scenario families separately.

| Suite | Current written scope | Compilation | Runtime evidence still needed |
| --- | --- | --- | --- |
| FEI public API | Nine disposable deployments: three frameworks × Kafka Streams, Pekko/PostgreSQL, Pekko/Cassandra. Each complete-cell entrypoint runs WorldCheck ingestion/screening, configured pipelines, two-tenant full/delta lifecycle, simple/correlated resolution, and State Street central-contract ingestion | `fixture-writing-fei-runtime-compile-02.log`, October 9 00:34 EDT | All nine **complete** cells; historical partial Quarkus/Pekko/PostgreSQL passes are retained below |
| FOWF API/recovery | Nine real framework/engine/backend deployments; publication, admission, HTTP effect, authorization/tenancy, idempotency and restart | Shared fixture and three FDS leaves compiled | Nine cells |
| FOWF overflow | Nine deployments, each with six scenarios: inline, stored response recovery, same-length corruption, missing object, maximum response rejection, real storage write failure | `fixture-writing-fds-overflow-compile-03.log`; FOWF full production/test reactor `fixture-writing-fowf-endpoint-full-compile-02.log` | Nine invocations / 54 scenario executions; neither number is a build count |
| FDS direct | Twelve real-worker classes across all three frameworks, two providers and two executors; no curl substitute | Three launcher leaves compiled | Current worker execution for each class |
| FDS bounded/continuous | Twelve bounded classes and six continuous classes; continuous cases assert actual two-wave OpenSearch data. Six Pekko bounded and three Pekko continuous classes also run with Cassandra selected | Three launcher leaves and shared fixture compiled | These configurations remain unverified; class counts are not method counts |
| FDE workflow adapter | Nine real FOWF deployments invoking the production FDE gRPC workflow; allowed, denied and cross-tenant requests; state retained after FDE restart | `fixture-writing-fde-deployment-compile-03.log`, October 9 00:38 EDT | Nine cases |
| FDE deployment | Six actual product Helmfile deployments: three frameworks × embedded/external Valkey; authenticated evaluation and retained facts after pod replacement | Same FDE compile log | Six cases |

Shared infrastructure now includes a caller-owned GCS emulator container in
`forwardmeasure-testcontainers-gcs` and Docker-network attachment for the shared Kubernetes fixture.
The emulator exercises actual storage HTTP operations; it does not establish Google IAM behavior.
FDE and FOWF acceptance reuse `PublicWorkflowAcceptanceClient` and `RealFowfWorkflowFixture`;
no duplicated in-process resource/service implementation stands in for their public APIs.

FOWF adds optional `openworkflow.operations.protocol.storage.endpoint` configuration. Absent the
setting, provider endpoint behavior is unchanged. This allows deployed engines/adapters to use the
real storage emulator. New local images are required for engine-kafka-streams, engine-pekko and
operation-adapter-kafka-streams, each across Quarkus/Spring/Micronaut: nine affected images.
The selected-module bounded package/image build passed at October 9 00:41 EDT with tag
`overflow-endpoint-20261009-1` (`overflow-runtime-local-images-02.log`, 1m38s). All nine images
exist locally. Attempt 01 failed on an empty-registry image name; corrected to `docker.io`.
No images were published and no operator deployment was changed. The final shared-fixture and
all-three-launcher compilation passed at 00:42 EDT (`fixture-writing-fds-final-compile.log`).

FEI runner: `scripts/run-public-ingestion-matrix.sh`, with `FEI_IMAGE_TAG` and `FOWF_IMAGE_TAG` set
to current local images. It provisions one isolated cell at a time, stops at failure and writes
per-cell logs plus CSV. Compilation and shell syntax validation passed; the script has not run.
FOWF shared fixtures accept per-component/framework image overrides such as
`-Dopenworkflow.acceptance.execution-management.quarkus.image=forwardmeasure/openworkflow-execution-management-quarkus:<tag>`
to use current fixes without rebuilding unaffected images. Runtime logs record actual selected images.

## Historical fixture-first checkpoint — October 8 evening

The user reaffirmed the required order: finish **all fixture implementation first**, then execute
remaining matrix runs. Do not restart scenario runs simply because an individual fixture becomes
ready. Compilation is separate and may continue. The tables below retain historical accounting;
they are not a claim that all their fixtures are written.

| Work | Written | Compiled | Executed |
| --- | --- | --- | --- |
| FEI configured pipeline parsing, replay/conflict, owned status/index | Yes | Yes | Passed 1/1 at 23:06 EDT, before the fixture-first correction |
| FEI State Street central-contract person/organization ingestion, exact raw/typed assertions | Yes; shared matrix plus disposable two-tenant entrypoint | Yes, `fixture-writing-fei-compile-03.log` | No |
| FEI two-tenant lifecycle provisioning | Written for disposable Quarkus/Pekko/PostgreSQL | Yes, `fixture-writing-fei-compile-03.log` | No |
| FEI all nine self-provisioned runtime deployments | Incomplete | No complete harness | No complete matrix |
| FEI correlated resolution configuration and real delivery fixture | Written: test-only pipeline configuration, REST source-profile creation, real Spark/Kafka/resolution delivery, two-tenant assertions | Yes, `fixture-writing-fei-compile-03.log` | No |
| FDS twelve direct fixtures requiring real current workers, curl fallback removed | Yes, all three frameworks | Yes, bounded compile logs 01–03 | Not rerun |
| FDS full-framework selection, optional Cassandra, six continuous real-data fixtures | Written; backend selector and real worker image prerequisites explicit | Yes, all three leaves plus shared fixture (`fixture-writing-fds-compile-03.log`) | Not rerun; no matrix pass claimed |
| FOWF nine-runtime public API, HTTP adapter effect and restart recovery | Written; includes real tenant provisioning and isolation checks | Yes, including tenancy extension, `fixture-writing-fds-compile-05.log` | No |
| FOWF deployed overflow/fault matrix | Incomplete: packaged GCS storage lacks an emulator endpoint configuration | No complete harness | Existing component evidence only |
| FDE adapter-to-gRPC and embedded/external Valkey deployment fixtures | Incomplete | No complete harness | Existing framework evidence only |

State Street adds a fourth parameterized FEI scenario; do not continue quoting the old 27-case
inventory as the final scope. Its checked-in 42-column person/organization inputs are synthetic,
and it consumes the central State Street source/mapping contracts. No customer export is copied.
FDS direct fixtures now require `fds.acceptance.pekko.image` / `fds.acceptance.kafka-streams.image`
and import the selected local image through the shared Kubernetes fixture with a real digest.
They no longer switch to writing a fabricated OpenSearch document when registry credentials are
missing. This removes false acceptance, but does not itself establish passing runtime evidence.
The FDS continuous fixtures now dispatch an actual executor and produce two waves only after
Deployment readiness, checking transformed fields, a later update and exactly two stable document
IDs. The shared runtime selects the framework for all four FOWF services and uses real Cassandra
through the shared container plus the production migration image when
`-Dfds.acceptance.pekko.persistence=CASSANDRA` is selected. PostgreSQL remains the default.
These additions have compile evidence only. The all-combination execution inventory still needs
to be reconciled with these selectors; writing a selector is not executing a matrix cell.

Persistent logs: `~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.

This audit corrects the statement “27 runs plus outstanding FOWF/FDS/FDE matrices.” That
statement combined scenario invocations, test classes, deployment configurations and unfinished
test infrastructure. They are different units and must not be summed into a remaining-run total.
No builds or tests were executed for the initial audit; subsequent execution is recorded below. Source inspection and retained execution evidence
are distinguished below; absence of a local report does not prove a test has never run.

## Subsequent full-file repair (separate from the matrix)

See [full-file implementation and live evidence](../../forwardmeasure-entity-intelligence/docs/full-file-ingestion-repair-2026-10-08.md).
October 8 run 05 **PASSED** at 19:01 EDT: the entire **5,611,953,182-byte / 5,818,856-record**
external WorldCheck export went through the Quarkus API, real Pekko/PostgreSQL FOWF and actual worker.
READY, zero rejected, exact OpenSearch count, finalized index metadata, raw samples and canonical
indexed queries all passed. Source SHA-256 matches the independent full-file scan. Admission to READY
was **14m08s**; sampled worker working set peaked at **~555 MiB**, kernel process VmHWM at **~614 MiB**
under a 1 GiB heap / 2 GiB Job memory. This is a separate full-file scale acceptance, not completion
of the activation/screening scenario or nine-runtime matrix below. Run 03's bad name-order assertion
and run 04's editor-crash interruption remain recorded and are not counted as passes.
Parsing overrides have since been added to both population APIs, configured pipeline triggers and
Studio. All three API/worker frameworks and Studio compiled; new override execution is deliberately
left to the operator's Studio testing, per their instruction. The inventory below remains historical
matrix accounting and does not describe the separate full-file test fixture.

## Evening continuation after operator build/push

Runbook correction `15cddaa` is pushed to FEI's backup branch. The operator reports publishing the
new images and is independently installing the dev deployment. Against current sources, the bounded
contract/Spark run passed **11/11** (9 contract cases and 2 real Spark/Kafka cases), and Studio
serialization passed **3/3**. This is newly executed component evidence; the per-request override
HTTP/browser behavior and remaining runtime cells are not claimed passed. Logs and method-level
summary are under `~/.local/state/forwardmeasure/validation/20261008-post-rollout/`.
The Quarkus/Pekko/PostgreSQL public WorldCheck ingestion/activation/screening refresh **PASSED 1/1**
at 19:38:15 EDT, 3m57s, against `full-file-ingestion-20261008-1` in disposable K3s. Raw fields,
selected canonical values, explicit vendor indexing, finalized ownership, activation and positive/
negative screening all passed. The disposable cluster was removed. This closes the refresh for the
first scenario in that runtime cell; it is not full/delta/resolution or other framework parity.
[Current image/source fingerprints and logs](../../forwardmeasure-entity-intelligence/docs/post-rollout-regression-evidence-2026-10-08.json).

## Delta lifecycle continuation — October 8, 20:22 EDT

A new disposable entrypoint, `IngestionPipelineFowfK3sVerificationTest#fullDeltaRollbackAndRejectedRowsUsePublicApi`,
passed **1/1** on Quarkus/Pekko/PostgreSQL in 4m54s. It shares the lifecycle assertions with the
matrix: missing-baseline rejection, full admission/replay/activation, staging, actual delta worker
and authenticated activation callback, wrong-owner/trigger rejection, rollback and preservation
after rejected rows. This entrypoint is single-tenant: the two-tenant isolation assertions in the
matrix still need execution, so this is partial coverage of that cell, not a second complete cell.

Attempt 02 exposed a production delta-status HTTP 500 (lazy relationship read after session closure).
The fix and the same correction in configured-pipeline status compiled across all three frameworks.
Attempt 03 passed using local `entity-intelligence-ingestion-service-quarkus:delta-status-repair-20261008-1`.
Configured-pipeline status subsequently passed its public-API regression at 23:06 EDT (see checkpoint above). Only the selected ingestion API needs
an additional published image; no user deployment or image push was performed by these tests.
[Source/image/log evidence](../../forwardmeasure-entity-intelligence/docs/delta-lifecycle-regression-evidence-2026-10-08.json).

## Historical FEI audit: original 27 parameterized invocations

Source: [ReferencePopulationPublicApiAcceptanceIT](../../forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-ingestion-k3s-verification/src/test/java/com/forwardmeasure/entityintelligence/ingestion/k3s/ReferencePopulationPublicApiAcceptanceIT.java).
Production and test compilation passed in `fei-provider-semantics-all-frameworks-compile.log`.

| Deployment | WorldCheck public ingestion/indexing/screening | Full/delta lifecycle | Simple + correlated resolution |
| --- | --- | --- | --- |
| Quarkus / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Quarkus / Pekko / PostgreSQL | Shared-helper scenario refreshed and passed 19:38 EDT on current images | Single-tenant lifecycle passed 20:22 EDT; two-tenant isolation pending | No retained passing acceptance evidence |
| Quarkus / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Pekko / PostgreSQL | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Spring / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Kafka Streams | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Pekko / PostgreSQL | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |
| Micronaut / Pekko / Cassandra | No retained passing acceptance evidence | No retained passing acceptance evidence | No retained passing acceptance evidence |

Exact methods and assertions:

- `premiumWorldCheckFileSchemaMappingAndIndexAreInvokedThroughThePublicApi`: real hosted
  three-record CSV/schema/mapping/index inputs, public admission/status, worker completion,
  owned OpenSearch documents and raw fields, activation and positive/negative screening.
- `fullDeltaRollbackIsolationAndRejectedRows`: tenant isolation, missing-baseline rejection,
  full revisions and distinct indices, activation, staged/applied delta, rollback, rejected-row
  failure and preservation of the active baseline.
- `simpleAndCorrelatedResolutionUseRealApiAndRemainTenantScoped`: source-feed profile creation,
  simple resolution admission, correlated pipeline trigger, workflow completion, resulting entity
  and assertions, tenant isolation. This uses generic resolution fixtures, not the State Street
  provider file. Provider-specific State Street public-API verification still needs wiring.

These are three invocations on each of nine deployments, not 27 separate full builds. The runner
requires endpoints, fixture hosting, tenant tokens and a preprovisioned correlated pipeline; it does
not yet provision all nine stacks itself. The prior WorldCheck pass is
`fei-worldcheck-public-k3s-05.log`, one case through `IngestionPipelineFowfK3sVerificationTest`.
Thus 26 complete invocations lack retained passing evidence; one of these now has passing
single-tenant lifecycle coverage but still lacks its two-tenant checks. The WorldCheck shared-helper
pass has been refreshed on current images (see evening continuation).
This is not evidence that 26 executions failed.

The new provider semantics have separate passing evidence: 23 focused FEI tests plus two matching
decoder tests. These include real OpenSearch and PostgreSQL. The external ten-record WorldCheck
sample was checked for mapping/validation, not public-API ingestion. Neither it nor the 5.6 GB
export is the existing public-API matrix fixture.

## Historical FDS audit: 30 named matrix classes and original fixture gaps

All classes are under `forwardmeasure-data-streaming-deployments/launcher/{quarkus,spring,micronaut}/src/test/java`.
The following ten class patterns each exist for `Quarkus`, `Spring` and `Micronaut`:

| Class pattern (`F` is the framework name) | Cases/configurations represented |
| --- | --- |
| `DirectIngestionMatrixFPekkoSmokeTest` | Direct WorldCheck / Pekko |
| `DirectIngestionMatrixFKafkaStreamsSmokeTest` | Direct WorldCheck / Kafka Streams |
| `DirectIngestionMatrixFPekkoTestCustomerMasterSmokeTest` | Direct customer master / Pekko |
| `DirectIngestionMatrixFKafkaStreamsTestCustomerMasterSmokeTest` | Direct customer master / Kafka Streams |
| `WorkflowBoundedMatrixFPekkoSmokeTest` | Workflow-managed WorldCheck / Pekko |
| `WorkflowBoundedMatrixFKafkaStreamsSmokeTest` | Workflow-managed WorldCheck / Kafka Streams |
| `WorkflowBoundedMatrixFPekkoTestCustomerMasterSmokeTest` | Workflow-managed customer master / Pekko |
| `WorkflowBoundedMatrixFKafkaStreamsTestCustomerMasterSmokeTest` | Workflow-managed customer master / Kafka Streams |
| `WorkflowContinuousMatrixFPekkoSmokeTest` | Workflow-managed Deployment readiness / Pekko |
| `WorkflowContinuousMatrixFKafkaStreamsSmokeTest` | Workflow-managed Deployment readiness / Kafka Streams |

This is 12 direct + 12 bounded workflow + 6 continuous-lifecycle classes, not 30 test methods:
some classes contain additional authorization tests. It is an inventory, not a verified unrun count.

Historical gaps identified by the initial audit (current repairs are in the checkpoint above):

1. All 12 direct classes contained a curl-container fallback. A stand-in can write a constant document
   without running the ingestion parser/mapping. Acceptance must require the real current worker.
2. The bounded workflow tests use `RealFowfWorkflowFixture`, whose engine/definition/adapter images
   are Quarkus and whose Pekko setup uses PostgreSQL. A Spring/Micronaut launcher does not prove
   a Spring/Micronaut FOWF stack. An execution-service framework overload does not close this gap.
   Cassandra is absent from this matrix. Extend actual runtime selection before claiming parity.
3. All six continuous classes use busybox and prove Kubernetes Deployment readiness, not actual
   continuous FDS data processing. Retain that useful lifecycle coverage and add real data assertions.
4. The two currently retained matrix XML reports are the Quarkus and Spring Kafka Streams classes,
   each selecting only `rejectsAGenuineTokenFromAnUnexpectedIssuer`. Both pass, but neither is a
   passing ingestion dispatch. Older two-Job simple/correlated and executor component passes remain
   valid separate evidence; they do not establish these launcher combinations.

## Historical FOWF audit: nine deployed runtime configurations

Use the same nine framework/engine/backend combinations listed for FEI. This is a deployment
coverage requirement, not nine fully implemented tests waiting in a queue. The missing integrated
proof is real definition publication and execution admission followed by actual selected-engine
execution, lifecycle/history/idempotency, authorization/tenancy and adapter side effects.

Overflow acceptance on those deployments must prove actual object-storage offload/materialization,
terminal persisted results, oversized-input rejection, storage failure and digest mismatch. Durable
restart/recovery must retain execution/artifact identity on Kafka, Pekko/PostgreSQL and Pekko/Cassandra.
Exhaustive algorithm boundary inputs stay in component tests; they need not all be duplicated nine times.

Already passed, and not invalidated by provider mapping changes:

- 345 execution/event HTTP cases across the three frameworks (`fowf-document-identity-http-suite.log`).
  These prove API behavior; they do not all run a real workflow engine.
- Kafka overflow/integrity 18, Pekko PostgreSQL/Cassandra overflow recovery 4, and shared Kafka
  restoration evidence recorded in the repair handover. They are component/runtime evidence,
  not a nine-deployment public-API acceptance result.
- Real Quarkus/Pekko/PostgreSQL FOWF dispatch through the FEI K3s cases.

The FEI repair handover records that the third combined Kafka run passed **94 cases across 18
classes**, zero failures/errors/skips, at 10:42:20 on October 8 (20m51s). It includes the repaired
retry-deadline assertion, all 26 restoration cases and multi-instance rebalance. Its historical log
was `/tmp/fowf-kafka-engine-combined-03-20261008.log`; that temporary file is no longer present at
this evening's audit, so this is retained handover evidence, not a newly reverified log. The earlier
statement that a clean combined rerun was outstanding was stale. The nine-deployment acceptance
harness/scenario work remains incomplete; this component-suite pass does not close those cells.

## Historical FDE audit: packaged framework evidence and two integration gaps

`DecisionEngineContainerConformanceTest.authenticatedFrameworkRoutesEachTenantToItsOwnPhysicalDatabase`
passed for Quarkus, Spring and Micronaut with real Keycloak, PostgreSQL and Valkey. The handover
records `fde-authenticated-shared-fixtures-conformance-20261008.log`: three cases, no failures,
errors or skips. This is recorded prior execution evidence; its former `/tmp` log is not newly
recreated by this audit. Do not call the three-framework FDE matrix outstanding.

Remaining requirements are:

1. Real FOWF workflow -> operation adapter -> authenticated FDE gRPC evaluation -> workflow result,
   including resolved credentials, tenant propagation and denied authorization.
2. Real deployment verification for both Helmfile Valkey modes: an embedded release and an externally
   supplied service. These are deployment shapes, not additional FDE runtime engines.

At the original audit these fixtures were unfinished. The October 9 inventory above supersedes
that status: nine adapter cases and six Valkey deployment cases are now written and compiled.

## Separate outstanding work and reporting rule

The full 5.61 GB acquisition/streaming acceptance is complete as recorded above; the old
whole-file-materialization blocker is repaired. Real Studio override/API acceptance, shared API
component deduplication, remaining fault injection, hundreds-of-GB scale execution and final
aggregate coverage gates remain separate work. They must not be hidden in “matrix runs.”

Preserve existing passing evidence unless a relevant change requires refresh. Record each future
execution by scenario, framework, actual engine/backend, image identity, source revision and log.
Reuse deployed fixtures where practical; do not count overlapping FEI/FDS/FOWF observations as
independent new builds or automatically multiply unrelated axes. Use the enumerated October 9
fixture inventory above; keep deployment cells, scenario executions and test-method counts distinct.
