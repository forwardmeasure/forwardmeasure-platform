# ForwardMeasure GCP Kubernetes Infrastructure

For the complete operator procedure, start with
[Deploy ForwardMeasure on GCP](GETTING_STARTED.md). This file describes the
implementation details and individual helper commands.

This OpenTofu stack provisions the complete GCP substrate consumed by the
ForwardMeasure Helmfile deployment:

- required project APIs;
- a custom VPC, subnet, explicit pod/service ranges, Cloud Router and Cloud NAT;
- Private Service Access for Cloud SQL;
- a regional external address for the Kubernetes Gateway;
- declarative node and workload service accounts with Workload Identity;
- a regional, VPC-native GKE Standard cluster with CPU and optional GPU pools;
- private, regional Cloud SQL for PostgreSQL, databases and built-in users;
- GCS platform buckets;
- a public Cloud DNS zone and all shared/wildcard platform records;
- the Secret Manager containers required by `deploy/helmfile`; and
- optional PostgreSQL extension bootstrap Jobs.

The `gcp-project-and-state` configuration can either use an existing billed GCP project or create and
attach a new project when `create_project=true` and `billing_account` is
provided. Everything inside that project that is required by the platform is
then declared by the primary stack.

## Layout

- `gcp-project-and-state/` creates the project, when requested, and the GCS
  bucket used for remote OpenTofu state. It uses
  local state because a backend cannot create its own bucket.
- the directory containing this README is the primary GCP stack;
- `modules/` contains the GCP capability modules;
- `environments/` contains non-secret examples only; and
- `scripts/` contains validation and Helmfile handoff helpers.

## Password lifecycle

Cloud SQL passwords are supplied through the ephemeral
`cloudsql_user_passwords` variable. The module writes them through
`google_sql_user.password_wo`; OpenTofu therefore does not persist the password
in its plan or state. PostgreSQL bootstrap Secrets use Kubernetes
`data_wo` for the same reason.

Export the values immediately before planning and applying:

```bash
export TF_VAR_cloudsql_user_passwords='{"keycloak":"...","superset":"..."}'
```

The corresponding application values must also be placed into the Secret
Manager containers created by this stack. Secret values are deliberately not
managed here because normal Secret Manager versions would place their payload
in OpenTofu state.

When rotating a password, increment its `password_version` and
`postgres_bootstrap_password_revision`, then apply with the new ephemeral map.

## Provisioning

Create the state bucket once:

```bash
tofu -chdir=gcp-project-and-state init
tofu -chdir=gcp-project-and-state apply \
  -var='project_id=forwardmeasure-production' \
  -var='state_bucket_name=forwardmeasure-production-opentofu-state'
```

For a completely new project, add
`-var='create_project=true' -var='billing_account=XXXXXX-XXXXXX-XXXXXX'`
and, where applicable, `folder_id` or `organization_id`.

Copy the environment examples to ignored private files, then initialise and
apply the primary stack:

```bash
cp environments/backend.hcl.example environments/production.backend.hcl
cp environments/production.tfvars.example environments/production.tfvars

./scripts/plan.sh production
# Review the saved plan.
./scripts/apply-plan.sh production
```

The PostgreSQL extension Jobs are part of the same apply. They wait for the
private Cloud SQL endpoint and run idempotent `CREATE EXTENSION IF NOT EXISTS`
statements. Their names include a digest of the database, image and extension
set, so a meaningful bootstrap change creates a new Job rather than attempting
to mutate an immutable completed Job.

The machine running OpenTofu must be able to reach the GKE control-plane
endpoint. If `private_endpoint` is enabled, run OpenTofu from the VPC or through
an approved administrative access path. The Kubernetes provider uses
`gke-gcloud-auth-plugin` exclusively so it can refresh credentials during a
long cluster and node-pool apply; install that plugin before applying.

## Helmfile handoff

After apply, render the non-secret Terraform outputs as a Helmfile environment
overlay:

```bash
./scripts/render-helmfile-environment.sh \
  ../../../deploy/helmfile/environments/gcp-production.generated.yaml
```

The generated file supplies GCP coordinates, Gateway IP, DNS hosts, bucket and
Workload Identity service-account values. It contains no credentials.

## Validation

```bash
./scripts/validate.sh
```

Validation does not contact GCP and does not create infrastructure. A real
greenfield acceptance run still requires a disposable or designated GCP
project with billing and sufficient quota, especially when GPU pools are
enabled.
