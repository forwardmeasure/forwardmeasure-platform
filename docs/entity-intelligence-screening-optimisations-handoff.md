# Entity Intelligence Screening and Reference-Population Handoff

Date: 2026-08-08
Audience: engineer or coding agent porting the proven Data Fabric changes into
the greenfield `entity-intelligence` project and then admitting that revision
to the `forwardmeasure-platform` release train.

## 1. Purpose and repository boundary

This is the authoritative consolidation of the screening, OpenSearch,
reference-population ingestion, Spark-runtime, and result-retrieval work proven
during the State Street entity-resolution POV.

The repositories have different responsibilities:

- `/home/pn/Documents/code/forwardmeasure/data-fabric` contains the source
  implementation and production evidence described below. Much of the relevant
  work is currently uncommitted in a mixed working tree; never copy the entire
  diff blindly or revert unrelated changes.
- `/home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-intelligence`
  is the new authoritative product implementation. Its architecture is not a
  file-for-file copy of Data Fabric, so port behavioral contracts and tests
  into the correct greenfield modules.
- `/home/pn/Documents/code/forwardmeasure/entity-intelligence` is a rejected,
  throwaway implementation. It may be inspected for algorithms or tests only;
  it is not an implementation or architectural baseline.
- `/home/pn/Documents/code/forwardmeasure/forwardmeasure-platform` is the
  compatibility release train and greenfield deployment umbrella. It must not
  absorb product source code. After the port, it must pin and verify the new
  `entity-intelligence` revision and deploy the corresponding configuration.
- `/home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-matching`
  contains an earlier port of the organization-name normalization work. It is a
  useful comparison, not the greenfield system of record.

At the time this document was written, a scan of the greenfield
`entity-intelligence` tree found none of the exact Data Fabric implementation
symbols such as `SourceEncodingDetector`, `ScreeningResultCursor`,
`plausiblePersonDates`, or `ScalaPayloadNormalizer`. Do not assume these fixes
have already arrived merely because the new project has equivalent high-level
capabilities.

## 2. Status summary

| Area | Data Fabric status | Greenfield action |
|---|---|---|
| Organization/branch name recall | Implemented, tested, production improvement observed | Port behavior and labelled regression cases |
| GLEIF legal-form normalization | Implemented with pinned ELF 1.6 resource, checksum, metadata, overrides, and updater | Package the same immutable resource and controlled update process |
| Fuzzy and asymmetric OpenSearch recall | Implemented across Spark, resolved-entity, and monitored-population builders | Port one normative query builder and benchmark recall/candidate volume |
| Typed-assertion signal extraction | Implemented once and used by Spark and persistence-backed screening | Preserve value-kind extraction; do not hardcode one assertion container key |
| Identifier scheme canonicalization | Implemented for LEI, Singapore BRN/AIC, and US EIN spellings, including existing-index aliases | Port centrally and retain compatibility with already-indexed provider spellings |
| Entity-kind compatibility | Implemented in OpenSearch recall and Java scoring; unknown remains an explicit fallback | Port as a non-negotiable precision invariant |
| Missing-evidence composite normalization | Implemented, but run 7 proved the active-weight denominator unsafe: hits rose from 91,949 to 300,593 | Do **not** port as-is; restore a stable score scale or explicit name-only cap |
| Fifteen diagnosed strong-candidate false negatives | Run 7 recovered 14/15 intended candidates; Fonds remains zero-hit and two prior transliteration hits regressed | Port fixtures plus the new negative/regression cases and verify end to end |
| Implausible person DOB handling | Implemented and unit tested | Port as a scoring invariant |
| Request-scoped scoring policy | Implemented and runtime-stabilized | Preserve greenfield contract/version snapshot semantics |
| Scala/Java durable payload normalization | Implemented and tested | Port at the greenfield durable boundary if equivalent representations occur |
| OpenSearch transient retry | Implemented and tested | Port or provide equivalent resilient client policy |
| Source-encoding discovery and adoption | Implemented, tested, production proven | Port to raw-byte ingestion before Spark decoding |
| WorldCheck schema encoding | Corrected to ISO-8859-1 for this source | Keep source-specific metadata, but retain detection rather than globally forcing it |
| GCS/Hadoop runtime compatibility | Implemented and production proven | Align dependency family and numeric block-size configuration |
| Spark driver/executor compatibility | Implemented and previously production proven | Enforce immutable, identical runtime tuple |
| Population progress reporting | Implemented and tested | Preserve durable, monotonic progress semantics |
| Screening-results list endpoint | Implemented and unit tested | Re-express in the greenfield API/persistence model |
| Task-status HTTP semantics | Corrected: submission is 202; successful status GET is always 200 | Explicitly enforced by greenfield code, OpenAPI, and contract test |
| Streaming export | Implemented and unit tested; production still has a 300-second Knative timeout | Port bounded keyset streaming, then prefer asynchronous object-storage exports for full populations |
| Result traversal database index | Implemented as Liquibase change | Add an equivalent index for the greenfield schema/query order |
| Transactional outbox publisher | Implemented and tested | Preserve direct, topology-independent Kafka publishing or equivalent |
| OpenSearch CPU increase | Implemented in retiring deployment | Size greenfield values from load tests; do not copy blindly |
| Full post-fix screening-quality report | Complete for run 7; precision at 0.65 failed despite successful structural fixes | Use the run-7 findings and revised acceptance gates in section 15 |

## 3. Production proof: corrected WorldCheck ingestion

The final reference-population ingestion task was:

```text
task ID:      c344a12f-7b07-4fc2-88d0-2e4b2bc4b2f4
population:   0f693929-4fde-4842-bb4f-f888ee1336ac
revision:     d49571b3-4833-4a06-9754-505d87b2cbb0 (revision 3)
source:       gs://datafabric-evidence-incoming-data-fabric-397316/worldcheck/premium-worldcheck.csv
completed:    2026-08-08T22:30:34Z (worker finished at 22:30:35Z)
```

Final worker counters:

```text
rowsSeen=5818856
converted=5818856
skipped=0
submitted=5818856
succeeded=5818856
failed=0
batchesSubmitted=45464
batchesFailed=0
```

The worker refreshed the new index and atomically moved the stable population
alias to:

```text
screening-records-0f693929-4fde-4842-bb4f-f888ee1336ac-
tenant-792a6af3-921b-4951-bd19-6c4ac82e701c-
run-c344a12f-7b07-4fc2-88d0-2e4b2bc4b2f4
```

A live `_count` through the alias returned exactly `5,818,856` root records,
with all three shards successful. OpenSearch may report a much larger
`docs.count` for this index because nested names, locations, and identifiers
are Lucene documents; that is not duplicate root-record ingestion.

The known corruption canary, WorldCheck UID `2787497`, previously contained
`Z�rich`. Through the new live alias it now contains:

```json
{
  "city": "Zürich",
  "state_or_province": "Zürich"
}
```

This proves both that decoding was corrected and that the alias points to the
new revision. The final reference-population-ingestion worker image was pushed
and the running pod image ID matched registry digest:

```text
sha256:256e1238aefc78c1a558584f868937bcb399743030c0e0f3228ad73a3b24590d
```

## 4. Encoding discovery must adopt the evidence

### Problem

The WorldCheck schema declared UTF-8, but the delivered file was
ISO-8859-1-compatible. Spark decoded it according to the declaration and
persisted replacement characters such as `Z�rich`.

Rejecting any decoded value that contains U+FFFD is not the solution. A
replacement character may be legitimate source content, and a post-decode
guard cannot recover the original bytes. Likewise, globally forcing UTF-8 or
ISO-8859-1 is wrong because future deliveries can use a different encoding.

### Implemented contract

`SourceEncodingDetector` inspects up to 1 MiB of the original source bytes
through Hadoop `FileSystem` before Spark parses the CSV. It:

1. honors UTF-8/UTF-16/UTF-32 byte-order marks;
2. performs strict UTF-8 validation with malformed and unmappable input set to
   `REPORT`;
