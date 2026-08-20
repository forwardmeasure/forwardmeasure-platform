# Deploy ForwardMeasure on GCP

This guide explains what to do, why each step exists, the commands to run and
how to tell whether each step succeeded.

Run all commands from:

```bash
cd /home/pn/Documents/code/forwardmeasure/forwardmeasure-platform/k8s-infrastructure/opentofu/gcp
```

## What this deployment creates

The deployment creates or configures:

1. A GCP project and its billing association, if you do not already have one.
2. A GCS bucket where OpenTofu records the cloud resources it manages.
3. The Google APIs needed by the platform.
4. The VPC, subnet, Kubernetes pod/service address ranges and outbound NAT.
5. A regional GKE cluster with CPU and optional GPU node pools.
6. A private PostgreSQL Cloud SQL instance containing databases for Keycloak,
   Superset, OKS and Entity Intelligence.
7. GCS buckets for the shared model cache and Entity Intelligence objects.
8. Service accounts and Kubernetes Workload Identity bindings.
9. A public Cloud DNS zone and IP address for the platform Gateway.
10. Google Secret Manager entries used by the Kubernetes applications.
11. Any configured PostgreSQL extensions.

After those cloud resources exist, a second deployment installs Kubernetes
software: Istio, Keycloak, Kafka, Apicurio, OpenSearch, KServe, the Platform
Dashboard, OKS and Entity Intelligence.

## Why there are two OpenTofu directories

OpenTofu must record the IDs and configuration of resources it creates. That
record is called **state**. The main deployment stores its state in a GCS
bucket so subsequent runs can update the existing resources rather than trying
to create duplicates.

The state bucket must exist before the main deployment starts. Therefore:

- `gcp-project-and-state/` creates the GCP project, when requested, and creates
  the state bucket.
- the directory containing this guide creates the actual platform resources.

`gcp-project-and-state` is normally run once per GCP project. The main
deployment is run whenever the platform infrastructure changes.

## Information you need before starting

Decide these values:

| Value | Example | Meaning |
|---|---|---|
| GCP project ID | `forwardmeasure-production` | Globally unique project identifier |
| Billing account ID | `XXXXXX-XXXXXX-XXXXXX` | Pays for the project |
| GCP region | `us-central1` | Region containing GKE and Cloud SQL |
| Root domain | `example.com` | Domain used for platform and tenant URLs |
| Administrator CIDR | `198.51.100.24/32` | Public address allowed to reach the GKE API |
| Environment name | `production` | Name used for files, labels and resources |
| First tenant code | `example-bank` | Produces `example-bank.example.com` |

If the project belongs to a GCP organization, also obtain its folder ID or
organization ID.

## Required local tools

The following commands must be installed and available on `PATH`:

```bash
gcloud --version
gke-gcloud-auth-plugin --version
tofu version
kubectl version --client
helm version
helmfile --version
yq --version
jq --version
```

Authenticate both the Google CLI and the Google client libraries used by
OpenTofu:

```bash
gcloud auth login
gcloud auth application-default login
```

The authenticated account needs permission to create or administer the chosen
project, attach its billing account and create the resources listed above.

## Step 1: Validate the source

Run:

```bash
./scripts/validate.sh
```

Why: this checks formatting, provider configuration, module wiring and a
complete simulated plan without creating anything in GCP.

Expected result:

```text
Success! The configuration is valid.
Success! 1 passed, 0 failed.
```

Do not proceed if this command fails.

## Step 2: Configure the project and state bucket

Create your working configuration:

```bash
cp environments/project-and-state.tfvars.example \
  environments/production.project-and-state.tfvars
```

Edit `environments/production.project-and-state.tfvars` and set:

- `project_id`
- `state_bucket_name`
- `billing_account`
- `region`
- `folder_id` or `organization_id`, when applicable

Set `create_project = true` for a new project. Set it to `false` when the
project already exists and already has billing enabled.

Review what OpenTofu will create:

```bash
mkdir -p plans
tofu -chdir=gcp-project-and-state init
tofu -chdir=gcp-project-and-state plan \
  -var-file=../environments/production.project-and-state.tfvars \
  -out=../plans/production-project-and-state.tfplan
```

