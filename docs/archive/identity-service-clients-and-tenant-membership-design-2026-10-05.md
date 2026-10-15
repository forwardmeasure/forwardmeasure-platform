# Service clients, secrets and tenant membership

Date: 2026-10-05. Status: implementation and regression fixtures written; production/test sources
compiled; integration tests not executed. Image/chart publication and live deployment are pending.

This revision replaces the earlier Phase 1 proposal. Review evidence, affected images and runnable
verification commands are in [the implementation handover](identity-implementation-handover-2026-10-05.md).
Codex owns the implementation; Claude should independently verify it against source.

## Implementation contract after source review

Codex owns implementation and verification code; Claude reviews the resulting diff and evidence.
No live deployment or credential rotation is performed by this implementation task.

- Client creation and secret assignment belong to the Keycloak bootstrap. It must accept both
  boolean values for authorizationServicesEnabled and must not require tenant Organizations.
- Organization, capability-policy and declared membership reconciliation must be the same for
  bootstrap and API-created tenants. FEI capability-pack support is included in completion scope;
  the original Phase 1-only proposal is not the completion target.
- A capability pack names its resource-server client. Product configuration is applied only to
  registered, entitled tenants, never indiscriminately to every Organization in the realm.
- Multi-tenant API callers request organization:<alias> from trusted request/job context. Token
  caches and generated API clients must be isolated by tenant. An unscoped or all-organizations
  token is not a substitute. AuthZEN evaluator credentials remain separate from evaluated subjects.
- Remove the one-tenant-bound-service-client-per-tenant preflight rule. The openworkflow resource
  server is shared; its legacy first-tenant hardcoded claims do not define tenant authorization.
- Preserve the existing administrator tenant scope by default. All-tenant administrator access
  requires explicit configuration. The workload membership policy and human membership policy
  must not be conflated.
- Validate member identities and role availability before applying memberships. Preserve human
  grants outside platform ownership. Do not describe an additive bootstrap as continuous desired
  state reconciliation or imply that removing an entry automatically revokes existing access.
- ExternalSecret output templates enumerate every final key. Synchronization waits for a refresh
  caused by this installation, including FDS and FEI namespaces, rather than a pre-existing Ready
  condition. Ownerless Secrets are adopted; ownership conflicts are surfaced, not deleted blindly.
- Credential adoption/rotation has an explicit restartable sequence: deliver the intended version,
  reconcile Keycloak, then reload or restart the affected consumers. Ordinary reruns preserve the
  generated secret. Failed/interrupted rotation must have an actionable recovery state.
- Use the existing external capability-pack directory mechanism before adding Kubernetes discovery.
  Both consumers need the same versioned packs, explicit resource-server targets and trusted writers.
- Compilation includes production and test sources. Test execution is handed to the owner unless
  explicitly requested. Rebuild instructions identify exact affected deployables and images.

Verification acceptance cases (source implementation and executable fixtures are both required):

| ID | Scenario | Required evidence |
|---|---|---|
| ID-01 | Empty realm, one complete install | Client bootstrap has no Organization dependency; roles, policies and memberships converge. |
| ID-02 | Two registered tenants | Both get only their eligible packs and declared members; administrator scope stays explicit. |
| ID-03 | Concurrent tenant-specific tokens | Interleaved calls never reuse another tenant's token or mutable API-client bearer configuration. |
| ID-04 | Non-member or mismatched tenant | Token issuance/API authorization denies; no fallback to another tenant or unscoped token. |
| ID-05 | Interrupted secret adoption/rotation | Re-entry completes the same intended version; stale Ready is insufficient; consumers reload only after identity reconciliation. |
| ID-06 | Repeated reconciliation | No duplicate memberships/policies; manual human grants remain intact; invalid configuration fails explicitly. |
| ID-07 | Composed Secrets | Final keys and JSON values match every consumer, without printing real values. |
| ID-08 | API-created FEI tenant | Same FEI groups, workers and policies as bootstrap; its workflow publication and worker callbacks use that tenant. |
| ID-09 | Configuration changes | Existing runtime tenants have an explicit reconciliation path; revocation semantics are documented and tested. |
| ID-10 | Boolean and inventory validation | false is accepted, invalid types and duplicate/reserved secret keys are rejected. |


