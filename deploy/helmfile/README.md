# ForwardMeasure greenfield platform deployment

This is the open-source shared-platform tier for a new cluster. It owns the
cluster services required by OKS and Entity Intelligence; it contains no
Entity Intelligence source or release.

The ordered release stages are:

1. `foundation`: Gateway API prerequisites, Istio, cert-manager and External Secrets.
2. `configuration`: Google Secret Manager integration, gateway, routes and certificates.
3. `identity`: Keycloak.
4. `messaging`: Strimzi, Kafka and Apicurio Registry.
5. `search`: OpenSearch and OpenSearch Dashboards.
6. `ml-serving`: Knative, KServe, GCS model cache and Docling.
7. `analytics`: authenticated Valkey and Superset.
8. `dashboard`: the tenant-routed, read-only operational dashboard for the
   shared platform services.

Open WebUI is deliberately not deployed. The implementation-neutral GLiNER NER
service is a shared platform capability because multiple vertical products use
it. Relationship extraction remains an Entity Intelligence model release. The
platform also supplies their common KServe substrate and model cache.

The Platform Dashboard is served at
`https://<tenant-code>.<root-domain>/platform/`. It is intentionally separate
from the proprietary Entity Intelligence Workbench and the OKS Studio. It
reports bounded health probes and links to operational consoles; it neither
duplicates vertical-product workflows nor exposes cluster credentials to the
browser.

## Infrastructure contract

Terraform must provide the GCP project, VPC, GKE cluster, static gateway IP,
Cloud SQL databases/users, GCS buckets, Cloud DNS zone, Google service
accounts, Workload Identity bindings, and the Secret Manager entries listed in
`environments/gcp.yaml` and its cluster leaf file (e.g.
`environments/gcp-openworkflow-prod.yaml.gotmpl`). Database addresses may be
private or public. Public PostgreSQL endpoints must require verified TLS;
complete connection URLs are stored in Secret Manager rather than
reconstructed by Helm.

No value is inherited from the retiring Data Fabric project. To stand up a
new cluster on an already-supported cloud, copy
`environments/gcp-greenfield-example.yaml`, replace its placeholders, add the
new environment name to `helmfile.yaml.gotmpl`'s `environments:` block and
`$layers` dict (and to `scripts/environment-files.sh`), and validate before
installation. To support a new cloud entirely, add an
`environments/<cloud>.yaml` cloud-wide tier (mirroring `environments/gcp.yaml`)
and `<cloud>/` overlay directories under `releases/` for whichever releases
need cloud-specific values (Workload Identity annotations, storage CSI
drivers, and similar) - see `releases/keycloak/gcp/`,
`releases/cert-manager/gcp/`, `releases/model-cache/gcp/` for the pattern.

## Configuration ownership

The Helmfile values are deliberately layered in this order:

1. `environments/chart-versions.yaml` contains versions for repository and OCI
   Helm charts. Local chart versions remain in their `Chart.yaml` files.
2. `environments/image-versions.yaml` contains container repositories, tags,
   optional immutable digests, and pull policies.
3. `environments/base.yaml.gotmpl` contains cloud-neutral platform configuration.
4. `environments/<cloud>.yaml` (e.g. `gcp.yaml`) contains cloud-wide defaults
   shared by every cluster on that cloud - present only for environments that
   select a cloud (not `base`).
5. `environments/<cloud>-<cluster>.yaml[.gotmpl]` contains the real project,
   domain, service-account, bucket, address, sizing, and secret-store values
   for one specific cluster.

Release-level values follow the same split where a release's own rendered
values genuinely differ by cloud: `releases/<name>/base.yaml.gotmpl` (cloud-
neutral) plus `releases/<name>/<cloud>/base.yaml.gotmpl` (cloud-specific),
included conditionally in the release's orchestrating `helmfiles/*.yaml.gotmpl`
file based on `cloudProvider`. Most releases don't need this split at all -
only add it where content genuinely changes per cloud.

Do not put image tags into `chart-versions.yaml`, and do not put Helm chart
versions into `base.yaml`. A `latest` image is accepted only when an immutable
SHA-256 digest is also supplied.

`chartSources` supports two explicit modes:

- `local`: `chart` is a reviewed source directory and Helm uses its
  `Chart.yaml` version.
- `repository`: `chart` is a repository-qualified chart name and Helmfile uses
  the corresponding entry in `chart-versions.yaml`.

The Keycloak and OpenSearch wrappers currently use reviewed local sources.
After publishing them, change only their `chartSources.*.mode` and
`chartSources.*.chart` values; their release versions already have dedicated
entries in `chart-versions.yaml`.

The umbrella `deploy/validate-platform.sh` and `deploy/install-platform.sh`
commands install or update the ForwardMeasure platform as a whole: shared
platform services plus every product built on them. Today that's the shared
services here plus OpenWorkflow (`forwardmeasure-openworkflow/deploy/helmfile`);
Entity Intelligence is a planned addition, not wired into these commands yet.

The corrected OpenSearch and Keycloak wrappers are consumed from the sibling
`helm-charts` checkout until versions `0.1.4` and `0.0.22` are published.
After publication, change their `chartSources` values to the published chart
names and restore version constraints on those releases.

```bash
./deploy/helmfile/validate.sh gcp-greenfield-example
./deploy/helmfile/install.sh gcp-greenfield-example
```

A single stage can be applied during controlled maintenance:

```bash
./deploy/helmfile/install.sh gcp-greenfield-example messaging
```

Uninstall deliberately retains the foundational controllers and namespaces:

```bash
./deploy/helmfile/uninstall.sh gcp-greenfield-example
```
