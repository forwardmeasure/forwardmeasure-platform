# ForwardMeasure Platform Technical Vision

## How to read this document

This document is the single description of what the ForwardMeasure platform is for, how it is
built, and the engineering rules everyone working on it follows. It is written for engineers and
coding agents, including people new to the platform. It replaces about 24 older documents, which
now live in `docs/archive/` (see [Sources](#31-sources)).

What this document contains:

- **Intended design and rules only.** It says what the platform must be and how it must be built.
- **No implementation or test status.** What is built, deployed or verified is recorded in two
  sibling documents:
  - [implementation-status.md](implementation-status.md): what exists, what is deployed, what is
    left to build;
  - [multi-framework-testing-status.md](multi-framework-testing-status.md): what has been tested,
    on which framework and engine, with what evidence.
- **OpenWorkflow (FOWF) detail lives in FOWF's own documents.** FOWF's requirements (by ID), status,
  backlog and test evidence are maintained only in
  [FOWF technical and functional vision](../../forwardmeasure-openworkflow/docs/architecture-design.md),
  [FOWF implementation status](../../forwardmeasure-openworkflow/docs/implementation-status.md) and
  [FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md).
  This document keeps the platform-wide rules that bind FOWF and short dated summaries that link
  there (ownership split agreed 2026-10-13).
- **Current rules, with history where it matters.** Where a decision replaced an earlier one, the
  current rule is stated and the earlier one is named, so readers of older code and documents can
  recognise it. Section 28 collects every superseded decision and recorded conflict.
- **Exact wording where the source gave it.** Quoted rules are in quotation marks and are the
  owner's or the original document's wording.
- **Unconfirmed statements are labelled.** For Data Streaming, Decision Engine, Agent OS and
  platform operations, no platform document states the product purpose. Those statements come from
  each repository's own README or `docs/` folder and are marked
  "(from repository documentation - to confirm)". Anything else that is unclear is listed in
  [Open questions](#29-open-questions).

Conventions:

- **FWP** is `forwardmeasure-platform`. **FOWF** (sometimes "fowf") is OpenWorkflow,
  `forwardmeasure-openworkflow`. **FEI** is Entity Intelligence. **FDS** is Data Streaming. **FDE**
  is Decision Engine.
- **OKS** ("OpenWorkflow on Kafka Streams") is the old name of FOWF's Kafka Streams engine and of
  the retired standalone repository. Older code and documents use it for FOWF as a whole.
- **Q/S/M** means Quarkus, Spring Boot and Micronaut.
- **K3s** is the lightweight Kubernetes distribution used for disposable test clusters.
- "Must", "never" and "always" are rules. "Should" is a strong default that needs a stated reason
  to deviate.

## Table of contents

**Part I: Functional vision**

1. [What the platform is for](#1-what-the-platform-is-for)
2. [Products](#2-products)

**Part II: Architecture**

3. [Repositories and the role of forwardmeasure-platform](#3-repositories-and-the-role-of-forwardmeasure-platform)
4. [Runtime topology](#4-runtime-topology)
5. [Deployment](#5-deployment)
6. [Multi-tenancy and data placement](#6-multi-tenancy-and-data-placement)
7. [Identity, authorization and tenant membership](#7-identity-authorization-and-tenant-membership)
8. [Data and API contracts](#8-data-and-api-contracts)
9. [Eventing, correlation and causality](#9-eventing-correlation-and-causality)
10. [OpenWorkflow behaviour](#10-openworkflow-behaviour)
11. [Entity Intelligence architecture](#11-entity-intelligence-architecture)
12. [Data Streaming architecture](#12-data-streaming-architecture)
13. [Decision Engine architecture](#13-decision-engine-architecture)
14. [UI architecture, design system and tenant branding](#14-ui-architecture-design-system-and-tenant-branding)

**Part III: Engineering guidelines**

15. [Engineering principles](#15-engineering-principles)
16. [Architecture invariants and requirements](#16-architecture-invariants-and-requirements)
17. [Contract-first APIs](#17-contract-first-apis)
18. [Persistence](#18-persistence)
19. [Database migrations](#19-database-migrations)
20. [Multi-framework parity](#20-multi-framework-parity)
21. [Engine separation and engine-agnostic behaviour](#21-engine-separation-and-engine-agnostic-behaviour)
22. [Versions and the build](#22-versions-and-the-build)
23. [Testing](#23-testing)
24. [Coverage](#24-coverage)
25. [Licensing](#25-licensing)
26. [Release and platform admission](#26-release-and-platform-admission)
27. [Working rules for engineers and coding agents](#27-working-rules-for-engineers-and-coding-agents)

**Part IV: Reference**

28. [Superseded decisions and recorded conflicts](#28-superseded-decisions-and-recorded-conflicts)
29. [Open questions](#29-open-questions)
30. [Glossary](#30-glossary)
31. [Sources](#31-sources)

---

# Part I: Functional vision

## 1. What the platform is for

### 1.1 The platform in one paragraph

ForwardMeasure builds an agentic evidence-intelligence platform. A general-purpose, durable
workflow engine (OpenWorkflow) runs every long-running process. Products built on it ingest data
(Data Streaming), screen and resolve entities and manage investigations and evidence (Entity
Intelligence), evaluate rules (Decision Engine), and will host AI agents. All products share one
identity system, one multi-tenant data model, one set of contracts, one build and version
foundation, one deployment layer and one UI design system. Every service is built for three Java
frameworks (Quarkus, Spring Boot, Micronaut), and FOWF runs on two engines (Kafka Streams and
Pekko).

### 1.2 Commercial identity

- The commercial brand is **Kriyagentic**: "ForwardMeasure's agentic evidence-intelligence
  platform, built on forwardmeasure-openworkflow and forwardmeasure-entity-intelligence"
  (from repository documentation - to confirm).
- Tenants are served on per-tenant hostnames, `<alias>.kriyagentic.com`. The first tenant is
  `lux`.
- The platform name displayed in the applications is lower-case "kriyagentic" for now. Whether it
  should be "Kriyagentic", as in the brand book, is an open question.
- A core product rule: "the model proposes; a person decides". Agent proposals never look like
  approved findings, and every approval names the approver and the policy version (from repository
  documentation - to confirm).

### 1.3 Who it is for

The owner's product direction for Entity Intelligence and the agent products (stated 2026-10-07):

- **Initial framing:** law-enforcement agencies (for example the DEA). The ambition is for FEI to
  become the global standard for how human investigators use AI agents to combat human
  trafficking. The opportunity is to give agencies a set of AI agents plus collaborative,
  cross-agency knowledge.
- **Broadened the same day:** anti-trafficking for agencies alone is too narrow. Screening must
  also serve KYC (know your customer) and AML (anti-money-laundering) broadly. The vision is a
  continuum: automated document ingestion, then processing, understanding, KYC/AML and
  anti-trafficking. It is built from reusable assets (skills, tools, agents) that also apply to
  domains with nothing to do with financial services, AML/KYC or trafficking.
- **Markets are global,** not US-first.
- **Market segments** named in repository documentation: anti-human-trafficking investigation
  (law-enforcement agencies lead), banks (KYC, AML, sanctions), wealth and asset managers,
  insurers, legal teams and journalism (from repository documentation - to confirm). The owner's
  rewrite direction for the anti-trafficking design document also asks to show value per segment
  "from wealth managers and hedge funds through to agencies", with anti-human-trafficking as the
  core and every capability monetised.
- **Where agents live:** decided 2026-10-07, "agents live on forwardmeasure-agent-os (not FEI)".
  Later documents leave this open (see [section 2.5](#25-agent-os-and-the-information-extraction-agent)).
- **Today's code is KYC/AML-shaped:** WorldCheck reference populations and monitored-population
  screening. The anti-trafficking direction reframes the FEI roadmap.

Still open, and to be asked rather than assumed: the beachhead buyer, whether the reusable asset
contract is open or proprietary, and compliance targets per region.

#### Agent catalogue

The owner chose these working-agent names on 2026-10-07 (functional portmanteaux, not
anthropomorphised names and not three-letter acronyms):

| Agent | Role |
|---|---|
| Custodex | Acquisition (evidence intake and custody) |
| Indexical | Extraction |
| Selfsame | Entity resolution |
| Chronoscope | Temporal analysis |
| Nexograph | Graph |
| Emergraph | Network learning |
| Deconflux | Collaboration (cross-agency deconfliction) |
| Narratrace | Reporting |
| Statebound | Coordinator / control loop |
| Safegress | Safety gate (sensitivity and quarantine) |
| Populens | Screening |
| Exposcope | Portfolio exposure |
| Veristate | Perpetual KYC |
| Shockpath | Scenario analysis |

- The owner rejects cutesy morphs (for example Entitwine, Identifold, Ongoligence) and prefers a
  root plus a concrete noun, or a real vivid word.
- Fund solutions (from the owner's fund-intelligence requirements) are modelled as solutions
  composed of core agents.
- The owner's anti-trafficking design input ("Agentic Intelligence for Anti Human Trafficking
  Investigations") describes specialist agents plus case coordination (which is not an agent);
  seven layers (Evidence, Content, Semantic, Identity, Knowledge Graph, Analytical, Decision); a
  claim and epistemic-status model; bitemporal data; OpenWorkflow on Kafka Streams; A2A, MCP and
  A2UI protocols; a JanusGraph fork; and Docling, GLiNER and TimesFM models.

#### Evidence principles for agentic investigation

Agreed on 2026-10-07 for the evidence-extraction showcase (an agent over redacted public court
files producing entities, activities, an entity-activity graph and indirect links):

- **"Non-repudiable" applies to the chain of custody,** not to identity: source hash, then
  extraction, then assertion, then resolution decision.
- **Never re-identify redacted victims.** Instead, categorise victims (for example age band or
  minor, exploitation type, recruitment method) and capture what can be deduced, guarded against
  re-identification through quasi-identifiers.

### 1.4 How the products fit together

```
                         Tenant users (browser)
                                  |
              one tenant host: <alias>.kriyagentic.com
                                  |
        +-------------------------+--------------------------+
        |                         |                          |
   FEI Studio /ei/studio/   OpenWorkflow Studio      Platform Dashboard
        |                         |                          |
   FEI APIs (screening,     FOWF APIs (definitions,   shared tenant-context
   ingestion, resolution,   executions, human tasks,  service /platform/*
   investigations)          tenant administration)
        |                         |
        |   starts long-running   |
        +------ work via -------->+  FOWF engine: Kafka Streams OR Pekko
                                  |  (one per installation)
                                  |
          operation adapters and correlated workers
          (HTTP/OpenAPI, gRPC, AsyncAPI transports,
           Kubernetes Jobs/Deployments, MCP, A2A)
                 |                |                 |
           FEI workers      FDS launcher        FDE (gRPC)
           (call FEI REST)  -> delivery engines  rule evaluation
                            (Pekko Streams /
                             Kafka Streams,
                             optional Spark stage)

Shared: Keycloak (one realm, an Organization per tenant), AuthZEN policy decisions,
PostgreSQL (one control-plane database plus one database per tenant), Kafka, OpenSearch,
Cassandra (Pekko journal only), Valkey, Apicurio registry, object storage.
```

- **FOWF is the execution substrate.** Every long-running process in every product is a FOWF
  workflow. Products are workflow authors, "not a second execution engine".
- **FEI** owns the entity-intelligence domain and its Studio. It starts work through FOWF's
  generated execution-management client; its workers call FEI's REST API.
- **FDS** moves data. It is started directly through its REST launcher or by a FOWF workflow.
  FEI's ingestion uses it.
- **FDE** evaluates rules over gRPC, called by FOWF through its real gRPC adapter.
- **Agent OS** is the intended home of agents (decision of 2026-10-07), but its role is not
  settled (section 2.5).
- **Platform operations** owns the Platform Dashboard and the shared tenant-context service.
- **forwardmeasure-platform** owns versions, shared contracts, shared libraries for request
  handling, and shared deployment infrastructure.

## 2. Products

### 2.1 OpenWorkflow (FOWF)

**Repository:** `forwardmeasure-openworkflow` (Apache-2.0).

**Purpose.** A general-purpose durable workflow engine. It implements the public Open Workflow
specification (the CNCF Serverless Workflow language) version 1.0.3, plus local approved
extensions. It "knows nothing about entity screening, watchlists, or your business". Its job is to
"accept a workflow written in that standard shape, run it reliably, and let you watch and control
it".

**Users.** "Workflow authors and platform operators", through OpenWorkflow Studio and the APIs.
Product teams (FEI, FDS, FDE) are workflow authors.

**One product, two engines.**

- "OpenWorkflow's Pekko and Kafka engines are one unified product entry. The platform imports
  `openworkflow-bom` and never imports the OpenWorkflow parent or lists the retired standalone
  engines as parallel products."
- The standalone `openworkflow-kafka-streams` (OKS) and `openworkflow-actor-engine` repositories
  are retired. Both engines live in `forwardmeasure-openworkflow`, "two pluggable execution
  engines (Kafka-Streams and Pekko/actor-based) behind one shared REST facade".
- Both engines are permanent requirements. Dropping one to make a problem smaller is never
  acceptable.
- Engine selection values: `kafka-streams`, `pekko-postgresql`, `pekko-cassandra`. One engine is
  deployed per installation.

**Scope** (summary, 2026-10-13). Definition management, execution management, human tasks,
operation adapters and correlated workers (including Kubernetes Jobs and Deployments), subworkflows,
the shared generic workflow-bundle publisher through which every product publishes, CloudEvent
ingress and lifecycle CloudEvents, tenant administration and OpenWorkflow Studio. The full functional
scope, requirement IDs and the declared executor list (HTTP/OpenAPI, gRPC, the AsyncAPI transports
including SNS/SQS and Pub/Sub, HTTP streaming/WebSocket/SSE, MCP, A2A, container/local process,
Kubernetes Job and Deployment) are in the
[FOWF vision §1](../../forwardmeasure-openworkflow/docs/architecture-design.md#1-product-purpose-and-completion-standard),
[§15](../../forwardmeasure-openworkflow/docs/architecture-design.md#15-shared-protocol-executors-and-correlated-external-work)
and [§21](../../forwardmeasure-openworkflow/docs/architecture-design.md#21-cross-product-boundaries-and-extension-roadmap).
Unsupported transports are documented explicitly, never silently marked as not applicable.

**Agents and FOWF.** The UI architecture decision states: "An agent invocation is an OKS
execution. If an agent-facing product is later justified, it must use OKS for execution state and
human interaction and domain APIs for business resources. Milestone UI work does not create an
empty agent application or a second workflow runtime model." The relationship of this rule to
Agent OS is an open question.

### 2.2 Entity Intelligence (FEI)

**Repository:** `forwardmeasure-entity-intelligence`. **Proprietary and closed-source** (section
25). The platform's `reactor.xml` builds it with the rest of the stack (checked 2026-10-13); being
proprietary does not keep it out of the source build. The earlier `entity-intelligence`
repository is "a rejected, throwaway implementation. It may be inspected for algorithms or tests
only; it is not an implementation or architectural baseline."

**Users.** "Entity Intelligence operators, investigators and analysts".

**Responsibilities.** "Tenant overview, ingestion, extraction, populations, screening,
resolution, entities, assertions, dossiers and investigations", plus evidence.

**What it does.**

- **Reference populations:** watchlist datasets (for example WorldCheck) ingested and indexed in
  OpenSearch, with revisions, activation, rollback and retention.
- **Monitored populations:** the subjects an organisation monitors (for example a customer
  master), resolved into entities with assertions and provenance.
- **Screening:** population screening runs and ad-hoc screening of an arbitrary or resolved
  entity against reference populations.
- **Resolution:** entities, assertions, candidates, merge and deletion.
- **Investigations:** dossiers, investigations, evidence upload and retrieval, candidates, LLM
  prompts.
- **Information extraction:** a planned agent that extracts structured AML/KYC facts from
  investigation evidence (section 11.6).
- **FEI Studio:** the analyst UI.

**Core user journeys** (the six functional goals set for October 2026):

1. Ingest the complete WorldCheck export through Studio and verify the persisted and indexed
   output.
2. Ingest a customer master through Studio and inspect entities and assertions.
3. Screen the entire customer master against WorldCheck.
4. Screen an arbitrary entity against WorldCheck.
5. Screen an existing resolved entity against WorldCheck.
6. Upload evidence to an investigation and retrieve its stored content.

**Purpose of a screening run.** Evidential accountability, not raw hit counts. "Every subsequent
run … must not equate 'zero hits' with 'platform bug' or 'has a hit' with 'the same entity exists
in WorldCheck'." Regulatory point-in-time snapshots are a contract: "Any deduplication of raw
evidence must preserve the regulatory point-in-time snapshot contract; do not replace persisted
snapshots with a lookup against the current mutable reference index."

**Relationship to FOWF.** FEI is a FOWF customer. Long-running work (ingestion, screening,
extraction) runs as FOWF workflows started through FOWF's generated execution-management client.
FEI has no Kubernetes Job orchestrator and no scheduler of its own.

**Providers supported today:** WorldCheck (reference) and the customer master
(monitored). The platform also intends OFAC and proprietary inputs to target the same screening
shape.

### 2.3 Data Streaming (FDS)

**Repository:** `forwardmeasure-data-streaming` (Apache-2.0).

**Purpose** (from repository documentation - to confirm): "Metadata-driven ingestion:
schema-described sources and targets, named transformations, Spark and Pekko Streams execution,
backpressure-controlled sinks - built on this org's proven connectors (Apache Camel,
`forwardmeasure-jpa`, `forwardmeasure-object-storage`) rather than reinventing connectivity." Its
architecture document calls FDS "the org's metadata-driven data-movement product".

**Scope** (from platform audit requirements, 2026-10-10):

- One canonical author-facing `IngestionSpec` with `sources: []`. One source is a single-source
  run; more than one is a correlated run.
- Two permanent delivery engines, Kafka Streams and Pekko Streams, each supporting bounded and
  continuous runs. Spark is an optional computation or staging step and is "not a third delivery
  engine".
- Bounded runs dispatch a Kubernetes Job; continuous runs dispatch a Kubernetes Deployment.
- Runs are admitted either directly through the REST launcher or through a FOWF workflow.
- Launcher hosts exist per framework. Delivery runners are framework-neutral `main()` programs.

**Users** (from repository documentation - to confirm): product teams that onboard sources (FEI is
the first consumer) and FOWF workflows that orchestrate ingestion. Per-schema generated model
classes and per-source mapping specs belong in the consuming product, not in FDS; the mapping
mechanism belongs in FDS.

**FDS has no database of its own.**

### 2.4 Decision Engine (FDE)

**Repository:** `forwardmeasure-decision-engine`.

**Purpose** (from repository documentation - to confirm): "General-purpose, gRPC-exposed,
Drools-based rule evaluation server." A caller sends a ruleset name plus a structured input
document; the server runs that ruleset's Drools rules against the input and returns a structured
decision. "The server has no idea what any particular ruleset means — it is a generic execution
engine, not a policy service for any one product."

**Why Drools and gRPC** (from repository documentation - to confirm): gRPC for a strongly typed,
low-overhead contract with native multi-language code generation. Drools for forward-chaining and
complex-event reasoning "across a sequence of calls, not just a single static input snapshot",
which simpler engines such as OPA/Rego lack. Stateful fact windows are the capability that
justifies the product.

**Relationship to Agent OS** (owner decision, 2026-10-13): Agent OS uses FDE as its policy
evaluator, for now - its `drools` evaluator setting is a gRPC client to FDE. FDE's own
documentation spoke of being "evaluated in parallel" with Agent OS's OPA-based evaluator; no such
comparison was ever built. FDE does not
depend on Agent OS and must stand on its own regardless of that comparison.

**Platform facts:** FDE is optional per installation (`enableFde`, default true). It is gRPC-only:
"Do not invent REST or a new Studio." FOWF calls it through the real gRPC adapter. Its migration
Jobs add its schema to each tenant database.

### 2.5 Agent OS and the information-extraction agent

**Repository:** `forwardmeasure-agent-os` (built by the platform reactor).

**What is known** (from repository documentation - to confirm):

- The owner started the repository some time ago and no longer remembers why; a read-only analysis
  of its purpose was commissioned on 2026-10-12. Its README is empty.
- Its modules cover domain and schema validation; API specifications and generated language
  bindings; an OpenWorkflow client, inbound protocol mapping and actor bindings; governance,
  execution and policy (including OPA and Drools policy modules); database migrations; a Studio;
  deployments; architecture and conformance tests.
- `agent-os-studio` is an empty skeleton (POMs only, no web app, no image, no deployment).

**What has been decided, and what conflicts:**

- 2026-10-07 (owner): agents live on `forwardmeasure-agent-os`, not in FEI.
- The UI architecture decision says an agent invocation is a FOWF execution and no second workflow
  runtime model should be built.
- 2026-10-12: the information-extraction handover says Agent OS is *not* established as the
  extraction agent's home, and the location of the owner's initial agent implementation is
  unknown.
- The Kriyagentic design system names a future `agent-os-studio` as the fourth UI on the shared
  design system.

The owner's agent architecture input ("Agent Control Loops Using Kubernetes Reconciliation and
OpenWorkflow", 2026-09-09) describes an outer reconciliation loop (intent, observed state,
conditions, budget ledger, convergence), bounded reasoning that only proposes, and an inner
deterministic loop that executes one typed action contract, with FOWF as the execution model.
Whether this is the approved Agent OS design is unconfirmed.

The information-extraction agent and its Studio integration are specified in section 11.6.

### 2.6 Platform operations, the Platform Dashboard and the tenant portal

**Repository:** `forwardmeasure-platform-operations` (open source). Earlier documents placed the
Platform Dashboard in the platform repository; the separate repository is current.

**Platform Dashboard.**

- Users: "Platform operators".
- Responsibilities: "Shared service health, Kafka, schema registry, storage, search, model-service
  and deployment operations".
- Boundary: it "explains shared platform health and operations without absorbing either product's
  business screens".
- Today it is a read-only launcher and infrastructure summary (from repository documentation - to
  confirm). Its own review recommends defining the tenant launcher separately from the
  platform-operator view and enforcing the chosen access contract at the API boundary.
- Health status semantics: a TCP check proves only socket connectivity, and an HTTP check that
  accepts any 2xx proves only that the endpoint answered. The UI must keep that distinction when
  richer readiness is added, and must not present the dashboard as end-to-end workflow or decision
  acceptance (from repository documentation - to confirm).

**Shared tenant-context service** (decision of 2026-10-12): `forwardmeasure-platform-operations`
owns "One shared tenant-context service" that answers "which tenant is this hostname, and what is
its brand?" for every application. "No app resolves tenants itself." See section 14.6.

**Tenant portal** (intended design): the "overarching shell and launch point" for all tenant
users. Responsibilities: "Product navigation, tenant identity, session entry, cross-product
notices, and shallow status summaries". It "may display high-level summary cards from both
domains, but it must not duplicate their operational screens or embed their applications in
iframes." Whether the portal is a separate application or part of the Platform Dashboard is an
open question.

### 2.7 Studio UIs

There are four applications on one design system (section 14):

| Application | Owner | Users |
|---|---|---|
| OpenWorkflow Studio | FOWF (open source) | Workflow authors, platform operators |
| FEI Studio | FEI (closed source) | Operators, investigators, analysts |
| Platform Dashboard | Platform operations (open source) | Platform operators |
| Agent OS Studio (future) | Agent OS | Not yet defined |

Older documents call OpenWorkflow Studio "OKS Studio" and FEI Studio the "Entity Intelligence
Workbench". The current names are OpenWorkflow Studio (FOWF Studio) and FEI Studio (Entity
Intelligence Studio).

### 2.8 Shared libraries

| Repository | Purpose |
|---|---|
| `forwardmeasure-jpa` | Shared multi-tenancy and ORM layer: entity, repository and service base classes; tenant-aware connection routing; tenant registry; `TenantId`, `Did`, `TenantDatabase`, `DidUrl`, `PlatformDids` |
| `forwardmeasure-database-migrations` | Migration API, JDBC and Liquibase support |
| `forwardmeasure-object-storage` | One storage API with S3, GCS and Azure providers (version pinning and checksums on all three). S3 uses `endpoint` for SDK operations and `publicEndpoint` for signing; "Public GCS endpoints must be HTTP(S) origins without path prefixes, userinfo, query or fragments." |
| `forwardmeasure-testcontainers` | Real-infrastructure test fixtures: PostgreSQL, Kafka, OpenSearch, Keycloak, Kubernetes (K3s), MinIO, GCS emulator, Valkey, Cassandra; JUnit Jupiter and Quarkus adapters |
| `forwardmeasure-authzen` | AuthZEN authorization client, Keycloak Organization claims, tenant-bound client credentials, test fixtures |
| `forwardmeasure-entity-matching` | Name normalisation, OpenSearch recall query building, matching; owns the production screening corrections |
| `forwardmeasure-document-intelligence` | Docling Serve client generated from the pinned image OpenAPI, immutable document artifacts/provenance, DocLang/DCLX validation, Spring/Quarkus/Micronaut bindings, CLI and Postman collection |
| `forwardmeasure-nlp` | Named-entity-recognition Java client generated from OpenAPI |
| `forwardmeasure-presentation-system` | Shared UI design system package `@forwardmeasure/presentation-system` (Apache-2.0) |
| `helm-charts` | Shared, framework-agnostic Helm charts (for example `java-microservice-helm-chart`, `openworkflow-workflow-bundle`, Keycloak, platform-secrets) |
| `openworkflow-k8s-setup` | Cloud infrastructure (OpenTofu) for the production GCP cluster |

The platform repository itself also hosts product-neutral libraries:
`forwardmeasure-platform-server-jaxrs`, `forwardmeasure-platform-micronaut-validation`,
`forwardmeasure-platform-spring-security`, `forwardmeasure-platform-quarkus-security`,
`forwardmeasure-platform-api-contracts` and `forwardmeasure-platform-data-contracts`.

The Data Fabric system and its repository are being retired and are not part of this platform.
Nothing is ported from it by copying its files or paths; behaviour is re-specified and rebuilt in
the correct greenfield modules. Active runtime source contains no legacy Data Fabric (or Asvinau)
identities, and negative guard tests keep it that way.

---

# Part II: Architecture

## 3. Repositories and the role of forwardmeasure-platform

### 3.1 Five responsibilities

`forwardmeasure-platform` has five responsibilities:

1. "Publish the shared Java parent, core dependency BOM and framework overlay BOMs."
2. "Publish a compatibility BOM representing one tested set of released artifacts."
3. "Validate shared build behavior and cross-component public contracts."
4. "Aggregate the Apache-licensed component repositories into one source build."
5. "Own shared deployment infrastructure."

What it is not:

- "It does not own product source. Each component keeps its own repository, modules, version,
  product BOM, tests and release." It "must not absorb product source code".
- It is a compatibility release train, not a monorepo. "OKS is the engine. Entity Intelligence is
  a car built to run on that engine. The platform repo is the test track and the parts catalogue
  — it doesn't build the car or the engine."

How the source build works today: the platform root builds its own modules; the separate
`reactor.xml` is the wider compatibility reactor, not a parent POM. `build.sh` runs the reactor,
which includes every product repository - FOWF, FDS, FDE, Agent OS, FEI and platform operations
(checked 2026-10-13: `reactor.xml` lists `../forwardmeasure-entity-intelligence`, and a full build
compiles FEI's modules). An older statement that "FEI remains outside that open-source
compatibility reactor" is superseded.

### 3.2 Layering

```
platform parent + core BOM + Quarkus/Spring/Micronaut overlay BOMs
        -> single open-source source reactor (reactor.xml)
        -> independently released component BOMs and artifacts
        -> forwardmeasure-platform-bom + build and artifact compatibility tests
              -> Platform Operations dashboard deployment
              -> shared infrastructure deployment
```

### 3.3 Provenance

- "The compatibility train records released artifact versions because those are the inputs it
  resolves and tests."
- Git revision provenance stays with each component release through "Maven SCM metadata, signed
  tags, SBOMs, and build attestations".
- "a second hand-maintained `platform-sources.json` is intentionally not used."

### 3.4 Contract ownership

"API specifications and generated clients remain owned and published by their service
repositories. The platform verifies consumers against those artifacts; it does not copy their
contracts." Shared, product-neutral contracts (errors, provider data) are owned by the platform
(section 8).

### 3.5 Shared infrastructure ownership

"The shared deployment owns common infrastructure such as Keycloak, messaging, search, and
routing. Product deployments supply product configuration and consume shared endpoints without
deploying duplicate infrastructure." Products still own their own releases (for example FDE owns
the decision-engine release).

### 3.6 The repositories in the build

The Maven version sweep covers 14 Maven repositories (about 480 reactor modules): platform,
testcontainers, database-migrations, jpa, object-storage, authzen, entity-matching,
data-streaming, openworkflow, decision-engine, entity-intelligence, agent-os, nlp and
platform-operations. `helm-charts` and `openworkflow-k8s-setup` are non-Maven repositories in the
same workspace.

Platform Operations keeps its own release version (1.0.0); explicit local-group coordinates in its
managed reactor dependencies stop the shared catalogue entry from shadowing them.

## 4. Runtime topology

### 4.1 Cluster services

The production environment is `gcp-openworkflow-prod`: a GKE cluster and a private Cloud SQL
PostgreSQL instance. A second environment, `gcp-greenfield-example`, exists in every repository.

In-cluster platform services:

| Service | Role |
|---|---|
| Kafka (Strimzi) | Messaging, FOWF Kafka Streams engine state and changelogs, AsyncAPI transports, FDS delivery |
| Keycloak | Identity: one realm `forwardmeasure`, an Organization per tenant, Authorization Services |
| OpenSearch | Reference-population indexes, screening recall |
| Valkey | FDE fact windows and caches |
| Cassandra | Pekko event journal (only in the `pekko-cassandra` profile) |
| Apicurio Registry | API documents (OpenAPI, AsyncAPI) referenced by published workflows |
| Istio | Gateway API routing for tenant hosts |
| cert-manager + Let's Encrypt | TLS certificates |
| Knative Serving and Eventing | Installed in-cluster as platform services |
| External Secrets Operator (ESO) | Delivers application Secrets from the single central store |
| Superset, Nominatim, NER model service | Analytics, geocoding, named-entity recognition |

Cloud SQL holds PostgreSQL databases (section 6). Object storage is GCS on GCP.

Strimzi's topic operator cannot change a topic's replication factor by itself; the platform-native
answer is to enable Cruise Control, not to script around it (section 27).

### 4.2 Workload shapes

- "API and UI services are ordinary durable Kubernetes workloads unless their execution semantics
  explicitly require another primitive. Secrets are referenced, images are digest-pinned, and
  routing preserves the tenant host."
- FOWF Pekko engines run as a headless service (container port 8080); `java-microservice` Services
  listen on port 80.
- FDS bounded runs are Kubernetes Jobs; continuous runs are Kubernetes Deployments.
- FEI population screening is a FOWF-dispatched Kubernetes Indexed Job with per-subject OpenSearch
  recall and no Spark.
- Correlated multi-source ingestion uses Spark on Kubernetes, triggered as one FOWF execution.

### 4.3 Routing

- One host per tenant: `<alias>.kriyagentic.com`, served through one shared Gateway with a wildcard
  `tenant-https` listener. The authentication host is `auth.kriyagentic.com`.
- Paths on the tenant host:
  - `/ei/studio/`: FEI Studio (`/ei`, `/ei/` and `/ei/studio` redirect to the trailing-slash form);
  - `/owf/studio/`: OpenWorkflow Studio (the original UI design proposed `/agents`);
  - `/platform/*`: the shared tenant-context and brand endpoints;
  - `/`: the portal or Platform Dashboard.
- "Istio routing can map those paths to separate services. A common host and Keycloak realm provide
  a coherent session without forcing the applications into one deployment or one JavaScript
  bundle."
- "Routing under a tenant hostname does not replace the APIs' JWT organization checks."
- Keycloak administration uses the `master` realm; the Studios sign in against the `forwardmeasure`
  realm.
- Request IDs: on the public HTTPS auth listener the gateway preserves caller-supplied request IDs
  (`preserveRequestId`, scoped to the gateway workload, the GATEWAY context, port 443 and the exact
  auth SNI), because the AuthZEN client validates that its correlation is echoed. "This treats
  request IDs as caller-provided correlation metadata; they are not authentication or authorization
  evidence."

### 4.4 Future direction: per-tenant clusters

Today one GKE cluster serves all tenants through one Gateway, one static IP and one wildcard DNS
record. The owner has said different tenants may eventually get dedicated clusters and IPs. New
DNS and tenant-routing work must not assume the single wildcard record is permanent; flag the
tension rather than extending the shared single-IP pattern. No action is needed until a
multi-cluster split is initiated.

## 5. Deployment

### 5.1 Charts and Helmfile

- Deploy "via the shared, generalized Helm charts and helmfile orchestration": framework-agnostic
  charts from the org's chart repository, driven by Helmfile, "not a one-off manifest, not a
  project-local chart, not a framework-specific chart".
- When a chart is too narrow, generalise it in place, or supersede it and migrate every consumer
  "in a single pass". Never fork. (Precedent: the Quarkus-only service chart was replaced by
  `java-microservice-helm-chart`.)
- Chart versions are owned by the release script, which bumps `Chart.yaml`, packages, updates the
  index and publishes. Never edit `version:` or `appVersion:` by hand; edit templates and values and
  say the chart needs releasing.
- The platform uses only the Kubernetes Gateway API, never Ingress resources.

### 5.2 The shared Helmfile layer

"`forwardmeasure-platform/deploy/helmfile/shared/` is the single source of truth for every value
more than one repository deploys with."

- Every repository's Helmfile loads it directly, last, by relative path
  (`../../../forwardmeasure-platform/deploy/helmfile/shared/...`). "Nothing is copied any more."
- FDS, FEI and FDE read `.Values.platform.*` directly. FOWF and the platform map shared values onto
  their own keys through an alias file loaded after their other environment files.

| File | Holds |
|---|---|
| `common.yaml.gotmpl` | Shared namespaces; in-cluster endpoints (Keycloak, Kafka, OpenSearch, Valkey, FOWF APIs, registry); identity realm, organization client, DID method (`platform.identity.didMethod`), service-client inventory; Gateway API version; platform-delivered Secret names; `platform.databases.*`; `platform.framework` and `platform.engine` |
| `deployment.yaml` | The deployment selection (section 5.3) |
| `chart-versions.yaml`, `image-versions.yaml` | Pins used by more than one repository |
| `clusters/<env>.yaml.gotmpl` | Domain; gateway IP and hostnames; tenants (alias and display name, with host and DID derived); GCP project, region and cluster; Cloud SQL instance; per-product workload-identity service accounts; FOWF's overflow bucket |
| `brands/` | Tenant brand folders (section 14.6) |

Verified Helmfile 1.3.2 behaviour to design around:

- an environment values file cannot see values loaded before it (use `readFile` plus `tpl`);
- `readFile` resolves relative to that file's own directory;
- a sub-helmfile gets no values from its parent unless they are passed explicitly, and
  `--state-values-set` does not reach FOWF's sub-helmfiles;
- `needs` does not cross sub-helmfiles.

Every repository declares both GCP environments. Each product's environment profile is the
cluster-neutral `environments/gcp-platform-cluster.yaml.gotmpl`; environments differ only in which
shared cluster file they load. `install-platform.sh <env>` and `validate-platform.sh <env>` work
for either. Platform CI validates the platform for `base` and both GCP environments and renders
FOWF, FEI, FDS and FDE for both GCP environments. FOWF's own validation treats the greenfield
environment as production. Adding a new cluster still needs an environment entry in each product's
Helmfile. Verify Helmfile refactors with before-and-after `helmfile build --embed-values` diffs per
repository and environment (plus `write-values` for file-based release values).

### 5.3 Deployment selection

One selection applies to a whole installation, held in `deploy/helmfile/shared/deployment.yaml`.

| Key | Environment override | Values | Default |
|---|---|---|---|
| `framework` | `FORWARDMEASURE_FRAMEWORK` | `quarkus`, `spring`, `micronaut` | `quarkus` |
| `engine` | `FORWARDMEASURE_ENGINE` | `kafka-streams`, `pekko-postgresql`, `pekko-cassandra` | `kafka-streams` |
| `enableFde` | `FORWARDMEASURE_ENABLE_FDE` | `true`, `false` | `true` |
| `displayName` | `FORWARDMEASURE_DISPLAY_NAME` | platform display name | `kriyagentic` |

Rules:

- Resolution order: explicit environment variable, then the named environment's shared
  configuration, then the shared top-level configuration, then the resolver's built-in default.
- "Empty/invalid environment values fail rather than silently falling back. An explicit YAML
  `false` remains false."
- `product-selection.sh` resolves, validates and prints the selection before any image lookup or
  write. It exports an internal JSON snapshot; child steps consume it and never recompute. "Do not
  export `FORWARDMEASURE_RESOLVED_SELECTION` in a shell profile." A snapshot from another
  environment is rejected. Direct Helmfile rendering invokes the same resolver.
- "Install entrypoints are the supported mutation path; raw `helmfile sync` bypasses installer
  preflight."
- "Selecting a framework with missing image digests does not invent a tag-only fallback."
- A successful install records the selection in ConfigMap
  `forwardmeasure-platform/forwardmeasure-deployment-selection`.
- Preflight inspects Helm releases; API or authentication errors fail preflight. "Run only one
  installer against a cluster at a time."
- Every product deploys all its services in the one selected framework. Service and release names
  carry no framework suffix; image names do (`-quarkus`, `-spring`, `-micronaut`; the Spark image
  has none).
- Quarkus is never the privileged or production flavour. Any cross-repository address, route,
  health check or callback URL must work unchanged whichever framework is deployed. Never bake a
  framework suffix into a derived value; treat existing `-quarkus`-only values as bugs.
- Another engine or framework is a different platform installation.

**Engine changes.** An existing or recorded different engine blocks installation. "There is
deliberately no `ALLOW_ENGINE_CHANGE=true` escape hatch." Engine persistence histories, command
identities, running operations and recovery positions "need a separate migration design. Keeping
tenant databases while changing engines is not by itself a migration."

**Retiring FDE.** Disabling FDE while its release exists blocks the install: "omitting a product
from the installer does not uninstall it". Drain or retire it explicitly first.

### 5.4 Namespaces

| Product | Namespace |
|---|---|
| FOWF | `forwardmeasure-openworkflow` |
| Platform | `forwardmeasure-platform` |
| FDS | `forwardmeasure-data-streaming` |
| FEI | `forwardmeasure-entity-intelligence` |
| FDE | `forwardmeasure-decision-engine` |
| Agent ensemble (Indexical, Studio) | `forwardmeasure-agent-ensemble` |

- Every product namespace carries the `forwardmeasure-` prefix; there is no other layout. Service,
  database, schema and tenant identities do not depend on the namespace.
- FOWF-hosted migration and workflow-bundle Jobs (including FEI's, FDS's, FDE's and the agent
  ensemble's migrations) run in the FOWF namespace.
- Workload-identity bindings name these namespaces exactly (openworkflow-k8s-setup's
  `workload_identities`).

### 5.5 Images

- **Digest-only deployment** (owner decision, 2026-10-01): "Every image of ours
  (`[docker.io/]forwardmeasure/...`) is pinned by digest on cloud installs; third-party images may
  be pinned or float by tag. kind/local are exempt (they load local images by tag)."
- `deploy/scripts/sync-image-digests.sh`, run by `install-platform.sh`, resolves every one of our
  image entries (including an empty `digest: ""`), fails the install listing every entry it cannot
  resolve, and has a `--check` mode. `check-image-digests.sh` checks every product's render and
  "catches a template that drops a digest". FOWF's `resolve-image-digests.sh` follows the same rule.
- Every repository keeps a `digest:` field on every image entry, makes every template use the digest
  when set, and commits the resolved digests the first install writes.
- Image tags inherit `${project.version}` from the root POM's `revision`. Never type an image tag
  literal by hand.
- Images are built by Maven (Maven-generated Dockerfiles). `deploy/scripts/build-product-images.sh
  <env> --push` takes the same environment as the installer, resolves the selection once, records
  the build plan, includes FDE when enabled and builds every selected image.
- Shared libraries are published before dependent images are packaged. "No existing image digest
  should be treated as covering these source changes": after a source change, rebuild the affected
  images; a shared jar gaining an unused class does not by itself require rebuilding unrelated
  images.
- Verify the image actually running: compare the Kubernetes `imageID`, not the requested tag.
- "Build future consumer images from their selected modules with the new platform artifacts
  installed first."
- Shaded jars merge `META-INF/services` files with the services resource transformer; the default
  merge silently dropped the PostgreSQL JDBC driver registration in one image.

### 5.6 Secrets and credentials

- **Exactly one central `ClusterSecretStore`, `fake-secret-store`,** using ESO's fake provider, in
  every environment. Helmfile populates its data at deploy time by fetching from GCP Secret Manager
  (`ref+gcpsecrets://...`). Nothing in-cluster reads Secret Manager directly; there is no `gcpsm`
  provider. The owner decides if and when to switch; do not raise it.
- Every product Secret is an `ExternalSecret` from that store, declared in the platform-secrets
  release. In-cluster material that other namespaces need (for example a CA certificate) goes into
  the store with an ESO `PushSecret` and out through a normal platform-secrets entry. Never add a
  second store.
- OpenTofu generates client secrets and other generated credentials in Secret Manager; ordinary
  applies preserve them. `with-client-secrets.yaml.gotmpl` composes them into the fake-store layer at
  render time. "No generated values need to be pasted into credential files."
- ESO is the sole writer of application Secrets. ESO templates enumerate every output key
  ("composed templates must declare every desired output key under Replace semantics";
  "Metadata-only templates preserve fetched data").
- Ownerless Secrets are adopted; conflicts surface with "no deletion fallback".
- Select platform-secrets resources by `meta.helm.sh/release-name=platform-secrets`, not by
  `app.kubernetes.io/managed-by` (Helm overwrites that label).
- Synchronisation waits for a unique marker written by the current install on every target Secret
  (in every namespace), not for a pre-existing Ready condition. Consumers reload when the ESO data
  hash differs from their pod-template annotation; ESO hashes only `Secret.Data`, so the marker does
  not change the data hash. JSON values in ESO templates are rendered with `dict | toJson`.
- Cluster prerequisites delivered by the platform: the product namespaces manifest; the image-pull
  Secret for product namespaces; product runtime database passwords. Cloud SQL Auth Proxy sidecars
  take the instance connection name from the product's own database-credentials Secret
  (`db-cloud-sql-instance`, through `cloudSqlProxy.existingSecretName`); each product ServiceAccount is
  annotated with its Google service account for workload identity.
- Real production secret values live in gitignored files on the operator's machine. "Do not open or
  log credential material during review." Never commit Secrets.
- The FOWF human-task review signing secret is a dedicated 64-character random value generated by
  OpenTofu, stable across ordinary applies and independent of OAuth and database credentials; it is
  required on all three frameworks.

### 5.7 Install order and operational rules

- Infrastructure first: apply OpenTofu (`make CLOUD=gcp infra-plan` and `infra-apply` in
  `openworkflow-k8s-setup`), then run the full platform installer. "A standalone product installer
  does not perform the complete identity cutover."
- Install entry points, not hand-run commands, are the mutation path. No manual deploy steps: no
  hand-run SQL, `kubectl` edits or console clicks to unblock a deployment; build the automation.
- **Let's Encrypt rule:** the seven TLS Secrets (six in `istio-system`, one in `keycloak`) are not
  Helm-owned and are reused. "Do not delete the `istio-system` or `keycloak` namespaces: that forces
  seven reissues against Let's Encrypt's weekly limits (5 per exact hostname set -
  `auth.kriyagentic.com` has two Certificates - and 50 per registered domain)."
- **Uninstall:** `helmfile destroy` runs with `--deleteWait`, because an operator uninstalled while
  its custom resources were still terminating leaves finalizers unrun and workloads orphaned.
- **Database reset tooling** drops databases through SQL from a throwaway in-cluster psql pod as the
  admin role, because the instance is private-IP only and the Cloud SQL API refuses to delete a
  database not owned by `cloudsqlsuperuser`.
- Password matching: before a production `tofu apply`, the component passwords exported to OpenTofu
  must equal those the components already use.
- **Identity rotation** is a maintenance operation, not a zero-downtime guarantee: deliver secrets,
  reconcile Keycloak, reload consumers. In order: build and publish the images and charts and apply
  OpenTofu; on a used environment drain Jobs and pause submissions; apply configuration and wait for
  the ESO marker on every target Secret; run the Keycloak identity stage even when Helm shows no
  diff; reload controllers that reference identity Secrets and wait for rollout; reconcile tenant
  packs through FOWF, then deploy products and bundles, with a final consumer check; reconcile
  existing API-created ACTIVE tenants through the tenant API if packs changed. Strategies that would
  need manual pod deletion fail explicitly. Reruns preserve the generated secret, so an interrupted
  rotation is recoverable. Running Jobs are never replayed; an active Job needing new credentials
  raises an explicit drain error. After an interruption, rerun the full installer with
  the same secret versions: "Do not generate another secret just because the previous run stopped.
  Stage-only installation is not the complete rotation procedure."
- Deploy new Kafka adapters before new Kafka engines when both change, and drain
  `openworkflow.commands` (or pause admissions) before every FOWF Kafka Streams engine upgrade
  (decided 2026-10-13; [FOWF vision §14](../../forwardmeasure-openworkflow/docs/architecture-design.md#14-kafka-native-execution-and-topic-ownership)).
- Before a live apply, ask whether the operator wants to run and verify it.

### 5.8 Cloud substrate

The application layer consumes logical capabilities (database, object storage, DNS, identity,
secrets) "without embedding GCP, AWS or Azure resource names".

| Phase | Cloud | Scope |
|---|---|---|
| 1 | GCP (`k8s-infrastructure/opentofu/gcp`) | Service enablement; VPC-native regional GKE with private nodes, Cloud NAT and explicit pod/service ranges; CPU and GPU node pools; IAM and Workload Identity; regional private Cloud SQL PostgreSQL plus database bootstrap; GCS; Cloud DNS and the regional Gateway address; Secret Manager containers; generated non-secret Helmfile values. Acceptance: plan and apply in a clean project, then greenfield Helmfile validation and install |
| 2 | AWS | VPC, private subnets, egress, load-balancer addresses; EKS with CPU/GPU node groups; IRSA/Pod Identity; RDS PostgreSQL with extension bootstrap; S3; Route 53; Secrets Manager; a generated Helmfile overlay. It "must preserve the logical output contract rather than copying GCP resource vocabulary" |
| 3 | Azure | VNet, subnets, egress, Gateway address; AKS CPU/GPU pools; Azure Workload Identity; Azure Database for PostgreSQL; Blob; Azure DNS; Key Vault; a Helmfile overlay |
| Convergence | All | A shared environment-output schema and validation suite |

The production cluster's infrastructure lives in `openworkflow-k8s-setup` (OpenTofu, `Makefile`).
New clusters use the platform's `k8s-infrastructure/opentofu/gcp`. When translating a proven
configuration into a new one, start from full parity with the reference and drop items only for a
verified reason. Always write every value explicitly in `.tfvars` files, even when it equals the
default ("We always override to be clear").

## 6. Multi-tenancy and data placement

### 6.1 Tenant identity

- A tenant is identified by a DID: `did:web:<alias>.kriyagentic.com` (in general,
  `did:web:<alias>.<domain>`).
- **`TenantId` is the only tenant reference that flows anywhere**: identity, authorization,
  commands, events, wire formats, workflows and workers. "TenantId is correct - so the database name
  never gets shunted around." The database name never leaves persistence and migration code.
  Workflows read `$runtime.metadata.tenantId`; workers read `TENANT_ID`.
- `TenantId` is always derived: `TenantId.forDid(did)` is `UUID.nameUUIDFromBytes(did UTF-8)`, a
  type-3 UUID. The tenant UUID is never stored or configured. Test vector:
  `tenantIdForDid("did:web:lux.kriyagentic.com") === "50b78292-e7b9-33ed-b6ad-8f9671278ff9"`.
- The tenant alias matches `[a-z][a-z0-9_-]{0,47}`. `control_plane` is reserved.
- Tenants are declared in the shared cluster file as alias plus display name; host and DID are
  derived. The tenant registry (section 6.2) is the runtime record.

### 6.2 Databases

- **"No product has a database of its own."** Tenant data lives in one PostgreSQL database per
  tenant, `forwardmeasure_<alias>` (for example `forwardmeasure_lux`), with one schema per product:
  `openworkflow`, `entity_intelligence`, `decision_intelligence`. FDS has no database. (FDE's schema
  is `decision_intelligence`; its runtime role is `decision_engine`.)
- **Tenant databases are always PostgreSQL,** in every deployment profile. The `pekko-cassandra`
  profile moves only the Pekko event journal to Cassandra; published definitions, the
  compiled-plan store and all tenant tables stay in PostgreSQL. Never ask whether a tenant-scoped
  feature works on Cassandra.
- **One control-plane database,** `forwardmeasure_control_plane` (shared key
  `platform.databases.controlPlane`), holds `tenant_registry` (tenant to database mapping, with
  tenant ID, DID, alias, database name, status, cell, timestamps) and Pekko's cluster-wide
  coordinator store.
- **Two connections per server:** a fixed control-plane URL for registry lookups and Hibernate
  bootstrap, and per-tenant connections built at request time from a host/port prefix plus the
  database name `TenantDatabaseResolver` finds in `tenant_registry`. "Tenant database names never
  appear in configuration."
- **Request routing:** the verified organization's tenant, then `tenant_registry`, then
  `forwardmeasure_<alias>`. Never accept caller-chosen database or schema names, and never let
  workflow input override the tenant.
- Only FOWF's `openworkflow-migrations` Job writes `tenant_registry`; other products' migration
  Jobs deliberately do not.
- Earlier designs used a PostgreSQL schema per tenant (`SET search_path`) and, later, a
  hex-named database per tenant. The current design is database per tenant, alias-named, with
  product schemas inside. Where shared libraries still contain schema-per-tenant code is an open
  question.

### 6.3 Database roles and ownership

| What | Created by | Password set by |
|---|---|---|
| `postgres` database and user | Cloud SQL | unused |
| `forwardmeasure_admin` | OpenTofu | OpenTofu (generated, in Secret Manager) |
| Databases `forwardmeasure_control_plane`, `keycloak`, `superset`, `nominatim`, `kriyagentic` | OpenTofu | not applicable |
| Component users `keycloak`, `superset`, `nominatim`, `kriyagentic` | OpenTofu | OpenTofu, from `TF_VAR_cloudsql_user_passwords` |
| Tenant databases `forwardmeasure_<alias>` | Migration Jobs, as `forwardmeasure_admin` | not applicable |
| Product runtime roles `openworkflow`, `entityintelligence`, `decision_engine` | Migration Jobs | Migration Jobs |

- `forwardmeasure_admin` (formerly `openworkflow_migration`) is the one admin every product's
  migration Job and FOWF's tenant administration connect as, and it owns every tenant database.
- Provisioning keeps each tenant database owned by the admin role (reclaiming it if an earlier run
  gave it away), revokes `PUBLIC` access and grants each product's runtime role only
  `CONNECT, TEMPORARY`. Each migration Job grants `CONNECT` only to its own runtime role.
- The runtime role gets `SELECT` on `tenant_registry`, granted by the migration code, never by hand.
- **One rule in both OpenTofu roots:** "Terraform creates the login role of every platform component
  that owns its own database (`keycloak`, `superset`, later e.g. Pinot) via the same `cloudsql_users`
  + write-only `cloudsql_user_passwords` variables; the migration Jobs own product runtime roles
  (`openworkflow`, `entityintelligence`, `decision_engine`) and nothing else." This also makes
  Keycloak's role exist before Keycloak installs. "Migration Jobs never touch component users'
  passwords or permissions."
- Cloud SQL users use `deletion_policy = ABANDON`, so removing a user from OpenTofu never drops the
  live role.
- **Accepted trade-off** (owner decision): Cloud SQL makes every API-created user a
  `cloudsqlsuperuser` member with `CREATEROLE`/`CREATEDB`, so component roles are admin-equivalent.
  This is kept as a consistent pattern until it proves insufficient; the stricter alternative is IAM
  database users plus a grant-only bootstrap Job.

### 6.4 Environment variable names

`<P>` is `OPENWORKFLOW`, `ENTITY_INTELLIGENCE` or `DECISION_ENGINE`. Old names have no fallback.

| Name | Meaning |
|---|---|
| `<P>_CONTROL_PLANE_DATABASE_URL` | Control-plane JDBC URL |
| `<P>_TENANT_DATABASE_HOST` / `_PORT` | Where tenant databases live |
| `<P>_RUNTIME_DATABASE_USERNAME` / `_PASSWORD` | Runtime role, used by servers for both connections |
| `<P>_ADMIN_DATABASE_USERNAME` / `_PASSWORD` | Admin role, used by migration Jobs and FOWF tenant administration |

Runtime role names are shared contracts (`platform.databases.runtimeRoles`). FEI uses the
`ENTITY_INTELLIGENCE_*` spelling; the retired `ENTITYINTELLIGENCE_*` spelling must not be used. FEI's
`ENTITY_INTELLIGENCE_INGESTION_*` settings from the ingestion repair need reconciling with `ENTITY_INTELLIGENCE_*`:
"do not silently rename shipped settings". Nothing compiles these string keys, so guard against
drift with tests.

### 6.5 Tenant provisioning

- `POST /v1/tenants` (FOWF tenant administration) creates or reconciles the Keycloak Organization,
  the tenant database, the registry row, capability-pack memberships and workflow bundles.
- Organization, capability-policy and membership reconciliation is identical for bootstrap tenants
  and API-created tenants.
- **FEI onboarding is two steps.** The FOWF tenant API does not run FEI's Liquibase changelog; FEI's
  migration step must run before any FEI data operation. "The FOWF API's ACTIVE result does not
  assert FEI schema readiness." "Do not describe automatic FEI schema provisioning as implemented."
  "Do not make FOWF depend on FEI's current migration artifact merely to hide the gap: that artifact
  contains product and test-container coupling that needs deliberate separation first." Whether
  automatic FEI schema provisioning should become an extension is an open question.
- Existing ACTIVE tenants are reconciled by repeating the authenticated tenant-creation request with
  the same alias and display name. Enumerate tenants from the registry, never from Keycloak
  Organizations, and never reactivate deleted tenants this way.
- Some per-tenant configuration (redirect URIs, certificate SANs, per-tenant secrets, the dashboard
  tenant list) is still Helm-only. The intended end state moves it to the tenant API and reconciler
  (section 14.6, Option B).
- Never reset tenant data to make an integration work.

### 6.6 Scale

Per-tenant connection pools multiplied by pods and products against one Cloud SQL instance "will
need small pools plus PgBouncer/managed pooling as tenant count grows".

## 7. Identity, authorization and tenant membership

### 7.1 Identity platform

- One Keycloak realm, `forwardmeasure`. Each tenant is a Keycloak Organization with attribute
  `forwardmeasure.tenant-did`. No realm per tenant.
- One platform-branded sign-in page serves every tenant; tenant branding applies from sign-in
  onwards (section 14.6).
- Authorization decisions use AuthZEN with Keycloak Authorization Services. The policy provider is
  `organization-role` with organization-scoped membership.
- Tenant role groups are carried on two clients: `openworkflow`, which the AuthZEN
  `organization-role` policy checks, and the organization client `forwardmeasure-public`, which
  services read from the token. Both must be mapped.

### 7.2 Identity rules

- "Tenant and actor identity come from verified authentication or a cryptographically bound
  internal workload identity. User-supplied tenant/actor headers are never authoritative."
- "Public APIs infer the tenant from the authenticated principal. Tenant identifiers are not
  editable business form fields. Every query and mutation is tenant-confined."
- The trusted active organization determines the tenant. The authenticated caller is separate from
  the authorized asserted subject (`subjectActor`); the caller's organization controls tenancy. When
  a launcher adopts `subjectActor`, it uses `launcherRole: workflow-operator` and the assert-subject
  capability.
- Merged top-level realm or client roles do not authorize tenant operations. Realm or user role
  grants "can make tests pass while bypassing the membership boundary".
- Internal calls use a workload JWT with "client, audience, role, tenant, actor, actor type, issuer
  and subject" verified. This replaced a shared-secret header that allowed identity substitution.
- The expected token issuer is distinct from container-reachable JWKS or PDP URLs. No
  `issuer=any`, and no test-only validator.
- Fail closed when the policy decision point is unavailable.
- Revocation is not automatic. "Existing tokens and authorization decision caches are not instantly
  invalidated by editing a YAML file." Explicit administrative revocation is required.
- "An OAuth client used to evaluate AuthZEN policy is not automatically an application caller with
  organization membership." AuthZEN evaluator credentials remain separate from the subjects they
  evaluate.
- Hostname-based tenant lookup is "Display only, never authority". After sign-in every application
  checks that the token's `forwardmeasure.tenant-did` equals the tenant it resolved. "APIs keep
  taking the tenant only from the verified token."
- Context (JWT, organization, actor) survives HTTP, Kafka, actor messages, workers, callbacks and
  async computation, without carrying an `EntityManager` or open transaction across threads.
- Documents and model output are untrusted data, never instructions authorizing tools,
  cross-tenant reads or actions.

### 7.3 Service clients and tenant membership

The design (2026-10-05) has these principles:

- **D1, central clients and tenant-owned memberships.** Keycloak bootstrap creates clients and has
  no tenant dependency.
- **D2, a shared map.** One shared capability-pack ConfigMap is used by bootstrap and by runtime
  tenant administration.
- **D3, inventory validation** instead of one client per tenant. The one-tenant-bound-client-per-
  tenant preflight is removed; the `openworkflow` resource server is shared; legacy first-tenant
  claims do not define authorization.
- **D4, OpenTofu owns client secrets.** "Rotation is a maintenance operation, not a zero-downtime
  guarantee."
- **D5, additive memberships.** "Removing an entry does not revoke an existing grant." "Do not
  describe an additive bootstrap as continuous desired state reconciliation."
- Rejected: deferring FEI's capability pack; publisher membership hard-coded in Java; dynamic
  Kubernetes pack discovery.
- "Reconciliation is restartable, not transactional across Keycloak and PostgreSQL" and "does not
  report cross-system atomicity or invent rollback".

**Client inventory.** The platform owns a nine-client inventory under
`platform.identity.serviceClients` (enabled, claim and authorization flags, and workload tenant
roles). Resource servers: `openworkflow`, `datastreaming`, `entityintelligence`. A platform-operator
machine identity is retained.

**Capability packs.**

- A pack names its resource-server client. Configuration applies only to registered, entitled
  tenants.
- Members name exactly one existing username or service-account client ID, a non-empty role set,
  and optional aliases. Roles must exist in the pack. No arbitrary usernames are created.
- Packs support role- or resource-specific grants (role, then resource, then scopes); global scope
  grants remain for existing FOWF packs.
- The selected packs (FOWF, FDS, FEI) are "cluster-wide product entitlements… There is no
  per-request product-entitlement selector." This is not a per-tenant licensing subsystem.
- FDS owns its pack beside its launcher authorization vocabulary, because its launcher performs its
  own AuthZEN checks against `datastreaming`. The pack declares both run resources and all six
  authorization actions.
- FEI policy comes from its grants manifest; service-account memberships come from the central
  inventory's tenant roles.
- "The publisher's role must be declared; the special Java membership mutation is gone."
- Human administrator membership is restricted to explicit aliases and defaults to the first
  configured tenant. All-tenant access is explicit (`administratorAllTenants: true`). Workload and
  human membership policies are not conflated. Human grants outside platform ownership are
  preserved.
- Membership reconciliation is additive.
- The pack ConfigMap (`openworkflow-tenant-capability-packs`) is mounted by the bootstrap Job and the
  tenant administration service, with a content-hash annotation that triggers reload. The existing
  external pack-directory mechanism is used rather than Kubernetes discovery.
- Keycloak bootstrap creates clients first, then groups and policies, then members. It accepts both
  boolean forms for `authorizationServicesEnabled` and does not require Organizations to exist.

**Tenant-bound client credentials (`TenantClientCredentials`).**

- Multi-tenant callers request `organization:<alias>`. "An unscoped or all-organizations token is
  not a substitute."
- It discovers membership with `organization:*` at the token endpoint, maps the DID-derived
  `TenantId` to exactly one alias, then requests `organization:<alias>`.
- "The discovery token is never sent to an application API."
- Each token "must contain exactly one Organization with the expected tenant DID"; otherwise it
  fails closed.
- "No incoming unverified JWT is used for selection."
- Caches are bounded and keyed by `TenantId` (256 tenants), with fresh discovery on a miss. FDS
  generated execution clients and FEI definition resolvers are tenant-bound; workflow-ID caches never
  cross tenants.
- Worker callback credentials, including the Spark driver's, are bound to the Job's asserted tenant.
- One publisher identity serves every tenant, because client credentials with
  `scope=organization:<alias>` yield exactly that organization. It needs membership of each tenant's
  organization and its role group.

## 8. Data and API contracts

### 8.1 Ownership

The owner approved (2026-10-08) "one authoritative owner per contract across FDS, FEI and FOWF,
with explicit versions. Shared provider data definitions belong in a dependency-neutral artifact;
product workflow/API contracts remain product-owned. Generated Java/TypeScript and admitted
snapshots are derived artifacts, not independent sources to edit."

| Contract family | Authoritative owner | Consumers |
|---|---|---|
| Shared RFC 9457 `Problem` and `Violation` | `forwardmeasure-platform-api-contracts` (`META-INF/forwardmeasure/openapi/problem-v1.json`) | Platform error handlers; FEI, FDS and FOWF API snapshots and bindings |
| Provider source and mapping definitions, canonical screening destination, index mappings | `forwardmeasure-platform-data-contracts` (`src/main/resources/contracts/`, indexed by `contracts/catalog.json`) | FEI through worker-common; FDS source-compatibility tests; generic runtime engines consume admitted mapping inputs |
| FDS ingestion contracts: `IngestionSpec`, launcher API, worker AsyncAPI, merge policy, ingestion and stop workflows | `forwardmeasure-data-streaming-api` | FDS bindings and FOWF operation/workflow publication; "no vendor-specific replacement schema" |
| FEI public API, domain value types and worker AsyncAPI | `forwardmeasure-entity-intelligence-api-specifications` | "Generated clients/models and three framework endpoints" |
| FOWF definition, execution, engine, human-task and tenant APIs; Studio canvas `workflow-schema.json` | `openworkflow-api-specifications` (`META-INF/openapi`) | "Generated bindings and workflow publication" |
| FEI workflow definitions | `forwardmeasure-entity-intelligence-workflows` (`definitions`) | Published FOWF bundle |
| FDE `.proto` contracts | `decision-engine-api-specifications` (`META-INF/proto`) | Generated Java, Python and TypeScript gRPC bindings |
| Historical flat-target vendor examples | FDS `test-fixtures`; FEI worker legacy test resources | "Explicit compatibility tests; not alternatives to the canonical destination" |

### 8.2 Dependency direction

- FEI worker-common and FDS shared-fixture tests depend on `forwardmeasure-platform-data-contracts`.
  "The resource-only artifact has no FEI, FDS or FOWF dependencies." The platform core BOM manages
  its version.
- FOWF owns no provider schemas. "Adding a provider-JAR dependency to FOWF merely to imply
  participation would create unnecessary coupling." Engines receive arguments and contract
  references through publication.
- "Original resource licensing is preserved explicitly; moving files does not relicense them."
- Classpath resource URIs are kept stable when resources move.
- Verify both the source checkout and the packaged dependency bytes. Test fixtures that assemble HTTP
  resources read the authoritative platform checkout, and packaging regressions compare bytes with the
  dependency artifact, so a stale jar fails.

### 8.3 The shared error contract (RFC 9457)

- RFC 9457 `Problem`/`Violation` is the platform's error shape. Older error shapes
  (`ErrorResponse`, `DetailedErrorResponse`) are being replaced; FDS has already moved.
- `forwardmeasure-platform-api-contracts` "generates `Problem` and `Violation` once, with no product
  dependency". The Java package `com.forwardmeasure.openworkflow.common.model` is retained for
  compatibility; the Maven artifact, schema ownership and generated helper package are
  platform-owned. FOWF common-models now generates only `Paging` and `ActorReference`.
- Product OpenAPI documents contain **generated inline snapshots** of the shared components,
  "because the current generators need local components to preserve existing cross-file names and
  client mappings".
  - "Edit only the platform JSON, then run `python3 scripts/sync-shared-api-components.py` from this
    repository. `--check` detects stale snapshots without writing."
  - The script converts nullable unions to `nullable: true` for FDS's OpenAPI 3.0 document. "FEI and
    FOWF retain OpenAPI 3.1 unions." Do not impose a platform-wide dialect upgrade.
  - Each product's `SharedProblemSchemaTest` compares the published resource with the dependency
    artifact; FOWF's ownership test rejects duplicate generated classes. "schema equality alone does
    not prove runtime wiring."
  - "Generated Java must never be patched manually."
- FDS publishes `correlation_id` and structured `violations`. "Nullable optional evidence is allowed
  without weakening required fields or status/type validation."
- Shared request handling lives in platform libraries with thin framework bindings:
  `forwardmeasure-platform-server-jaxrs`, `forwardmeasure-platform-micronaut-validation`,
  `forwardmeasure-platform-spring-security`, `forwardmeasure-platform-quarkus-security`.
- Error statuses (400, 401, 403, 404, 409, 412, 413, 415, 422, 429, 500, 503) are checked against
  each declared contract, not a blanket policy. No duplicates, and no leaked exception or credential
  detail.

### 8.4 HTTP security behaviour ("401 everywhere", owner-approved 2026-10-01)

| Request | Response |
|---|---|
| No token, any protected path, including unknown paths | 401 Problem with a `Bearer` challenge |
| Invalid token, any path | 401 |
| Valid token, unknown path | 404 Problem |
| Designated health endpoints | Open |

FDE has a separate gRPC status contract. Each framework's mechanism is in section 20.4.

### 8.5 Provider data contracts

Canonical resources in `forwardmeasure-platform-data-contracts/src/main/resources/contracts/`
(catalogue status strings in `catalog.json` are dated evidence):

- `worldcheck-source-schema-v1.yaml` (WorldCheck source schema);
- `worldcheck-opensearch-mapping-v1.yaml` (despite the name, an FDS `TransformSpec`, not an index
  body);
- `screening-document-schema-v1.yaml` (the shared, versioned screening document);
- `customer-master-source-schema-v1.yaml`;
- `customer-master-resolved-entity-mapping-v1.yaml`;
- `resolved-entity-record-schema-v1.yaml` (the resolution target, extracted from the customer-master
  contract);
- `opensearch/reference-population-index-mapping.json` (the standard index body) and other
  `opensearch/` mappings.

Earlier JSON names (`worldcheck-source-v1.json`, `worldcheck-mapping-v1.json`,
`screening-document-v1.json`, `customer-master-source-v1.yaml`,
`customer-master-resolution-mapping-v1.yaml`, `opensearch/worldcheck-index-v1.json`)
were converted to the YAML names above.

The shared versioned screening shape is selected and "must not be reopened". All unmapped provider
fields go under `source_data`; only explicitly mapped extra fields are searchable; stored-only fields
round-trip through `_source`.

### 8.6 Workflow portability and resource resolution

- "Immutable workflow definitions identify logical operations and immutable contract resources.
  Deployment-specific service addresses, credentials, namespaces and storage coordinates are
  resolved at an authorized adapter boundary." Definitions use "logical immutable contract
  coordinates and deployment bindings own physical endpoints".
- **Workflow resources are addressed by DID URL:** `did:<method>:bundle:<domain>/<file>`.
  - The platform DID method lives in exactly one place, `platform.identity.didMethod` (`fwm`).
    Services receive it as `FORWARDMEASURE_DID_METHOD`, with no default anywhere. Tests set it to
    `test`.
  - Definitions write `endpoint: "@BUNDLE_DID@/<file>"` (quoted); the publisher renders the token, so
    neither the method nor the domain appears in a definition.
  - `DidUrl` (RFC 3986 relative references) and `PlatformDids` (bundle and entity DIDs) live in
    `forwardmeasure-jpa-tenancy`.
  - Entity DIDs are `did:<method>:entity:<tenantId>:<uuid>`.
- **API documents go to the Apicurio registry** before definitions are published, and definitions
  name them only by bundle DID URL. Definition management dereferences the DID through the registry,
  choosing the `apicurio-registry-reader` credential itself; a definition carries no registry URL or
  authentication block (the registry-URL-plus-`authentication.oidc.use` form is superseded,
  2026-10-03). How FOWF resolves bundle documents — allowlists, secrets, size and deadline bounds,
  digest provenance — is in the
  [FOWF vision §6](../../forwardmeasure-openworkflow/docs/architecture-design.md#6-did-resources-and-portable-workflow-bundles).
- External resources are resolved and pinned during validation and publication; there is no silent
  runtime fetch.

### 8.7 Typed values

Keep explicit boundaries between API DTOs, domain values, entities and durable events. Dates,
time zones, durations, null versus missing, enums, arrays and maps, numbers and precision, and
Jackson/Serde behaviour are checked on every framework. No silent truncation, coercion or loss.
Never expose JPA entities or engine events as public DTOs.

## 9. Eventing, correlation and causality

### 9.1 CloudEvents and lifecycle events

- FOWF emits workflow lifecycle CloudEvents and accepts CloudEvents through its event ingress.
- **Externally visible engine behaviour is one shared implementation** (owner decision,
  2026-10-13): lifecycle CloudEvent shapes, CloudEvent publisher transports and configuration, error
  JSON, error type URIs and durable data placement. Each engine supplies only the mapping from its
  own internal events. Separate per-engine copies drift silently. This applies to every product with
  more than one engine (section 21).
- Effects, including outgoing events, happen only after durable intent is recorded. Duplicate
  observations do not apply twice.
- CloudEvent and scheduled starts obey the same input limits and overflow rules as API starts.
- FOWF's implementation (shared modules, `openworkflow.cloud-events.*` and `openworkflow.eventing.*`
  settings, topics, closed loop, known gaps) is in
  [FOWF vision §12](../../forwardmeasure-openworkflow/docs/architecture-design.md#12-common-engine-semantics-and-recovery)
  (requirement ENG-07 and §12.1) and
  [FOWF implementation status §5](../../forwardmeasure-openworkflow/docs/implementation-status.md#5-execution-data-and-projection-state).

### 9.2 Correlation and tracing

- "Business correlation, causation and W3C tracing are distinct. `traceparent` is never used as a
  business correlation identifier. A root execution without upstream correlation uses its execution
  ID; children preserve the intended causal chain."
- "Default root/control correlation to execution ID, derive stable CloudEvent correlation when
  absent, preserve explicit upstream and causal child correlation."
- A `BusinessCorrelationId` type has explicit correlated and uncorrelated factories.
- All identities (causation, command, event, execution, branch, child, correlation) are preserved
  end to end.

### 9.3 Asynchronous contracts

- AsyncAPI documents describe worker contracts (for example FEI's
  `ingestion-worker-kubernetes-job.yaml`, FDS `ingestion-job.yaml` and `ingestion-deployment.yaml`).
- Kafka topic ownership is explicit; tests never pre-create production-owned topics.
- Outbox: "All claimants of a shared outbox must be upgraded as one compatibility set." A producer
  uses `acks=all`, idempotence, bounded synchronous acknowledgement, event keys and invocation
  headers, and closes at shutdown.
- Command, event and snapshot formats are versioned with manifests and golden fixtures; unknown
  versions fail explicitly. New wire types append golden payloads "without replacing previously
  established bytes".

## 10. OpenWorkflow behaviour

FOWF's required behaviour on both engines and all three frameworks is specified, with requirement
IDs, in the [FOWF technical and functional vision](../../forwardmeasure-openworkflow/docs/architecture-design.md).
The objective is "full parity of the required observable contract, not identical internal
implementations". This section is a dated summary (2026-10-13) of what other products rely on.

- **Definitions and publication** ([FOWF vision §4–§6](../../forwardmeasure-openworkflow/docs/architecture-design.md#4-workflow-language-compiler-and-deterministic-publication)):
  lifecycle `DRAFT → PUBLISHED → DEPRECATED`; maker-checker only for human-in-the-loop tasks (owner,
  2026-10-04), so FEI reference-population approval runs as a FOWF human task and the publisher uses
  one identity. At most one PUBLISHED revision per workflow, enforced by a unique index; definition
  management deprecates the previous revision atomically when publishing its replacement.
- **Publishing by name** (owner decision, 2026-10-01; contract that FEI, FDS and FDE consume): look
  workflows up by `GET /v1/workflows?name=<domain>-<name>`, never by a configured workflow ID (no
  `*_WORKFLOW_ID` settings); names are `<domain moniker>-<name>`; one generic FOWF publisher seeds
  every entitled tenant eagerly from each product's `openworkflow-workflow-bundle` release, also on
  tenant creation. Detail: [FOWF vision §5–§6](../../forwardmeasure-openworkflow/docs/architecture-design.md#5-definition-authoring-and-lifecycle).
- **Starting and controlling runs** ([FOWF vision §11](../../forwardmeasure-openworkflow/docs/architecture-design.md#11-durable-admission-controls-and-canonical-query)):
  `POST /v1/workflow-executions` with a required `Idempotency-Key` and a body of `revisionId`,
  `input` and optional `subjectActor`; execution IDs are random; `202 Accepted` acknowledges
  admission only, and "HTTP 202, producer acknowledgement and an accepted Job are not terminal-success
  evidence". Pause, resume and cancel follow the same contract.
- **Unknown outcomes, recovery, timers and ordering** ([FOWF vision §12](../../forwardmeasure-openworkflow/docs/architecture-design.md#12-common-engine-semantics-and-recovery)):
  ambiguous remote acceptance without trustworthy deduplication fails with `OPERATION_OUTCOME_UNKNOWN`;
  "Do not reset deadlines on restart"; "Do not claim generic exactly-once external effects."
- **Payload limits and overflow** ([FOWF vision §16](../../forwardmeasure-openworkflow/docs/architecture-design.md#16-data-references-limits-and-object-storage)):
  one durable placement rule for both engines (inline only within the offload threshold and
  32 KiB, inclusive; larger values in shared object storage), tested at 32,767 / 32,768 / 32,769
  serialized bytes, covering every start path including CloudEvent and scheduled starts.
- **Adapters, egress, subworkflows, human tasks and native durability**: FOWF vision
  [§13–§15](../../forwardmeasure-openworkflow/docs/architecture-design.md#13-pekko-native-execution-and-persistence)
  and [§17](../../forwardmeasure-openworkflow/docs/architecture-design.md#17-human-task-functional-contract).
  "External I/O executes only in adapters or workers and returns observations; workers never advance
  the graph."
- **Historical OKS runtime design** (August 2026, background only) is kept in
  [FOWF vision §24.1](../../forwardmeasure-openworkflow/docs/architecture-design.md#241-historical-runtime-design-background-only).

Current FOWF status and open work: [FOWF implementation status](../../forwardmeasure-openworkflow/docs/implementation-status.md).

## 11. Entity Intelligence architecture

### 11.1 Boundaries

- **FOWF owns long-running FEI orchestration.** "Do not add a Studio-owned background workflow or a
  parallel scheduler. Persist durable identity and correlate asynchronous completion/failure. Large
  documents belong in object storage, not oversized workflow input JSON."
- **Workers do not access FEI tenant databases.** Dispatched workers call FEI REST APIs with scoped
  workload identities, through generated clients. No JDBC URLs, credentials, drivers or
  `forwardmeasure-jpa` in worker paths. Exceptions: FDS source connectors and direct OpenSearch
  indexing.
- **Interaction pattern:** the FEI backend starts runs through FOWF's REST API; workflow tasks are
  `call: openapi` back into FEI's own endpoints; heavy bulk work runs as a durable external operation
  (a Kubernetes Job or Spark job) correlated back to the workflow over an AsyncAPI/Kafka contract.
- FEI publishes its workflows through the shared bundle publisher with moniker `fei` and the
  configured DID method. `fei-<name>` resolves to the published revision at launch.
- FDS supplies transformation and runtime components; FOWF transports and executes workflows. No
  engine redesign is planned for FEI work. "Any newly discovered cross-repository production change
  must be reported explicitly with its reason, not folded into an undefined wider matrix."
- Old API payloads, persisted FEI data and old images "are not a supported mixed-version rollout
  target".

### 11.2 Population model

The agreed objective: "Give a population a stable destination contract while permitting multiple
provider input formats."

**Terms.**

| Symbol | Meaning |
|---|---|
| S1 | Target document schema. "Fixed for the lifetime of the population." |
| M1 | Destination configuration; for reference populations, the OpenSearch settings and mappings. "Fixed alongside S1; not uniquely derivable from S1." |
| S2 | Provider source-record schema, with parsing metadata. "Part of an immutable version of an input definition." |
| M2 | Transformation from S2 records into S1 records. "Versioned together with S2 and explicitly bound to S1." |

**Relationships.**

- One reference population has one immutable S1/M1 pair and can register several S2/M2 pairs.
- Different reference populations may share the same target contract. "WorldCheck, OFAC and
  proprietary inputs can target the same screening shape without being combined into one dataset."
- An ingestion selects one registered input-definition version, inherits the population's target
  and pins the effective four-part contract. "Revision snapshots establish provenance and
  deterministic replay; they do not permit arbitrary changes to the population target."
- **Monitored populations:** "Their destination is FEI's resolution model. M1 is a platform-owned
  resolution/persistence binding, not a user-defined OpenSearch index mapping. Do not move
  customer-master persistence into a reference index to make the terminology symmetrical."

**Architectural qualifiers.**

1. "**Share principles, not necessarily identical machinery.**" Reference populations are indexed
   datasets with revision activation and index lifecycle. Monitored populations are memberships over
   candidates and resolved entities, backed by source records and assertion provenance. "Do not force
   both into one persistence hierarchy, revision model or deletion lifecycle… do not invent a
   configurable mapping language simply to create a literal counterpart to the OpenSearch M1
   document."
2. "**Keep implementation proportional.**" Out of scope: "A general-purpose schema registry,
   arbitrary schema-conversion platform, user-programmable persistence layer and universal
   compatibility theorem prover… Prefer existing services and supported contract constructs."
3. "**Treat lifecycle rules as essential.**" "Immutability alone does not prevent one feed from
   withdrawing another feed's membership or evidence." Membership, refresh and lifecycle rules are
   completion requirements.
4. "**Keep population identity, target contract and ingestion definitions distinct.**" "Membership
   answers whom we monitor, resolution identifies the entity, and provenance identifies evidence
   supporting assertions. None of these relationships substitutes for the others."
5. "**Use a new population for a changed S1/M1.**" Target migration "may be considered later… not
   part of this implementation, and must not be introduced implicitly."

**Immutable target contract.**

- Persist the resolved S1/M1 definitions, schema identity and version, sink kind, origin URIs and
  content digests. "A URI alone is insufficient: its content can change." Include referenced schema
  resources in the immutable admitted bundle.
- Two ways to create a population:
  - **Standard:** "resolve and freeze the platform screening schema/index pair";
  - **Custom compatible:** "require both target schema and index mapping; validate and freeze them
    together". A single custom document fails before side effects.
- The pair is frozen at creation, including for DRAFT populations. "No hidden override through
  update, trigger, configured pipeline, worker environment or fallback to a newer classpath default
  is allowed." Per-run target and index overrides are removed.
- Immutable contract records are persisted once and referenced from populations and runs. The
  portable worker snapshot is "a transport representation, not a second independently editable
  owner".
- Ownership is tenant-scoped. "Do not introduce cross-tenant mutable shared records."

**Source input definitions.**

- Named, with numbered immutable versions binding S2/M2, target identity and digest, source identity
  namespace and parsing defaults. One definition can be the population default.
- `SourceFeedProfile` "must not double as the population identity"; the binding moves to the
  input-definition level.
- "Changing a delimiter or mapper version must not accidentally create a new logical source record.
  Provider record identity is namespaced by a stable source/feed identity."
- "S2/M2 creation and updates are paired operations."

**Validation and admission.**

"Use one admission service across simple reference ingestion, configured pipelines, correlated
ingestion and monitored resolution ingestion. No alternate path may bypass target ownership."

Before dispatch:

1. Authorize the tenant, population and selected input definition.
2. Resolve the immutable target and exact input version.
3. Validate schemas, mapping targets, transforms, parsing and identity rules.
4. Validate S1/M1 compatibility for the supported vocabulary.
5. Persist the run's immutable contract references or snapshot and its idempotency state atomically.
6. Dispatch using the admitted data. "Retries recover it instead of fetching mutable documents
   again."

Further rules:

- Compatibility is "a documented supported subset": scalar, array and object shapes, nested
  mappings, numeric and date constraints, analyzers, indexing flags and dynamic-field policies.
  "Reject unsupported custom constructs with an actionable explanation." Supported cases are tested
  against real OpenSearch.
- "Vendor data stays under `source_data` by default… Preserve raw values rather than silently
  coercing or discarding them."
- Records are validated against S2 and output against S1. "A CSV source schema is not the output
  schema." Java DTO field checks are not full validation. Rejected records and indexing errors are
  reported honestly.
- "Do not permit unrelated complete target rewrites: screening currently depends on canonical
  fields. Supporting arbitrary target semantics would be a separate query architecture project."
- Configured pipelines come under the frozen-target model: every pipeline owns a fixed target,
  sources name stored input versions, and triggers copy them and cannot route past the target.
  Monitored-population pipelines use the standard resolution target. Correlated sources are ordered
  explicitly.

**Monitored membership invariants** (mandatory regardless of table names):

- A resolved entity can belong to several monitored populations within the tenant.
- Investigation-derived candidates, entities and assertions can exist without membership.
- No mandatory population foreign key on resolved entities or assertions.
- Each contributing record's assertion provenance is preserved; deduplication keeps every support.
- Membership is backed by source records. Removing one support does not remove membership while
  another is active.
- No source ID collisions across feeds or tenants. A new parser or mapping version cannot change a
  feed's identity namespace.
- Resolution, re-resolution and merges preserve or recalculate membership and support
  transactionally. "Do not create a new split feature."
- Retiring a population withdraws only that population's associations; it never cascade-deletes
  shared entities or another source's evidence.
- Historical provenance survives membership changes, subject to the retention policy.

Logical relationships: monitored population, then source-record associations, then ingested record
versions (with run, input version and source provenance) and assertion support. A source record or
candidate optionally links to a resolved entity. Population and resolved entity are linked by
membership supported by source records.

**Membership data model** (owner-approved, 2026-10-12):

- `monitored_population_membership` (population, resolved entity), unique on the pair;
- `membership_support` (membership, source record) with `added_by_run`, `withdrawn_at` and
  `withdrawn_by_run`; a membership is active while any support is active;
- `source_record`: one row per (stable feed identity, record ID), with ingested versions beneath it;
- no population column on resolved entities or assertions.

**Decisions** (owner, 2026-10-12):

1. Shared entities arise through merge or investigation resolution, not ingestion matching. Merge
   stays absorb-into-existing with the absorbed entity hard-deleted. Memberships and supports are
   carried across; duplicate (population, entity) pairs are folded with all supports kept.
2. Re-ingestion updates a record. A full refresh withdraws absent supports only after it completes
   successfully.
3. Retiring a monitored population withdraws only its memberships. Explicit entity deletion remains
   an operator hard delete.
4. Configured pipelines come under the frozen-target model.
5. Screening selects by membership and reports unresolved members. A run targets either a reference
   or a monitored population.

**Explicitly deferred** (owner decision): merging into a new entity with the originals retired and a
choice of survivor; unmerge/split; automatic merge or cross-source matching at ingestion with
explicit rules.

**Full, delta and multi-source semantics.**

- Reference full revisions inherit S1/M1 and create their own physical index. Deltas must match the
  target and identity semantics of their baseline; mismatches are rejected. A delta must keep its
  baseline's source identity namespace and identity rules.
- The reference trigger takes optional `revision_kind: full | delta` (default `full`). A delta with
  no active base returns 409. A staged READY delta owns its own query index and does not replace the
  active population until explicit activation.
- Several input definitions describing alternative formats "does not automatically implement
  combined-provider dataset management". Reference populations keep population-wide full
  replacement. "A one-provider full load must never be advertised as preserving other providers by
  accident."
- Monitored refreshes reconcile by population plus stable feed identity. "Do not infer deletion from
  absence until a supported full refresh has completed successfully." Failure, partial ingestion or
  retry never withdraws. A delta changes only the records in its scope. Unsupported modes are
  rejected, and upsert is never silently substituted for replacement.
- Rollback reuses the superseded revision's own index.
- Retention and purge are manual, keeping the last N revisions; there is no scheduled deletion.
- There is no FEI maker-checker; approval uses FOWF human tasks.
- Index names, ownership, aliases and metadata are server-controlled and scoped by tenant,
  population and revision. Reject takeover. Validate bulk item outcomes, refresh and finalization
  before READY.
- Progress is cumulative, replay-safe and accurate (converted, submitted, acknowledged, rejected).
  Rejected records are failures. Duplicate or out-of-order callbacks cannot make totals go backwards.
  Progress comes from durable state; in-memory "accumulators are fallback telemetry, not the source
  of truth".

**Screening scope.** Select members by explicit population identity, not by source key. Deduplicate
resolved entities within a run while keeping memberships and provenance. Do not silently omit
unresolved members. "Population membership is not blanket permission to expose unrelated
investigation data": carry authorization and tenant restrictions through selection, detail views and
exports.

**API direction.** Target selection at creation, with read-only target identity in views;
population-scoped input-definition registration, versions and defaults; monitored population
management and ingestion by ID; effective target and input identities on ingestion and revision
views; specified errors for missing pair, incompatibility, immutable field, cross-tenant access and
idempotency conflict. "Regenerate server interfaces/models and Java/TypeScript bindings; never
hand-edit generated Java."

### 11.3 Ingestion

- **Schema-driven, never compiled-in.** Adding a provider without recompiling Java is an explicit
  exception to the generated fixed-model rule (section 17). Parsing, mapping and destination
  validation stay runtime-contract-driven: no `TARGET_MODEL_CLASS`, no provider-specific compiled
  target classes, no reflection over generated `JSON_PROPERTY_*` constants. The owner: a target model
  "should never have been burned into Java code".
- **Source pinning and integrity** (owner decisions, 2026-10-12):
  1. Workers fetch admitted contracts from the ingestion API by revision ID and check the digest
     their Job was launched with. Contracts are not passed in the Job environment.
  2. Sources are pinned at admission (object version, CRC32C or MD5, size) and streamed from object
     storage at exactly that version, verified at end of stream. Spark stages the pinned version.
     Unversioned overwrites are refused.
  3. "Every FEI request body rejects properties its schema does not declare (400), in all
     frameworks."
- **Parsing overrides:** `source_parsing` accepts delimiter, quote and encoding for reference and
  monitored ingestion. An empty quote disables quoting; omission inherits; JSON null is not a
  disable instruction. Headers remain required. Overrides cannot alter schema or mapping, or operator
  memory or disk limits. Configured pipeline triggers use `source_parsing_overrides` keyed by the
  exact configured source URI; unknown URIs are rejected; the no-body form is kept. Changing options
  under the same idempotency key is a conflict. "Do not describe header control or parser
  resource-limit overrides as existing public options."
- **Full-file scale.** The design target is files in the hundreds of gigabytes with bounded memory
  (the immediate dataset is the full WorldCheck export, about 5.6 GB and 5.8 million rows).
  Requirements: configurable acquisition limits and disk capacity, incremental CSV/TSV parsing,
  bounded in-flight records and backpressure, 64-bit counters, streaming batches,
  cleanup/cancel/recovery, disk-full and storage errors. No whole-file `byte[]`, `String` or list
  materialization, and no silent small-file cap.
- **Encoding detection** (`SourceEncodingDetector` contract):
  1. Inspect up to 1 MiB of raw bytes through the Hadoop `FileSystem` before parsing.
  2. Honour UTF-8/16/32 byte-order marks.
  3. Validate strictly as UTF-8 with malformed and unmappable input set to report.
  4. Select UTF-8 at confidence 100 when non-ASCII content is valid UTF-8.
  5. An ASCII-only sample is ambiguous; configured metadata breaks the tie.
  6. After strict rejection, use ICU4J detection and adopt the supported detected charset under the
     confidence and fallback rules. The configured charset is used only when the sample is empty or
     ambiguous, detection is unsupported or insufficient, or inspection fails.
  7. Log configured, detected and selected charset, confidence, evidence and sample size, and pass
     the selected charset to the parser.
  Detect from raw bytes and never force a charset globally. "There is deliberately no U+FFFD
  fail-fast rule." Encoding metadata is source-specific. Committed fixtures are synthetic.
- **Provider field ledger:** all 35 WorldCheck columns and 42 Customer Master logical fields are
  accounted for.
  - WorldCheck: alias quality, date-of-birth precision, the entity-indicator precedence, brace-
    qualified and masked identifiers, country and location, update categories.
  - Customer Master: duplicate trailing headers (blank last column wins), provider-scoped identifier
    keys, person versus organization dates, unknown-type rejection, address roles, typed raw
    assertions, typed dates, identifiers and addresses, source-order independence, scalar trust
    precedence, and union and deduplication of identifiers, aliases and assertions.
  - Recorded limitation: "Raw named-field retention is not physical preservation of duplicate column
    positions".
  - A destination or index shape change needs a fresh full revision.
- Verify indexed record counts with `_count` (root documents); an index's `docs.count` also counts
  nested Lucene documents and is not evidence of duplicates.
- **WorldCheck entity kind.** The `E/I` column is overloaded: `M`, `F` and `U` encode an
  individual's gender (legacy `I` means individual) and `E` means non-person. `CATEGORY` is unsafe
  because some categories mix people and organisations. Rules:
  1. `M`/`F`/`U`/`I` map to PERSON regardless of category.
  2. `E` maps to non-person: vessel or aircraft to PHYSICAL_ASSET, otherwise ORGANIZATION.
  3. A missing or unknown flag uses only unambiguous categories; mixed categories map to UNKNOWN.
  4. The mapping declares the `E/I` source under `transform_sources.entity_indicator`; no converter
     hard-codes the column. Named transforms in governed deterministic mappings support this.
  A change to this mapping requires full re-ingestion.
- **Spark runtime:** the driver and executors use one immutable, identical runtime tuple (Spark
  4.2.0, Scala 2.13, Java 25, a compatible Netty family). The executor image build verifies the Java
  version and the exact Spark jars, rejects duplicates and excludes Spark-owned Netty from shading.
  "Prefer immutable digests and verify Kubernetes `imageID`, not merely the requested mutable tag."
- **GCS:** "enforce dependency convergence and test the actual GCS code path". The GCS connector
  block size is set as a decimal byte count (`hadoop.gcs.block-size-bytes`, default 67108864),
  because the connector reads it as a long.
- **Spark is for multi-source correlation and aggregation,** which is a genuine shuffle problem.
  Single-source ingestion and per-subject screening recall do not need Spark.

### 11.4 Screening

**Architecture principles.**

- **Recall and scoring are separate contracts.** "Do not make OpenSearch tuning part of the composite
  scoring-policy override… If recall parameters become configurable, version and validate them
  separately and persist the effective recall-policy snapshot with the run."
- **One normative query builder** or one normative query specification, with parity enforced across
  the single-entity and population paths.
- **Signal extraction belongs at the typed-value boundary,** "not in a source-specific mapping
  workaround". One typed-assertion extractor projects canonical, full and legal names (with
  given-plus-family fallback), all date-of-birth and incorporation dates, all nationality,
  citizenship, country and incorporation-country values, every address country and every
  identifier. Do not hard-code one assertion container key.
- **Entity-kind compatibility is a "non-negotiable precision invariant".** Known person versus
  organization or trust is a hard gate in both recall (`must_not`) and Java scoring. Organization and
  trust form one group. `UNKNOWN` goes through the fallback policy. Zero known person-to-organization
  hits is the gate.
- **Stable score scale.** The composite denominator is the complete configured weight sum; missing
  evidence contributes zero. "Unavailable" is modelled separately from "conflicting".
- **Implausible data is unavailable evidence, not a conflict.** A person's date of birth is plausible
  if it is not null, not in the future (UTC) and not earlier than today minus 125 years. If either
  side has none, the evidence is unavailable, carries no penalty and cannot trigger the hard
  date-conflict gate. Organisation incorporation dates are unaffected.
- **Durable policy snapshot.** Persist the effective scoring policy and `scoring_policy_version` with
  each run.
- **One canonical durable payload boundary,** tested against every representation the stack
  actually produces.
- **No runtime network dependency for reference lexicons.** The GLEIF entity legal form list is
  packaged on the classpath as a pinned, checksummed resource with metadata, overrides and an updater
  that fetches only the pinned URL.

**Name handling and recall.**

- Organisation names use a discriminative form for ORGANIZATION and TRUST only: strip
  parentheticals, titles, particles, organisation noise and a separate structure-noise set, branch
  and office descriptors, and trailing legal forms from the GLEIF lexicon (suffix-only, never erasing
  the whole name); fold diacritics; keep a non-blank fallback. Person normalisation is unchanged. The
  scoring gates use the same representation.
- Organisation identity words stay. Fund, trust, asset-management, portfolio, equity, commodity,
  index and income terms are never discarded.
- Identifier aliases are canonicalised (for example `INT-LEI`, `LEI@GLEIF`, `INT_LEI` to `LEI`;
  `AIC`, `SG_BRN` to `SG-BRN`; `US_EIN`, `EIN` to `US-TIN`; `UK_CRN`, `CRN`, `*-RCS`, `*-KRS` to
  company registration). Values are uppercased with whitespace and hyphens removed. Recall issues a
  `terms` clause over all indexed aliases, so no reindex is needed. Exact-value recall may omit the
  scheme, but scoring still requires type plus value.
- Fuzzy recall is an additive guarded clause in one implementation.
- Blank textual names are rejected.
- Generic two-token containment is disabled. Use kind-specific rules instead.
- Person completion uses `FAMILY, given names`, with role-aware anchors and middle names; a bare
  first or family completion is allowed only when exactly one recalled candidate has the anchor.
- Organisation containment is directional: the customer legal name may sit inside the provider name
  with at most three leading qualifier tokens; the reverse needs an exact canonical company
  registration ID.
- Branches: an exact external ID wins and suppresses weaker parents; a parenthetical branch matches
  its exact parent root; a non-parenthetical branch matches when the provider city or state appears
  in the customer name; branch-country disagreement is unavailable evidence.
- Further controls: query-side splitting of known joined particles; an additive single-token clause
  for identity tokens of 15 or more characters; organisation singular/plural equivalence; an explicit
  legal-form conflict gate; a 0.93 name floor when a long exact brand token and the provider
  jurisdiction both appear in the customer name (falling back to the exact normalised location
  country name when no country code exists); person-only corroborated transliteration overlap (two
  aligned tokens, at least one literal exact).
- Positive geography corroborates only after the name clears the threshold; conflicting geography is
  negative.
- Candidate recall is capped at 128 per subject. "Do not lower it without a recall@K curve."
  "Candidate volume and recall@K must be measured before broadening" recall.

**Scoring policy override.** An optional request-scoped `scoring_policy_override` adjusts name
weights (Jaro-Winkler, Levenshtein, Double Metaphone, token-sort ratio, token-set ratio, fuzzy score)
and composite weights for persons (name, date of birth, nationality, identifier) and organisations
(name, incorporation date, country, identifier). The defaults are: name weights Jaro-Winkler 0.28,
Levenshtein 0.13, Double Metaphone 0.14, token-sort 0.22, token-set 0.17, fuzzy 0.06; person
composite name 0.70, date of birth 0.15, nationality 0.06, identifier 0.09; organisation composite
name 0.70, incorporation date 0.10, country 0.10, identifier 0.10. Invariants: reject unknown, invalid, non-finite,
out-of-range, all-zero or wrongly normalised weights; partial overrides inherit defaults; name and
composite weights stay separate; public terms are `person` and `organization`; durable fields are
snake-case; the effective policy and version are persisted and visible in progress and results.

**OpenSearch resilience.** Retry 3 attempts with a 200 ms exponential base for statuses 429, 500,
502, 503 and 504 and transport I/O errors; never retry ordinary 4xx; restore the interrupt flag.
OpenSearch CPU is sized "from load tests; do not copy blindly". Future performance work is measured
first: per-query latency and candidate distributions, branch contribution and cost, `_msearch` with
bounded concurrency, deduplication by full query signature plus index revision, purpose-built fields,
a compact recall-only index, person/organisation index separation, shards, routing, replicas, heap and
CPU, and top-K sensitivity.

**HTTP semantics for tasks and runs.** "POST command/submission accepted: 202 Accepted / GET existing
task/run current status: 200 OK / GET unknown task/run: 404 Not Found". "Its `PROCESSING` state
belongs in the response body and does not turn the GET into HTTP 202."

**Listing versus bulk export.**

1. Keep the interactive list projection small, with explicit expansion flags (`include_total`,
   `include_assertions`, `include_hits`, `include_raw_hit`; `hit_count` stays accurate when hits are
   omitted).
2. Use database-level DTO projections so excluded JSON is not loaded.
3. Page by keyset (`created_at DESC, id DESC`, ID as tie-breaker) with an opaque, versioned,
   URL-safe cursor; a cursor and a non-zero offset cannot be combined. Push filters (hit status,
   reference population, minimum score, has hits) into the query and batch-load per page. Back the
   order with a matching composite index.
4. Enable and verify HTTP compression for JSON and NDJSON.
5. For full-population evidence, use an asynchronous export job that writes a compressed artifact to
   object storage and returns a task plus the artifact location and checksum. "Do not rely on a
   longer synchronous HTTP timeout as the greenfield end state."
6. Consider exporting subject results and unique reference-record snapshots as separate related
   datasets.

One canonical `resolved_subject_id` and provenance assertions live at subject level.

**Quality report.** Every production screening run is reported with:

1. unique alert entities and rows from the benchmark screening system;
2. linked and absent customer-master entities;
3. linked entities present in the output;
4. hits and misses at the threshold;
5. hit and score distributions, including borderline scores;
6. what was recovered and what regressed against every prior run;
7. strong candidates for misses, found by independent raw-file search;
8. absent entities confirmed by brute-force search;
9. true identity versus incidental token overlap;
10. duplicates (benchmark rows, record IDs, customer keys, semantically duplicate hits);
11. coverage per category and per kind;
12. false-positive samples and top-K saturation;
13. evidence of the revision, index and policy used;
14. operational completeness: count, failures, skips, retries, duration, throughput, missing
    references.

Preserve inputs, scripts, commands, reports, hashes, IDs and timestamps.

**Acceptance gate for the customer-master screening benchmark.** Every benchmark record ID gets a
disposition: valid hit, valid zero-hit (no matching WorldCheck entity exists) or unscreenable
omission (absent from the customer master), with zero unexplained misses. "The 348-ID disposition is
the hard production acceptance gate." Omissions "must not silently include them in a matcher
false-negative denominator". The labelled inventory of controls (wrong kind, distinct fund versus
parent, branch, transliteration and others) is in the archived screening handoff and must be ported
as regression fixtures. Two real-data gates are required: a real-data screening regression over
real OpenSearch with the production mapping that asserts the exact accepted UID set, and a full-export
scoring backtest. "A small hand-written unit test is not an adequate substitute."

### 11.5 Resolution

- Merge is absorb-into-existing with the absorbed entity hard-deleted; memberships and supports carry
  across (section 11.2, decision 1).
- Entity deletion is an explicit operator hard delete.
- Merge has no public REST endpoint today; a managed-service merge test must not be labelled REST
  proof.
- Deferred by owner decision: merge into a new entity, unmerge/split and automatic cross-source
  matching at ingestion.

### 11.6 Information extraction

**Target journey:** "Studio investigation/evidence → authorized extraction request → durable dispatch
→ real agent/model → validated result → FEI persistence → Studio status/results →
assertions/candidates under the agreed review policy."

**Rules** (established 2026-10-12, quoted):

1. "**Contract first.** Extend canonical schemas/OpenAPI and generate models/interfaces/clients. Never
   edit generated Java/TypeScript directly. Reuse existing extraction types where they fit. Map typed
   API/domain/entity boundaries with MapStruct where appropriate."
2. "**Service boundaries.** REST/application orchestration calls domain services; repositories remain
   behind domain/persistence services. Use `forwardmeasure-jpa`, managed transactions and Criteria
   queries with generated static metamodels. Do not introduce raw SQL business logic or
   framework-specific replacement repositories."
3. "**FOWF owns long-running FEI orchestration.** … Do not add a Studio-owned background workflow or a
   parallel scheduler. Persist durable identity and correlate asynchronous completion/failure. Large
   documents belong in object storage, not oversized workflow input JSON."
4. "**Workers do not access FEI tenant databases.** Use authorized FEI APIs and scoped workload
   identities."
5. "**Shared infrastructure.** Reuse `forwardmeasure-object-storage`, existing clients, authorization,
   workflow publication and deployment mechanisms."
6. "**Security and tenancy.** Trusted organization determines tenant; real OIDC/AuthZEN governs
   admission, evidence access and result persistence. Preserve authenticated caller versus asserted
   subject. Documents/model output are untrusted data, not instructions authorizing arbitrary tools,
   cross-tenant reads or actions."
7. "**Frameworks.** FEI changes must support Quarkus, Spring and Micronaut. For workflow integration,
   account for Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra."
8. "**Persistence compatibility.** Preserve deployed migration history and admitted
   workflow/contracts. Add forward migrations if necessary. Never reset tenant data to make
   integration work."
9. "**UI ownership.** Extend the existing Maven-built FEI Studio and shared API proxy. Display real
   persisted status/results/errors. Do not add UI-only simulated extraction or expose internal
   deployment details as analyst controls."
10. "**Build/deployment.** Versions belong in the platform; preserve justified existing exceptions.
    Use shared Helm/Helmfile configuration and actual image pins."

**Existing contract and domain to build on:**

- The FEI OpenAPI document defines `InformationExtractionStatus` (accepted, processing, skipped,
  failed, completed, partially_completed), `InformationExtractionResult` discriminated by
  `result_type`, `AmlKycExtractionResult` (names, dates of birth, nationalities, identifiers,
  addresses, charges, associated entities, key dates, jurisdictions, a source summary) and
  `ExtractionError`.
- The domain has an `LlmExtraction` entity, service and repository linking an extraction to evidence
  and investigation, storing model name, prompt key and version, status, typed result, structured
  error, token counts and raw model response. "Do not create a parallel extraction-result table or
  incompatible model without a demonstrated need."
- Intended behaviour: extraction outputs become assertions and associated entities become candidate
  entities. "Evidence upload is not an implemented information-extraction pipeline."
- The model choice is open: a comment naming KServe "is not a requirement to use KServe or a
  particular model".
- The current evidence upload UI uses a documented handwritten multipart-binding exception. "Do not
  copy this exception into new ordinary JSON APIs."

**Proposed acceptance criteria** (proposed, not yet agreed):

- Invoke extraction from a real investigation in Studio.
- Durable queued, processing and terminal status with structured failure, using the existing
  statuses.
- The correct evidence bytes are read under the correct tenant; outputs stay associated with their
  evidence and investigation.
- Output is validated against the canonical schema, including malformed, empty and partial output.
- Results and provenance are displayed; model and prompt versions are preserved; no raw documents or
  model responses appear in logs.
- Assertion and candidate persistence (or the review path) is verified with independent expected
  results and committed re-reads.
- Duplicates, reruns and failure/recovery follow the idempotency policy and avoid repeated billed
  calls.
- Unauthorized and wrong-tenant access is denied before retrieval, model invocation and mutation.
- Fixtures are synthetic; authorised real evidence is used only in development environments.
- Evidence is recorded per framework and per engine.
- "Do not expand the first usable integration into every conceivable document format, agent protocol
  or UI feature."
- "A deterministic model-response fixture can prove validation/mapping, but cannot be presented as
  successful execution of the real agent/model."

### 11.7 FEI Studio

- Separate the target contract from source definitions. Default to the managed screening target; a
  custom target requires both documents. Show the target identity read-only after creation.
- Manage named source definitions and versions; preserve parsing overrides; select a definition and
  version at ingestion.
- Add and select monitored populations rather than treating the source key as the population. Show
  membership and provenance.
- "Use product language in ordinary flows; implementation hashes may live in advanced diagnostics."
- Show real progress, failure and recovery; support full and delta; allow screening of real output.
- Studio shows an em dash for unavailable counts and keeps a measured zero as zero.

## 12. Data Streaming architecture

The rules below come from the FDS architecture design (owner decisions of 2026-09-20, 2026-09-21
and 2026-10-04) and the platform audit requirements (from repository documentation where noted - to
confirm).

### 12.1 Model and planning

- **One author-facing type, `IngestionSpec`:** `sources: []`, a transform list, a destination and
  the `ExecutionMode`. `ExecutionPlan` and `ExecutionProfile` are compiled internal types; authors
  never write them. There is no separate correlation spec.
- **The author declares only the execution mode** (`BOUNDED` or `CONTINUOUS`), never an engine. The
  planner selects the delivery engine, and whether a Spark stage is needed, from the declared
  characteristics of each named transform (cardinality, state, cost, order sensitivity,
  determinism). A stateful transform selects Kafka Streams; a heavy or large-scale correlated
  transform inserts a Spark stage.
- **The capability space is 2 × 2 × 2:** source cardinality (single or correlated), execution mode
  (bounded or continuous) and delivery engine (Pekko Streams or Kafka Streams). Spark and Camel are
  not axes.
- **Exactly two permanent delivery engines,** `PekkoStreamsDeliveryEngine` and
  `KafkaStreamsDeliveryEngine`. Each branches on execution mode and owns the final write and its
  lifecycle. (This supersedes the earlier decision D4, which reserved Kafka Streams for a future
  continuous case and omitted it; from repository documentation - to confirm.)
- **Spark is only an optional compute stage** before delivery. It never writes to a business
  destination; it hands off to the selected delivery engine through Kafka or an in-process handoff
  (its sink factory cannot construct a non-Kafka writer).
  Spark-staged runs must launch both the Spark Job and the delivery Job and verify the final sink.
- "Correlated" never implies Spark. A delivery engine can correlate natively (for example a Kafka
  Streams `KTable` join) (from repository documentation - to confirm).
- **Camel is connector plumbing,** not an engine: one generic route builder parameterized by URI,
  not one module per protocol.
- **Dynamic mapping:** `FieldMappingEngine` and `NamedTransformRegistry` are the runtime,
  metadata-driven mapping boundary. They are not MapStruct. MapStruct belongs only at fixed-type
  compile-time boundaries such as JPA entity to generated model in the JDBC connector (from
  repository documentation - to confirm).
- Per-schema generated model classes and per-source mapping specs belong in the consuming product
  (for example FEI), not in FDS (from repository documentation - to confirm).

### 12.2 Invocation and execution

- Direct REST admission and FOWF-managed admission use the same planner and produce equivalent
  plans, including for Spark-staged runs.
- Bounded runs dispatch a Job; continuous runs dispatch a Deployment. Both are reachable directly and
  through FOWF. The shared Kubernetes lifecycle library is reused rather than reimplemented.
- The FDS launcher performs its own AuthZEN evaluations against the `datastreaming` resource server.
- **Applicability:** source cardinality × bounded/continuous × engine × direct/workflow × framework ×
  FOWF backend. "Historical 39-cell/57-test counts are evidence sets, not a complete behavior
  denominator." Preserve the eight base capability cells (single/correlated × bounded/continuous ×
  Pekko/Kafka) and the historical 18-cell baseline for both WorldCheck and the customer master; the
  18-cell baseline omits direct continuous invocation.

### 12.3 Correlation and merge

- Cover missing and blank keys, join cardinality, unmatched records, duplicates and deterministic
  trust and union. No hidden provider-specific merge logic.
- Merge policy is a URI-referenced, versioned artifact. The highest-trust source wins singular
  fields; lower-trust evidence and aliases remain; identifiers and assertions are unioned and
  deduplicated. Tests use at least three sources, both orders and ties.
- Pekko correlation durability uses a Kafka compacted changelog topic. Do not confuse it with FOWF
  journals. (Valkey-held state was considered and not chosen; from repository documentation - to
  confirm.)

### 12.4 Kafka semantics

- Bounded Kafka runs use a fixed read-committed frontier, handle aborted transactions and control
  records, then drain and terminate.
- Continuous runs keep state and cancel and drain cleanly.
- Commit and acknowledgement ordering, sink failure, bounded retries, malformed-record handling,
  stable IDs and no duplicate effects are required.

### 12.5 Images and dependencies

- Kafka Streams and Spark images contain no Pekko. Pekko images contain no Kafka Streams engine.
  Shared FDS modules (`-core`, `-connector-jdbc`, `-connector-object-storage`) must not depend on
  `pekko-stream` (section 21).
- A neutral Kafka client for an approved transport or changelog is not a Kafka Streams dependency.
- JDBC/JPA connector contracts may use real repositories directly. Bulk Spark JDBC extraction is a
  separate, approved mechanism.
- FDS API and worker contracts are generated and centralized, with the shared exception and security
  handling.
- Acceptance uses real workers and real OpenSearch effects: no curl fallback and no fixture writing
  the output directly.

## 13. Decision Engine architecture

(From repository documentation and the platform audit requirements - to confirm where marked.)

- **Framework-neutral core.** "The engine core must never depend on any application framework."
  Drools `KieServices`, `KieContainer` and `KieSession` calls live in one framework-neutral module.
  Quarkus, Spring and Micronaut modules exist only to expose the core over gRPC, each through its own
  framework's gRPC extension; no Drools-specific framework integration is used (from repository
  documentation - to confirm).
- **gRPC only.** Canonical `.proto` ownership in one specifications module, with generated Java,
  Python and TypeScript bindings. "Do not invent REST or a new Studio."
- **RPC inventory:**
  - management: `CreateRulesetVersion`, `GetActiveRulesetVersion`, `ListRulesetVersions`,
    `ActivateRulesetVersion`, `DeleteRulesetVersion`;
  - evaluation: `Evaluate`;
  - administration: `GetStatistics`, `GetCacheStatus`, `UnloadRuleset`, `ClearCompiledRulesCache`,
    `WarmRuleset`.
  - "Do not invent update, validate or publish RPCs."
- **Wire tests** go through generated clients and cover Struct, list, null and number mapping,
  correlation IDs, fired-rule semantics, malformed requests and unsupported outputs.
- **Versioning:** no version reuse after deleting the highest version; correct concurrent allocation,
  activation and deletion; pinned evaluation; the first version must be active; the active version
  cannot be deleted.
- **Compiled-rule cache:** tenant-scoped with unique KIE identities, safe disposal during active
  evaluations, bounded limits. "A firing limit is not a wall-clock sandbox."
- **Fact windows in Valkey** (ruleset definitions are kept in PostgreSQL, from repository
  documentation - to confirm): keys scoped by tenant, ruleset and version
  with unambiguous encoding; TTL, concurrency, restart and authentication covered; embedded and
  external Valkey modes.
- **Identity:** a verified JWT (organization, issuer, audience, expiry) plus distinct AuthZEN
  evaluate, manage and admin capabilities. "Unsigned `tenant-id` is not identity." Conflicting
  metadata is rejected. Evaluate-only callers cannot upload rules or administer caches. This
  supersedes the original unsigned internal-caller trust model.
- **Health:** gRPC liveness and readiness, health marshaller compatibility, numeric probe ports and
  framework registration work in real images; health is exempt from the tenant interceptor.
- **FOWF integration:** FOWF calls FDE through the real gRPC adapter with a pinned proto, an exact
  egress endpoint and tenant credentials, across all nine FOWF cells.
- **Outcome semantics:** append plus evaluate is not a distributed exactly-once transaction; a lost
  response after append may be outcome-unknown. Process-local admin statistics are not cluster-wide.
- **Rehabilitation acceptance list** (each case needs its own evidence): populated old-schema upgrade
  and counter seeding under restricted roles; cache-warm tenant deactivation; real authorization TTL;
  Valkey script eviction and response loss; read-only-root startup with writable compiler temporary
  space; secret rotation; unavailable-dependency recovery; proto compiler packaging without runtime
  Maven network access.

## 14. UI architecture, design system and tenant branding

### 14.1 Decision

"One tenant-scoped product experience without building one monolithic application."

Composition rules:

1. "Authentication and tenant resolution happen once through the shared shell contract."
2. "Cross-product navigation uses normal same-origin routes and full application navigation;
   micro-frontends and iframes are not the default architecture."
3. "Domain applications own domain vocabulary and colour-rich visualizations; shared tokens own
   typography, structure, controls, and theme behaviour."
4. "The OKS workflow visualization is reusable in future agent experiences and in Entity
   Intelligence execution detail pages."
5. "The portal aggregates only stable, intentionally exposed summary APIs. It must not query
   internal stores or reconstruct workflow state."
6. "Generated clients are immutable build outputs derived from the authoritative specifications and
   verified in the full source-composed reactor."

Ownership: "Closed-source Entity Intelligence owns only its Workbench. Open-source OKS owns Studio.
The open-source Platform project owns the Platform Dashboard. No product imports packages named for
another product."

### 14.2 API access from the browser

- "Each back-end repository owns its API specifications and generated clients." Applications consume
  those artifacts and "do not copy specifications or run a second generator with local options. Thin
  UI adapters may add authentication, tenant-aware base URLs, and product-specific error handling
  without redefining transport models."
- UI adapters may create presentation and graph models derived from generated wire models but must
  not redefine a second wire contract.
- Direct `fetch` is acceptable only for runtime configuration and presigned object transfer.
- Each Studio is one Maven-built web application with thin framework hosts. Generated TypeScript
  clients are imported as Maven `provided` dependencies, ordered before the frontend build.
- The OpenAPI document must declare its security scheme, or the generated TypeScript client will
  never attach the bearer token (section 17.3).
- Studio API proxies must forward correlation and concurrency headers (`X-Correlation-ID`,
  `If-Match`, `Idempotency-Key`).
- Studio content security policy allows images and styles from `'self'` and `data:` only. The
  tenant-context and brand endpoints are same-origin, so they need no policy change.

### 14.3 Design system

- All four UIs (OpenWorkflow Studio, FEI Studio, the Platform Dashboard and the future Agent OS
  Studio) standardise on the **Kriyagentic design system**, published as a claude.ai artifact and
  implemented in `forwardmeasure-presentation-system` as the npm package
  `@forwardmeasure/presentation-system` (Apache-2.0, may be public).
- Visual basis: Sanzo Wada colour combination 243 (ivory surface, slate ink, Raw Sienna accent).
  **Light and dark themes are both mandatory.** The dark theme uses warm charcoal surfaces.
- Icons: Lucide (ISC licence), 1.5 px stroke. Fonts are bundled (OFL licence): Roboto, Didact Gothic
  and IBM Plex Mono, addressed through font roles (`--font-display`, `--font-sans`, `--font-mono`).
- Components are CSS-only `kg-` classes (top bar, theme toggle, buttons, fields, tables, pagination,
  cards, key-value lists, identifier values, tabs, operational status, banners, toasts, loading,
  empty and error states, dialogs, menus, tooltips, drawers, tags, icons). The shell (top bar with
  section groups, theme toggle, account menu) lives in the package as plain-JavaScript React with
  React as a peer dependency. New UI work uses `kg-` components from the package.
- Labels use **title case** (minor words of three letters or fewer stay lower case inside a label;
  hyphenated parts are capitalised; sentences stay sentences).
- White-label rules apply even before white-labelling ships: role tokens only, names passed in,
  font roles, an empty logo slot.
- Packaging: the package is built into a jar; applications unpack it at Maven `initialize` and
  depend on it with a `file:` reference, with `.npmrc` `install-links=true` so the copied package
  resolves the application's React. The version property lives in `forwardmeasure-platform`. The
  repository's `design/` folder is a pinned byte copy of the published design artifact with
  documented sync steps.
- Do not run FEI's `npm install` with `--legacy-peer-deps`; it rewrites the lock file without peers.
- Changes to the published design artifact need the owner's approval, and it is edited elsewhere too,
  so re-read it before publishing. The FEI Studio navigation has five groups (owner's choice).
- Visual checks run the real application in Playwright against a fake API (snake_case JSON fixtures,
  emulated colour scheme).
- The original UI plan named neutral packages (`@forwardmeasure/ui-tokens`, `ui-components`,
  `ui-auth`, `ui-application`, `ui-workflow`, `ui-testing`) in a future `forwardmeasure-ui`
  repository. `@forwardmeasure/presentation-system` replaces that plan. What the foundation must
  cover is unchanged: tokens, components, authentication (Keycloak bootstrap, token refresh, tenant
  context, authorization, logout), application shell, workflow visualisation and testing support.

### 14.4 Runtime display name

- Served as `platformDisplayName` in the uncached runtime `config.js`, from property
  `forwardmeasure.platform.display-name` on all three framework hosts (Helm passes
  `FORWARDMEASURE_PLATFORM_DISPLAY_NAME`).
- "The name never determines OIDC realm/client, tenant DID, API path, database or Java package."
- The Keycloak bootstrap Job reconciles the realm display name.

### 14.5 Branding decisions (owner, 2026-10-12)

- **D1:** "A brand belongs to a tenant. Each tenant uses exactly one brand. Many tenants may share a
  brand, and `kriyagentic` is the default."
- **D2:** "One platform-branded sign-in page serves every tenant… no per-tenant Keycloak theme and no
  realm per tenant."
- **D3:** Option A first (tenants and brands declared in git, applied by redeploying); Option B later
  (the tenant administration API and registry become the source of truth, with brand files in object
  storage).
- **D4:** brand folders live in `deploy/helmfile/shared/brands/`, not in the presentation-system
  repository, because "customer logos must not sit in it".
- **D5:** one shared tenant-context service in platform operations; "No app resolves tenants itself."
- **D6:** the platform name is lower-case "kriyagentic" for now.
- Key rule: "apps only ever call the tenant-context contract".

### 14.6 Brand design

**Brand folder:** `brand.json` (key, platform name, colours with light and dark values, `fonts`
null), optional `logo.svg`, optional `favicon.svg` (falling back to the default's). The default brand
`kriyagentic` is a folder with no overrides, so every tenant uses the same code path.

**Brandable:** the accent family (`accent`, `accent-hover`, `on-accent`, `accent-text`,
`accent-soft`); optionally `secondary` and `on-secondary`; the platform name, logo and favicon.
Product names stay the products' own.

**Locked** (a brand may never set them): surfaces, ink and lines; status colours (`danger`,
`success`, `warning`, `info`, `on-status`); evidence bands and their soft tints; sensitivity,
quarantine and redaction colours; `focus` and `scrim`; the `chart-*`, `seq-*`, `div-*` and `conf-*`
scales. "A customer's red must never read as 'Failed'."

**Fonts:** null until needed; licensing and bundling are a later extension.

**Brand check:** `node scripts/brand.mjs check|compile <folder>` in the presentation-system web
module. It rejects locked or unknown tokens; a missing light or dark value; text pairs below 4.5:1
contrast (on-accent on accent and accent-hover; accent-text on surface, raised surface and
accent-soft); control and focus pairs below 3:1; and SVGs containing scripts, event handlers or
external references. The deployment runs the check and fails on any rejection.

**Compile:** produces `brand.css` with overrides under `:root[data-theme="light"]` and
`[data-theme="dark"]`, loaded after `tokens.css`. Brand CSS is compiled at render time and delivered
in a `platform-brands` ConfigMap, with a configuration checksum that restarts consumers.

**Tenant-context contract:**

- `GET /platform/tenant-context` (no authentication) returns `tenant_did`, `tenant_name`,
  `display_name`, `oidc_url`, `oidc_realm`, `oidc_client_id`, and
  `brand { key, platform_name, css, logo, favicon, version "sha256:…" }`.
- `GET /platform/brand/{key}/{file}` serves only `brand.css`, `logo.svg` and `favicon.svg`.
- Unknown hosts return 404. Brand files are long-cached with a version query string; tenant context
  is `no-store`.
- Built on Quarkus, Spring and Micronaut through the shared three-way service pattern, routed on
  every tenant host, and tested through the REST API with real HTTP, including the unknown-host and
  wrong-file cases.

**Application behaviour:** each Studio calls `/platform/tenant-context` before initialising
Keycloak, takes its OIDC settings from it, applies the brand, and after sign-in checks the token's
tenant. Per-deployment fields leave `config.js`; nothing hard-codes a tenant alias.

**Tenant registry:** a forward-only Liquibase changeset adds `display_name` and `brand` to
`tenant_registry` ("Never edit the applied changeset"); reconciliation and tenant provisioning fill
them, and the tenant listing returns them.

**Onboarding under Option A:** add the tenant entry (and brand folder), open a pull request (the
brand check runs in CI), merge and redeploy.

**Option B (later):** `POST /v1/tenants` takes `brand`; `PUT /v1/brands/{key}` and
`PUT /v1/brands/{key}/files/{file}` guarded by the platform-operator role; a private
`platform-brands` bucket through `forwardmeasure-object-storage`; shared validation rules readable
by both Node and Java ("Decide this when B starts"); redirect URIs and web origins move to the
reconciler, per-tenant secrets to External Secrets, certificate SANs to a wildcard or per-host
certificates. The contract and the applications stay unchanged.

Acceptance for branding includes a K3s test with two tenants on two brands (colours, logo, names,
and cross-tenant sign-in refused).

---

# Part III: Engineering guidelines

## 15. Engineering principles

These principles apply to every repository and every piece of work. Each has a rule, the reason
for it, and how to apply it. Most reasons are real incidents in this codebase.

### P1. Ground every claim in the real system, not memory or assumption

- **Rule:** "Read the actual code, config, or documentation before asserting how something
  behaves… check the primary source, every time, even when it feels obvious."
- **Why:** an AI-summarised library version did not exist on the registry; a documentation
  comment's claim was accurate only because nobody had checked, and checking surfaced a narrower
  real issue. "Assumptions compound silently."
- **How:** "Cite exact evidence — file:line, a real command's output, a real config value… When
  something can't be verified, say so plainly." A comment, Javadoc, handover or plan file (including
  one you wrote) is not ground truth; it decays as soon as the code changes. When two records
  disagree, compare their dates and prefer the newer, saying so.

### P2. Consolidate on one canonical implementation

- **Rule:** before building a new connector, client or mapper, check whether one exists, including
  under a different name, module or sibling repository.
- **Why:** S3 and GCS clients existed independently twice; OpenSearch access existed three
  uncoordinated ways; two "named transformation" mechanisms existed, one "silently broken for
  multi-input cases".
- **How:** "name it explicitly and propose a consolidation direction — don't quietly add a third
  implementation".

### P3. Reuse proven infrastructure, evaluated on merit

- **Rule:** "Neither 'we've always done it this way' nor 'this is the newer, more capable option' is
  an argument by itself."
- **Why:** a design conversation swung twice, and both reflexes were wrong until real usage was
  checked.
- **How:** ask "is this genuinely proven here, genuinely absent, or genuinely being moved away
  from?" When the platform is standardising on a target mechanism (for example FOWF for durable
  execution), check each piece of existing infrastructure against what the target already provides.
  Being well built does not justify keeping a duplicate durable-execution system.

### P4. If it's built, it's wired to something real

- **Rule:** "A capability that compiles and has its own passing test, but is never invoked from
  anything real, is not a completed feature." Neither is provisioned configuration the code never
  reads.
- **Why:** a durable outbox was coded but never started for a release cycle; a nested-field mapping
  path was unreachable from the day it shipped; a production Spark environment was configured and
  unused while the code stayed single-threaded.
- **How:** "trace the real call path from an actual trigger… through to the effect." When porting,
  trace one real end-to-end data path through the original system and confirm the port can carry it;
  a stand-in that compiles but cannot do its job is not done.

### P5. Full parity, no silent scope-cutting

- **Rule:** "A real bug or gap found while working on something else gets fixed then… unless that's
  been explicitly agreed, not assumed."
- **How:** "'Feature X is on hold' doesn't excuse a parity gap inside X's existing surface." Defer
  only a genuinely larger, separate effort, "and say so explicitly". See section 27.2.

### P6. No mocking frameworks; use forwardmeasure-testcontainers

- **Rule:** "Prefer real infrastructure — a real broker, a real database, a real actor system — over
  Mockito or an equivalent mocking framework, without exception." "Mockito is banned in this org."
- **How:** check `forwardmeasure-testcontainers` first; if a wrapper is missing, add it there. A
  hand-written test double is acceptable only where a real instance is impractical, with a comment
  saying why "and why this isn't a mocking framework in disguise". Prefer real end-to-end coverage
  over a narrow double.

### P7. Keep repositories out of business and orchestration code

- **Rule:** the application-service layer (authorization, transactions, what REST calls) "depends on
  domain services, not on `Repository` types directly. Repository access is confined to the
  domain-service layer beneath it."
- **Example:** `DossierApplicationService` takes `DossierService`, `InvestigationService`, a
  transaction executor and an authorization service, and no repository.
- **How:** an application service injecting a `*Repository` is "a layering violation worth stopping
  and fixing". Classify by responsibility, not by class name. (FOWF's older rule PER-03 allowed
  application services to inject repositories; the stricter rule applies. See section 28.)

### P8. Schema-driven design, realised as generated code

- **Rule:** describe shapes and contracts in schema (OpenAPI), but "resolve that schema into real
  generated code at build time".
- **How:** "`openapi-generator` for schema-described model classes and client/server stubs.
  MapStruct for mapping between them, including named custom-transform methods." Use a runtime
  interpreter only when the stated requirement is "add a new case without recompiling anything"
  (for example provider onboarding in FEI and FDS mapping).

### P9. Default toolkit; deviations need a stated reason

| Concern | Default |
|---|---|
| Persistence | JPA behind a repository behind a service, via `forwardmeasure-jpa`, with Criteria queries over the static metamodel; "not raw JDBC, not an ORM-adjacent framework's own query layer, unless there's a specific, stated reason" |
| Object mapping | MapStruct |
| Schema-described data | OpenAPI-generated models, "not Protobuf, not a bespoke type-description format" (gRPC services such as FDE use `.proto` contracts) |
| Storage | `forwardmeasure-object-storage` |
| Test infrastructure | `forwardmeasure-testcontainers` |

"Using something else *silently*… is not [fine]." (The manifesto originally said "JPA/JPQL"; the
Criteria-plus-metamodel rule supersedes the JPQL preference.)

### P10. Build and verify multi-framework parity

- **Rule:** "build and verify all of them together, not one now and the others 'later.' Treat
  framework parity as a real requirement."
- **Why:** "a deliberate bug-detection technique… A gap or inconsistency that shows up in only one
  of the three is usually a sign the 'shared' logic wasn't as framework-agnostic as it looked."
- **How:** "scaffold and verify it against Quarkus, Spring, and Micronaut in the same pass… Treat 'it
  works on Quarkus' as an incomplete answer." See section 20.

### P11. Deploy through the shared Helm charts and Helmfile

See section 5.1.

### P12. Verify before claiming success

- **Rule:** "Compiling, or passing an isolated unit test, is not proof of functional correctness."
- **How:** "'this compiles' and 'this works end-to-end' are different claims." "Never infer
  completion from implementation presence."

### P13. Build and deploy discipline

- **Rule:** local read-only checks are fine. "Anything that touches a live system — a real cluster, a
  deployed service, a published package, a pushed branch — needs explicit authorization first, every
  single time." "Hand off real builds and test runs rather than running them speculatively."
  Tests, image builds and publication, and deployment are separate operations, each needing the
  owner's instruction. See section 27.4.

### P14. No speculative diagnosis

- **Rule:** get real proof for each step or label a step as a guess. "'Same error' needs the same
  root cause confirmed… read the full exception, including any prose above the stack trace."

### P15. When one bug has a shape, look for its siblings

- **Rule:** "Once a bug's shape is understood, search for that shape elsewhere in the codebase before
  considering the fix complete." Apply this to code that is really reachable; flag hypothetical
  future risks separately.

## 16. Architecture invariants and requirements

### 16.1 Release-gate invariants

These invariants are the release gate for production claims. Wording is exact.

| Boundary | Required invariant |
|---|---|
| Deterministic runtime | "OKS durable-processing, runtime-core, runtime-api and definition modules contain no Quarkus, HTTP, JPA, Kafka-client or cloud-provider dependencies." |
| Orchestration | "Kafka Streams decides and durably records workflow progress. External I/O executes only in adapters or workers and returns observations; workers never advance the graph." (Written before Pekko became an equal engine; the engine that is deployed decides and records progress.) |
| Contracts | "OpenAPI, AsyncAPI and JSON Schema are authoritative. Generated clients own transport types. UI adapters may create presentation models but must not redefine a second wire contract." |
| Workflow portability | "Immutable workflow definitions identify logical operations and immutable contract resources. Deployment-specific service addresses, credentials, namespaces and storage coordinates are resolved at an authorized adapter boundary." |
| Identity | "Tenant and actor identity come from verified authentication or a cryptographically bound internal workload identity. User-supplied tenant/actor headers are never authoritative." |
| Causality | "Business correlation, causation and W3C tracing are distinct. `traceparent` is never used as a business correlation identifier. A root execution without upstream correlation uses its execution ID; children preserve the intended causal chain." |
| Tenancy | "Public APIs infer the tenant from the authenticated principal. Tenant identifiers are not editable business form fields. Every query and mutation is tenant-confined." |
| Persistence | "Application resources call domain/application services. JPA repositories and `EntityManager` stay behind persistence or explicitly named transaction adapters. Raw JDBC is limited to migrations or deliberately optimized persistence adapters." |
| Artifacts | "Business code uses `forwardmeasure-object-storage`; it does not depend directly on S3, GCS, Azure or MinIO SDKs. Object-store URIs are discovered or issued by governed APIs, not typed by business users." |
| UI ownership | "Closed-source Entity Intelligence owns only its Workbench. Open-source OKS owns Studio. The open-source Platform project owns the Platform Dashboard. No product imports packages named for another product." |
| Deployment | "API and UI services are ordinary durable Kubernetes workloads unless their execution semantics explicitly require another primitive. Secrets are referenced, images are digest-pinned, and routing preserves the tenant host." |
| Acceptance | "Production claims require real PostgreSQL, Kafka, object storage, Apicurio, OpenSearch, Keycloak, K3s and browser paths as applicable. Mocks are not acceptance evidence." |

`EntityManager` use "is confined to JPA repositories, explicitly named transaction adapters and
dependency-wiring configuration. REST resources and deterministic domain modules do not inject it."

### 16.2 Architecture requirements

Each requirement has a stable identifier in its product's architecture design document (FOWF
families ARCH, DSL, DEF, RES, PUB, TEN, SEC, API, FW, EXE, ENG, PEK, KS, ADP, DATA, HT, PER, MIG,
WIRE, STU, DEP, TEST, maintained in the
[FOWF vision](../../forwardmeasure-openworkflow/docs/architecture-design.md); FEI families FARCH, FWF, FDOM, FSEC, FAPI, FPER, FDEP; FDS families SMOD, SINV,
SMAP, SCON, SIND, SDUR, SSEC, STST; FDE and JPA by section). Those documents record approved
requirements; their dated BUG/PASS/NOT ASSESSED labels need current verification. "Code establishes
actual behavior; it does not silently redefine the intended contract." Keep approved requirements,
documented implementation decisions, observed code behaviour and audit-derived scenarios separate.

| Area | Requirement |
|---|---|
| Dependency direction | Portable model, compiler, contracts and domain behaviour stay framework-neutral. Bindings hold host integration, not duplicated domain logic. Find cycles and engine, framework or JPA leaks by following declared ports and real dependencies. |
| Engine separation | Kafka engine and adapter deployments must not carry or start Pekko. Pekko and shared common-plane modules must not depend on Kafka Streams engine internals. A neutral Kafka client for an approved transport or changelog is not automatically a Kafka Streams dependency. Inspect resolved runtime artifacts and startup paths. |
| Native durability | Each engine uses its own native durability; FOWF detail in [FOWF vision §13–§14](../../forwardmeasure-openworkflow/docs/architecture-design.md#13-pekko-native-execution-and-persistence). |
| One canonical implementation | One owner each for the compiler, identity, authorization, publication, common execution model, human tasks, Studio, schemas, clients and reusable connectors. No split packages or duplicate generated classes. No parallel S3, GCS or OpenSearch clients and no copied business implementations. Shared code must actually be wired into every consumer. |
| Contract-first HTTP | Public and internal HTTP capabilities have authoritative OpenAPI contracts, declared security, status and error shapes, and generated server interfaces, models and clients. Resources implement those interfaces. "Schemas, not Java patches, drive contract fixes." Distinguish a bad specification, a bad generator, template or configuration, and an implementation violation. |
| Generated code | Never edit generated Java or TypeScript; regenerate. Verify artifact and resource identity and client compatibility. Do not expose JPA entities or engine events as public DTOs. Use MapStruct for applicable typed edge mappings. |
| Dynamic ingestion | Section 11.3. |
| Shared errors | Section 8.3. |
| Service/repository boundary | Resources, consumers, workers, schedulers and orchestration services call domain service interfaces. Repository access sits behind domain or persistence service implementations. No hand-built repository graphs in orchestration. |
| JPA foundation | Application-owned relational persistence uses the `forwardmeasure-jpa` service, repository and entity bases. No replacement Panache, Spring Data or Micronaut Data architecture. This does not apply to engine-native Kafka, Cassandra or Pekko persistence or approved bulk connector internals. |
| Static metamodel | Fixed attributes and relationships use generated metamodel classes and Criteria queries. String JPQL/HQL, native SQL/JDBC and string Criteria paths are inspected. Validated dynamic sort and filter is a separate case. Metamodel generation must work in real builds. |
| Entity conventions | Shared entity bases, the `entity` package and Lombok conventions, auditing, ownership and optimistic-version semantics as applicable. The generated metamodel must match the mapped relationships. Do not impose an owner relationship where the base contract does not require one. |
| ORM access and transactions | Section 18. |
| Query exceptions | Provisioning DDL, migrations, backend plugins and proven JPA limitations may need SQL. Record location, necessity, tenant and transaction behaviour, and tests. Do not use raw SQL to implement business behaviour in acceptance tests. |
| Migrations | Section 19. |
| Identity and tenancy | Sections 6 and 7: the trusted active organization determines the tenant; DID, `TenantId` and registry lookup are consistent; never accept caller-chosen database or schema names; never let workflow input override the tenant; check roles, schema ownership, reset on pool return, missing or inactive tenant rejection, and a fresh scope on async threads. |
| Typed values | Section 8.7. |
| Build and deployment ownership | Versions are managed in the platform BOM and properties; local overrides only when necessary and documented. Shared framework-neutral Helm charts and platform Helmfile values. Maven-generated Dockerfiles and images. One selected framework and engine per installation. No hidden dependence on a developer's installed JAR, a hand-set property or an unrelated service. |

## 17. Contract-first APIs

### 17.1 The rule

- Every HTTP capability, public or internal, starts as an OpenAPI document. Asynchronous worker
  contracts are AsyncAPI. Data shapes are JSON Schema. gRPC services (FDE) use canonical `.proto`
  files.
- Server interfaces, models and clients are generated with `openapi-generator`. Resources implement
  the generated interfaces.
- FOWF's HTTP baseline is OpenAPI 3.1 with `/v1` paths, including the defined internal prefix. FDS's
  launcher document is OpenAPI 3.0.
- Generated Java, TypeScript and Python clients come from the same source. "Internal self-calls do
  not justify handwritten clients."
- "Generated clients are immutable build outputs." Never edit generated code. "Generated Java must
  never be patched manually."
- Contract fixes are made in the schema or the generator templates, never by patching generated
  code, and never field by field. "We have our own generator templates. We have to fix this
  properly."

### 17.2 Never hand-write a client

Every call to an OpenAPI-defined API goes through a generated client, including a product calling
its own API (for example FEI workers calling FEI's REST API). If the generated client module is
missing or empty, wiring the generator is the first step, not a reason to hand-roll an HTTP client
with hard-coded paths. Before writing any REST call, look for the generated `*Api` class for that
specification. Configure it with `ApiClient.setBasePath` and `setBearerToken(Supplier)`.
Hand-rolled plumbing hides gaps (for example a workflow that never passed its worker the API base
URLs).

### 17.3 Security schemes are required

Every JWT-secured API declares `components.securitySchemes.BearerAuth` (`type: http`,
`scheme: bearer`, `bearerFormat: JWT`) and a top-level `security: [{BearerAuth: []}]`. The
TypeScript generator only emits the `Authorization` header for operations with a declared security
requirement; without one, every call silently goes out unauthenticated even though the calling code
type-checks. Check generated operations for `Authorization` handling.

### 17.4 Generator configuration rules

- Micronaut validation fails on container-level `@Valid` on `List` or `Map` getters. Put `@Valid`
  only on model types, on the element (`List<@Valid X>`, `Map<String, @Valid X>`), never on free-form
  values. FOWF's template overrides in `openworkflow-api-specifications` do this; any repository
  whose Micronaut services validate generated models needs the same overrides.
- Use `useOneOfInterfaces=true` with a model template override for `oneOf` types; the default wrapper
  classes break Micronaut validation and serde.
- Repositories whose generated models are validated or serialised by Micronaut run the Micronaut
  inject, serde and validation processors in the models module itself (section 20.5).
- Specification jars attach an `early` resource classifier at `process-resources` that code
  generators unpack (FEI and Agent OS use this pattern).
- The OpenAPI generator goal and the dependency unpack/copy goals always run, even on build-cache
  hits.

### 17.5 Contract coverage

A capability's contract tests diff the specification's `operationId`s against the operations
exercised, so a missed endpoint fails the build. FEI's operation inventory is derived from the
canonical OpenAPI document, not a frozen count.

### 17.6 Configuration conventions

- Application configuration is YAML (`application.yaml`).
- Deployment selection keys are `platform.framework` and `platform.engine`; release names are
  framework-independent; startup is role-specific; image pins are shared.
- Temporary image-tag overrides are inventoried separately from the normal version and digest
  policy.

## 18. Persistence

### 18.1 Rules

- **Base classes.** Repositories and services always build on `forwardmeasure-jpa`'s repository and
  service bases, never plain JPA repositories, and never Panache, Spring Data or Micronaut Data.
  Entities use the shared bases: `AuditedEntity` (ID, `created_at`/`updated_at`, version for
  optimistic locking) and `OwnedEntity` (adds a required owner `Actor`).
- **Actors.** An `Actor` is `HUMAN` or `SERVICE`, identified by identity provider and subject (OIDC
  issuer plus subject). Actors are provisioned deliberately; there is no automatic user creation on
  first login, and an unknown or disabled actor is a security error (403).
- **One actor table per tenant.** Each tenant database holds its actors once, in the platform-owned
  `identity` schema (`identity.actor`), shared by every product; no product schema has an actor
  table of its own. Every product's foreign keys to actors point at `identity.actor`, and products'
  runtime roles may only read it. FOWF's tenant-access role alone writes it: a person's `HUMAN`
  actor is created when tenant access is granted (and by the tenant-access reconciliation for
  people added directly in Keycloak), and disabled, never deleted, when access is removed;
  engines' runtime identities are `SERVICE` actors created at onboarding. See
  `forwardmeasure-jpa/docs/migrations.md` and FOWF `docs/actor-provisioning-plan.md`.
- **Queries** use the Criteria API over the JPA static metamodel (`Entity_`). String JPQL/HQL, native
  SQL and raw JDBC are allowed only where JPA cannot practically be used (for example database and
  role provisioning DDL), each justified in place. This replaces the manifesto's earlier "JPA/JPQL"
  preference.
- **Only repositories touch `EntityManager`, `Session` or `SessionFactory`.** Never inject them into
  services, resources, framework bindings or deployments. (Justified infrastructure factories and
  explicitly named transaction adapters are the only other places.)
- **Layering:** resource, then application service, then domain service, then repository.
- **Transactions:** production-managed services own transactional behaviour. Commit precedes
  acknowledged success; rollback is effective; locking is correct; streams do not escape
  transactions; tenant scopes and connections close and reset. No open-in-view, and no runtime ORM
  schema generation (`hbm2ddl`).
- **Pekko persistence is the exception:** journal, snapshots, projections and offset stores use
  idiomatic Pekko plugins and configuration.
- **Exceptions are catalogued:** location, necessity, tenant and transaction behaviour, and tests.
- Native statements do not inherit Hibernate's default schema as the PostgreSQL `search_path`; native
  allocation and locking use the tenant-routed connection's schema.

### 18.2 Object mapping

- New mappers use MapStruct ("I fucking asked Claude a million times to use mapstruct" - the owner).
  "Stay consistent with existing hand-written mappers" is not a reason to add another hand-written
  one; existing hand-written mappers are acknowledged debt.
- Converting the existing hand-written mappers is a separate migration that needs explicit scoping.
  When it is unclear whether work is a new mapper or a change to an existing one, ask.
- MapStruct is for typed, compile-time boundaries (API, domain, entity). Runtime, metadata-driven
  mapping (FDS `FieldMappingEngine`, FEI provider mappings) is deliberately not MapStruct.

## 19. Database migrations

- **Software has been deployed.** "'Nothing is deployed' is false and never authorizes changing
  historical migrations, destructive resets or dismissing wire compatibility."
- **Forward-only.** Preserve deployed Liquibase history and checksums. Never edit an applied
  changeset; add a forward changeset. No drop or rename without explicit authorization for a
  disposable reset.
- New `NOT NULL` rules on existing tables are added in a way that keeps earlier rows readable (for
  example `NOT VALID` constraints).
- Run real, ordered migration Jobs; never startup `create-drop`.
- Cassandra CQL is versioned, idempotent and owned by its adapter.
- Kafka topic ownership is explicit.
- Never write tests that repair Liquibase bookkeeping or pre-create production-owned topics.
- Migration Jobs create tenant databases and runtime roles and grant `tenant_registry` read access;
  no grant is ever applied by hand.
- Liquibase XML must stay well-formed: never put `<...>` placeholders in XML comments; use
  `{method}`-style placeholders and parse-check changed XML.
- Test data that a deployment's own API cannot create is seeded with a test-fixture Liquibase
  changelog passed to the migration call the test already makes (fixed UUID literals referenced as
  constants), not with JPA, CDI or hand-built sessions.

## 20. Multi-framework parity

### 20.1 The rule

Every real service and entry point gets genuine Quarkus, Spring Boot and Micronaut treatment, with
no exceptions:

- REST resources, Kubernetes Job workers, CLI tools, migration mains and gRPC services alike. If a
  shape seems awkward for one framework, that is a question of how, never whether. A batch worker
  uses `@QuarkusMain` on Quarkus, `CommandLineRunner` with `web-application-type=none` on Spring, and
  a non-HTTP application context on Micronaut.
- Never build one framework "as a first pass" or "to unblock deployment".
- Quarkus is never the privileged or default flavour, in code or in deployment configuration.
- A behavioural difference between frameworks is a parity bug to fix (or an explicit question to
  ask), never a "side effect". Before a change, tabulate each framework's behaviour for the edge cases
  (unknown path, anonymous, invalid token, health) before and after.
- Framework-selective Maven profiles are deferred until testing is complete, and are never a reason
  to omit a framework.

### 20.2 Why

Building all three at once is a deliberate bug-detection technique: "the whole purpose of doing 3
frameworks simultaneously is to suss out design issues". Micronaut's stricter context propagation has
no implicit session fallback, so it fails loudly where Quarkus and Spring stay silent about the same
latent misuse (for example repository reads outside a transaction). When one framework misbehaves,
first ask whether it is exposing a flaw in the shared layer, and push past the first fix that makes a
test pass.

### 20.3 Structure

- A framework-neutral core (application, domain and JAX-RS or gRPC layers) plus thin framework
  bindings (dependency-injection wiring only) plus deployment leaves (bootable assemblies with the
  container-image profile), one per framework.
- A framework-neutral `-jaxrs` module never carries Micronaut annotation processors itself; the
  Micronaut deployment subclass is the only sanctioned bridge.

### 20.4 Settled three-framework patterns

Before investigating framework-specific behaviour, look for an existing solution in FEI, FDS, FDE or
FOWF. Settled patterns:

- **Request body binding uses Jackson 2 on all three.** Quarkus REST uses Quarkus's `ObjectMapper`.
  Spring Boot's Jersey integration hands the Jackson 2 mapper to Jersey only when it is the single
  candidate. Micronaut uses the shared `JacksonRequestBodyReader` (a `MessageBodyReader<T>` declared
  over a type variable, `@Order(-100)`, unknown fields ignored, JDK 8, JSR-310 and parameter-names
  modules registered), because Micronaut Serde coerces booleans to numbers.
- **RFC 9457 errors come from one shared module** of mappers (constraint violation, Jackson problem
  mappers, an unhandled-exception catch-all). Quarkus discovers them through the Jandex index and the
  catch-all unwraps wrapped parse errors. Spring Jersey registers them explicitly through a feature
  with no `@Provider` annotation. Micronaut needs `@Singleton` edge subclasses compiled with the
  JAX-RS processor and redeclared `@Priority`, plus `@Replaces` for Micronaut's own conversion,
  unsatisfied-argument, JSON, Jackson and constraint exception handlers. Per the JAX-RS specification,
  an unconvertible path or query parameter is a 404 and a header or cookie parameter a 400.
- **Authentication behaviour** (section 8.4): Quarkus authenticates at the HTTP layer with a problem
  challenge mechanism at higher priority than OIDC; Micronaut URL maps end with open health paths and
  `/**` authenticated, plus an invalid-bearer-token rule; Spring internal chains also run the resource
  server.
- **Micronaut settings on every service:** `micronaut.security.reject-not-found: false` (so unmatched
  routes fall through to a 404 Problem), `micronaut.serde.serialization.inclusion: ALWAYS` (to match
  Jackson's output on Quarkus and Spring), health-only management endpoints.
- **Root resources use `@Path("/")`,** never `@Path("")`, which Micronaut turns into unmatched `null/`
  routes.
- **Quarkus tests:** modules with more than one test resource class use
  `@QuarkusTestResource(restrictToAnnotatedClass = true)`.
- **Health:** every framework exposes health; gRPC services register `grpc.health.v1.Health` on all
  three.

### 20.5 Known framework pitfalls

**Micronaut hosting a shared JAX-RS resource** (check all of these for every new Micronaut
deployment):

1. Give the module its own `netty-bom` import using the shared Netty version; importing the Quarkus
   and Micronaut BOMs into one shared dependency management lets Quarkus's Netty pin win.
2. Add `micronaut-jaxrs-processor` to the module's annotation processors; the runtime artifact alone
   creates no routes.
3. Exclude unused `jersey-common`, which otherwise wins the JAX-RS `RuntimeDelegate` lookup and
   produces unserialisable responses.
4. Give generated models explicit `@Introspected` and `@SerdeImport` coverage, with the serde
   processor on the annotation-processor path.
5. Micronaut's serialisation stack uses Jackson 3 internally; classic Jackson `JsonNode` fields need
   a hand-written serializer and deserializer bean.
6. Package-wide introspection does not recurse into nested enums; list them explicitly.
7. Never combine `classes=` with `packages=` in one `@Introspected` annotation; use a separate marker
   interface.
8. Never put Micronaut processors on a neutral `-jaxrs` module; duplicate routes make every request
   return 400.
9. JAX-RS exception mappers in neutral modules need the same thin `@Singleton` subclass bridge as
   resources (remove `final`).
10. Shared response models such as `Problem` need serde coverage in every module that does not
    depend on the module that first introspected them.
11. Keycloak realm-import users need `email`, `firstName` and `lastName` set, or password login fails
    with "Account is not fully set up".
12. With the validation processor, resource edge classes and their inherited methods must not be
    `final` (AOP proxies).

**Micronaut configuration placeholders:** `${A:http://localhost:8080/x}` with `A` unset resolves
wrongly because colons are read as further fallback names; wrap colon-containing defaults in
backticks. `${A:${B:c}}` is broken in every case; use the chain form `${A:B:c}`. Spring and Quarkus
handle both forms.

**Micronaut Serde and records:** a record with three or more constructors silently deserializes
through the shortest one; annotate the canonical constructor with Jackson's `@JsonCreator`. Verify
with a plain `ApplicationContext` serde round-trip test.

**Micronaut introspection must be built from source:** introspection generated in another module
from a compiled jar loses type-argument annotations, so `List<@Valid X>` never cascades. Run the
processors in the models module.

**Quarkus interceptor bindings:** an interceptor binding such as `@ActivateRequestContext` on a
method of an anonymous object returned from a `@Produces` method is rejected by Quarkus's real build
(`ArcProcessor`), which only `mvn install` runs. Use a named, directly implementing bean class.

**Quarkus gRPC interceptors:** `@GlobalInterceptor` on a `@Produces` factory method silently
registers nothing; annotate a concrete bean class instead, and confirm in a live run that the
interceptor fires.

**Readiness must reflect the actual runtime.** A Micronaut engine that answered HTTP while its lazy
engine factory never ran is the reference case. Calling a binding's factory method directly proves
the method body, not that the framework invokes it at startup.

## 21. Engine separation and engine-agnostic behaviour

- **The two engines must be genuinely independent.** A Kafka Streams installation must not carry or
  run Pekko, and the Pekko engine must not carry Kafka Streams engine internals. The same rule
  applies to each engine's operation adapter: FOWF has separate Kafka Streams and Pekko adapter
  bridges and deployables. "Shared code built on one engine is never acceptable, even when the extra
  engine is never run." A build check must fail if Pekko appears on a Kafka Streams classpath. Never
  describe a dependency's scope narrowly to make it sound smaller.
- The rule applies to every product with several engines. In FDS, Pekko Streams stays a delivery
  engine, but shared modules must not depend on `pekko-stream`; the Kafka Streams delivery engine and
  the Spark stage carry no Pekko.
- FOWF's earlier leak through the protocol transport interface is fixed in source (`ProtocolTransport`
  is in the engine-neutral `openworkflow-engine-api`); whether packaged images are Pekko-free is not
  yet re-checked. Status: [FOWF implementation status §6](../../forwardmeasure-openworkflow/docs/implementation-status.md#6-engine-separation-and-deployment-state).
- **Engine behaviour that is externally visible lives in shared modules** (section 9.1). Before
  adding or changing engine behaviour, decide whether it is engine-specific; if not, put it in a
  shared module and add a cross-engine parity test.
- **Parity is complete or the work is not done.** A construct supported on one engine is built on the
  other immediately, including inside forks and other containers, whether the gap was found by a sweep,
  by the owner or in your own new work. Never frame "restrict to match" versus "build parity" as a
  choice.
- Verify every applicable engine × framework cell yourself before declaring a cross-cutting FOWF
  capability complete. FOWF's per-cell record is its
  [multi-framework testing document](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md),
  updated with any change to a cell; a cell not re-verified stays unverified. (The older
  `engine-framework-parity-ledger.md` is archived history in FOWF's `docs/archive/`.)
- Routinely search for "designed but never wired" markers (`.unconfigured()`, `boundedInline()`, "not
  yet implemented", "not yet wired").

## 22. Versions and the build

### 22.1 Version authority

- "`forwardmeasure-platform` is the Maven parent and version authority for ForwardMeasure Java
  repositories, including proprietary FEI. Each product retains its own reactor and release identity."
- The parent owns first-party, Java library and Maven plugin versions. `forwardmeasure-platform-core-bom`
  (a flattened publish of the parent's dependency management) exports them to consumers; Quarkus,
  Spring and Micronaut BOMs are framework overlays; `forwardmeasure-platform-bom` records compatible
  component releases and is imported by no product. A platform-owned artifact's version therefore
  belongs in the parent's dependency management.
- Inherit `com.forwardmeasure.platform:forwardmeasure-platform` (currently 1.1.0) with relative path
  `../forwardmeasure-platform/pom.xml`.
- "Omit local library/plugin version declarations and use the managed versions. A local `revision`
  names the product being built; it does not authorize overriding sibling product dependency
  versions." Parent coordinates remain explicit.
- **Product versions** (`forwardmeasure.*`: openworkflow, jpa, authzen, data streaming, entity
  matching and others) are never overridden downstream; they move as one release train. Downstream
  repositories never redefine a property the platform defines; delete the redefinition rather than
  re-syncing its value.
- **Third-party overrides** only when necessary, each with an exact key and a reason in the local
  policy configuration: "Actual third-party compatibility exceptions require an exact key and a
  reason in the local policy plugin configuration. First-party dependency-version exceptions are
  forbidden." Example:
  `<override><key>property:netty.version</key><reason>Quarkus 3 requires the platform-pinned Netty 4.1 line.</reason></override>`.
  Key forms: `property:NAME`, `dependency:GROUP:ARTIFACT` (with type and classifier),
  `plugin:GROUP:ARTIFACT`. "Reasons must be nonblank; wildcard exceptions and exceptions for
  first-party dependency versions are rejected. Parent exception configurations do not silently
  authorize declarations in children."
- A convergence fix needed by more than one repository goes in the platform. If a directly used
  library brings a transitive family (for example httpcore5 under httpclient5), the platform pins the
  family.
- `io.micronaut.jaxrs:*` is a separate artifact family from `micronaut-platform` with its own slower
  release cadence. The Micronaut overlay BOM imports `micronaut-jaxrs-bom` explicitly, ahead of
  `micronaut-platform`, so the JAX-RS line stays pinned.
- Python constraints and data-format versions are outside the Maven version sweep.

### 22.2 The policy plugin

`forwardmeasure-platform-policy-maven-plugin` runs at `validate` through an inherited profile. It
"derives ownership from the actual platform POM and inspects local declarations in both ordinary and
inactive profile configuration. There is no separately maintained Java list of owned versions."
"Dependencies are distinguished by type and classifier; a test jar is not interchangeable with its
production jar." "Ordinary dependencies and plugins inherit managed versions without repeating
them." `platform.policy.bootstrap` disables only the inherited check for bootstrap installs: "Do not
use it for normal consumer validation." Bootstrap goals "do not substitute for license or packaging
gates".

### 22.3 Pins decided during consolidation (2026-10-07)

The platform POM is authoritative; these are the decisions and their reasons, not values to copy.

| Pin | Decision |
|---|---|
| ArchUnit, duplicate-finder, OpenSearch, JSON-P/Parsson, animal-sniffer | Moved into the platform "rather than left as ungoverned local pins" |
| Kubernetes client | One platform version (7.8.0), replacing a pin local to Testcontainers |
| HK2 | Aligned with the Jersey line in use |
| Logback | One `logback.version` for both classic and core |
| httpcore5 family | httpcore5 and httpcore5-h2 pinned with httpclient5 |
| Camel | One platform version |
| Explicit compatibility selections | Quarkus Netty, Lettuce, Rest Assured |
| Property aliases | `forwardmeasure.datastreaming.version`, `forwardmeasure.entitymatching.version`, `forwardmeasure.authzen.version` |

### 22.4 Technology baseline

| Technology | Version or rule |
|---|---|
| Java | Temurin 25; the enforcer requires `[25,26)` |
| Maven | `[3.9.9,4)` |
| Keycloak | 26.7.4 (with the authorization extension image) |
| External Secrets Operator | 2.0.1 |
| Apicurio Registry | 3.3.1 |
| Helmfile | 1.3.2 |
| OpenTofu | 1.11 or later |
| Spark | 4.2.0 with Scala 2.13 and Java 25, identical for driver and executors |
| K3s (test clusters) | A version with `MutatingAdmissionPolicy` GA (v1.36 line) |

The parent also manages shared plugins: compiler, Surefire and Failsafe, Spotless with
google-java-format, the flatten plugin, an enforcer rule, Jandex indexing and Apache RAT.

### 22.5 Building

- **Fresh checkout:** install the parent and policy plugin first (bootstrap), then component
  artifacts in dependency order. On a partially cold local repository, install the parent, core BOM
  and framework BOMs non-recursively, then resume the full reactor.
- **One Maven build at a time,** under the shared cross-repository lock, with memory limits. No
  parallel Maven execution (`-T`), and no competing builds from other sessions.
- **The bounded wrapper** (`forwardmeasure-openworkflow/scripts/build-bounded.sh`, also copied in
  FEI) runs every build: always pass an absolute `-f` (it changes directory). It takes the lock
  without blocking: if another bounded build holds it, it prints "Another bounded ForwardMeasure Maven
  build is running." and exits 1, so chained builds must wait on an explicit completion marker. It
  runs Maven in a memory-limited cgroup scope (defaults 32G high, 40G max, 4G swap, overridable with
  `FWD_BUILD_MEMORY_HIGH`, `FWD_BUILD_MEMORY_MAX`, `FWD_BUILD_SWAP_MAX`).
- **Compile check** of production and test sources:
  `-B -ntp -Dmaven.build.cache.enabled=false -DskipTests -DskipITs -Dmaven.test.skip=false test-compile`.
- `build.sh --module <module>` repairs incrementally; `build.sh --run-tests` enables coverage.
- Do not edit a repository's sources while a build of that repository is running.
- After renaming or removing a type, field or method that other modules use, or after changing a
  compile-time constant, run `clean` builds on every downstream module: Maven's incremental compiler
  does not notice a changed dependency jar, and constants are inlined into consumers.
- Quarkus's real build-time validation runs only in `mvn install`, not in `compile`.
- Do not add `-am` out of habit when dependencies are already built; but if an upstream module
  changed in this session, install it first or use `-am`, or the test runs against a stale jar.
- Keep the Maven cache; do not wipe `~/.m2` or the build cache.
- Build and install the platform contracts and core BOM before a standalone FEI build; test-support
  classifiers are produced at `test-compile`. Install FEI's API specifications and common-models
  modules together, or code generation unpacks a stale specification.
- A slow build step is first checked against the build scope's cgroup memory throttling (compare
  `memory.current` with `memory.high` and watch the `high` counter), after reproducing the step
  outside the scope. Never kill processes or skip steps before the cause is proven.
- **JVM crash workaround:** container and test JVMs on the build host intermittently crash in the C2
  JIT. Test runs carry `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1`, the same in `MAVEN_OPTS`, and
  `-Dforwardmeasure.testcontainers.opensearch.jvm-options` and
  `-Dforwardmeasure.testcontainers.keycloak.jvm-options` set to the same flag (Docker services do not
  inherit the test JVM's options; `forwardmeasure.acceptance.java-tool-options` passes them). Results
  obtained this way are labelled as C1-only and are "not production-default JIT/performance evidence".
- `MAVEN_OPTS` is set by the shell's `jdkNN` functions; `build-bounded.sh` appends its own heap
  settings, and the last value wins.

### 22.6 Maven build cache

- The platform owns `.mvn/extensions.xml` (Apache maven-build-cache-extension) and
  `.mvn/maven-build-cache-config.xml`; FOWF, FDS, FEI and FDE link to them, so those checkouts need
  the platform sibling. There is no remote cache; three entries per artifact are kept locally.
- Library outputs can be restored, but compiler, build-helper, OpenAPI generation and dependency
  unpack/copy goals always execute to register reactor artifacts and generated-source roots.
  "Skipping these lifecycle side effects broke downstream resolution and generated JPA metamodel
  Javadoc."
- Frontend and Dockerfile-layout modules opt out. Runtime packaging always runs. "This initial scope
  deliberately does not promise full deployment assembly reuse."
- Cache inputs include sources and resources, schemas and protobuf, Liquibase files, frontend lock
  files and the JDK release file; not node modules, frontend build output or browser test reports.
- Surefire and Failsafe, frontend execution, validation, install and deploy, and image publication
  always run. "Test result reports are not attached as fresh execution evidence." "Do not count
  historic cached reports as tests run in the current invocation." "Cache hits never constitute fresh
  execution evidence."
- "Disable cache if a generator's input/output contract is not captured; extend the policy and
  regression before restoring reuse." Use `-Dmaven.build.cache.enabled=false` for fresh evidence.
  `--no-build-cache` disables the Maven cache; `--no-cache` is the separate Docker layer cache switch.

### 22.7 Test-contract packaging

- Reusable test contracts live in `src/test/java` and are attached as a test jar at `test-compile`.
  Each framework leaf consumes them with `<type>test-jar</type><scope>test</scope>`.
- Fixture resources live in `src/test/resources`. Fixture dependencies never enter deployment runtime
  dependencies.
- After relocating test classes, clean only those fixture modules: "Do not repeatedly clean upstream
  modules and discard their coverage evidence."

## 23. Testing

### 23.1 Philosophy

- **Real infrastructure, always.** Tests run against real PostgreSQL, Kafka, Keycloak, OpenSearch,
  object storage, Valkey, Cassandra and Kubernetes (K3s) through `forwardmeasure-testcontainers`.
- **Never H2.** "We never deploy against H2. We deploy against PostgreSQL, Keycloak, OpenSearch, etc.
  using our testcontainer framework." Never write "H2 or Postgres" or suggest H2 as a lighter option.
- **No Mockito or any mocking framework.** "No Mockito or H2. A small handwritten double is permitted
  only for a justified narrow condition and must never be presented as real integration evidence."
- **No self-agreeing fakes.** A fake server written by the same hand as the code it tests can only
  prove the two agree with each other. For external protocols (OIDC, REST APIs, brokers) use the real
  thing; "never H2, always testcontainers" extends to authentication providers.
- **Tests exist to find real failures.** "The user explicitly requires meaningful regressions
  protecting real behavior, not perfunctory tests added merely to clear coverage gates... Coverage
  reports identify missing scenarios; they are not the behavioral specification." "More tests should
  mean more failure modes covered, not cloned assertions in many classes." A bug a test finds is
  fixed, not asserted around.
- **Never shrink a test.** "Shrinking a test is a brutal violation of my ask." Never write a narrower
  test than full coverage, never drop operations or error cases, never substitute a 401-only smoke
  test or direct class construction for real HTTP calls, and never offer a narrower version as an
  option. If the full test is expensive, build the fixtures. Asking permission to shrink is not a way
  out; full coverage is the directive.
- **"Live-verified" has one meaning:** every operation in the capability's OpenAPI specification has
  had a successful authenticated call, plus its error cases, through a real running server with real
  PostgreSQL and Keycloak, on each framework. The agreed shape: one shared contract per capability
  (an abstract contract with a subclass per framework), every operation and error case, plus a check
  that diffs specification `operationId`s against operations exercised.

### 23.2 Tests through the REST API, and the direct-test layers

**Rule (owner, 2026-10-07):** tests never construct repositories or service implementations
directly (`new XRepository()`, `new XServiceImpl(...)`, a hand-built `EntityManager` harness). They
invoke behaviour through the product's real REST API, in the deployment-leaf tests, on Quarkus,
Spring and Micronaut, against real infrastructure. Coverage is met through REST-level tests (FEI's
aggregate coverage merges every module's execution data, so deployment-leaf tests count toward
domain coverage). Behaviour no API reaches is surfaced as possibly dead code, not tested directly.

**Complementary layers (agreed 2026-10-07, restated as a product-owner decision 2026-10-10):** "The
user explicitly agreed to both direct tests and REST tests, after discussing the limits of REST-only
coverage. Preserve useful focused coverage and add missing acceptance coverage. Quarkus, Spring and
Micronaut are all required. The earlier FEI REST-only handover is superseded on that point."
Legitimate algorithm and storage tests must not be deleted under the REST-only interpretation. How
the two statements fit together is recorded as an open question; both agree that hand-constructed
repositories and services are never acceptable.

| Layer | Correct entry point and infrastructure | What it proves | What it cannot prove |
|---|---|---|---|
| Pure algorithm or value object | Construct the component; fixed inputs | Mapping, normalisation, matching and invariants | Dependency injection, HTTP, authorization or persistence |
| Business service integration | Production service from its framework container; real migrated PostgreSQL and other services | Service rules, managed transactions, persistence, tenant routing, concurrency | HTTP binding or JWT admission |
| Repository, connector or storage contract | Real persistence adapter, real database and schema, shared fixtures | Queries, locking, rollback, paging, backend parity, recovery | Application wiring or admission |
| HTTP contract and acceptance | Real framework application; real HTTP; real identity and AuthZEN | Routing, DTO binding, validation, authentication, authorization, dependency injection, observable behaviour | Worker or image dispatch unless that path runs |
| Workflow and worker acceptance | Admit through the owning API; dispatch current packaged workers; observe terminal state and effects | Cross-process ingestion and recovery | Cells not executed |
| Studio acceptance | Real browser and API stack | The usable flow and displayed state | Other frameworks or backends without runs |

### 23.3 Test rules

- "Do not describe a direct call to a `Resource`, launcher or service as REST acceptance. Do not use
  an in-memory repository or permit-all authorization in an acceptance fixture."
- "Production-managed service tests must resolve the actual production factories/producers and
  transaction interceptors. Moving `new Repository()` into a shared test helper does not meet that
  requirement. Never add production methods or public endpoints solely to make coverage convenient."
- "Before writing a test, record its concrete failure… Choose assertions that would fail with that
  defect present… do not derive expected output with the mapper/normalizer under test." Every fixture
  states the defect it detects.
- "Avoid assertions such as 'not 500', 'non-null', '202 returned', or 'a method was called' as the
  sole acceptance result. Retrying must have a bounded deadline and report the last observed state.
  Do not use broad catches, skipped failing tests, lower thresholds or extra exclusions to pass a
  gate."
- "Framework adapters must still boot separately." "Code with no REST caller may serve a worker,
  scheduler or recovery loop; trace those callers before labelling it unreachable."
- "Calling a binding's factory method directly can prove its method body calls `start()`, but cannot
  prove that the framework ever invokes it during startup… require the real application to become
  ready and execute a public request before declaring the framework operational. Do not create
  production-owned topics in a fixture to hide this kind of startup defect."
- Boot real applications with shipped configuration. Lazy beans, missing providers, Micronaut
  duration and placeholder parsing, Jackson/Serde differences, Quarkus bean removal and Spring
  conditional and JPA auto-configuration are all things to test for.
- "Never use permit-all authorization as acceptance evidence." "A test that gets past a denial by
  making authorization or network policy permissive invalidates its acceptance evidence." "HTTP
  protocol fixtures are not substituted for real PDP integration evidence"; fault injection through a
  proxy in front of real Keycloak is "not a stubbed successful authorization service". Global role
  grants must not substitute for organization policy tests.
- Authorization tests cover two organizations under one caller with a grant in only one, fail-closed
  behaviour on policy outage, and revocation and cache expiry.
- Token negative cases: missing, invalid or expired tokens; wrong audience; a real same-signing-key
  wrong issuer; valid but denied identities; inactive organization; tenant spoofing; cross-tenant
  access. Valid requests must keep working after a rejection. "Positive JWT fixtures must obtain a
  genuinely signed token… Negative cases should change one boundary at a time… A forged-claims case
  must retain the original signature."
- "One Quarkus result does not establish Spring or Micronaut parity." "This is a nine-cell acceptance
  matrix, not three names attached to one runtime." "Changing a parameter label while invoking one
  shared fake runtime does not create a matrix." "Do not infer protocol parity from compilation or a
  passing Kafka adapter test."
- "Posting authenticated canonical engine events tests the API projection contract. It does not
  establish that real engines emit those events… Do not backfill a passing matrix result from
  component tests or source compilation."
- "`RUNNING` is a START acknowledgement, not evidence of ingestion." An unknown-revision 404 is not
  dispatch evidence. Acceptance requires workflow terminal success, the product's own status and the
  persisted effects ("READY or HTTP 202 alone is insufficient").
- "Never assume framework mapper defaults when a deployment supplies a custom producer."
- Studio acceptance covers lossless authoring and round-trip, validation, publication and live
  control through generated clients. "A TypeScript compile or screenshot is not browser/API
  acceptance."
- Recovery fixtures seed the journal ("Do not re-enable legacy snapshot recovery to rescue a
  fixture"). Golden wire payloads are appended, never replaced.
- Persistence-context regressions preserve the triggering state; do not clear the context before a
  merge.
- Compiler diagnostics in assertions "exclude random temporary paths".
- Regression records preserve missing fields as well as populated values.
- Fixtures are synthetic or approved bounded data with independent expected results.
- The gateway hop belongs in the K3s matrix; direct Keycloak containers bypass it.
- Each bug fix has a reproduction, regression code, compilation evidence, the exact image rebuild
  scope and post-deployment verification.

### 23.4 Test infrastructure rules

- Use `forwardmeasure-testcontainers` entry points (`PostgreSqlTestContainer`, `KafkaTestContainer`,
  `OpenSearchTestContainer`, `KeycloakTestContainer`, `KubernetesTestContainer`,
  `MinioTestContainer` and their `With...Container` JUnit annotations, the GCS emulator, Valkey and
  Cassandra fixtures) and shared helpers (`PublicWorkflowAcceptanceClient`, `RealFowfWorkflowFixture`,
  `AuthzenKeycloakFixture`, FEI's `WithMigratedEntityIntelligenceTenant`). If a wrapper is missing,
  add it to the library; raw containers are a consolidation item, not a reason to drop backend
  assertions. The shared library itself needs its own tests.
- Destructive tests use isolated tenant databases.
- Business data is created through the API under test. Test-only Liquibase fixtures are only for
  prerequisites with no creation path. SQL is only for catalogs, privileges, upgrade snapshots and
  limited observation. Read JDBC booleans with `getBoolean()` and check for NULL.
- No `hbm2ddl=create-drop`.
- Concurrency tests use distinct transactions, and include a case with no outer transaction to prove
  the interceptor.
- The e2e harness mirrors production: a container superuser "hides grant bugs", and a shared runtime
  role "would hide exactly the grant bugs this harness should catch". Ownership tests run as a
  non-superuser admin with `CREATEDB`/`CREATEROLE`.
- Provision every participating identity per tenant, including worker service accounts.
- **Packaged-image tests** run product images inside K3s through `KubernetesTestContainer`. Coverage
  of those images is collected with `withJacocoCoverage(<image prefix>)`, which injects the JaCoCo
  agent through a `MutatingAdmissionPolicy` and copies the execution data out at close. Images must
  be rebuilt from current code first, or JaCoCo silently drops mismatched classes. On SELinux hosts,
  coverage files stay inside the node container.
- When a K3s run reports success but wrote nothing, read the line coverage of the K3s execution data
  and reproduce on the image's own classpath.
- Keep logs outside `/tmp` (a reboot loses them). Capture diagnostics before teardown, without
  credential-bearing pod environments. Refresh Docker port mappings after a restart. Apply Helmfile to
  the intended namespace. Serve protobuf as `text/x-protobuf`. The egress host separator is `|`.
- Workers that build an OpenSearch client need `commons-logging` at runtime in each framework leaf;
  and workers must catch `Throwable`, not only `Exception`, so errors are never counted as success.

### 23.5 Matrices

- **The nine supported installations** (3 frameworks × 3 runtime configurations):

  | Cell | Framework | Engine | Engine durability |
  |---|---|---|---|
  | Q-K | Quarkus | Kafka Streams | Kafka topics, state stores, changelogs, offsets |
  | Q-PG | Quarkus | Pekko | PostgreSQL journal, snapshots, projections |
  | Q-PC | Quarkus | Pekko | Cassandra journal, snapshots, projections |
  | S-K, S-PG, S-PC | Spring Boot | as above | as above |
  | M-K, M-PG, M-PC | Micronaut | as above | as above |

- Record the framework of every participating FEI, FDS and FDE service. "A Spring FOWF run against
  Quarkus-only product services does not establish an all-Spring installation."
- No pairwise sampling, and no arbitrary mixed-framework combinations without a contract basis.
- Every conclusion has three dimensions: contract conformance ("Identical defects are not
  correctness"), behavioural parity and architecture compliance.
- Any full-parity statement is tied to an explicit contract revision and evidence baseline. "No known
  difference in these tested cases" is not an unqualified 100% claim. "Do not conclude '100% parity'
  because the current tests pass."
- Matrices certify their recorded image identities, "not every subsequent source change or whatever
  is currently deployed".
- Deployment-selection rendering covers all 18 framework × engine × FDE combinations, snapshot
  propagation, unknown values, false flags, existing-engine detection, recorded-selection mismatch,
  FDE retirement, repeated install and namespace rollback.

### 23.6 Required acceptance scenarios

- **FEI:** an authenticated public request; premium CSV, schema, mapping and index documents
  supplied separately (with a differing default overridden, to detect ignored fields); all record IDs
  and rejected counts; workflow terminal success, FEI revision status and OpenSearch documents; index
  ownership, strict mappings, `source_data` and `_source` round-trip; screening of a person and of an
  organization with a blank first name, plus a negative; simple, correlated and resolution ingestion
  with correlation key and cardinality errors; full and delta, updates and deletes, activation,
  rollback, retention, idempotent retries, concurrent triggers, tenant isolation and index ownership;
  malformed inputs, worker failure, callback retry, restart and partial progress; Studio through the
  real API; all nine cells.
- **FEI population contracts** (scenarios R1 to R18): standard population with a visible fixed
  target; a single custom document fails before side effects; compatible and incompatible custom
  pairs including vendor field type mismatches; target changes through update, trigger or pipeline
  rejected; two input versions on one target with real indexed documents; changed hosted files or
  defaults after admission do not affect replay; full and delta keep target identity and an
  incompatible baseline is rejected; WorldCheck through REST produces the expected OpenSearch fields
  and raw data; Customer Master monitored ingestion through REST produces resolution records, assertions
  and membership; two populations resolving to the same entity (through merge) keep distinguishable
  supports; idempotent repeat; removal or scoped refresh leaves other supports, populations and
  entities intact; failed or partial refresh withdraws nothing; merge, re-resolution and reversal keep
  provenance without duplicates; investigation-only entities need no membership and keep
  authorization; screening selects members, deduplicates, reports unresolved members and keeps
  provenance; cross-tenant, collision and unauthorized operations are rejected; Studio forms send the
  intended requests and display errors. Public API scenarios run on all three frameworks, covering
  simple and correlated paths and each affected worker runtime. "Do not claim full FOWF/FDS/FDE
  parity from this focused change."
- **FOWF lifecycle and overflow:** real publication, admission, controls, recovery and
  human-task races, and nine-cell overflow with real transports, shared object storage and each
  durable backend. The scenario lists and the remaining verification rows (T-01 to T-17) are in
  [FOWF multi-framework testing §8](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md#8-remaining-verification-work).
- **FDS:** start through the launcher's HTTP API; direct and FOWF; bounded and continuous; Pekko,
  Kafka Streams and Spark; Spark staging plus delivery Jobs; malformed rows, key policy, read-committed
  frontiers, sink failure, timeout and cancel, retry and idempotency, and drain.
- **FDE:** real gRPC on each packaged server; shared PostgreSQL and Keycloak; tenant registry through
  provisioning; audience mapper; two actors in different tenants with the same ruleset names and keys;
  evaluate, manage and admin separation, with denials leaving state unchanged; golden evaluations,
  pinned versions, cache lifecycle and health; the FOWF adapter path tested separately.
- **Identity:** an empty realm and one complete install; two registered tenants; concurrent
  tenant-specific tokens; a non-member or mismatched tenant; interrupted secret adoption and rotation;
  repeated reconciliation; composed Secrets; an API-created FEI tenant; configuration changes;
  boolean and inventory validation; the gateway hop on the K3s matrix.
- **Screening quality:** the two real-data gates in section 11.4.
- **Branding:** a K3s test with two tenants on two brands.

### 23.7 Evidence labels and reporting

- Evidence labels: not inspected; statically traced; fixture missing; fixture written; compiled;
  executed pass; executed fail; skipped or blocked; packaged-image verified; deployed verified.
- Units are not additive: deployment cells, scenario families, test methods and classes are reported
  separately.

| Evidence | Required report |
|---|---|
| Written | File, scenario, entry point, real dependencies, remaining fixture prerequisites |
| Compiled | Exact command, module and framework scope, log, exit status, whether test sources compiled |
| Executed | Selected tests, count, pass/fail/skip, framework and backend, actual image identity, log |
| Accepted | Required matrix cells passed, persisted effects verified, known gaps disclosed |

- "A test written behind an unset environment property is not executed. A skipped matrix cell is a
  gap. A reactor's formatting phase reporting SUCCESS is not a passing regression suite."
- Record commit (and whether the tree was dirty), contract hash, fixture class and method, command,
  JVM, framework and backend, image tag and digest, log hash, counts and persisted-effect assertions.
  Do not double-count reruns. Keep failed attempts; never relabel a failed run as successful.
- "Never equate compiled with passed." "Compilation is not test execution." "Rendered YAML is not
  runtime proof." "Offline Helm rendering and syntax checks are separate from behavioural proof."
- Report feature work on these dimensions: implemented, fixtures written, compiled, tests executed,
  images built and pushed, deployed, exercised through Studio.
- Report in plain language for a person: what was tested, what happened, what it means, what is
  next, with times rather than run numbers.

### 23.8 Delivery order

1. Complete and review the code.
2. Generate contracts and clients, compile, build production frontends, render Helm.
3. "Run one clean full regression after the code is complete."
4. Clean K3s deployment and authenticated, tenant-routed browser acceptance last.

"Broad regressions are deliberately not repeated while implementation is still changing. Focused
static checks and compilation remain valid development feedback; they do not count as production
acceptance." "Finish all fixture implementation first, then execute remaining matrix runs." Tests,
image builds and pushes, and rollout are separate phases. Closure evidence for a release records
contract and client generation and drift; Java compilation and static analysis and frontend builds;
Helm lint, schema and render; one clean full regression; clean K3s deployment and recovery;
authenticated tenant-routed browser acceptance for each UI; and a disposition for every finding.

A feature's implementation is complete when: the API, domain, migrations, services, workers and
Studio implement the agreed invariants; every regression scenario is written and wired into the
framework suites; affected production and test sources compile through the bounded wrapper and
frontend checks pass; there is no placeholder, silent unsupported path or unreported blocker; and the
hand-off names the exact images, build commands and fresh-tenant deployment order, with an evidence
table (scenario or module, written, compiled, executed, result, log). Completing one workstream of a
larger request is a milestone, not completion of the request.

## 24. Coverage

- **Thresholds:** 85% line and 85% branch coverage on handwritten Java "across the stack", set in the
  platform parent. Aggregates also require zero missed classes (`jacoco.minimum.line.coverage` 0.85,
  `jacoco.minimum.branch.coverage` 0.85, `jacoco.maximum.missed.classes` 0). (This replaced the earlier per-product
  85% line, 75% branch and zero missed classes; "Old 75% branch text is superseded.")
- **Generated code is exempt** (owner instruction, 2026-10-07): identified by generated-source
  provenance or retained `Generated` annotations, with exact class exclusions written to
  `target/coverage-generated-excludes.txt`. Whole packages are not exempted. "Unknown provenance
  remains subject to coverage." Handwritten adapters are not generated code, even in mixed packages.
- **Collection:** `build.sh --run-tests` enables the `coverage` profile; direct Maven regression
  builds add `-Pcoverage`. Coverage applies to Java, not to browser or Python code. Missing or empty
  execution data for handwritten classes fails through the `require-coverage-data` goal; explicit
  test-skip flags allow compilation without claiming coverage.
- **Aggregates:** JPA, OpenWorkflow and FEI use product aggregate checks. A module's
  `jacoco.module.check.skip=true` exception is honoured only when a selected aggregate includes the
  module at the same version, stages its classes, and reports and checks it with the same thresholds;
  aggregates reject missing merged data. FDS and FDE are checked through their configured ownership
  "rather than inventing a new aggregate". Component selection needs an explicit inventory.
- Coverage from remote and container tests does not contribute automatically; packaged services need
  the injected agent, dumped before shutdown, with class IDs checked.
- "Thresholds must not be lowered to get a build through. Passing compilation is not evidence of
  passing these gates." Lowering a threshold is shrinking a test.
- "Coverage measures execution, not semantic parity. 100% coverage would not prove 100% correctness."
- Never add perfunctory tests, weaken behaviour checks or add production methods to meet a number.
  Report coverage progress only as measured numbers from real runs.

## 25. Licensing

- **Open repositories are Apache-2.0** (the platform, FOWF, FDS, `forwardmeasure-jpa`,
  `forwardmeasure-object-storage`, `forwardmeasure-testcontainers`, `forwardmeasure-presentation-system`
  and the other shared libraries). Every new source file gets the full 14-line ASF header.
- **FEI is proprietary** ("FEI is not Apache 2.0"). Every FEI file carries this header, in the file's
  comment syntax:

  ```
  Copyright (c) 2026 ForwardMeasure, Inc. All rights reserved.

  This file is part of ForwardMeasure Entity Intelligence and is
  proprietary and confidential. Unauthorized copying, modification,
  distribution or use of this file, via any medium, is strictly
  prohibited without the prior written permission of
  ForwardMeasure, Inc.
  ```

  The FEI repository has a `LICENSE` file and its POM declares a proprietary licence. FEI's RAT
  configuration approves the proprietary family and denies the Apache family, so an ASF header in FEI
  fails the build. Do not assume any other repository is proprietary without asking.
- **RAT runs at `verify`** in every repository that inherits the platform parent (Markdown is
  excluded in the platform repository). Before handing off a build, check: RAT offline, per module
  (Maven runs it from each module's directory, so root `.gitignore` rules do not apply); every
  dependency without a `<version>` in a changed POM is managed by the platform or an imported BOM; and
  each changed module compiles. JVM crash dumps (`hs_err_pid*.log`, `replay_pid*.log`) are excluded.
  Treat a RAT run with no "Unapproved:" summary as a failure, and prove the check by reproducing a
  known failure before trusting a pass.
- "Original resource licensing is preserved explicitly; moving files does not relicense them."
- Third-party UI assets: Lucide icons (ISC), bundled fonts (OFL).

## 26. Release and platform admission

- Products release independently with their own version and product BOM. Charts are released by the
  release script, never by hand-edited versions.
- Admitting a product revision to the platform release train:
  1. Commit to the product's `develop` branch.
  2. Publish an immutable release with SCM metadata, an SBOM and a provenance attestation.
  3. Select the version in `forwardmeasure-platform-bom`.
  4. Carry the product's settings into its own deployment configuration and the umbrella Helmfile
     values (never copying retiring Data Fabric paths).
  5. Run the full platform verification build (`mvn -B clean verify` on Java 25 and Maven 3.9.9 or
     later) and `./deploy/validate-platform.sh <environment>`.
  6. Verify attestations and immutable container references.
- Dependency changes outside a product need runtime acceptance before release.
- Production-complete language in roadmaps waits for evidence.

## 27. Working rules for engineers and coding agents

These are the owner's standing rules for how work is done. They apply to people and to coding
agents.

### 27.1 Truthfulness and verification

- **Never say "ready", "done" or "deployable" without live, end-to-end verification.** Use specific
  language instead: "compiles clean", "chart renders clean (not yet applied to a real cluster)",
  "unit-tested, no live integration test". When asked whether something is ready, list exactly what
  has and has not been verified.
- **Confidence must match verification.** If a claim can be checked in a few minutes with the tools
  at hand, check it before stating it. Do the check that would falsify the claim, not only one that
  confirms it. When uncertain, say so in the first statement, not as a later correction.
- **Verify fixes against the running system** (a live HTTP call, a debug pod, a direct database query,
  decompiled bytecode) before calling them fixed. The same applies to claiming something is broken or
  rating its severity: write and run the smallest reproduction when feasible; static reading of
  dispatch logic is not a substitute.
- **Verify functional fidelity, not just compilation.** Trace a real data path through the reference
  behaviour.
- **Do not trust reports.** Treat a handover, gap report, plan file or memory as a hypothesis until
  checked against current code. Compare the dates of conflicting records and prefer the newer.
- **No speculative diagnosis.** Prefer observable facts over inferred chains; if checks are
  inconclusive, say "I don't have enough to explain this". Distinguish "confirmed" from "best guess"
  out loud. Read the full exception text, including any "precise causes" prose above the stack trace.
- **Decompile before declaring something unresolved.** After two or three reasonable fixes fail
  without changing the symptom, read the dependency's actual bytecode or source. Bytecode shows what
  can fail; when the real component is one `docker run` away, run it and read its own error.
- **Check the packaging manifest for the real entry point** (`<mainClass>`, Dockerfile `ENTRYPOINT`,
  Helm `command:`), never infer it from which class has a `main` method.
- **A security claim is a claim:** check the rule's actual behaviour before stating who can reach
  what.
- **Do not overclaim blockers either.** Before calling something blocked, check whether it is just an
  action not yet taken (a scoped install, a configuration value).
- After deleting, moving or renaming a class, search the whole repository for its name, including
  Javadoc and prose.
- When a bug has a shape, sweep for siblings of the same shape in reachable code.

### 27.2 Scope

- **Always build to 100% parity.** Never cut scope to call a phase done, never make a functional
  sacrifice for a deadline, and never ask whether to "restrict" or "build" when a parity gap is found:
  build it. "We never make functional sacrifices just to make some artificial deadline."
- **Never defer a real capability unilaterally.** Before writing code that does less than full
  parity (throwing `UnsupportedOperationException`, skipping a matrix cell, choosing one engine or
  framework), stop and ask, unless the owner has scoped it out in their own words. Presenting "leave
  it unbuilt" as one of two equal options is itself a unilateral deferral; identify the gap and
  recommend closing it.
- **Fix bugs as you find them,** in any module, without asking whether they are in scope. Never
  soften a defect ("harmless", "worth knowing") or park it as an offer. Only a genuine design decision
  goes to the owner as a question. Known bugs are not a menu to prioritise; fix them all and mention
  optional hardening separately.
- **Do the complete, literal thing asked.** Do not privately decide that some remaining asymmetry is
  fine.
- **Never drop an engine, framework or capability** to compensate for a defect; fix the architecture.
- **Finish in-progress work before opening a new area,** weighting "closest to done" above "biggest
  new chunk".
- **Once a plan is approved, keep going** through the whole plan; stop only for a decision only the
  owner can make or a genuine external blocker.

### 27.3 Decisions

- **Surface architecture decisions explicitly.** When a real fork exists (for example a worker
  reaching into the database directly versus calling the product's own API), present it before
  choosing, especially for what a process may touch directly and for patterns that will be copied.
- **Design from requirements, not code.** Design items must be requirements the owner stated or
  confirmed. Anything inferred from current code goes in a separate list labelled "current behaviour,
  unconfirmed". Where a requirement is unknown, ask. Never put invented text in quotation marks as if
  the owner said it.
- **Platform-native over workaround scripts.** Lead with the component's proper mechanism (for
  example Cruise Control for Kafka replication changes), offer a workaround only as a labelled
  stopgap, and wait for the owner's choice before writing code.
- **Mirror proven reference configuration fully;** narrowing it is a proposal, not a default.
- **Take the owner's stated actions as given;** answer only what their action does not cover.
- **Edits during live deployment:** while the owner is actively deploying, do not edit charts,
  release files, configuration or secrets without stating the cause, the exact change and the
  verification, and getting a yes. In ordinary development work, fix the issues you identify, verify,
  then report.

### 27.4 Builds, deployments and cost

- **Builds:** the owner runs long builds and test suites by default (the owner has at times asked an
  agent to run them for a testing programme). Scoped compile and install checks without tests are
  allowed for verifying one's own edits. Never run a full unscoped reactor "just to be safe".
- **Command hand-off format:** a single unbroken line with no `cd` prefix, teeing output to a file;
  in a sequence, one file per repository (`/tmp/mvn-<repo>.out`). A scoped verification build has
  the form `mvn -Drat.skip=true spotless:apply install -DskipTests -pl <modules> -am 2>&1 | tee
  /tmp/<name>.mvn.out`. When the owner returns a log path, read its summary lines rather than the
  whole file.
- **One build at a time;** never two Maven processes or two installers at once.
- **Ask before any live apply** and offer the owner the choice of running it.
- **No manual deploy steps;** build the automation. Do not reopen settled decisions (for example the
  fake secret store).
- **Never bump chart versions by hand;** fetch and compare with `origin/develop` before reasoning
  about chart state.
- **Never type an image tag literal;** let it inherit `${project.version}`.
- **Always write tfvars values explicitly.**
- **Cost awareness:** state the cost and get agreement before launching more than one or two agents
  or any long test run; do not re-run a suite to re-confirm something proven; when the owner says stop
  running tests, run none. In a very large context, batch work into fewer, larger steps and propose a
  fresh session.
- **Commit status is not a readiness signal;** report functional state only, and leave commits to the
  owner.
- In shell automation, never use `pkill -f` or `pgrep -f` with a pattern naming the target script
  (it matches the calling shell); select processes by exact argument vector.
- A context gap about code written earlier is described as such ("I don't have this file's history
  loaded"), never as code that is "unrecognised".

### 27.5 Documents

- When rewriting a person's document, keep all of its content unless they agree to a cut; map every
  source section to its destination first. A rewritten document reads as a standalone current
  document and is delivered in the requested format.
- Write for a human reader: plain sentences, explained abbreviations, no private shorthand.
- The Data Fabric repository is off limits: do not search, read or reference it unless the owner
  names a specific path for a specific purpose, and never present it as a design option.

---

# Part IV: Reference

## 28. Superseded decisions and recorded conflicts

### 28.1 Superseded decisions

| Topic | Earlier decision or name | Current rule |
|---|---|---|
| Product names | OKS, OKS Studio, Entity Intelligence Workbench; artifacts `oks-api-contract`, `oks-api-client-java`, `oks-api-client-typescript` | OpenWorkflow (FOWF), OpenWorkflow Studio, FEI Studio; `openworkflow-api-specifications` and generated bindings |
| Workflow engines | Standalone `openworkflow-kafka-streams` and `openworkflow-actor-engine` repositories | Both engines in `forwardmeasure-openworkflow`, one product entry, both permanent |
| Definition lifecycle | `DRAFT → IN_REVIEW → APPROVED → PUBLISHING → PUBLISHED` with definition maker-checker and an author/reviewer publisher pair | `DRAFT → PUBLISHED → DEPRECATED`; maker-checker only for human tasks; one publisher identity (2026-10-04) |
| Workflow lookup | Configured `*_WORKFLOW_ID` values; per-product publishers (for example FEI's own publisher image) | Lookup by `<domain>-<name>`; one generic FOWF publisher and workflow-bundle chart (2026-10-01) |
| Workflow resource URLs | Hard-coded `https://specs.forwardmeasure.com/...` (a domain that never existed); `did:datafabric:` entity IDs | DID URLs `did:<method>:bundle:<domain>/<file>` and `did:<method>:entity:<tenantId>:<uuid>` (2026-10-03) |
| Tenant isolation | PostgreSQL schema per tenant (`SET search_path`); later a hex-named database per tenant | One alias-named database per tenant, `forwardmeasure_<alias>`, with product schemas |
| Shared databases | Shared `openworkflow` database (a stopgap) and an unused `platform` database holding the registry | `forwardmeasure_control_plane` holds the tenant registry and Pekko coordinator store |
| Tenant reference | "Tenant alias" wording; a configured `tenants[].id`; the database name carried through authorization, commands, events, journals and workers | `TenantId` only, always derived from the DID; the database name stays in persistence (2026-10-04) |
| Admin database role | `openworkflow_migration` | `forwardmeasure_admin` (2026-09-29) |
| Database environment variables | `<P>_DATABASE_URL`, `<P>_DATABASE_USERNAME`, `OPENWORKFLOW_TENANT_REGISTRY_*`, the `ENTITYINTELLIGENCE_*` spelling | `<P>_CONTROL_PLANE_DATABASE_URL`, `<P>_TENANT_DATABASE_HOST/PORT`, `<P>_RUNTIME_DATABASE_*`, `<P>_ADMIN_DATABASE_*`; no fallback |
| Product role provisioning | FOWF's `additionalRuntimeDatabases` and per-product database ownership handed to runtime roles | OpenTofu creates component login roles; migration Jobs create product runtime roles; the admin owns tenant databases |
| Query style | Manifesto preference for JPA/JPQL | Criteria API over the JPA static metamodel |
| Coverage | Per-product 85% lines, 75% branches, zero missed classes | Platform-wide 85% lines and 85% branches, zero missed classes on aggregates, generated code exempt (2026-10-07) |
| Error contract | `ErrorResponse`, `DetailedErrorResponse` and multiple `common-definitions.yaml` lineages; later `Problem` generated in FOWF common-models | RFC 9457 `Problem`/`Violation` generated once by `forwardmeasure-platform-api-contracts`, with inline snapshots (2026-10-08/09) |
| Shared request handling | `openworkflow-common-server-jaxrs`, `openworkflow-micronaut-validation` in FOWF | `forwardmeasure-platform-server-jaxrs`, `forwardmeasure-platform-micronaut-validation` and the security modules in the platform |
| Provider contracts | An interim FEI ingestion-contracts module; JSON contract names | `forwardmeasure-platform-data-contracts` with YAML contract names |
| FEI target model | A compiled `TARGET_MODEL_CLASS` (WorldCheck shape) for every reference load | Schema-driven ingestion, no compiled target model |
| FEI population contracts | Editable default schema, mapping and index URIs; per-run overrides; index-mapping resolution order "revision override, then population default, then canonical default" | Target contract frozen at population creation; named, versioned input definitions; per-run target and index overrides removed (2026-10-12) |
| FEI Liquibase for population contracts | Plan to edit the initial table definitions directly | Forward changesets (deployed history is never edited) |
| FEI workers | Workers connecting directly to the tenant database (a decision never put to the owner) | Workers call FEI REST through generated clients with workload identities |
| FEI Studio location | A dedicated hostname | Tenant-routed `/ei/studio/` on the tenant host |
| FDS engines | Decision D4: no Kafka Streams executor until a continuous case exists | Two permanent delivery engines (2026-09-20, confirmed 2026-10-04) |
| FDS errors | `ErrorResponse` | RFC 9457 `Problem` on all three frameworks |
| FDE identity | Unsigned internal-caller trust model with a `tenant-id` header | Verified organization JWT plus AuthZEN evaluate/manage/admin capabilities |
| Deployment state | "Nothing is deployed" (2026-09-29) | Software is deployed (since 2026-10-06); forward migrations only |
| Platform Dashboard location | In the platform repository | `forwardmeasure-platform-operations` |
| Shared UI packages | `@forwardmeasure/ui-*` packages in a future `forwardmeasure-ui` repository | `@forwardmeasure/presentation-system` in `forwardmeasure-presentation-system` |
| UI routes | `/ei` for the Workbench "during coexistence"; `/agents` for OKS Studio | `/ei/studio/` and `/owf/studio/` |
| Orchestration invariant | "Kafka Streams decides and durably records workflow progress" | The deployed engine (Kafka Streams or Pekko) decides; workers never advance the graph |
| Service clients | One service client per tenant; first-tenant claim lists; Keycloak chart mutating tenant membership; Keycloak-generated, unrecoverable credentials; FDS and FEI realm-admin client Jobs; Job and ESO both writing Secrets | Central nine-client inventory; additive capability-pack memberships reconciled in Java; OpenTofu-generated secrets; ESO as the sole Secret writer |
| Shared Helmfile values | Copied files and sync scripts (`sync-platform-endpoints.sh`, `sync-chart-versions.sh`) | One shared layer loaded by relative path |
| Engine configuration | Two engine URLs and `OPENWORKFLOW_DEFAULT_ENGINE` | One engine per installation (`OPENWORKFLOW_ENGINE_ID/_URL`) |
| Image builder | Positional framework and engine arguments | Shared deployment selection |
| Service Helm chart | A Quarkus-only chart | The framework-agnostic `java-microservice-helm-chart` |
| Build cache | Generator and dependency goals skipped on cache hits | Always executed |
| Screening scoring | Active-weight denominator; generic two-token containment floor; missing-evidence normalisation (unsafe: hits rose from about 92,000 to about 300,000) | Full configured denominator with missing evidence contributing zero; kind-specific rules; "Do not port as-is" |
| FDE conformance tests | An opt-in, unsigned conformance test with raw containers | Signed tokens, shared fixtures, real Keycloak, PostgreSQL and Valkey |

### 28.2 Recorded conflicts that are not resolved

| Topic | Positions | Working rule until resolved |
|---|---|---|
| The platform's build role | "Aggregate the Apache-licensed component repositories into one source build" versus "The platform root builds its own modules; the separate `reactor.xml` is the wider compatibility reactor… FEI remains outside" versus an older statement that the platform does not compile siblings | The `reactor.xml` description is the most precise; see open question 8 |
| Repositories in application services | Manifesto and FEI FARCH-07 prohibit them; FOWF PER-03 allows them | Apply the stricter boundary and record the conflict |
| REST-only testing | Owner rule of 2026-10-07 (behaviour through the REST API only) versus the agreement of 2026-10-07 restated on 2026-10-10 (direct and REST tests) | Never hand-construct repositories or services; keep legitimate algorithm and storage tests; prove behaviour through REST on all frameworks |
| Tenancy in shared libraries | Database per tenant (current FOWF design) versus schema per tenant in older shared-library documents and retained legacy code | Establish where each is actually used; do not assert either platform-wide |
| Agents | No separate agent runtime (UI decision); agents live on Agent OS (2026-10-07); Agent OS not established as the extraction agent's home (2026-10-12) | Ask before building agent runtime pieces |
| Long-name recall fallback | "Do not add a generic two-token fallback" versus "Preserve the controlled long-name recall fallback" | Ask before porting |
| Display-name key | `displayName` versus `platform.branding.displayName` | Probably a nested path; verify |
| Infrastructure location | `k8s-infrastructure/opentofu/<cloud>` versus `openworkflow-k8s-setup` | Production uses `openworkflow-k8s-setup`; new clusters the platform's OpenTofu |

## 29. Open questions

**Product direction**

1. **What is Agent OS for?** Its own repository documents no purpose. Is it the platform's agent
   runtime (and the home of the named agents and the information-extraction agent), should parts fold
   into FOWF or FEI, or should it be retired? How does it reconcile with the rule that an agent
   invocation is a FOWF execution with no second runtime model?
2. Is the "Agent Control Loops Using Kubernetes Reconciliation and OpenWorkflow" design the approved
   Agent OS design?
3. Resolved 2026-10-13 (owner decision): Agent OS uses FDE for policy evaluation, for now. Agent OS
   selects its evaluator with `agent-os.policy.evaluator`: `drools` is its gRPC client to FDE
   (`DroolsPolicyEvaluator`, via `agent-os.policy.drools-target`); `opa` calls an external OPA
   server. No parallel evaluation exists in code - a deployment uses one evaluator - so the earlier
   "evaluated in parallel" wording described an intended comparison that was never built. Neither
   is in use today: Agent OS is not deployed, and no OPA server runs.
4. FEI strategy sub-decisions: the beachhead buyer, an open or proprietary asset contract, and
   compliance targets per region.
5. Is the platform name "kriyagentic" or "Kriyagentic" in the applications?
6. Is the tenant portal a separate application from the Platform Dashboard, and what does a tenant
   user (as opposed to a platform operator) see on the dashboard?
7. Should the unused platform-operations Keycloak theme module be retired or integrated, and should
   the dashboard come under the central framework selection and build orchestration?

**Architecture**

8. Resolved 2026-10-13: yes, `reactor.xml` includes FEI and builds the whole stack (see section 3).
9. Where, if anywhere, do shared libraries still use schema-per-tenant code paths, and should they be
   removed?
10. Should FOWF's PER-03 be amended to match the stricter repository rule?
11. Should automatic FEI schema provisioning become an extension of tenant creation? (Not by making
    FOWF depend on FEI's current migration artifact.)
12. Removing a person's or service account's access to a tenant. The setup tooling only adds access
    to a tenant's Keycloak organization: deleting an entry from configuration does not take away
    access already granted, and issued tokens and cached authorization decisions stay valid until
    they expire. How should access be removed when someone must lose it? (This is tenant access,
    not the entities a monitored population contains.)
13. The human administrator "defaults to the first configured tenant" while first-tenant claim lists
    were removed: confirm the intended wording and mechanism.
14. Engine changes need a migration design (persistence histories, command identities, running
    operations, recovery positions). When is it needed?
15. When should per-tenant connection pooling (PgBouncer or managed pooling) be designed?
16. When should the remaining lineage-1 API error shapes (for example `forwardmeasure-nlp`) move to
    RFC 9457, and how should Agent OS adopt the shared request-handling libraries?
17. Who governs Python constraints and data-format versions, which are outside the Maven version
    sweep?
18. Resolved 2026-10-13 (owner decision): every FEI setting uses the `ENTITY_INTELLIGENCE_` prefix,
    and Java test properties use `entity-intelligence.`; no `FEI_` setting or `fei.` property
    remains. The rename has no fallback to old names, so no ingestion worker Job may be running
    while an installation is upgraded.
19. Earlier FOWF gaps, each needing re-checking against current code and a decision on whether it is
    a requirement: compile-time compatibility of adjacent task schemas; per-run SLO, QoS or priority;
    FOWF acting as an A2A server. (Tracked as
    [FOWF backlog F-24](../../forwardmeasure-openworkflow/docs/implementation-status.md#9-remaining-functional-work-and-completion-backlog).)
20. Code-derived FOWF design items still needing the owner's confirmation: resources fetched only at
    publication; the command set and version checks; exactly-once delivery wording; who may act on
    human tasks; versioned wire formats. (Recorded in
    [FOWF vision §24](../../forwardmeasure-openworkflow/docs/architecture-design.md#24-delivery-milestones-and-change-control).)
21. Per-tenant clusters: when tenants get dedicated clusters, what replaces the shared wildcard DNS
    record?

**Branding**

22. Should the sign-in page receive a tenant hint through Keycloak's `organization` scope?
23. Which brand surfaces outside the applications (Keycloak and platform emails, PDF and CSV exports,
    screening reports) are in scope before Option B?
24. Custom fonts per brand: licensing and bundling.
25. Option B's shared validation rules for Node and Java ("Decide this when B starts").

**Entity Intelligence**

26. Should the controlled long-name recall fallback be ported (section 28.2)?
27. WorldCheck encoding: the supplier has not confirmed whether the export is ISO-8859-1 (Latin-1) or
    Windows-1252.
28. Should the 125-year date-of-birth plausibility limit become a named, validated policy setting
    with jurisdiction- or population-specific values?
29. Information extraction:
    - Where does the owner's initial agent live, in what language and branch, and what is its
      invocation and deployment contract?
    - Which document formats come first, and is OCR needed for scanned PDFs and images?
    - Which model, provider, endpoint and credential mechanism? (Configuration references, not
      secrets.)
    - Is extraction analyst-initiated, automatic after upload, or both?
    - Is persistence automatic or subject to analyst review, and how are reruns, corrections and
      duplicates handled?
    - Does the agent provide document, page and span provenance, prompt and model versioning, token
      usage and partial-failure reporting?

**Testing**

30. How should the owner's "tests only through REST APIs" rule (2026-10-07) and the agreement on
    complementary direct and REST tests (2026-10-07, restated 2026-10-10) be stated as one rule?
31. Resolved 2026-10-13: the FOWF gate is 85% line and 85% branch coverage with zero missed classes.
    `openworkflow-coverage` reads `jacoco.minimum.line.coverage`, `jacoco.minimum.branch.coverage` and
    `jacoco.maximum.missed.classes` from forwardmeasure-platform's pom (0.85 / 0.85 / 0); the older
    85%/75% record is superseded.

## 30. Glossary

| Term | Meaning |
|---|---|
| Actor | A `HUMAN` or `SERVICE` identity, identified by identity provider and subject |
| Adapter (operation adapter) | The FOWF component that performs a workflow task's external I/O and reports an observation back to the engine |
| Admission | Durable acceptance of a run or ingestion, with its contracts and idempotency state pinned |
| AuthZEN | The authorization API standard the platform uses to ask a policy decision point whether a subject may act on a resource |
| Bundle | A product's set of workflow definitions, published by the generic publisher and addressed by DID URL |
| Capability pack | A declaration of the roles, grants and memberships a product needs in each tenant |
| Cell | One deployment configuration in a test matrix (framework × engine × backend) |
| Control plane | The `forwardmeasure_control_plane` database holding the tenant registry and Pekko coordinator store |
| Correlated worker | An external worker (for example a Kubernetes Job) whose result is correlated back to the waiting workflow |
| DID | Decentralized identifier; tenants use `did:web:<alias>.<domain>`, bundles and entities use the platform DID method |
| Execution (run) | One running instance of a published workflow definition, identified by tenant and execution ID |
| Input definition | A named, versioned FEI source contract (S2 and M2) registered on a population |
| Membership and support | A monitored population's link to a resolved entity, kept active by at least one source-record support |
| Monitored population | The subjects an organization monitors, resolved into entities |
| Overflow | Moving a payload too large for inline transport to shared object storage and passing a reference |
| Reference population | An indexed watchlist dataset screened against |
| Selection | The installation-wide choice of framework, engine, FDE and namespace layout |
| Target contract | A population's immutable destination schema and destination configuration (S1 and M1) |
| Tenant | A customer organization, a Keycloak Organization with its own database and host |
| Tenant context | The shared service that maps a hostname to a tenant and its brand |
| Workload identity | A service's cryptographically verified identity for internal calls |

## 31. Sources

This document consolidates the following documents, which have moved to `docs/archive/`:

| Archived document | Subject |
|---|---|
| `forwardmeasure-engineering-manifesto.md` | Engineering principles P1 to P15 |
| `platform-architecture-explained.md` | Historical explanation of the platform and the OKS runtime (August 2026) |
| `architecture.md` | Responsibilities of the platform repository |
| `ui-architecture.md` | UI decision, composition rules, shared foundation and routing |
| `production-ui-delivery-architecture-audit.md` | Release-gate invariants and the UI delivery audit |
| `java-build-platform.md` | Version authority, policy plugin and coverage policy |
| `maven-build-cache.md` | Maven build cache configuration and rules |
| `version-consolidation-2026-10-07.md` | Version consolidation log, bootstrap and coverage decisions |
| `shared-contract-ownership-2026-10-08.md` and `.json` | Contract ownership decision and shared error contract |
| `k8s-infrastructure-roadmap.md` | Multi-cloud infrastructure phases |
| `access.md` | Access URLs and accounts |
| `deployment-configuration-and-cutover.md` | Deployment selection, namespaces, runtime display name, cutover runbook |
| `tenant-branding-design-2026-10-12.md` | Tenant branding design |
| `identity-service-clients-and-tenant-membership-design-2026-10-05.md` | Service clients and tenant membership design |
| `identity-implementation-handover-2026-10-05.md` | Identity implementation and operations |
| `cross-repo-handover-2026-09-29.md` | Shared Helmfile layer, database model, digest-only images, workflow publishing |
| `checkpoint-backup-2026-10-07.md` | Source checkpoints |
| `testing-guide-for-claude-2026-10-07.md` | Testing layers, rules and required scenarios |
| `runtime-matrix-status-2026-10-08.md` | Runtime matrix results and fixes |
| `claude-testing-completion-handover-2026-10-09.md` | Testing programme completion and remaining work |
| `independent-parity-audit-request-2026-10-10.md` | Approved requirements, decisions and audit criteria |
| `entity-intelligence-screening-optimisations-handoff.md` | Screening recall, scoring and quality requirements |
| `population-contract-and-membership-implementation-plan-2026-10-12.md` | FEI population contract and membership design |
| `information-extraction-agent-studio-handover-2026-10-12.md` | Information-extraction agent requirements |
| `testing-boundary-inventory-2026-10-07.json` | Static test-boundary inventory |
| `*-runtime-evidence-2026-10-09.json`, `fowf-*-evidence-2026-10-09.json`, `fei-component-corrections-evidence-2026-10-09.json`, `fde-grpc-lifecycle-evidence-2026-10-09.json`, `shared-api-ownership-evidence-2026-10-09.json`, `shared-contract-ownership-2026-10-08.json` | Machine-readable test evidence |

It also states the owner's standing engineering rules and product direction as recorded in the
owner's working notes (October 2026), which take precedence over the older documents where they
conflict.

Statements marked "(from repository documentation - to confirm)" come from:

- `forwardmeasure-data-streaming/README.md` and `docs/architecture-design.md`;
- `forwardmeasure-decision-engine/README.md` and `docs/IMPLEMENTATION-SPEC.md`;
- `forwardmeasure-agent-os/docs/agent-os-purpose-analysis-handover-2026-10-12.md`;
- `forwardmeasure-platform-operations/README.md`.

Implementation and test status: [implementation-status.md](implementation-status.md) and
[multi-framework-testing-status.md](multi-framework-testing-status.md). FOWF's own requirements,
status and test evidence: [FOWF vision](../../forwardmeasure-openworkflow/docs/architecture-design.md),
[FOWF implementation status](../../forwardmeasure-openworkflow/docs/implementation-status.md) and
[FOWF multi-framework testing](../../forwardmeasure-openworkflow/docs/multi-framework-testing.md).