3. selects UTF-8 with confidence 100 when non-ASCII content is valid UTF-8;
4. treats an ASCII-only sample as ambiguous and uses configured metadata as the
   tie-breaker;
5. after strict UTF-8 rejection, uses ICU4J charset detection and adopts the
   supported detected charset according to its confidence/fallback rules;
6. uses the configured charset only when the sample is empty/ambiguous,
   detection is unsupported or insufficient, or source inspection fails; and
7. logs configured, detected, selected, confidence, evidence, and sample size.

The selected charset is passed to Spark's CSV reader. There is deliberately no
U+FFFD fail-fast rule.

For the proven WorldCheck delivery, the worker logged:

```text
configured=UTF-8
detected=ISO-8859-1
selected=ISO-8859-1
sampleBytes=1048576
evidence=ICU charset detection after strict UTF-8 rejection
```

The source-specific schema metadata was also corrected from UTF-8 to
ISO-8859-1. Detection remains necessary because metadata can be stale and a
future provider delivery can change encoding.

Data Fabric source references:

```text
entity-intelligence-ingestion-worker-common/.../spark/SourceEncodingDetector.java
entity-intelligence-ingestion-worker-common/.../spark/SparkBatchProducer.java
entity-intelligence-ingestion-worker-common/.../spark/SourceEncodingDetectorTest.java
entity-intelligence-specifications/.../schema-worldcheck-reference-population.yaml
```

Required regression cases are BOM precedence, valid non-ASCII UTF-8, ambiguous
ASCII fallback, invalid configured UTF-8 adopting the detected charset, and raw
byte inspection through a Hadoop filesystem.

## 5. Organization-aware name recall and scoring

### Root cause

OpenSearch candidate generation used a discriminative subject name and chose
`minimum_should_match` solely from the number of subject tokens:

```java
tokenCount >= 4 ? "75%" : tokenCount >= 2 ? "2" : "1"
```

For example:

```text
subject:   Deutsche Bank Aktiengesellschaft (Singapore, SG, Branch)
candidate: DEUTSCHE BANK SECURITIES INC.
```

The parenthetical branch/location qualifier and spelled-out legal structure
inflated the subject to six meaningful-looking tokens, so four were required.
The true candidate shared only `deutsche` and `bank` and was never recalled;
the downstream threshold of `0.65` could not help because the candidate never
reached scoring.

### Implemented behavior

`NameNormalisationUtils.discriminativeName(raw, entityKind)` is now shared by
the single-entity and Spark population query builders.

For `ORGANIZATION` and `TRUST` only, it:

- strips complete parenthetical spans before normalization;
- strips titles, particles, existing organization noise, and a separate
  organization-structure noise set;
- strips branch/office/representative/subsidiary descriptors through the small
  code-owned structural-noise set;
- strips legal forms only as complete trailing phrases through the packaged
  GLEIF ISO 20275 ELF lexicon described below, instead of maintaining an
  ever-growing Java token list;
- folds diacritics for noise-token comparison; and
- preserves a nonblank meaningful fallback.

The structure-noise set is deliberately separate from the universal
discriminative token set. Otherwise a real person such as `Michael Branch`
would lose a surname. Person normalization remains unchanged.

The matcher JAR now packages GLEIF ELF version 1.6, published 2026-02-19. The
source CSV is pinned to SHA-256
`c55edc421e49ce362457f772d6bfa41f5fc63ecaadea74db5735722625506ef4`.
The generated TSV contains 6,821 active legal-form names and abbreviations.
It is loaded from the classpath; there is no network fetch at startup or while
screening. A checked-in Python updater downloads only the explicitly pinned
URL, verifies the checksum, and regenerates the TSV and metadata during a
reviewed maintenance operation.

Legal forms are suffix phrases, not globally disposable words. The matcher
therefore removes them only at the end of an organization/trust name and never
when doing so would erase the whole name. Country-specific lookup is supported;
when customer country is unavailable, the complete pinned list is used. A tiny
reviewed override file contains source-observed variants absent from the GLEIF
abbreviation rows: `ULC`, `Spka Akcyjna`, and `Spka`. Latin folding explicitly
handles characters such as Polish `ł`, which does not decompose under Unicode
NFD, so `spółka` and source ASCII `spolka` resolve consistently.

Entity-kind-aware name variants are also used by ensemble scoring, and the
organization precision gates compare the same organization-aware
discriminative representation. This prevents a candidate recalled by the new
query from being rejected by a scoring gate that still sees the old noisy
name.

The production comparison improved Firco coverage from 276/348 unique alert
entities with hits to 301/348, with no entity flipping from hit to miss. The 25
recovered entities were dominated by the intended branch/location family,
including Deutsche Bank, BBVA, Bank Polska Kasa Opieki, Citibank, BNP Paribas,
UBS, Bank of America, Credit Agricole, HSBC, Barclays, and RBS.

Data Fabric source references:

```text
entity-intelligence-name-matcher/.../scoring/NameNormalisationUtils.java
entity-intelligence-name-matcher/.../scoring/LegalFormLexicon.java
entity-intelligence-name-matcher/.../resources/legal-forms/gleif-elf-v1.6.tsv
entity-intelligence-name-matcher/.../resources/legal-forms/gleif-elf-v1.6.metadata.json
entity-intelligence-name-matcher/.../resources/legal-forms/legal-form-overrides.tsv
entity-intelligence-name-matcher/.../scripts/update-gleif-elf-lexicon.py
entity-intelligence-name-matcher/.../matching/EnsembleNameMatchingService.java
entity-intelligence-name-matcher/.../scoring/ScreeningScoringEngine.java
entity-intelligence-screening-worker-common/.../ResolvedEntityScreeningProcessor.java
entity-intelligence-spark-common/.../OpenSearchScreeningRecallClient.java
```

The comparison implementation in `forwardmeasure-entity-matching` is in
`NameNormalizer` and `OpenSearchRecallQueryFactory` on its `develop` branch.

## 6. Fuzzy recall without uncontrolled noise expansion

Fuzzy matching was added as an additive `should` branch using
`fuzziness: AUTO`, so it can add candidates but cannot remove candidates found
by existing exact, phrase, token, or phonetic branches.

The Spark population path was subsequently tightened:

- fuzzy match the meaningful discriminative name on the standard analyzed
  field;
- use the same token-coverage rule as the discriminative branch; and
- for people without a discriminative variant, use the primary name with a
  minimum coverage of one.

This avoids fuzzy-OR expansion over every raw organization qualifier, which can
fill the candidate limit with weak matches. The Data Fabric single-entity path
still has an earlier raw-primary-name fuzzy clause. The greenfield port should
define one query builder or one normative query specification and enforce
parity across single and population paths; prefer the guarded discriminative
behavior unless labelled evaluation indicates otherwise.

Do not make OpenSearch tuning part of the composite scoring-policy override.
Candidate-recall policy and downstream evidence scoring are separate contracts.
If recall parameters become configurable, version and validate them separately
and persist the effective recall-policy snapshot with the run.

## 7. Fifteen-false-negative correction package

The run-6 audit found 27 screened Firco IDs with zero hits. A brute-force scan
of all 5,818,856 raw World-Check rows separated them into:

| Disposition | IDs | Meaning |
|---|---:|---|
| Strong same-entity candidate, OpenSearch recall failure | 5 | Candidate existed but the mandatory name query excluded it |
| Strong same-entity candidate, Java scoring failure | 10 | Candidate was recalled but did not become a hit |
| No corresponding World-Check record found | 10 | Customer exists in the combined master but this World-Check snapshot supplies no attributable record |
| Related but distinct BNP securities entity | 1 | Not proven to be the same legal entity |
| Ambiguous Michael Roberts identity | 1 | Insufficient DOB/identifier evidence for attribution |

The implementation addresses the 15 technical false negatives. It does not
manufacture hits for the remaining 12 data/adjudication cases.