## 1. Ownership and data flow

`forwardmeasure-platform/deploy/helmfile/shared/common.yaml.gotmpl` declares nine clients under
`platform.identity.serviceClients`, keyed by logical workload. Entries specify the client ID,
secret key, Secret Manager key, booleans and optional tenant roles. Environment values no longer
replace this inventory with tenant-specific lists. Resource servers are `openworkflow`,
`datastreaming` and `entityintelligence`; FDS requires Authorization Services because its launcher
performs its own AuthZEN evaluations.

OpenTofu generates the client secrets. The new eight entries reuse the existing random-password,
GSM Secret and GSM SecretVersion resources. Ordinary apply preserves their values. Client creation
and updating from those values belongs to the Keycloak bootstrap. Valid `false` configuration is
accepted. This hook does not create Organizations or tenant memberships.

The tracked `with-client-secrets.yaml.gotmpl` composes generated entries into the existing local
credential layer at render time. It fetches `<cluster>-<remoteKey>` from GSM for the fake provider;
it does not require copying generated passwords into gitignored files. The existing non-client
credentials and provider configuration remain operator-owned. The existing fake-store provider is
retained; switching to a real provider still requires appropriate remote-key mapping and IAM.

ESO owns final application Secrets. All composed outputs are explicit, including both ID and
secret fields and the Apicurio JSON value. The retired product Helm releases have `installed:
false`, so an upgrade removes their Jobs, ConfigMaps and Secret-writing RBAC. Their chart sources
remain solely as uninstall coordinates; they are not active writers. Ownerless Secrets are adopted
by ESO. A Secret controlled by a different owner must surface an error; no deletion fallback exists.

## 2. Problems and how this revision addresses them

| Problem in the original review | Resolution and boundary |
|---|---|
| Keycloak-generated, unrecoverable application credentials | Tofu/GSM is the source; Keycloak receives the same intended value on every identity reconciliation. |
| Product Job and ESO competing over Secret keys | ESO is the only active writer; final output templates list every consumer key; retired releases are uninstalled. |
| FDS/FEI needing realm-admin credentials to create clients | Their client/grants Jobs and privileged ExternalSecrets are retired. FOWF tenant provisioning retains the admin access it requires. |
| Helm-time membership missing API-created tenants | Both batch and runtime provisioners load the same mounted capability packs and use the same Java reconciler. |
| First-tenant claims on multi-tenant service clients | No shared resource server is tied to a first-tenant claim. API callers select an organization-specific token from the trusted actor/job tenant. |
| Membership needed before the Keycloak client bootstrap could finish | Bootstrap creates clients first; tenant reconciliation creates groups/policies before adding members. |
| Multi-tenant token and definition caches | Bounded caches are keyed by TenantId; API clients retain a tenant-bound token supplier. FEI workflow-name caches are also tenant-specific. |
| FDS authorization client lacked its resource policy | A product-owned FDS pack declares both run resources and all six authorization actions. |
| Flattening FEI grants could widen resource access | `resourceGrants` preserves role → resource → scopes; repeated scope names on another resource do not grant access there. |
| ESO Ready could be left over from an earlier sync | Installation waits for a unique marker on each target Secret, across product namespaces, as well as Ready. |
| Running processes retain rotated secrets | Identity reconciliation precedes consumer reload; pod-template fingerprints use ESO data hashes, with rollout completion checked on re-entry. |

This is not a claim of successful runtime verification. Each behaviour has source/fixture evidence;
the integration cases still require execution.

## 3. Tenant policy and runtime callers

A pack names its resource-server client and declares roles, resources, grants and members. Members
name exactly one existing username or service-account client ID, a nonempty role set, and optional
tenant aliases. Roles must exist in that pack. Provisioning never creates an arbitrary username
because a spelling was wrong. Policy/groups precede membership assignment.

