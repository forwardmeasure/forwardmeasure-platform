# Production UI Delivery Architecture Audit

## Purpose

This document is the release gate for the current cross-repository UI delivery.
It records the design invariants that must remain true, the evidence inspected,
and every deviation found while completing Entity Intelligence Milestone 9,
OKS Studio, and the Platform Dashboard. A successful build alone does not close
this gate.

## Delivery Strategy

1. Complete and review the code.
2. Generate contracts and clients, compile all changed modules, build the
   production frontend bundles, and render the Helm releases.
3. Run one clean full regression after the code is complete.
4. Run the clean K3s and authenticated tenant-routed browser acceptance last.

Broad regressions are deliberately not repeated while implementation is still
changing. Focused static checks and compilation remain valid development
feedback; they do not count as production acceptance.

## Normative Invariants

| Boundary | Required invariant |
| --- | --- |
| Deterministic runtime | OKS durable-processing, runtime-core, runtime-api and definition modules contain no Quarkus, HTTP, JPA, Kafka-client or cloud-provider dependencies. |
| Orchestration | Kafka Streams decides and durably records workflow progress. External I/O executes only in adapters or workers and returns observations; workers never advance the graph. |
| Contracts | OpenAPI, AsyncAPI and JSON Schema are authoritative. Generated clients own transport types. UI adapters may create presentation models but must not redefine a second wire contract. |
| Workflow portability | Immutable workflow definitions identify logical operations and immutable contract resources. Deployment-specific service addresses, credentials, namespaces and storage coordinates are resolved at an authorized adapter boundary. |
| Identity | Tenant and actor identity come from verified authentication or a cryptographically bound internal workload identity. User-supplied tenant/actor headers are never authoritative. |
| Causality | Business correlation, causation and W3C tracing are distinct. `traceparent` is never used as a business correlation identifier. A root execution without upstream correlation uses its execution ID; children preserve the intended causal chain. |
| Tenancy | Public APIs infer the tenant from the authenticated principal. Tenant identifiers are not editable business form fields. Every query and mutation is tenant-confined. |
| Persistence | Application resources call domain/application services. JPA repositories and `EntityManager` stay behind persistence or explicitly named transaction adapters. Raw JDBC is limited to migrations or deliberately optimized persistence adapters. |
| Artifacts | Business code uses `forwardmeasure-object-storage`; it does not depend directly on S3, GCS, Azure or MinIO SDKs. Object-store URIs are discovered or issued by governed APIs, not typed by business users. |
| UI ownership | Closed-source Entity Intelligence owns only its Workbench. Open-source OKS owns Studio. The open-source Platform project owns the Platform Dashboard. No product imports packages named for another product. |
| Deployment | API and UI services are ordinary durable Kubernetes workloads unless their execution semantics explicitly require another primitive. Secrets are referenced, images are digest-pinned, and routing preserves the tenant host. |
| Acceptance | Production claims require real PostgreSQL, Kafka, object storage, Apicurio, OpenSearch, Keycloak, K3s and browser paths as applicable. Mocks are not acceptance evidence. |

## Findings And Disposition

### Must Fix Before The Final Regression