Apply that exact reviewed plan:

```bash
tofu -chdir=gcp-project-and-state apply \
  ../plans/production-project-and-state.tfplan
```

Expected result: the command prints the project ID and state-bucket name.

Set the new project as the active CLI project:

```bash
gcloud config set project YOUR_PROJECT_ID
```

## Step 3: Configure the platform infrastructure

Create the two environment files used by the main deployment:

```bash
cp environments/backend.hcl.example environments/production.backend.hcl
cp environments/production.tfvars.example environments/production.tfvars
```

Edit `environments/production.backend.hcl` and set the state bucket created in
Step 2.

Edit `environments/production.tfvars` and replace every example value. At a
minimum, review:

- project, region and root domain;
- the administrator CIDR;
- CPU and GPU node-pool sizes;
- Cloud SQL size and deletion protection;
- database and database-user names;
- PostgreSQL extensions;
- GCS buckets; and
- the tenant-specific OKS client-secret name in `additional_secret_ids`.

The example creates four databases and users:

| Database | User | Consumer |
|---|---|---|
| `keycloak` | `keycloak` | Keycloak |
| `superset` | `superset` | Superset |
| `openworkflow` | `openworkflow` | OKS |
| `entity_intelligence` | `entity_intelligence` | Entity Intelligence |

## Step 4: Supply the database passwords

The database passwords are deliberately not placed in the environment file.
Create or retrieve four passwords, then export them for this terminal session:

```bash
read -rsp 'Keycloak database password: ' KEYCLOAK_DB_PASSWORD; echo
read -rsp 'Superset database password: ' SUPERSET_DB_PASSWORD; echo
read -rsp 'OKS database password: ' OKS_DB_PASSWORD; echo
read -rsp 'Entity Intelligence database password: ' EI_DB_PASSWORD; echo

export TF_VAR_cloudsql_user_passwords="$(jq -nc \
  --arg keycloak "$KEYCLOAK_DB_PASSWORD" \
  --arg superset "$SUPERSET_DB_PASSWORD" \
  --arg openworkflow "$OKS_DB_PASSWORD" \
  --arg entity_intelligence "$EI_DB_PASSWORD" \
  '{keycloak:$keycloak,superset:$superset,openworkflow:$openworkflow,entity_intelligence:$entity_intelligence}')"
```

Why: OpenTofu passes this map to Cloud SQL through a write-only provider field.
It is available during the command but is not saved in the plan or state.

Keep this terminal open until both the plan and apply finish. The apply needs
the same values because the saved plan does not contain them.

## Step 5: Create and apply the infrastructure plan

Run:

```bash
./scripts/plan.sh production
```

This initializes the remote state, calculates all proposed GCP changes and
saves them to `plans/production.tfplan`. Read the plan before continuing.

Apply the saved plan:

```bash
./scripts/apply-plan.sh production
```

This creates the GCP and GKE resources. It may take tens of minutes. GPU pools
also require sufficient regional quota and accelerator availability.

The apply finishes by running Kubernetes Jobs that install the configured
PostgreSQL extensions. A failed extension installation causes the OpenTofu
apply to fail rather than silently leaving the database incomplete.

## Step 6: Connect kubectl to the new cluster

Run:

```bash
CLUSTER_NAME="$(tofu output -json cluster | jq -r .name)"
CLUSTER_REGION="$(tofu output -json cluster | jq -r .location)"
gcloud container clusters get-credentials "$CLUSTER_NAME" \
  --region "$CLUSTER_REGION" --project YOUR_PROJECT_ID
kubectl get nodes
```

Expected result: `kubectl get nodes` lists the new GKE nodes or node pools that
are currently scaled to zero.

## Step 7: Configure DNS delegation

When this deployment created a new Cloud DNS zone, display its name servers:

```bash
tofu output -json dns | jq -r '.name_servers[]'
```

Configure those name servers at the domain registrar. DNS records will not be
publicly resolvable until that delegation is complete.

If the domain already uses an existing Cloud DNS zone, configure
`create_managed_zone = false` and supply that zone's name before applying.

## Step 8: Load application values into Secret Manager