FOWF's shared Helm helper composes selected packs into `openworkflow-tenant-capability-packs`.
Both the bootstrap Job and tenant-administration service mount that ConfigMap. The runtime pod
annotation hashes the complete pack content so a configuration change replaces the service process
that loaded the files. The GCP platform profile explicitly selects `openworkflow`, `data-streaming`
and `entity-intelligence`; local/CI selections remain configurable. These are cluster-wide product
entitlements applied to the tenants passed through provisioning, not discovery of all realm users
or Organizations. There is no per-request product-entitlement selector.

FEI policy permissions come from its grants manifest; deployed service-account memberships come
from the central client inventory's tenant roles. The manifest's historical `serviceAccounts`
entries describe default identities, not an independent client-creation mechanism. FDS owns its
capability document beside its launcher authorization vocabulary. Human administrator membership
stays restricted to explicit aliases, falling back to the first configured alias. All-tenant human
access requires `administratorAllTenants: true`.

`TenantClientCredentials` obtains membership discovery directly from the configured OAuth token
endpoint using `organization:*`. It maps the expected DID-derived TenantId to exactly one alias,
then requests `organization:<alias>`. The discovery token is never sent to an application API.
Every returned token must contain exactly one Organization with the expected tenant DID. Missing,
ambiguous, malformed or mismatched claims fail closed. No incoming unverified JWT is used for
selection. The receiving API still performs normal signed-token verification.

The tenant cache is bounded to 256 entries. Discovery is fresh for a new tenant/cache miss, so an
API-created membership does not wait for an older all-organizations token to expire. Scoped tokens
retain their normal expiry cache. FDS's generated execution API clients and FEI's definition
resolvers are bound to their tenant and bounded separately. Worker callback credentials are bound
to the Job's asserted tenant. AuthZEN evaluator credentials remain independent of the subject
being evaluated.

## 4. Decisions, including changes to the original proposal

| Decision | Outcome |
|---|---|
| D1: central clients; tenant-owned memberships | Kept. Keycloak bootstrap has no tenant dependency. |
| D2: shared map | Kept; workload IDs and secrets flow from it to consumers. |
| D3: special preflight for tenantClaims clients | Replaced. Inventory validation checks types, unique IDs/keys and reserved keys; no one-client-per-tenant requirement remains. |
| D4: Tofu owns client secrets | Kept, with a forced identity sync and explicit consumer reload after ESO delivery. Rotation is a maintenance operation, not a zero-downtime guarantee. |
| D5: additive memberships | Kept and stated as such. Removing an entry does not revoke an existing grant. |
| D6: defer FEI's capability pack | Rejected. FEI's pack is included now, removing its grants Job. FDS also needs a pack. |
| Publisher membership hard-coded in Java | Removed. Enabling publication requires the publisher's role membership to be declared in a pack. |
| Discover packs from Kubernetes dynamically | Rejected for this implementation. Existing external-directory loading and an ordinary ConfigMap suffice. |

Reconciliation is restartable, not transactional across Keycloak and PostgreSQL. A failure is
returned to the caller; retry the same alias to finish the declared configuration. It does not
report cross-system atomicity or invent rollback of already applied external changes.

## 5. Install and rotation sequence

1. Build the affected images; publish Keycloak chart `0.0.28` and platform-secrets chart `0.1.7`.
   Apply the Tofu additions before rendering GSM-backed values. Existing image digests do not cover
   this change.
2. For adoption/rotation on a used environment, drain active Jobs and pause new submissions first.
   Client-secret cutover can temporarily interrupt authentication; zero downtime is not promised.
3. Apply the platform configuration and wait for this installation's ESO sync marker on every
   target Secret. This includes FDS and FEI namespaces.
