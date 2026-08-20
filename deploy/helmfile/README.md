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
`environments/base.yaml`. Database addresses may be private or public. Public
PostgreSQL endpoints must require verified TLS; complete connection URLs are
stored in Secret Manager rather than reconstructed by Helm.

No value is inherited from the retiring Data Fabric project. Copy
`environments/gcp-greenfield.example.yaml`, replace its placeholders, add the
new environment to `helmfile.yaml.gotmpl`, and validate before installation.

## Configuration ownership

The Helmfile values are deliberately layered in this order:

1. `environments/chart-versions.yaml` contains versions for repository and OCI
   Helm charts. Local chart versions remain in their `Chart.yaml` files.
2. `environments/image-versions.yaml` contains container repositories, tags,
   optional immutable digests, and pull policies.
3. `environments/base.yaml` contains cloud-neutral platform configuration.
4. `environments/<environment>.yaml` contains the cloud project, domain,
   service-account, bucket, address, sizing, and secret-store values for one
   deployment.

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

The umbrella `deploy/validate-greenfield.sh` and `deploy/install-greenfield.sh`
commands also ask the proprietary Entity Intelligence repository to render its
tenant-aware OKS integration policy into a temporary file. That file is passed
to the generic OKS Helmfile as an explicit state-values overlay and then
deleted. This preserves product ownership: the shared platform and OKS sources
contain no Entity Intelligence endpoints, while the composed deployment still
installs the endpoint bindings, resource bindings, outbound authorization and
OAuth secret references required by Entity Intelligence workflows.

The corrected OpenSearch and Keycloak wrappers are consumed from the sibling
`helm-charts` checkout until versions `0.1.4` and `0.0.22` are published.
After publication, change their `chartSources` values to the published chart
names and restore version constraints on those releases.

```bash
./deploy/helmfile/validate.sh gcp-greenfield-example
./deploy/helmfile/install.sh gcp-greenfield
```

A single stage can be applied during controlled maintenance:

```bash
./deploy/helmfile/install.sh gcp-greenfield messaging
```

Uninstall deliberately retains the foundational controllers and namespaces:

```bash
./deploy/helmfile/uninstall.sh gcp-greenfield
```
