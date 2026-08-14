# ForwardMeasure UI Architecture

## Decision

ForwardMeasure will present one tenant-scoped product experience without
building one monolithic application.

The tenant portal is the overarching shell and launch point. Three product
surfaces remain independently owned because they answer different questions:

- open-source OKS Studio explains how workflows are authored and executed;
- the closed-source Entity Intelligence Workbench combines the tenant overview
  with evidence, population, screening, resolution, entity, dossier and
  investigation work;
- the open-source Platform Dashboard explains shared platform health and
  operations without absorbing either product's business screens.

The portal may display high-level summary cards from both domains, but it must
not duplicate their operational screens or embed their applications in
iframes.

## Product Surfaces

| Surface | Primary users | Responsibilities |
| --- | --- | --- |
| Tenant portal | All tenant users | Product navigation, tenant identity, session entry, cross-product notices, and shallow status summaries |
| OKS Studio | Workflow authors and platform operators | Workflow authoring, catalogue and lifecycle, execution graph and timeline, step input/output drill-down, pause/resume/cancel, human tasks, and adapter health |
| Entity Intelligence Workbench | Entity Intelligence operators, investigators and analysts | Tenant overview, ingestion, extraction, populations, screening, resolution, entities, assertions, dossiers and investigations |
| Platform Dashboard | Platform operators | Shared service health, Kafka, schema registry, storage, search, model-service and deployment operations |

An agent invocation is an OKS execution. If an agent-facing product is later
justified, it must use OKS for execution state and human interaction and domain
APIs for business resources. Milestone UI work does not create an empty agent
application or a second workflow runtime model.

## Shared UI Foundation

Cross-product code must be owned by a neutral ForwardMeasure UI foundation,
not by OKS or Entity Intelligence. The intended packages are:

- `@forwardmeasure/ui-tokens`: colour, typography, spacing, elevation, motion,
  and light/dark theme contracts;
- `@forwardmeasure/ui-components`: accessible primitives and layout;
- `@forwardmeasure/ui-auth`: Keycloak bootstrap, token refresh, tenant context,
  authorization, and logout;
- `@forwardmeasure/ui-application`: application shell, navigation contract,
  error boundaries, notifications, and runtime configuration;
- `@forwardmeasure/ui-workflow`: execution graph, timeline, iteration,
  scatter/gather, step-data inspection, and human-task presentation;
- `@forwardmeasure/ui-testing`: browser fixtures and accessibility helpers.

Entity Intelligence currently incubates the first foundation and component
packages inside `entity-intelligence-ui`. OKS and the Platform Dashboard must
not depend on packages named or owned by Entity Intelligence. Once the visual
and application contracts stabilize, the neutral
packages move to a dedicated `forwardmeasure-ui` repository and join the
`forwardmeasure-platform` source reactor. Until a package registry is justified,
the TypeScript packages can use the same Maven-JAR packaging pattern as the
generated API clients.

## API Ownership

Each back-end repository owns its API specifications and generated clients:

- OKS owns `oks-api-contract`, `oks-api-client-java`, and
  `oks-api-client-typescript`;
- ForwardMeasure Agents owns `forwardmeasure-agent-contracts`,
  `forwardmeasure-agent-api-client-java`, and
  `forwardmeasure-agent-api-client-typescript`;
- Entity Intelligence owns `entity-intelligence-contracts`,
  `entity-intelligence-api-client-java`, and
  `entity-intelligence-api-client-typescript`.

Applications consume those artifacts. They do not copy specifications or run a
second generator with local options. Thin UI adapters may add authentication,
tenant-aware base URLs, and product-specific error handling without redefining
transport models.

## Composition Rules

1. Authentication and tenant resolution happen once through the shared shell
   contract.
2. Cross-product navigation uses normal same-origin routes and full application
   navigation; micro-frontends and iframes are not the default architecture.
3. Domain applications own domain vocabulary and colour-rich visualizations;
   shared tokens own typography, structure, controls, and theme behaviour.
4. The OKS workflow visualization is reusable in future agent experiences and
   in Entity Intelligence execution detail pages.
5. The portal aggregates only stable, intentionally exposed summary APIs. It
   must not query internal stores or reconstruct workflow state.
6. Generated clients are immutable build outputs derived from the authoritative
   specifications and verified in the full source-composed reactor.

## Deployment Shape

The products may be deployed independently while appearing under one tenant
host: `/` for the portal/Platform Dashboard, `/ei` for the Entity Intelligence
Workbench during coexistence, and `/agents` for OKS Studio. Istio routing
can map those paths to separate services. A common host and Keycloak realm
provide a coherent session without forcing the applications into one
deployment or one JavaScript bundle.