4. Run the Keycloak identity stage with `helmfile sync`, even if chart manifests have no diff.
5. Reload existing controllers that reference Secrets labelled as identity client credentials.
   The installer compares ESO data hashes with pod-template fingerprints and waits for rollout.
   An interrupted rollout is waited on again without another template mutation. Running Jobs are
   never replayed; an active Job requiring new credentials produces an explicit drain error.
6. Reconcile selected tenant packs through FOWF, then deploy product services and workflow bundles.
   A final consumer check covers newly created controllers. Controller strategies requiring manual
   pod deletion fail explicitly if credentials changed.
7. Reconcile existing API-created ACTIVE tenants through the tenant API if their pack configuration
   changed; they are not implicitly added to the static Helm tenant list.

After interruption, rerun the full installer against the same intended GSM versions. It repeats
ESO delivery, client reconciliation and unfinished rollouts. Do not generate another secret just
because the previous run stopped. Stage-only installation is not the complete rotation procedure.

## 6. API-created FEI tenants: explicit supported boundary

`POST /v1/tenants` creates/reconciles the FOWF database, selected product identity policies,
declared memberships and configured workflow publication. With the FEI pack selected, workers and
FEI role groups are created by the same reconciler as static tenants.

It does **not** run FEI's Liquibase changelog. FEI schema/runtime-role provisioning remains its
separate migration step. An API-created tenant must receive that migration before FEI data
operations or worker execution. The FOWF API's ACTIVE result does not assert FEI schema readiness.
The new real PostgreSQL/Keycloak fixture proves the identity/FOWF part and repeated reconciliation;
it must not be presented as a successful end-to-end FEI ingestion run.

This preserves existing two-step onboarding, pending the owner's answer on whether automatic FEI
schema provisioning should be a separate extension. Do not make FOWF depend on FEI's current
migration artifact merely to hide the gap: that artifact contains product and test-container
coupling that needs deliberate separation first.

For existing ACTIVE tenants, call the authenticated tenant API again with the same alias and a
valid display name after updating the mounted packs. Enumerate from the tenant registry/API,
not from arbitrary Keycloak Organizations. Do not use this path to reactivate deleted tenants.

## 7. Verification evidence

Production and test compilation succeeded for FOWF, FDS, FEI and AuthZEN. No test suite was executed.
Offline Helm rendering and syntax checks are separate from behavioural proof. Detailed commands,
logs, fixtures and outstanding live checks are listed in the handover.

The verification set includes real Keycloak multi-tenant issuance/concurrency/non-member cases;
real PostgreSQL/Keycloak runtime tenant provisioning; resource-specific permission isolation;
FEI concurrent workflow resolution; actual bootstrap boolean parsing; central inventory and final
Secret-key contracts; interrupted synchronization/reload transport fixtures; and a disposable-cluster
ESO test using synthetic values only. Deployment verification must still check first install,
reconciliation reruns, selected-framework HTTP endpoints and real product work after schema setup.

## 8. Open questions resolved or bounded

- **O1 — shared resource server:** keep one `openworkflow` client; remove legacy first-tenant claims.
  Tenant identity comes from verified Organization claims and explicit active-tenant selection.
- **O2 — administrator scope:** preserve first-configured-tenant access by default; explicit alias
  lists or an explicit all-tenants switch control any expansion.
- **O3 — platform operator:** retain the machine identity and publish both credential keys. No
  in-cluster consumer was found; an authenticated external caller can use its realm role for tenant
  administration. This change does not invent an external integration or send credentials anywhere.
- **O4 — revocation:** additive reconciliation preserves manual human grants. Removing a declaration
  alone does not revoke membership, a role mapping or an old permission. Explicit administrative
  revocation and session/token handling remain required. Existing tokens and authorization decision
  caches are not instantly invalidated by editing a YAML file.
- **O5 — pack delivery:** a shared ConfigMap mounted into both provisioners, with explicit
  resource-server IDs and a content hash for process reload.
- **Additional boundary:** automatic FEI database onboarding is not included under the assumed
  two-step contract in §6; the owner has been asked whether to extend this scope.
