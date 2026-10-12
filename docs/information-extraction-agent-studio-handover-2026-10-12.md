# Information-extraction agent and FEI Studio — fresh-session handover

Prepared October 12, 2026, America/New_York. Intended for a new Claude or Codex session.

## 1. Assignment

The user has **an initial implementation of the information-extraction agent**. Inspect and build on that implementation, then integrate it with FEI's investigation evidence and Studio. Do not start by designing a replacement agent or assuming the existing implementation is absent.

Its location, language, branch and invocation contract have **not been supplied in this conversation**. Ask the user for the repository/path or locate it within the authorized workspace. A limited directory check found `forwardmeasure-agent-os`, but did not find a `forwardmeasure-agents` directory at the workspace root. Neither observation establishes where the initial implementation lives. Do not search unrelated customer directories or assume Agent OS is the chosen host.

Workspace: `/home/pn/Documents/code/forwardmeasure`.

This document transfers context and intended outcomes. The current turn authorized writing this handover, not executing extraction, tests, builds or deployments. Follow the user's next instruction for implementation. Do not turn historical restrictions into a claim that broader engineering is cancelled or paused. Honor authorization already granted in the new session without repeatedly asking for it.

## 2. Corrected schedule and intent

The user's final schedule moved everything forward one day:

- **October 11:** testing cutoff at **9:00 AM New York time**, then checkpoint code, build/publish the corresponding images, deploy to dev, and exercise real functionality through Studio.
- **October 12:** implement/complete the information-extraction agent and integrate it into Studio.

New York is on EDT on these dates. The corrected schedule is historical planning context, **not evidence that the October 11 testing, checkpoint or rollout actually happened**. Verify current deployment and test status before relying on them; do not silently move the dates again.

October 11 functional goals were:

1. Ingest the complete WorldCheck export through Studio and verify persisted/indexed output.
2. Ingest the State Street customer master through Studio and inspect entities/assertions.
3. Screen the entire customer master against WorldCheck.
4. Screen an arbitrary entity against WorldCheck.
5. Screen an existing resolved entity against WorldCheck.
6. Upload evidence to an investigation and retrieve its stored content.

The user explicitly accepts **recorded code-coverage shortfalls**, to be addressed in subsequent nightly runs. The permanent target remains 85% handwritten lines and branches, with generated code exempt. A temporary coverage exception does not excuse functional failures, hidden skips or misleading test claims. It also does not authorize scheduling nightly jobs without a request.

The user is running parity tests separately. Claude was running other tests at the last user report; the user instructed this session not to start additional work then. Recheck current activity before using the build lock or editing overlapping files. Do not restart the broad parity audit as a prerequisite for extraction integration.

The intent is to use the deployed product and discover concrete functional defects. Avoid an indefinite testing program that prevents implementing or exercising the requested functionality.

## 3. Existing implementation: verified starting points and limits

The following were inspected locally during handover preparation. They establish source structure, not successful execution of the extraction feature or current deployed behavior. Paths below are relative to `forwardmeasure-entity-intelligence`.

### Evidence UI and APIs

- `forwardmeasure-entity-intelligence-studio/webapp/src/main/webapp/src/InvestigationInspector.tsx` exposes an investigation evidence tab.
- `forwardmeasure-entity-intelligence-studio/webapp/src/main/webapp/src/EvidencePanel.tsx` supports evidence references, multipart file upload, metadata edits, deletion and a content-download link.
- Upload calls `POST /investigations/{investigationId}/evidence/upload` through the Studio API proxy with bearer authentication. File upload uses generated metadata serialization plus multipart `fetch`; it is not proof that a generated upload operation exists.
- Content retrieval is `/investigations/{investigationId}/evidence/{evidenceId}/content`.
- Upload binding implementations exist for Quarkus and Spring as `InvestigationEvidenceUploadResource`; Micronaut uses `MicronautInvestigationEvidenceUploadController` under `forwardmeasure-entity-intelligence-resolution-service/framework-bindings/`.
- `forwardmeasure-entity-intelligence-resolution-service/forwardmeasure-entity-intelligence-resolution-service-application/src/main/java/com/forwardmeasure/entityintelligence/resolutionservice/application/InvestigationApplicationService.java` streams/stages uploaded content through object storage and persists evidence metadata, content reference, size and checksum. Inspect current transaction and failure-cleanup behavior before extending it.

**Evidence upload is not an implemented information-extraction pipeline.** The inspected upload method stores content and metadata; it does not establish agent invocation, extraction completion or assertion creation. Existing comments explicitly defer extraction UI/pipeline behavior. Recheck for changes by another agent rather than assuming comments remain current.

The current upload UI documents a handwritten multipart-binding exception. Audit the actual OpenAPI/generator boundary before adding endpoints. Do not copy this exception into new ordinary JSON APIs or widen the task into an unrelated upload rewrite.

### Existing extraction models and persistence

Canonical API/schema file:

`forwardmeasure-entity-intelligence-api-specifications/src/main/resources/META-INF/openapi/entity-intelligence-api.yaml`

It already defines:

- `InformationExtractionStatus`: `accepted`, `processing`, `skipped`, `failed`, `completed`, `partially_completed`.
- `InformationExtractionResult`, discriminated by `result_type`.
- `AmlKycExtractionResult`, including names, dates of birth, nationalities, identifiers, addresses, charges, associated entities, key dates, jurisdictions and source summary.
- Structured `ExtractionError` and extraction-related evidence fields.

The `AmlKycExtractionResult` schema describes outputs becoming Assertions and associated entities becoming CandidateEntities. **That description is intended behavior, not evidence that the conversion is wired.** Trace existing services before implementing or claiming it. Establish whether persistence is automatic or requires analyst approval from current requirements and the initial agent; do not invent that decision.

Existing domain components:

- `forwardmeasure-entity-intelligence-domain/src/main/java/com/forwardmeasure/entityintelligence/domain/entity/LlmExtraction.java`
- Corresponding `LlmExtractionService`, `service/impl/LlmExtractionServiceImpl` and `repository/LlmExtractionRepository`.

The inspected entity links an extraction to evidence and investigation and stores model name, prompt key/version, status, typed JSONB result, structured error, token counts and raw model response. Its KServe-related comment is **not a requirement to use KServe or a particular model**. Inspect migrations, framework wiring and actual service behavior; do not create a parallel extraction-result table or incompatible model without a demonstrated need.

## 4. Architecture and engineering requirements

Read applicable `AGENTS.md` files before edits, and these workspace-relative references:

| Reference | Purpose |
|---|---|
| `forwardmeasure-platform/docs/forwardmeasure-engineering-manifesto.md` | Shared ownership, service boundaries, real infrastructure and framework parity |
| `forwardmeasure-entity-intelligence/docs/architecture-design.md` | FEI workflow, worker, security, persistence and Studio requirements |
| `forwardmeasure-platform/docs/testing-guide-for-claude-2026-10-07.md` | Settled direct/managed-service/API test boundaries |
| `forwardmeasure-jpa/docs/architecture.md` | Shared JPA entity/service/repository/transaction infrastructure |
| `forwardmeasure-platform/docs/shared-contract-ownership-2026-10-08.md` | Canonical contracts and generated snapshots |
| `forwardmeasure-platform/docs/independent-parity-audit-request-2026-10-10.md` | Detailed architecture criteria; its separate audit assignment is not this task |
| `forwardmeasure-entity-intelligence/docs/fei-strategy-conversation-handover-2026-10-07.md` | Product context; distinguish proposals from approved requirements |

Apply these established rules:

1. **Contract first.** Extend canonical schemas/OpenAPI and generate models/interfaces/clients. Never edit generated Java/TypeScript directly. Reuse existing extraction types where they fit. Map typed API/domain/entity boundaries with MapStruct where appropriate.
2. **Service boundaries.** REST/application orchestration calls domain services; repositories remain behind domain/persistence services. Use `forwardmeasure-jpa`, managed transactions and Criteria queries with generated static metamodels. Do not introduce raw SQL business logic or framework-specific replacement repositories.
3. **FOWF owns long-running FEI orchestration.** Inspect the initial agent's invocation mechanism and the existing FOWF adapters before choosing the integration. Do not add a Studio-owned background workflow or a parallel scheduler. Persist durable identity and correlate asynchronous completion/failure. Large documents belong in object storage, not oversized workflow input JSON.
4. **Workers do not access FEI tenant databases.** Use authorized FEI APIs and scoped workload identities. No tenant database credentials or direct repository graph in an extraction worker.
5. **Shared infrastructure.** Reuse `forwardmeasure-object-storage`, existing clients, authorization, workflow publication and deployment mechanisms. Do not create duplicate storage/agent clients without first inspecting existing ones.
6. **Security and tenancy.** Trusted organization determines tenant; real OIDC/AuthZEN governs admission, evidence access and result persistence. Preserve authenticated caller versus asserted subject. Documents/model output are untrusted data, not instructions authorizing arbitrary tools, cross-tenant reads or actions.
7. **Frameworks.** FEI changes must support Quarkus, Spring and Micronaut. For workflow integration, account for Kafka Streams, Pekko/PostgreSQL and Pekko/Cassandra. Scope the affected regression cases explicitly; do not restart every unrelated matrix or claim one tested runtime proves them all.
8. **Persistence compatibility.** Preserve deployed migration history and admitted workflow/contracts. Add forward migrations if necessary. Never reset tenant data to make integration work.
9. **UI ownership.** Extend the existing Maven-built FEI Studio and shared API proxy. Display real persisted status/results/errors. Do not add UI-only simulated extraction or expose internal deployment details as analyst controls.
10. **Build/deployment.** Versions belong in the platform; preserve justified existing exceptions. Use shared Helm/Helmfile configuration and actual image pins. Inventory affected images, including Studio, and provide targeted build/push/rollout instructions when implementation is ready.