OpenTofu creates the Secret Manager entries, but an entry is empty until it has
at least one value version. List all entries:

```bash
tofu output -json secret_ids | jq -r 'keys[]' | sort
```

Add a value interactively:

```bash
./scripts/add-secret-version.sh YOUR_PROJECT_ID SECRET_NAME
```

Add a value from a file, for example the Docker registry configuration:

```bash
./scripts/add-secret-version.sh YOUR_PROJECT_ID \
  platform-container-registry-dockerconfigjson /path/to/config.json
```

At minimum, load values for:

- the four database usernames and passwords;
- Keycloak's JDBC URL, administrator credentials and client secrets;
- Superset's SQLAlchemy URI, component values and administrator credentials;
- OpenSearch's username, password, bcrypt password hash and cookie secret;
- the Docker registry configuration;
- the Hugging Face token when model downloads require it;
- the Valkey password;
- the OKS database username and password;
- the Entity Intelligence database username and password; and
- every tenant-specific OKS operation-adapter client secret.

The Cloud SQL private address is available with:

```bash
tofu output -json cloudsql | jq -r .private_ip
```

Use that address when constructing the Keycloak JDBC URL, Superset SQLAlchemy
URI, OKS JDBC URL and Entity Intelligence JDBC URL.

## Step 9: Create the Kubernetes deployment environment files

Render the values discovered from GCP:

```bash
./scripts/render-helmfile-environment.sh \
  ../../../deploy/helmfile/environments/production.infrastructure.yaml
```

This file contains the project, region, cluster, Gateway IP, domain, bucket and
service-account values. It does not contain passwords.

Create the shared-platform environment:

```bash
yq eval-all '. as $item ireduce ({}; . * $item)' \
  ../../../deploy/helmfile/environments/gcp-greenfield.example.yaml \
  ../../../deploy/helmfile/environments/production.infrastructure.yaml \
  > ../../../deploy/helmfile/environments/production.yaml
```

Then edit `deploy/helmfile/environments/production.yaml` and replace the
remaining tenant, email, client and container-image digest examples.

Create and edit the corresponding environment files in the sibling projects:

```bash
cp ../../../../openworkflow-kafka-streams/deploy/helmfile/environments/gcp-greenfield.example.yaml \
  ../../../../openworkflow-kafka-streams/deploy/helmfile/environments/production.yaml

cp ../../../../forwardmeasure-entity-intelligence/deploy/helmfile/environments/gcp-greenfield.example.yaml \
  ../../../../forwardmeasure-entity-intelligence/deploy/helmfile/environments/production.yaml
```

For both files, set the same tenant ID, code, hostname and DID. Also set:

- the Keycloak issuer and public client ID;
- the Cloud SQL private IP and appropriate database name;
- the GCP project, Entity Intelligence service-account email and object bucket;
- the tenant-specific OKS adapter client-secret name; and
- every deployed container-image digest.

Finally, register `production` in each repository's
`deploy/helmfile/helmfile.yaml.gotmpl`, following the existing
`gcp-greenfield-example` entry. This step is currently manual and is recorded
as deployment tooling work in `k8s-infrastructure/TODO.md`.

## Step 10: Validate and install the software

From the `forwardmeasure-platform` repository root, run:

```bash
./deploy/validate-greenfield.sh production
```

This renders and validates the shared platform, OKS and Entity Intelligence
without installing them.

When validation passes, install all three tiers:

```bash
./deploy/install-greenfield.sh production
```

The installer deploys, in order:

1. the shared platform services;
2. OKS; and
3. Entity Intelligence.

It then waits for the configured readiness checks. A successful command means
the Kubernetes releases are installed; browser and API acceptance testing is
still required before declaring the environment ready for users.

## What to send when a command fails

Capture the exact failing command and its output. Useful diagnostics include:

```bash
tofu show plans/production.tfplan
tofu state list
kubectl get pods -A
kubectl get events -A --sort-by=.lastTimestamp
helmfile --file ../../../deploy/helmfile/helmfile.yaml.gotmpl \
  --environment production list
```

Do not rerun `apply` repeatedly without first identifying which resource or
Kubernetes Job failed.