### 7.1 Shared typed-assertion projection

The Spark mapper previously retained a primary name, one nationality, DOB, and
identifiers found under one hardcoded key. Customer `country`, incorporation
country, address countries, and identifiers stored under provider/domain keys
could disappear before recall and scoring. Persistence-backed screening had a
separate projection with different behavior.

`TypedAssertionSignalExtractor` is now the single projection contract. Both
`ScreeningSubjectMapper` and `ResolvedEntitySignalExtractor` delegate to it.
It extracts:

- canonical/full/legal names, with given-plus-family fallback;
- all DOB/DOI date assertions;
- all nationality/citizenship and country/incorporation-country values;
- every `AddressValue` country regardless of its assertion key; and
- every `IdentifierValue` regardless of its assertion key.

The OpenSearch recall client now boosts every retained nationality, country,
location country, date, and identifier. Java scoring receives the identical
signal collection. This is the Spark-related correction for Barclays,
Commonwealth, and Marco; it belongs in code at the typed-value boundary, not in
a source-specific mapping workaround.

### 7.2 Identifier canonicalization without reindexing

`IdentifierSignal` canonicalizes schemes and values on construction. Controlled
aliases include:

| Provider/source spelling | Canonical scheme |
|---|---|
| `INT-LEI`, `LEI@GLEIF`, `INT_LEI` | `LEI` |
| `AIC`, `SG_BRN` | `SG-BRN` |
| `US_EIN`, `EIN` | `US-TIN` |

Values are upper-cased and stripped of whitespace and hyphens. Java evidence
comparison therefore recognizes Barclays customer `LEI` versus World-Check
`INT-LEI`, and customer `AIC` versus World-Check `SG-BRN`.

Existing World-Check indices still contain provider spellings. Recall queries
therefore issue a `terms` clause containing every known indexed alias rather
than querying only the canonical spelling. This is intentionally backward
compatible: no World-Check reingestion or index rebuild is required.

### 7.3 Controlled long-name recall fallback

The strict discriminative branches remain in place. For names with at least
four discriminative tokens, all three OpenSearch builders now add a low-boost,
non-fuzzy `names.value.standard` branch requiring two tokens. It is an additive
`should` clause and does not lower the strict branch. This recovers documented
long-name/short-alias cases such as Fonds de Compensation and Mahinda
Siriwardana. The downstream precision scorer remains authoritative.

The same query policy is present in:

```text
entity-intelligence-spark-common/.../OpenSearchScreeningRecallClient.java
entity-intelligence-screening-worker-common/.../ResolvedEntityScreeningProcessor.java
entity-intelligence-screening-common/.../MonitoredPopulationRecallQueryBuilder.java
```

The greenfield implementation should express this once, not preserve three
copies. Candidate volume and recall@K must be measured before broadening the
fallback further.

### 7.4 Composite scoring and partial-name semantics

The Data Fabric implementation changed the composite denominator to the sum of
weights for evidence actually comparable for that subject and candidate. Run 7
proved that this semantic is unsafe at the existing threshold and must **not**
be ported unchanged. A name-only organization with raw name score 0.85 changed
from approximately `0.85 × 0.70 = 0.595` in run 6 to
`0.595 / 0.70 = 0.85` in run 7. Total hits rose from 91,949 to 300,593;
54,018 name-only hits landed at exactly 0.85.

The greenfield implementation needs a stable score scale across missing-data
patterns. Use the full configured denominator or an explicit name-only cap and
model “unavailable” separately from “conflicting”; do not renormalize a partial
name into a complete high-confidence score merely because DOB or identifiers
are absent.

For omitted middle names and legal qualifiers, Data Fabric applies a complete
token-containment floor. Run 7 showed that the two-token minimum is not a
sufficient precision guard: PricewaterhouseCoopers Hong Kong Limited matched
records named only HONG KONG at 0.85, and distinct Goldman funds collapsed to
parent or generic clean-energy organizations. Replace the generic floor with
kind-specific rules: given/family-name containment for people and only
GLEIF-suffix/branch differences for organizations. Product and identity terms
such as fund, trust, asset management, portfolio, equity, commodity, index, and
income must not be discarded wholesale.

Positive geography is allowed to corroborate once the name clears the partial
name threshold and has adequate discriminative overlap. Conflicting geography
continues to contribute negative evidence. Implausible DOBs remain unavailable
as specified in section 8.

### 7.5 Entity-kind compatibility is a hard gate

Known person and organization/trust kinds are now incompatible. OpenSearch
adds a `must_not` filter so wrong-kind records do not consume the candidate
limit, and Java repeats the check defensively before scoring. Organization and
trust are treated as one compatible group. `UNKNOWN` is not rejected and
continues through the documented fallback policy.

This directly addresses the run-6 result in which 38,769 hits crossed the
person/organization boundary. The labelled negative control uses Johnson &
Johnson as an organization probe against a person candidate. The required next
run gate is zero known person↔organization hits.

### 7.6 Labelled regression inventory

The five recall-stage IDs are mBank (two customer IDs), Fonds de Compensation,
Brookfield Asset Management ULC, and Kanakarathna Mudiyanselage Mahinda
Siriwardana. Tests cover GLEIF/override normalization, long-name two-token
fallback construction, alias-compatible identifier queries, all geography
signals, and the known-kind OpenSearch exclusion.

The ten Java-stage IDs are Bruce Neil Carnegie Brown, Michail Emad Farah
Samawi (two customer IDs), Hong Kong Exchanges and Clearing, Pirelli Tyre,
Nicola Jane Shaw, Lourens Daniel Erasmus, Barclays Singapore,
Commonwealth Bank New York, and Marco Giovanni Mazzucchelli. Labelled scoring
fixtures cover each distinct name pair, the duplicate Michail customer ID,
identifier aliases, missing-evidence normalization, and incompatible-kind
negative controls.

Component verification completed in Data Fabric:

```text
entity-intelligence-name-matcher: 35 tests, 0 failures
entity-intelligence-spark-common:  96 tests, 0 failures
population-screening-worker compile reactor: BUILD SUCCESS
```

These are component-level proofs. The definitive acceptance test remains a
fresh 219,779-subject production screening run and comparison against the
immutable run-6 export. The new run must recover or explicitly explain all 15
strong candidates while preserving the prior 316 Firco hits.

### 7.7 Deployment and data impact

- Database migration required: **no**.
- World-Check reingestion/index rebuild required: **no**.
- Customer-master reingestion required: **no**, assuming the existing typed
  assertions are present.
- Runtime network dependency on GLEIF: **no**.
- Required action after building and deploying the affected worker images:
  rerun population screening against stable reference population ID
  `0f693929-4fde-4842-bb4f-f888ee1336ac`.

Detailed evidence remains in:

```text
/home/pn/Documents/code/workfusion/implementations/state-street-er-pov/
screening-results-run6-detailed-analysis.md
/home/pn/Documents/code/workfusion/implementations/state-street-er-pov/
screening_results/firco_coverage_run6/remaining-15-reconciliation.csv
```

## 8. Implausible person DOBs are unavailable, not conflicts

Person DOB evidence is now filtered before comparison. A person date is
plausible only when it is:

- not null;
- not in the future in UTC; and
- not earlier than `today - 125 years`.

After filtering both subject and reference date collections:

- if either side has no plausible date, DOB evidence is unavailable;
- no mismatch penalty is applied;
- an optional hard DOB-conflict gate therefore cannot reject the candidate;
- if both sides retain plausible dates, exact/same-year/conflict behavior is
  unchanged; and
- organization incorporation-date behavior is unchanged.

This is intentionally different from silently rewriting a date or treating an
implausible value as corroboration. Tests cover implausible subject and
reference dates independently, ordinary DOB scoring, negative evidence, and
the explicitly enabled hard-conflict gate.

Data Fabric source:

```text
entity-intelligence-name-matcher/.../scoring/ScreeningScoringEngine.java
entity-intelligence-name-matcher/.../scoring/ScreeningScoringEngineStabilizationTest.java
```

The 125-year limit should become a named, validated policy setting in the new
project if compliance users need jurisdiction- or population-specific limits.

## 9. Request-scoped scoring policy and durable execution

Population and single screening accept an optional `scoring_policy_override`
with independently optional name-algorithm weights and person/organization
composite-evidence weights. The known-good request shape is:

```json
{
  "reference_population_id": "<uuid>",
  "trigger_source": "manual",
  "threshold": 0.65,
  "correlation_id": "<uuid>",
  "scoring_policy_override": {
    "name_similarity": {
      "algorithm_weights": {
        "jaro_winkler": 0.28,
        "levenshtein": 0.13,
        "double_metaphone": 0.14,
        "token_sort_ratio": 0.22,
        "token_set_ratio": 0.17,
        "fuzzy_score": 0.06
      }
    },
    "composite_evidence_weights": {
      "person": {
        "name_similarity": 0.70,
        "date_of_birth": 0.15,
        "nationality": 0.06,
        "identifier": 0.09
      },
      "organization": {
        "name_similarity": 0.70,
        "date_of_incorporation": 0.10,
        "country": 0.10,
        "identifier": 0.10
      }
    }
  }
}
```

Required invariants:

- reject unknown fields and invalid, non-finite, out-of-range, all-zero, or
  incorrectly normalized weights;
- let a partial override inherit unspecified runtime defaults;
- keep name-algorithm weights separate from composite-evidence weights;
- use explicit public terms `person` and `organization`;
- serialize durable task fields with stable snake-case names;
- persist the effective policy and `scoring_policy_version` with the run so a
  retry cannot silently use new defaults; and
- make the effective policy observable in progress/final results.

Durable JSON can cross Hibernate, Kafka, and Spark as Java maps, Jackson trees,
JSON strings, or Scala immutable maps. Data Fabric observed a nested
`scala.collection.immutable.Map$Map2` and rejected it as not an object.
`ScalaPayloadNormalizer` recursively canonicalizes maps and iterables before
contract decoding. The greenfield design should establish one canonical
durable boundary and test every representation its stack actually produces.

## 10. OpenSearch client resilience and performance controls

The Spark recall client now retries transient failures only:

```text
maximum attempts: 3
base delay:       200 ms exponential backoff
statuses:         429, 500, 502, 503, 504
also retries:     transport IOException
does not retry:   ordinary 4xx responses
```

Interruption restores the thread interrupt flag. Tests cover a transient 5xx
followed by success and a non-retryable client failure.

The retiring Data Fabric OpenSearch release increased each node from a 1 CPU
request/2 CPU limit to a 2 CPU request/4 CPU limit while retaining 6 GiB memory.
This is evidence that search capacity was constrained, not a universal
greenfield sizing prescription.

The current candidate ceiling was 128 per subject. Do not lower it without a
recall@K curve. Future performance work should measure, rather than assume:

- per-query latency and candidate-count distributions;
- unique contribution and cost of each query branch;
- `_msearch` batching with bounded concurrency/backpressure;
- deduplication by a complete query-affecting signature and index revision;
- purpose-built normalized, token, phonetic, and character-ngram recall fields;
- a compact recall-only index;
- person/organization index separation;
- shard count, routing, replicas, heap, and CPU; and
- top-K sensitivity.

The earlier detailed optimisation brief is:

```text
/home/pn/Documents/code/forwardmeasure/data-fabric/docs/
entity-intelligence-screening-service/opensearch-recall-optimisation-handoff.md
```

## 11. Spark, GCS, and image-runtime corrections

### Driver/executor compatibility

A production run failed because the Spark driver used Spark 4.2.0/Java 25
while executors pulled Spark 4.1.1/Java 21. Spark RDD deserialization failed
with `InvalidClassException` and a `serialVersionUID` mismatch.

Driver and executor must use an identical tuple:

```text
Spark 4.2.0
Scala 2.13
Java 25
compatible Spark/Netty dependency family
```

The executor Docker build verifies the Java version and exact Spark core/SQL
jars, rejects duplicates, and excludes Spark-owned Netty artifacts from the
application shade. The custom image module also bridges the repository-wide
`-Dquarkus.container-image.push=true` property to its own Docker push step.
Prefer immutable digests and verify Kubernetes `imageID`, not merely the
requested mutable tag.

### Google client dependency alignment

Quarkus 3.38.1 and Google Cloud BOM 26.86.0 selected incompatible members of
the GAX/Google HTTP Client family. This produced runtime linkage failures such
as missing `HttpJsonConscryptUtils` and
`NetHttpTransport.Builder.setSecurityProvider(...)`.

The final Data Fabric dependency management aligns:

```text
gax-httpjson:       2.83.0
google-http-client family: 2.2.0
gcs-connector:      4.0.4 shaded
```

All Google HTTP Client modules used by the reactor are pinned to the same
family. Do not copy these exact versions if the greenfield Quarkus/Google BOMs
differ; enforce dependency convergence and test the actual GCS code path.

### Numeric GCS block size

Hadoop 3.5's default may expose `fs.gs.block.size=64m`, but GCS Connector 4.0.4
reads it through `Configuration#getLong`, which requires decimal digits. The
configuration now exposes `hadoop.gcs.block-size-bytes`, defaulting to
`67108864`, and sets:

```text
spark.hadoop.fs.gs.block.size=67108864
```

This was required for the successful production ingestion.

## 12. Population progress must reflect durable work

Spark accumulators are not sufficient for live progress. Executors update them
continuously, but the driver sees merged values only as tasks/partitions
complete. A long-running run therefore reported zero screened while work was
being committed.

The correction derives progress from durable state and persists canonical run
counters:

- terminal child screening rows determine screened/failed counts;
- persisted monitoring hits determine hit count;
- updates occur independently from task-progress reporting so one failure does
  not suppress the other;
- tenant context is explicitly established for the progress write and restored
  afterward;
- API mapping never regresses already persisted counters; and
- accumulators are fallback telemetry, not the source of truth.

The relevant Data Fabric implementation is
`SparkPopulationScreeningTaskExecutionService`, its support/service queries,
`PopulationScreeningRunMapper`, and their tests.

## 13. Screening-result retrieval and export optimisation

The original retrieval path became progressively slower over a 219,779-subject
population because it relied on offset traversal, always counted the entire
result set, materialized large hit/raw payloads, and built exports in memory.

### Task-status HTTP semantics

Keep transport acceptance separate from domain lifecycle state:

```text
POST command/submission accepted:       202 Accepted
GET existing task/run current status:   200 OK
GET unknown task/run:                   404 Not Found
```

A processing task is still a successfully retrieved resource representation;
its `PROCESSING` state belongs in the response body and does not turn the GET
into HTTP 202. Data Fabric's `TasksApiImpl.retrieveAsyncTaskStatus` was fixed in
both the active and duplicate legacy source paths. The greenfield
`PopulationScreeningApiResource.getPopulationScreeningRun` already returned
200; its implementation and OpenAPI now state the rule explicitly, and
`TaskStatusHttpSemanticsContractTest` prevents `202` from being introduced on
the GET operation.

### List endpoint

The optimized endpoint supports stable keyset pagination ordered by:

```text
screening_run.created_at DESC, screening_run.id DESC
```

The surrogate ID is a mandatory tie-breaker when timestamps are equal. The API
accepts `cursor=start` for the first page and returns an opaque, versioned,
URL-safe `paging.next_cursor`. Cursor and nonzero offset cannot be combined.

Optional projection/query controls are:

```text
include_total       defaults true for legacy offset, false for cursor
include_assertions  defaults true
include_hits        defaults true; hit_count remains accurate when false
include_raw_hit     defaults true; ignored when hits are omitted
```

Hit status, reference-population ID, minimum score, and `has_hits` filtering
are pushed into `EXISTS`/batch queries. For each page:

- child runs and resolved subjects are fetched once;
- assertions are batch-loaded only when requested;
- hits are batch-loaded only when requested;
- otherwise grouped database counts preserve accurate `hit_count`; and
- raw hit JSON can be omitted from the API response mapping.

Important implementation limitation: Data Fabric currently loads
`MonitoringScreeningHit` entities for the page. Because `raw_hit` is a basic
JSONB attribute on that entity, Hibernate still selects and deserializes the
column even when `include_raw_hit=false`; the flag only prevents it from being
copied into the response model. A greenfield implementation should use a real
DTO/database projection when raw hits are excluded so the JSONB value never
crosses the database boundary.

The response contract was also corrected to expose one canonical
`resolved_subject_id` at subject-result level and provenance assertions at that
same level. A redundant hit-level resolved-subject identifier was removed.

The supporting PostgreSQL index is created concurrently:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS
    idx_screening_run_population_created_id
ON screening_run
    (population_screening_run_id, created_at DESC, id DESC);
```

Adapt the table and column names to the greenfield model; preserve the query
order and tie-breaker.

### Export endpoint

JSON and CSV export now return a lazy `StreamingOutput`. Both compressed and
uncompressed bodies are written in bounded keyset-paginated pages. Each page is
assembled in a short transaction, avoiding:

- a full-run object graph in API memory;
- a single long database transaction while a client downloads;
- deep offsets; and
- an intermediate full `byte[]`/ZIP buffer.

A production export of 219,779 subjects exposed a separate transport boundary:
the Knative service terminated the single streamed response at its five-minute
timeout, after approximately 53.9 MB, before `ZipOutputStream` could write the
central directory. The downloaded body was therefore not a valid ZIP even
though every database page written before the cutoff was valid. Data Fabric's
screening-service timeout is changed from 300 to 900 seconds in the Data Fabric
Helm source, but that configuration was **not deployed** during this exercise:
the live Knative service still reported `timeoutSeconds: 300`. Do not treat the
source edit as production proof, and do not rely on a longer synchronous HTTP
timeout as the greenfield end state.

For analytical extraction, bounded cursor requests remain preferred because
they are independently retryable and can checkpoint between pages; the POV
`screening_results/export-screening-results.sh` uses that strategy and validates
its final row count. This improves recoverability, not necessarily speed.

### Live retrieval evidence and remaining bottleneck

The cursor endpoint and its composite tenant-schema index were live for run
`722c9f9f-a5f8-41b6-8d6e-08bc9c3e202f`. PostgreSQL contained:

```sql
idx_screening_run_population_created_id
    (population_screening_run_id, created_at DESC, id DESC)
```

The live API logs showed stable, non-degrading keyset traversal. Individual
500-subject requests generally completed in roughly 2--5 seconds. The dominant
remaining cost was payload volume, not deep-offset scanning:

```text
312 cursor requests / 156,000 subjects observed
891,816,359 response bytes (850.5 MiB)
approximately 2.2--4.1 MB per 500-subject page
```

Extrapolating the observed payload gives roughly 1.2 GiB for all 219,779
subjects before any transport compression. The request explicitly included
assertions, hits, match-score breakdowns, and `raw_hit`. Each persisted hit can
therefore repeat a substantial WorldCheck record. Cursor pagination prevents a
single timeout from discarding all progress, but it cannot make that data
volume cheap.

The greenfield improvement should separate interactive listing from bulk
evidence export:

1. keep the interactive list projection small and require explicit expansion;
2. implement database-level DTO projections so excluded JSONB is not loaded;
3. enable and verify HTTP compression for JSON/NDJSON responses;
4. for full-population evidence, create an asynchronous export job that writes
   a compressed artifact to object storage and returns a task plus artifact
   location/checksum when complete; and
5. consider exporting subject results and unique reference-record snapshots as
   separate related datasets, avoiding repeated raw WorldCheck records while
   retaining auditable source IDs and point-in-time provenance.

Any deduplication of raw evidence must preserve the regulatory point-in-time
snapshot contract; do not replace persisted snapshots with a lookup against the
current mutable reference index.

Regression tests prove that the response body is lazy and that cursor summary
mode avoids total-count, assertion, hit, and raw-hit materialization.

Data Fabric source references:

```text
entity-intelligence-screening-api/.../ListScreeningRunSubjectResultsHandler.java
entity-intelligence-screening-api/.../ExportScreeningRunHandler.java
entity-intelligence-screening-api/.../ScreeningResultCursor.java
entity-intelligence-screening-api/.../MonitoringScreeningHitSummaryMapper.java
entity-intelligence-common-domain/.../ScreeningRunRepository.java
entity-intelligence-common-domain/.../MonitoringScreeningHitRepository.java
data-fabric-liquibase-migration/.../entity-intelligence-service-changelog-0002.xml
data-fabric-services-api-spec/entity-intelligence-api.yaml
data-fabric-services-api-spec/common-definitions.yaml
```

## 14. Transactional outbox correction

The old outbox publisher injected a conditionally enabled SmallRye
`MutinyEmitter`. Quarkus fixes the messaging topology at augmentation, so the
runtime failed with `SRMSG00019` when the channel was unavailable.

Screening, ingestion, and extraction services shared the physical database,
tenant schemas, and outbox rows. Any claimant could win a row, so upgrading
only the screening dispatcher was insufficient.

The corrected publisher owns a lazy Kafka producer independent of Reactive
Messaging topology. It uses string serializers, `acks=all`, idempotence,
bounded synchronous acknowledgement, dynamic destination topics, event keys,
JSON payloads, and invocation/outbox headers, and closes at shutdown. All
claimants of a shared outbox must be upgraded as one compatibility set.

See Data Fabric
`docs/entity-intelligence-screening-service/transactional-outbox-reactive-messaging-regression.md`
for the original failure analysis.

## 15. Required post-screening quality report

The run-7 assessment is complete at:

```text
/home/pn/Documents/code/workfusion/implementations/state-street-er-pov/
screening-results-run7-detailed-analysis.md
```

Its immutable export has 219,779 rows, SHA-256
`e1627eea0fda460858858b7dd318bb1a9f300d2fb85e36a53452722dc89433d8`.
Headline results are: zero execution failures, zero known person/organization
cross-kind hits, 14/15 targeted candidates recovered, two same-kind
transliteration regressions, and an unacceptable increase from 91,949 to
300,593 hits. Virtual threshold evaluation proves that threshold alone cannot
separate all known good and bad cases: clear false candidates score above some
intended candidates, and related organization records can score 1.0.

Every subsequent run must repeat the following reproducible assessment. It must
not equate “zero hits” with “platform bug” or “has a hit” with “the same entity
exists in WorldCheck.”

At minimum report:

1. unique Firco alert entities and alert rows;
2. entities linked to the combined customer master and entities absent from it;
3. customer-master-linked entities actually present in the screening output;
4. hits and misses at the requested composite threshold;
5. hit-count and score distributions, including borderline scores;
6. recovered and regressed entities versus every prior comparable run;
7. exact/strong WorldCheck candidates for misses found by independent raw-file
   search, not through OpenSearch;
8. entities absent from the exact WorldCheck extract, confirmed by brute-force
   grep across all raw columns;
9. distinctions between likely true identity candidates and incidental common
   token overlap;
10. duplicate Q1 Firco rows, duplicate Record IDs, duplicate customer-master
    keys, and semantically duplicate screening hits;
11. per-category and per-entity-kind coverage;
12. false-positive review samples and candidate saturation at the top-K limit;
13. evidence that the intended reference revision/index and scoring/recall
    policy were used; and
14. operational completeness: subject count, failures, skips, retries,
    duration, throughput, and missing source references.

Known source-provenance facts that must remain separate:

- five unique Firco Record IDs were absent from the combined customer master in
  the prior analysis;
- mBank was initially misclassified as absent by an overly strict audit
  heuristic; the raw snapshot contains UIDs 1966948 (`MBANK SA`) and 1133062
  (`MBANK`), and run 7 recovered both mBank customer IDs;
- ten customer IDs have no attributable exact entity in the raw snapshot even
  though five of them now receive partial/parent/geographic run-7 hits; and
- the controlled two-token long-name fallback can increase candidate volume,
  so recall@K, saturation, and false-positive volume must be reported rather
  than assumed safe from component tests alone.

Preserve raw inputs, scripts, exact command lines, generated CSV/JSON reports,
hashes, run IDs, reference revision IDs, and timestamps so every conclusion is
auditable.

## 16. Greenfield port and platform admission checklist

### Entity Intelligence implementation

- [ ] Map every section above to the greenfield architecture before editing.
- [ ] Add raw-byte encoding detection and source-specific encoding metadata.
- [ ] Add organization-aware discriminative variants and scoring/query parity.
- [ ] Package the pinned GLEIF ELF 1.6 lexicon, metadata, checksum, overrides,
      and explicit update tool; never fetch it on the screening path.
- [ ] Use one typed-assertion signal extractor for Spark and persistence paths.
- [ ] Canonicalize identifier schemes/values while querying existing indexed
      aliases so the port does not force a reindex.
- [ ] Add guarded fuzzy recall to one normative query implementation.
- [ ] Do not add a generic two-token fallback. Port the controlled,
      entity-kind-specific rules in section 18; Fonds UID 2760894 is recovered
      by exact registration evidence plus the shortened legal name.
- [ ] Preserve a stable composite score scale across missing-evidence patterns;
      do not port Data Fabric's active-weight denominator unchanged.
- [ ] Replace the generic two-token containment floor with kind-specific person
      and organization rules and retain identity-bearing fund/product terms.
- [ ] Enforce known entity-kind incompatibility in recall and scoring while
      preserving an explicit unknown-kind fallback.
- [ ] Treat implausible person DOBs as unavailable evidence.
- [ ] Preserve versioned, durable scoring-policy snapshots.
- [ ] Canonicalize durable payload representations at one boundary.
- [ ] Add bounded transient OpenSearch retry and instrumentation.
- [ ] Enforce identical immutable Spark driver/executor runtime versions.
- [ ] Resolve GAX/Google HTTP/GCS dependencies as one tested family.
- [ ] Set GCS block size as decimal bytes when the connector requires it.
- [ ] Make progress durable, tenant-correct, and monotonic.
- [ ] Implement keyset result traversal, optional projections, and streaming
      export in the greenfield persistence/API model.
- [ ] Add the matching composite database index.
- [ ] Preserve reliable outbox delivery across every claimant.
- [ ] Port all 15 labelled false-negative fixtures plus wrong-kind,
      PWC→HONG-KONG, distinct-fund/parent, and transliteration-regression
      controls for Mnz/MUENZ and BNZIGER/BAENZIGER.
- [ ] Add labelled recall@K, candidate-volume, encoding, retry, progress,
      cursor, export, and policy regression tests.

### ForwardMeasure Platform admission

- [ ] Commit and push the greenfield implementation to
      `forwardmeasure-entity-intelligence:develop`.
- [ ] Publish the immutable Entity Intelligence release with SCM metadata,
      SBOM, and provenance attestation.
- [ ] Select that released Maven version in `forwardmeasure-platform-bom`.
- [ ] Carry required OpenSearch/Spark/GCS settings into the greenfield
      `forwardmeasure-entity-intelligence/deploy` and umbrella Helmfile values;
      do not copy
      retiring Data Fabric paths.
- [ ] Run the full platform `mvn -B clean verify` on Java 25/Maven 3.9.9+.
- [ ] Run `./deploy/validate-platform.sh <environment>` and inspect rendered
      images, immutable references, resources, secrets, and index settings.
- [ ] Before release, verify the selected artifact attestations and immutable
      container references.
- [ ] Execute a representative ingestion plus population screening in the
      greenfield environment and repeat the canary/data-quality checks.

## 17. Suggested prompt for the next coding chat

```text
Port all production-proven screening and reference-population fixes described
in this handoff into the greenfield Entity Intelligence architecture:

/home/pn/Documents/code/forwardmeasure/forwardmeasure-platform/docs/
entity-intelligence-screening-optimisations-handoff.md

Read the entire document first. Validate each behavioral requirement against
the Data Fabric source references and map it to the new architecture; do not
copy the mixed Data Fabric working tree wholesale. Implement the changes and
regression tests in
/home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-intelligence.
Preserve the controlled long-name recall fallback, but benchmark recall@K,
candidate volume, and saturation. Port the pinned GLEIF resource/update
process, shared typed-signal projection, identifier aliases, and hard known-kind
gates. Do not port the active-evidence denominator or generic two-token
containment floor unchanged: run 7 proved both unsafe. Implement a stable score
scale and kind-specific containment, and add the run-7 false-positive and
transliteration regressions. After the component is green, update the
ForwardMeasure Platform source pin and deployment configuration, run component
and umbrella verification, and document any intentional architectural
differences.
```

## 18. 2026-08-09 real-data correction and mandatory regression gate

This section supersedes any earlier wording that recommends active-evidence
normalization, a generic two-token containment floor, or global removal of
organization words such as `fund`, `trust`, `asset`, `management`, `equity`,
`commodity`, `portfolio`, or `income`. Run 7 proved those behaviors unsafe.

### Evidence-backed test corpus

Data Fabric now has a reproducible real-data corpus generated from the immutable
run-6 and run-7 exports and the raw World-Check delivery:

- `entity-intelligence-spark-common/src/test/resources/real-data/screening-regression-expectations.json`
- `entity-intelligence-spark-common/src/test/resources/real-data/screening-regression-fixture.json`
- `entity-intelligence-spark-common/src/test/scripts/extract-real-screening-regression-fixtures.py`

The current fixture has 36 customer cases and 154 real World-Check documents.
Its provenance records the run-6 SHA-256
`1bac0d7a8940c0187f17a24d70f284e6d998a7d6d5b40a79c5e950f5460378fa`
and run-7 SHA-256
`e1627eea0fda460858858b7dd318bb1a9f300d2fb85e36a53452722dc89433d8`.
Test data is intentionally retained: this is evidence, not a synthetic sample.

`RealDataScreeningRegressionTest` starts OpenSearch 3.2 with the phonetic
analysis plugin, creates an index from the production mapping, indexes the real
World-Check documents, constructs real Spark rows from exported assertions,
and executes the production subject mapper, OpenSearch recall client, ensemble
matcher, scoring engine, request override, precision gates, and threshold. It
asserts the exact accepted UID set for every case, not merely “one hit exists.”

The corpus includes the 15 technical cases; Fonds UID 2760894; Stefanie
Mnz/MUENZ UID 6346823; Hugo BNZIGER/BAENZIGER UID 3012977; branch/parent cases;
and negative controls for the Goldman funds, PWC Hong Kong, BNP Tokyo, Michael
Murray Roberts, Edward Jones, Alliance, HSBC Global Asset Management, Henkel
Master Trust, Interactive Broker, and source-absent entities.

### Corrected scoring semantics

The implementation validated by that corpus has these properties:

1. The composite denominator is the complete configured weight sum. Missing
   evidence contributes zero; it does not shrink the denominator and promote a
   weak name-only candidate.
2. Organization identity words remain in the name. Only trailing legal forms
   from the pinned GLEIF lexicon and narrowly scoped structural qualifiers are
   removed.
3. Generic two-token containment is disabled.
4. Person completion uses World-Check's `FAMILY,given names` structure. Family
   and given anchors are evaluated in their roles, middle-name agreement is
   used when present, and a bare first/family completion is allowed only when
   exactly one recalled candidate has that anchor. This rejects the common-name
   `Michael Murray Roberts` expansion while recovering Lourens Erasmus, Nicola
   Shaw, Mahinda Siriwardana, Marco Mazzucchelli, Stefanie Mnz, and Hugo
   BNZIGER.
5. Organization containment is directional. A complete customer legal name
   may occur inside a provider name with at most three leading qualifier tokens.
   The unsafe reverse direction is allowed only with an exact canonical company
   registration identifier.
6. `UK_CRN`, `CRN`, and jurisdictional `*-RCS`/`*-KRS` schemes canonicalize to
   company registration. Exact registration-value recall may omit the provider
   scheme constraint because the customer source mislabels foreign register
   numbers as `UK_CRN`; final Java scoring still requires the canonical type and
   exact normalized value.
7. Branch customers do not collapse indiscriminately to every parent/affiliate.
   An exact external identifier wins and suppresses weaker duplicate parents.
   Otherwise, a parenthetical branch may match its exact parent root, and a
   non-parenthetical branch may match when the provider location city/state is
   explicitly present in the customer name. A branch-country disagreement is
   unavailable evidence rather than a conflict because one side commonly stores
   parent incorporation and the other branch jurisdiction.
8. Implausible person DOBs are unavailable and cannot trigger a conflict.

### Full-export candidate backtest

`FullExportScoringBacktestTest` is an opt-in streaming test over a complete
export. It does not call the deployed service or re-screen. It re-scores every
previously accepted `raw_hit` with current production Java code and writes
`target/screening-backtest-summary.json`. It therefore measures removals and
retention from the old candidate set; it cannot prove recall for candidates
that the old run never exported.

Final run-7 backtest at threshold 0.65:

| Measure | Run 7 | Corrected scorer |
|---|---:|---:|
| Subjects | 219,779 | 219,779 |
| Hit-bearing subjects | 47,207 | 19,010 |
| Hits | 300,593 | 49,091 |
| Firco IDs with a run-7 hit | 335 | 324 |

The 11 Firco IDs reduced to zero are precisely the labelled negative controls:
three absent Goldman funds, PWC Hong Kong, BNP Tokyo, Michael Murray Roberts,
Edward Jones Money Market Fund, Alliance Capital Management, HSBC Global Asset
Management, Henkel of America Master Trust, and Interactive Broker LLC. The
backtest also retained the branch cases that an earlier over-broad branch guard
incorrectly removed. The separate OpenSearch integration gate proves the three
valid run-7 zero-hit recoveries: Fonds, Stefanie, and Hugo.

### Final production-run reconciliation and acceptance target

The complete Q1 Firco benchmark contains 494 alert rows but 348 unique,
nonblank Record IDs. Of those unique IDs, 343 occur exactly once in the
supplied 219,779-row combined customer master and are screenable. Five are
absent from the master and cannot be screened by any population run over that
input.

The corrected production run should therefore reconcile approximately as
follows:

| Final disposition | Unique Firco Record IDs |
|---|---:|
| Valid hits | **331** |
| Valid zero-hit results | **12** |
| Unscreenable customer-master omissions | **5** |
| Unexplained technical misses | **0** |
| **Complete Firco benchmark** | **348** |

This is the exact accountability identity:

```text
348 = 331 valid hits + 12 valid zero-hits + 5 unscreenable omissions
```

The expected valid-hit coverage is 331/343, or 96.50% of screenable Firco
Record IDs. Against the complete 348-ID benchmark it is 95.11%. Run 8 produced
328 hit-bearing Firco IDs, including the newly validated AllianceBernstein /
Alliance Capital record. The post-run exhaustive raw World-Check audit found
three further valid records that run 8 missed: PricewaterhouseCoopers Hong
Kong, Interactive Brokers LLC, and Noura/Nourah Al-Fassam. These three are now
exercised by the real-data OpenSearch-to-Java regression test. The 348-ID
disposition is the hard production acceptance gate.

The 12 valid zero-hit Record IDs are:

| Record ID | Customer | Expected explanation |
|---|---|---|
| `KYC100873AML` | Goldman Sachs Trust - Goldman Sachs Commodity Strategy Fund | Parent/related Goldman records only; exact fund absent |
| `KYC101370AML` | Goldman Sachs Variable Insurance Trust - Goldman Sachs Equity Index Fund | Parent/related Goldman records only; exact fund absent |
| `KYC156135AML` | BNP Paribas Tokyo Branch | Parent or different affiliate only; exact branch not proven |
| `KYC298841AML` | HSBC Global Asset Management Limited | Parent/different-jurisdiction affiliate only |
| `KYC301133AML` | Goldman Sachs Trust - Goldman Sachs Clean Energy Income Fund | Parent Goldman or generic clean-energy records; exact fund absent |
| `KYC449b82ce-e72e-4046-af12-472f83f4d27fAML` | Michael Murray Roberts | Common-name ambiguity without sufficient corroboration |
| `KYC7490AML` | Henkel of America Master Trust | Henkel corporate record is not the named master trust |
| `KYC91892AML` | Edward Jones Money Market Fund | Parent Edward Jones record only; exact fund absent |
| `KYC29846AML` | Vanguard Variable Insurance Funds - Real Estate Index Portfolio | Exact fund, LEI, and TIN absent from the World-Check snapshot |
| `KYC29846AMLZ1` | Vanguard Variable Insurance Funds - Real Estate Index Portfolio | Separate customer ID; same exact fund, LEI, and TIN absence |
| `KYC33774AML` | The Citigroup Pension Plan | Exact plan and EIN absent from the World-Check snapshot |
| `KYC212390AML` | Tom Dowd | Technical miss: World-Check UIDs `1631256` and `5958906` (`DOWD,Thomas`); corrected through exact-family/phonetic-prefix recall and strict short/long given-name scoring |

The five unscreenable customer-master omissions are:

| Record ID | Customer recovered from Firco workbook | Q1 decision | Firco rows | Customer-master rows |
|---|---|---|---:|---:|
| `KYC286155AML` | Arial CNP Assurances | PEP_NoRisk | 2 | 0 |
| `KYC322791AML` | Susan Sullivan | PEP_NoRisk | 1 | 0 |
| `KYC349234AML` | Naomi Walsh | PEP_Risk | 1 | 0 |
| `KYC939e89dc-d276-4253-b868-10120de5efb2AML` | James Murphy | MatNN_NoRisk | 1 | 0 |
| `KYCec853fed-499c-4067-b357-5d88ba97fb95AML` | Minji Park | PEP_Risk | 2 | 0 |

These five are upstream input-coverage omissions, not OpenSearch or Java
matching failures. The final production report must preserve that distinction
and must not silently include them in a matcher false-negative denominator.

Presentation-ready acceptance material is available alongside the State Street
analysis:

```text
/home/pn/Documents/code/workfusion/implementations/state-street-er-pov/
  final-production-screening-validation-slides.md
  final-production-screening-validation.pptx
