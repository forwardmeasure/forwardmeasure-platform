# ForwardMeasure Engineering Manifesto

**How we build software here — independent of any one repo or project. Point any agent or engineer at this instead of restating it.**

---

## 1. Ground every claim in the real system, not memory or assumption

Read the actual code, config, or documentation before asserting how something behaves. A dependency's real version, a component's real maturity, a schema's real declared types, whether a capability is actually wired to anything — check the primary source, every time, even when it feels obvious.

**Why**: An AI-summarized library version once turned out not to exist on the real registry. A doc comment's claim about a legacy class's behavior turned out to be accurate only because nobody had checked — and the check itself surfaced the real, narrower issue. Assumptions compound silently; a wrong one three decisions back is expensive to unwind.

**How to apply**: Cite exact evidence — file:line, a real command's output, a real config value — instead of paraphrasing from memory. When something can't be verified, say so plainly instead of implying confidence you don't have.

---

## 2. Consolidate on one canonical implementation; don't let duplicates accumulate

Before building a new connector, client, or mapper, check whether one already exists — including under a different name, in a different module, or in a sibling repo.

**Why**: Found repeatedly, not hypothetically: S3 and GCS clients existing independently twice in this org, different packages, zero cross-reference. OpenSearch access existing three uncoordinated ways. Two different "named transformation" mechanisms in the same codebase, one of them silently broken for multi-input cases. Every duplicate started as a reasonable local decision and became a maintenance and correctness liability nobody chose deliberately.

**How to apply**: When a duplicate is found, name it explicitly and propose a consolidation direction — don't quietly add a third implementation next to the first two. When starting new work, spend real effort searching for what already exists before writing anything.

---

## 3. Reuse proven infrastructure — evaluated on merit, not habit or novelty

Neither "we've always done it this way" nor "this is the newer, more capable option" is an argument by itself. Check what's actually deployed, actually tested, and actually load-bearing in production before adopting or rejecting a tool.

**Why**: A real design conversation swung twice on this exact axis — first toward keeping a hand-rolled approach because "we haven't used the alternative here before" (a bad argument, correctly called out), then toward adopting an existing framework for a use case where checking the actual codebase found zero real precedent and a pattern of the org's richest usage of that same framework being *retired*, not extended. Both the reflexive "stick with what we know" and the reflexive "let's finally use X properly" were wrong until the real usage was checked.

**How to apply**: For each real technology choice, go look — is this genuinely proven here, genuinely absent, or genuinely being moved away from? Let that answer, not instinct in either direction, decide.

---

## 4. If it's built, it's wired to something real — no dead scaffolding

A capability that compiles and has its own passing test, but is never invoked from anything real, is not a completed feature. Neither is configuration that's provisioned and pointed at a real cluster but never read by the code it was meant for.

**Why**: Found repeatedly across this codebase's history: a durable outbox mechanism sat fully coded but never started in any real deployment for an entire release cycle. A nested-field mapping path was structurally unreachable from the day it shipped. A production Spark execution environment — real executor image, real cluster address, real resource sizing — sat configured and unused while the code path it was meant for stayed single-threaded. None of these were caught by their own tests, because the tests never had to prove the wiring, only the unit.

**How to apply**: Before calling something done, trace the real call path from an actual trigger (a user request, a scheduled job, a deployed config) through to the effect. If nothing in production actually reaches the new code, that's the next thing to fix, not a detail to note and move on from.

---

## 5. Full parity, no silent scope-cutting

A real bug or gap found while working on something else gets fixed then, not deferred behind "that's out of scope for this change" — unless that's been explicitly agreed, not assumed.

**Why**: Scope creep in the other direction — quietly *shrinking* what "done" means — is the more common failure mode, and it's harder to notice because each individual cut looks reasonable in isolation.

**How to apply**: "Feature X is on hold" doesn't excuse a parity gap inside X's existing surface. When a bug is found mid-task, fix it as part of the task rather than filing it for later, unless fixing it is genuinely a larger, separate effort — and say so explicitly if you're deferring it.