| ID | Finding | Required correction | Status |
| --- | --- | --- | --- |
| CAUSAL-001 | OKS and consuming APIs treated `traceparent` as a fallback business correlation ID. | Remove the fallback everywhere; establish deterministic root correlation and keep tracing separate. | Corrected in code; final regression pending |
| CAUSAL-002 | Root, scheduled and event-triggered executions did not have one explicit, documented correlation rule. | Default root/control correlation to execution ID, derive stable CloudEvent correlation when absent, preserve explicit upstream and causal child correlation. | Corrected in code; final regression pending |
| WF-001 | Entity Intelligence workflow bundles hard-code Kubernetes contract-service and runtime-service URLs. | Introduce an authorized deployment binding at the operation-adapter boundary and publish environment-neutral logical endpoints/resources. | Corrected: definitions use logical immutable contract coordinates and deployment bindings own physical endpoints |
| AUTH-001 | Internal Entity Intelligence OpenAPI calls accept tenant and actor identity from headers authenticated only by a shared bearer secret. Possession of that shared secret permits identity substitution. | Bind the asserted actor/tenant to a signed workload token or independently verifiable service identity and reject mismatches. | Corrected: workload JWT client, audience, role, tenant, actor, actor type, issuer and subject are verified |
| UI-001 | The Workbench omits reference revision/reprocess/legal-hold/rollback/retention, monitored-ingestion control, and entity-screening detail/cancellation even though the APIs exist. | Add the missing generated-client-backed product operations and business status views. | Corrected; the authoritative 85-operation inventory and generated-client call inventory have no differences |
| UI-002 | Population Sources is rendered twice. | Remove the duplicate rendering and add a UI regression assertion. | Corrected |
| UI-003 | The Platform Dashboard is documented but has no application module. | Implement the open-source dashboard, its explicit summary/health backend contract, runtime configuration, authentication and deployment. | Corrected; targeted production package is green |
| UI-004 | OKS Studio has extensive authored code but no retained acceptance for the current catalogue rewrite and authoring surface. | Complete static/build checks and the final authenticated author/publish/run/control/HITL browser path. | Code and targeted production package complete; final authenticated browser gate pending |
| DEPLOY-001 | The three repositories have charts/Helmfiles, but the final same-host route and immutable Keycloak theme/client activation have not been proven together. | Complete render checks, deploy the three tiers, and pass tenant-routed browser acceptance. | Deployment composition implemented; final clean deployment/browser gate pending |

### Architecture Review Required During Completion

| ID | Observation | Release treatment |
| --- | --- | --- |
| PERSIST-001 | Several deployed Entity Intelligence classes inject `EntityManager` directly. Some are correctly named transaction/persistence adapters; application coordinators must not bypass the service layer. | Classified: use is confined to JPA repositories, explicitly named transaction adapters and dependency-wiring configuration. REST resources and deterministic domain modules do not inject it. |
| CLIENT-001 | OKS Studio uses generated transport clients but maps them into handwritten presentation types. | Retained types are presentation/graph models derived from generated wire models; browser transport remains generated-client-owned. |
| TYPE-001 | Correlation remains represented as nullable `String` in several runtime records. | Corrected with `BusinessCorrelationId` and explicit correlated/uncorrelated actor factories; serialized compatibility is covered by wire tests. |
| DOC-001 | Roadmap text contains production-complete language for code that has not passed the current final gate. | Update only after retained evidence exists. Never infer completion from implementation presence. |

## Review Evidence

- Focused import scans show no Quarkus, HTTP, JPA, Kafka-client or cloud-provider
  imports in the deterministic OKS cores.
- Focused import scans show no framework or provider imports in Entity
  Intelligence domain/core modules.
- Entity Intelligence uses generated TypeScript OpenAPI clients for business
  calls; direct `fetch` is currently limited to runtime configuration and
  presigned object transfer.
- The Workbench operation inventory was compared directly with the published
  Entity Intelligence and Evidence OpenAPI operation IDs.
- The current OKS Studio imports the generated OKS TypeScript client, but its
  presentation mapping requires the CLIENT-001 classification above.
- The Platform repository now contains the authenticated Platform Dashboard,
  its generated client, backend summary/health contract, chart and Helmfile
  release.
- Active OKS, Entity Intelligence and Platform runtime source/configuration
  contains no Asvinau or legacy Data Fabric identity. Historical migration
  documentation and negative dependency/manifest guards retain those names
  deliberately.
- Targeted production packages pass for OKS Studio, Entity Intelligence
  Workbench and Platform Dashboard. These are compilation and assembly
  evidence, not substitutes for the final regression or browser gate.

## Final Closure Evidence

This section remains empty until implementation is complete. It must record:

- contract/client generation and drift checks;
- Java compilation/static analysis and frontend production builds;
- Helm lint/schema/render results for all three tiers;
- the single clean full regression result;
- clean K3s deployment and recovery results;
- authenticated tenant-routed browser acceptance for Entity Intelligence,
  OKS Studio, and the Platform Dashboard;
- an explicit disposition for every finding above.