```

The greenfield Entity Intelligence port must implement both gates. A small
hand-written unit test is not an adequate substitute for the exact real-data
OpenSearch-to-Java regression test and the full exported-candidate backtest.

### Run-8 post-audit recall and scoring fixes (2026-08-09)

An exhaustive single-pass audit of all 5,818,856 raw World-Check records was
performed for every run-8 Firco zero-hit. It checked primary names, all alias
fields, external identifiers, person DOBs, spelling/spacing variants and
jurisdiction context. This overturned three earlier zero-hit dispositions:

| Customer Record ID | Customer | Required World-Check UID | Run-8 failure |
|---|---|---:|---|
| `KYC190207AML` | PricewaterhouseCoopers Hong Kong Limited | `7120638` | OpenSearch required two of three customer-name tokens; provider retained only the exceptionally distinctive `PRICEWATERHOUSECOOPERS` brand and stored Hong Kong as jurisdiction |
| `KYC82088AML` | Interactive Broker LLC | `159122` | Candidate was recalled with name score `0.91775`, but the Java precision gate treated `broker`/`brokers` as only one exact-token overlap |
| `KYCcddf3fb2-571e-4986-944f-704d1e37c314AML` | Nourah AlFassam | `8061861` | Joined particle and transliteration differences: `Nourah AlFassam` versus `Noura ... AL-FASSAM` |

Implemented controls:

- query-side splitting of known joined name particles without generic
  camel-case splitting; `AlFassam` is split, while the `mBank` brand remains
  unchanged;
- an additive single-token recall clause only for identity tokens at least 15
  characters long; this recovers `pricewaterhousecoopers` without weakening
  ordinary multi-token coverage requirements;
- a controlled organization singular/plural token equivalence for the Java
  precision overlap gate;
- an explicit legal-form conflict gate, so `Interactive Brokers LLC` is
  accepted but the distinct `Interactive Brokers Corp.` record is rejected;
- a `0.93` name floor only when an exceptionally long exact brand token and
  the provider jurisdiction both appear in the customer legal name;
- a person-only, positively corroborated transliteration overlap requiring two
  aligned tokens and at least one literal exact token; and
- corrected real-data fixture extraction for World-Check `M`, `F`, `U`, and
  legacy `I` person indicators. Production ingestion already implements these
  values through `map_worldcheck_entity_kind`; this was a test-fixture defect,
  not a new index-mapping change.

Verification completed:

- focused normalization, recall-query, precision-gate and negative-control
  tests: 45 tests, zero failures;
- real-data end-to-end regression: 36 real customer cases and 157 real
  World-Check records, exact expected UID sets, zero failures;
- run-8 full exported-candidate backtest: 219,779 subjects and 73,150 exported
  hit candidates rescored; 71,601 retained and 1,549 removed by the stricter
  current precision rules; build successful.

No World-Check reingestion or OpenSearch index rebuild is required for the
name-recall and scoring changes in this subsection alone. The following
entity-kind correction supersedes the combined deployment instructions and
does require full reingestion before screening.

### World-Check overloaded entity/gender discriminator correction (2026-08-09)

The preceding no-reindex statement applies only to the name-recall and scoring
changes. A subsequent complete profile of all 5,818,856 rows found an
independent `entity_kind` ingestion defect that **does require World-Check
reingestion and a replacement index revision**.

`E/I` is overloaded by the provider: `M`, `F`, and `U` encode individual gender
(with legacy `I` meaning individual), while `E` means a non-person entity.
`CATEGORY` is not a safe substitute because categories such as `CRIME -
ORGANIZED`, `CRIME - NARCOTICS`, and `RELIGION` contain both people and
organizations.

Full-file evidence:

| Provider flag | Rows | Correct canonical handling |
|---|---:|---|
| `M` | 3,488,559 | `PERSON` |
| `F` | 1,212,206 | `PERSON` |
| `U` | 97,912 | `PERSON` |
| `E` | 1,020,179 | `ORGANIZATION`, except vessel/aircraft categories become `PHYSICAL_ASSET` |

No blank flags occur in the current file. The corrected population is
4,798,677 people, 978,829 organizations, and 41,350 physical assets.

The former transform correctly made `M/F/U/I` authoritative but failed to make
`E` authoritative. Consequently:

- 60,162 `E`/`ORGANISATION` records were indexed as `UNKNOWN` because the UK
  spelling was absent from the category map; and
- 5,021 `E` records in `CRIME ...` or `RELIGION` categories were indexed as
  `PERSON`.

This matters twice: OpenSearch recall hard-excludes incompatible entity kinds,
and Java scoring rejects incompatible candidate kinds. The bad index values can
therefore create genuine technical misses rather than merely inaccurate
metadata.

Implemented rule:

1. `M/F/U/I` always means `PERSON`, regardless of `CATEGORY`.
2. `E` always means non-person; only vessel and aircraft categories refine it
   to `PHYSICAL_ASSET`; all other `E` rows become `ORGANIZATION`.
3. If the flag is missing or unknown, only unambiguous category values are
   used. Mixed-use `CRIME ...` and `RELIGION` values become `UNKNOWN`.
4. The YAML mapping explicitly declares `E/I` under
   `transform_sources.entity_indicator`; the Spark converter no longer
   hardcodes the provider column name.

Tests include real `E`/crime organization shapes, person/category conflicts,
physical-asset refinement, both `ORGANISATION` spellings, missing-indicator
behavior, Spark row conversion, and the exact 36-customer/157-reference
regression. The focused suite ran 94 tests with zero failures. The fixture
generator also re-derives kind from raw provider fields so historical bad index
values cannot contaminate the oracle.

Deployment sequence is now:

1. build and deploy the ingestion and screening images;
2. publish the updated mapping YAML to GCS; the current production object was
   inspected and does not yet contain `transform_sources.entity_indicator`;
3. force a full World-Check reingestion using the same stable reference
   population ID and canonical mapping URI;
4. wait for the new revision to become active and verify its entity-kind
   distribution;
5. start population screening against that active revision.

### Run-9 production result and final PwC production-shape correction (2026-08-10)

The corrected full World-Check reindex completed successfully with 5,818,856
records, zero failures and active revision
`2ef3f232-5d27-4491-b8f0-7f0df2cde2a4`. Population run
`6bdfaae0-60d6-46f4-881c-dfd7b9e379d0` then completed all 219,779 subjects
with zero failed or duplicated subject results and retained 73,292 hit pairs.

The Q1 Firco benchmark observed 330 valid-hit IDs, 12 evidence-backed valid
zero-hits, five customer-master omissions and one diagnosed technical miss.
Interactive Broker LLC and Nourah AlFassam were recovered exactly as intended.
The remaining miss was `KYC190207AML`, PricewaterhouseCoopers Hong Kong
Limited, against World-Check UID `7120638`, `PRICEWATERHOUSECOOPERS`.

The prior unit test had supplied a synthetic `countries=[HK]`. The production
World-Check record instead has a registered location with
`country_name=HONG KONG` and no `country_code`. The long-distinctive-brand rule
was therefore correct in principle but could not see the jurisdiction it
required. This is an important fixture-design lesson: regression records must
preserve missing fields as well as populated values.

The final correction has two layers:

1. `ScreeningScoringEngine.referenceJurisdictionAppearsInSubjectName(...)`
   retains its ISO-code path but falls back to exact normalized
   `location.country_name` phrase matching. This fallback is reachable only
   within the existing one-token, exceptionally distinctive brand rule, so a
   country name alone cannot promote an ordinary candidate.
2. `NamedTransforms` explicitly resolves `HONG KONG` to `HK` and `MACAU` /
   `MACAO` to `MO`, preventing future indexes from losing these derived codes.

The exact production-shaped scoring fixture uses the real customer name and a
reference location containing only `country_name=HONG KONG`. The focused
scoring and ingestion-transform suites pass 90 tests with zero failures.

The Java fallback can operate against the existing active index; reindexing is
not required merely to validate this scoring correction. A later reindex will
populate `country_code=HK` and exercise the primary path as well.

Final evidence and presentation artifacts:

- `/home/pn/Documents/code/workfusion/implementations/state-street-er-pov/screening-results-run9-final-analysis.md`
- `/home/pn/Downloads/state-street-data-analysis-final.pptx`
- `screening_results/firco_coverage_run9/exhaustive-zero-hit-audit.csv`

### Forward port into the replacement implementation (2026-08-10)

The three production corrections above are no longer confined to
`data-fabric`:

- `forwardmeasure-entity-matching` now owns the PERSON-only Tom/Thomas recall
  clause, the strict final-scoring gate and negative controls;
- its canonical OpenSearch response binding now preserves
  `locations.country_name`, and core scoring contains the constrained PwC
  distinctive-brand/jurisdiction rule;
- `entity-intelligence` now supports named `transformSources` in governed
  deterministic mappings and derives World-Check kind from both `CATEGORY`
  and the mapping-declared `E/I` source.

The executable and deployment handoff for these changes is
`/home/pn/Documents/code/forwardmeasure/forwardmeasure-entity-matching/docs/production-screening-corrections.md`.