---

## 6. No mocking frameworks — use `forwardmeasure-testcontainers` for real infrastructure

Prefer real infrastructure — a real broker, a real database, a real actor system — over Mockito or an equivalent mocking framework, without exception. `forwardmeasure-testcontainers` is this org's shared library of ready-made, reusable Testcontainers wrappers (Postgres, MinIO, Keycloak, Kubernetes/K3s, OpenSearch, and growing) — that's the concrete mechanism, not a per-project ad hoc container setup.

**Why**: A mocked test can pass while the real integration is broken, and the gap only surfaces in production. This has burned the team before. A shared containers library also means real-infrastructure tests are as easy to write as mocked ones would have been — there's no excuse of convenience left for reaching for a mock.

**How to apply**: Check `forwardmeasure-testcontainers` for an existing container wrapper before writing a new one. Where a real instance is genuinely impractical to stand up in a test, a plain hand-written test double is acceptable — but state explicitly, in a comment, why a real instance wasn't used and why this isn't a mocking framework in disguise.

---

## 7. Keep repositories out of business/orchestration code

A use-case/application-service layer — the code with authorization checks, transaction boundaries, the thing a REST endpoint actually calls — depends on domain services, not on `Repository` types directly. Repository access is confined to the domain-service layer beneath it.

**Why**: Confirmed as a real, deliberate boundary in this codebase, not a suggestion: `DossierApplicationService`'s constructor takes `DossierService`, `InvestigationService`, a transaction executor, and an authorization service — no `Repository` anywhere in it. That separation is what lets the domain layer's persistence be changed, tested in isolation, or reasoned about without the application layer's authorization/transaction concerns leaking into it, and vice versa.

**How to apply**: If a class with `ApplicationService`/use-case responsibilities (authorization, orchestration across multiple domain services, transaction boundaries) starts injecting a `*Repository` directly, that's a layering violation worth stopping and fixing, not a shortcut worth taking once.

---

## 8. Schema-driven design, realized as generated code — not a runtime interpreter

Prefer describing data shapes and service contracts in schema (OpenAPI) rather than hardcoding them, but resolve that schema into real generated code at build time, not a generic structure interpreted at runtime.

**Why**: A generic runtime type system (a hand-rolled field-spec tree, a reflection-driven mapper) looks more flexible on paper, but loses compile-time safety, tends to develop exactly the kind of unevenly-implemented dispatch that makes a "registry" only cover some cases, and duplicates what a real code generator already does correctly.

**How to apply**: `openapi-generator` for schema-described model classes and client/server stubs. MapStruct for mapping between them, including named custom-transform methods for anything beyond a straight field copy. Reach for a genuinely dynamic, runtime-interpreted mechanism only when the requirement is *actually* "add a new case without recompiling anything," stated as a real requirement — not assumed as a default virtue of "metadata-driven."

---

## 9. Default toolkit; deviations need a stated reason, not silence