## 5. First steps in the new session

1. Locate and read the user's initial agent implementation, its README/contracts/tests and relevant instructions. Record what actually works and what remains a stub. Preserve its design unless inspection identifies a concrete reason to change it.
2. Read current FEI evidence/extraction code, API schema, migrations and Studio components above. Check current branches/dirty paths and coordinate ownership with Claude; do not overwrite concurrent test or deployment changes.
3. Trace the desired path: **Studio investigation/evidence → authorized extraction request → durable dispatch → real agent/model → validated result → FEI persistence → Studio status/results → assertions/candidates under the agreed review policy**.
4. Identify only the decisions needed to implement that path. Continue independent work while awaiting answers. Do not silently convert suggestions below into user-approved requirements.
5. Produce a finite integration plan with exact contract/service/UI/fixture changes, then implement when instructed. Report concrete blockers promptly, not an expanding generic backlog.

Questions not yet settled in this conversation:

- Where is the initial agent, and how is it invoked/deployed?
- Which document formats are required first? Do scanned PDFs/images require OCR now, or does the agent already provide it?
- Which model/provider/endpoint and existing credential mechanism should be used?
- Is extraction initiated explicitly by the analyst, automatically after upload, or both?
- Are extracted assertions/candidates persisted automatically, or only after analyst review? How should reruns, corrections and duplicates behave?
- Does the initial implementation already provide document/page/span provenance, prompt/model versioning, token usage and partial-failure reporting?

Prefer answers available in the initial implementation and established product requirements over asking the user to restate them. Request credential **configuration references**, not secret values in chat.

## 6. Proposed integration acceptance criteria

These are concrete checks for the new implementation, not claims that fixtures exist or tests passed:

- From a real investigation in Studio, select uploaded evidence and invoke extraction through the actual supported control.
- Observe durable queued/processing/terminal status and a useful structured failure when the agent/model rejects or cannot process the document. Define which existing status represents each supported outcome; avoid unsupported UI-only states.
- Retrieve the correct evidence bytes under the correct tenant identity. Verify persisted outputs remain associated with the original evidence and investigation.
- Validate model output against the chosen canonical schema before accepting it. Exercise malformed output, empty output and partial success without fabricating facts or reporting success for an unusable result.
- Display structured results and provenance supported by the contract; preserve model/prompt version and relevant audit metadata. Do not dump raw customer documents or model responses into test logs.
- Verify the agreed assertion/candidate persistence or analyst-review path with independent expected results and committed rereads. A displayed JSON response alone does not prove integration.
- Test duplicate submission, rerun and failure/recovery according to the agreed idempotency policy. Avoid repeated billed calls or duplicate assertions from blindly retried callbacks; handle ambiguous outcomes explicitly.
- Deny unauthorized and wrong-tenant access through real authentication/authorization. Verify denial precedes document retrieval, model invocation and database mutation.
- Use representative synthetic documents for committed fixtures and explicitly authorized external real evidence for dev exercises. Use `forwardmeasure-testcontainers` for infrastructure; retain meaningful focused tests plus real API and browser integration checks.
- Keep per-framework and per-engine evidence explicit. A deterministic model-response fixture can prove validation/mapping, but cannot be presented as successful execution of the real agent/model.

Do not expand the first usable integration into every conceivable document format, agent protocol or UI feature. Identify required versus deferred behavior explicitly with the user.

## 7. Validation, coordination and reporting

Use `/home/pn/Documents/code/forwardmeasure/forwardmeasure-openworkflow/scripts/build-bounded.sh`, inspect its current locking behavior, and coordinate with Claude's builds. Compile production and test sources for affected closures; do not start concurrent Maven processes or clear the shared Maven cache. Use the current session's instructions for test execution, model calls, publication and deployment.

Write meaningful fixtures before the broader runtime execution phase, preserving the user's fixture-first preference. Coverage shortfalls may be documented for later nightly work; do not replace behavioral assertions with perfunctory coverage tests or hide functional failures behind the coverage waiver.

Report separately: **implemented, fixtures written, compiled, tests executed, images built/pushed, deployed, exercised through Studio**. Record source/contract/image identities and relevant logs. Historical ingestion/runtime passes do not certify new extraction code or current deployment images.

Completion means the agreed extraction journey is integrated and supported by the stated evidence, with exact remaining limitations and targeted rollout instructions. Do not say “100% parity” or “done” on compilation alone.

## 8. Suggested opening message

> Read `/home/pn/Documents/code/forwardmeasure/forwardmeasure-platform/docs/information-extraction-agent-studio-handover-2026-10-12.md`. My initial information-extraction agent is at **[repository/path]**. Inspect it and the existing FEI evidence/extraction/Studio paths first, then help me complete the integration. Coordinate with Claude's current work and preserve uncommitted changes. Keep the work focused on a usable real extraction journey and distinguish implementation from executed verification.