- **Persistence**: JPA/JPQL, behind a Repository, behind a Service — via `forwardmeasure-jpa` (this org's shared multi-tenancy/ORM layer, already providing tenant-aware schema/connection routing) — not raw JDBC, not an ORM-adjacent framework's own query layer, unless there's a specific, stated reason.
- **Object mapping**: MapStruct, not hand-rolled reflection-based coercion.
- **Schema-described data**: OpenAPI-generated model classes, not Protobuf, not a bespoke type-description format, unless the schema genuinely isn't OpenAPI-shaped to begin with.
- **Cloud/object storage**: `forwardmeasure-object-storage`, not a project-local S3/GCS/Azure client.
- **Test infrastructure**: `forwardmeasure-testcontainers` (Principle 6).

**Why**: These are defaults because they're already proven at real scale across this org's codebases, not because they're universally "best." Consistency has compounding value — an engineer or agent moving between projects should find the same shape of solution to the same shape of problem.

**How to apply**: Using something else is fine. Using something else *silently*, without a stated reason anyone reviewing the code can see, is not — the deviation itself is a decision that deserves the same visibility as the code.

---

## 10. Build and verify multi-framework parity — Quarkus, Spring, Micronaut

Where a component is meant to run on more than one of this org's supported frameworks, build and verify all of them together, not one now and the others "later." Treat framework parity as a real requirement, not an aspiration.

**Why**: This is a deliberate bug-detection technique, not just a support-matrix obligation — building three independent framework bindings side by side has repeatedly surfaced real bugs a single implementation would have hidden, because each framework's own conventions force a slightly different angle on the same shared logic. A gap or inconsistency that shows up in only one of the three is usually a sign the "shared" logic wasn't as framework-agnostic as it looked.

**How to apply**: When a new capability needs a framework binding, scaffold and verify it against Quarkus, Spring, and Micronaut in the same pass, not sequentially with the last two deferred. Treat "it works on Quarkus" as an incomplete answer, not a finished one, whenever the other two are in scope at all.

---

## 11. Deploy via the shared, generalized Helm charts and helmfile orchestration

Use this org's shared, framework-agnostic Helm charts (published to the org's own chart repository) driven by helmfile — not a one-off manifest, not a project-local chart, not a framework-specific chart when a generalized one already covers it.

**Why**: The same consolidation problem from Principle 2 shows up in deployment config just as easily as in application code — a framework-specific chart (e.g. Quarkus-only) quietly become the assumed default until every non-Quarkus service either can't deploy cleanly or has to fork its own chart. The fix that's already been done once — generalizing a Quarkus-specific chart into a framework-agnostic one and migrating every real helmfile consumer onto it in a single pass, not a partial migration — is the shape this should always take.

**How to apply**: Before writing a new chart or a one-off manifest, check whether an existing shared chart already covers the shape of workload being deployed. When a chart needs to change to support a new case, generalize it in place (or supersede it with a new, more general chart and migrate every real consumer in one pass) rather than forking a parallel, narrower one.

---

## 12. Verify before claiming success

Compiling, or passing an isolated unit test, is not proof of functional correctness.

**Why**: A ported or generalized component can compile clean, pass its own test, and still be functionally inert — never reachable from anything real (see Principle 4), or subtly wrong in exactly the case its own test didn't cover.

**How to apply**: Trace a real data-flow path through the system before calling something done. State plainly what has and hasn't actually been verified — "this compiles" and "this works end-to-end" are different claims, and conflating them is worse than just being honest about which one you've proven.

---

## 13. Build and deploy discipline

Local, read-only checks — lint, a template render, a syntax check, reading logs — are fine to run without asking. Anything that touches a live system — a real cluster, a deployed service, a published package, a pushed branch — needs explicit authorization first, every single time, regardless of what was authorized in a previous, similar step.

**Why**: The cost of pausing to confirm is low. The cost of an unwanted irreversible action — especially one that affects shared, live infrastructure — is not.

**How to apply**: Hand off real builds and test runs rather than running them speculatively "just to check." When in doubt about whether an action is reversible or affects something live, treat it as if it does.

---

## 14. No speculative diagnosis

During live debugging, don't chain theories from partial evidence and present the result as a conclusion.

**Why**: A plausible-sounding chain of inferences that skips verifying each link produces a confident-sounding wrong answer, which is more expensive to recover from than an honest "I don't know yet."

**How to apply**: Get real proof for each step, or say explicitly that a step is a guess. "Same error" needs the same root cause confirmed, not just a matching line in a stack trace — read the full exception, including any prose above the stack trace itself, before concluding two failures are the same issue.

---

## 15. When one bug has a shape, look for its siblings

A bug caused by a specific, describable pattern (a copy-pasted mistake, a stale assumption baked into several similar files) is rarely unique to the one place it was found.

**Why**: Fixing the one instance and moving on leaves the same defect live everywhere else it was copied from or to.

**How to apply**: Once a bug's shape is understood, search for that shape elsewhere in the codebase before considering the fix complete.
