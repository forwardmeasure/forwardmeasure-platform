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

The example creates three databases and three users:

| Database | Consumer |
|---|---|
| `forwardmeasure_control_plane` | tenant registry and Pekko's cluster-wide coordinator store |
| `keycloak` | Keycloak |
| `superset` | Superset |

| User | Used by |
|---|---|
| `keycloak` | Keycloak |
| `superset` | Superset |
| `forwardmeasure_admin` | every product's migration Job (admin role) |

No product has a database of its own. Tenant data lives in one database per tenant,
`forwardmeasure_<alias>`, with one schema per product; the migration Jobs create those
databases and each product's runtime role (`openworkflow`, `entityintelligence`,
`decision_engine`) at deploy time, connecting as `forwardmeasure_admin`.

## Step 4: Supply the database passwords

The database passwords are deliberately not placed in the environment file.
Create or retrieve three passwords, then export them for this terminal session:

```bash
read -rsp 'Keycloak database password: ' KEYCLOAK_DB_PASSWORD; echo
read -rsp 'Superset database password: ' SUPERSET_DB_PASSWORD; echo
read -rsp 'Migration admin database password: ' ADMIN_DB_PASSWORD; echo

export TF_VAR_cloudsql_user_passwords="$(jq -nc \
  --arg keycloak "$KEYCLOAK_DB_PASSWORD" \
  --arg superset "$SUPERSET_DB_PASSWORD" \
  --arg forwardmeasure_admin "$ADMIN_DB_PASSWORD" \
  '{keycloak:$keycloak,superset:$superset,forwardmeasure_admin:$forwardmeasure_admin}')"
```

The Keycloak and Superset passwords must match the values their own charts read
(`platform-keycloak-database-password` / `platform-superset-database-password`, delivered by
platform-secrets). Terraform is the only thing that sets these two roles' passwords.

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

Every repository's Helmfile (this one, `forwardmeasure-openworkflow`,
`forwardmeasure-data-streaming`, `forwardmeasure-entity-intelligence` and
`forwardmeasure-decision-engine`) reads the facts about a cluster from one
shared file in this repository: `deploy/helmfile/shared/clusters/<env>.yaml.gotmpl`.
Values shared across clusters live next to it in `deploy/helmfile/shared/`
(see `common.yaml.gotmpl`'s header). The products' own settings for "a GCP
cluster running this platform" are already written, in each product's
`deploy/helmfile/environments/gcp-platform-cluster.yaml.gotmpl`; a new cluster
does not need new product files.

The worked example to copy is `gcp-greenfield-example`. In the commands below,
`<env>` is your new environment's name (for example `gcp-acme-prod`), used the
same way in all five repositories.

1. Render the values discovered from GCP:

   ```bash
   ./scripts/render-helmfile-environment.sh generated/terraform-values.yaml
   ```

   This file contains the project, region, cluster, Gateway IP, domain, bucket
   and service-account values. It does not contain passwords.

2. Create the shared cluster file:

   ```bash
   cp ../../../deploy/helmfile/shared/clusters/gcp-greenfield-example.yaml.gotmpl \
     ../../../deploy/helmfile/shared/clusters/<env>.yaml.gotmpl
   ```

   Set its variables from `generated/terraform-values.yaml`:
   `$domain` (`platform.domain`), `$loadBalancerIp` (`gateway.loadBalancerIp`),
   `$gcpProjectId`, `$gcpRegion` and `$gcpClusterName` (`gcp.projectId`,
   `gcp.region`, `gcp.clusterName`), and `$cloudSqlInstance` (the Cloud SQL
   instance name). Set `$tenants` to your tenants' aliases and display names;
   each tenant's host and DID are derived from its alias and the domain, and
   its UUID is never written down. Gateway hostnames are derived from the
   domain too.

   Also set forwardmeasure-openworkflow's service accounts and overflow bucket
   (`$openworkflowRuntimeServiceAccount`, `$openworkflowMigrationsServiceAccount`,
   `$openworkflowOverflowBucket`). **This OpenTofu configuration does not create
   them yet**: they must exist, with Workload Identity bindings for the
   `openworkflow-runtime` and `openworkflow-database-migration` Kubernetes
   service accounts, before OpenWorkflow can start.

3. Create this repository's own environment file:

   ```bash
   cp ../../../deploy/helmfile/environments/gcp-greenfield-example.yaml \
     ../../../deploy/helmfile/environments/<env>.yaml
   ```

   Set the platform-only values from `generated/terraform-values.yaml`
   (`gcp.dnsServiceAccountEmail`, `gcp.externalSecretsServiceAccountEmail`,
   `gcp.modelServiceAccountEmail`, `gcp.modelCacheBucket`) plus the Keycloak and
   Superset service accounts, the ACME email, and the `openworkflow` service
   client's `tenant_did` (it must equal your tenant's DID; `scripts/preflight.sh`
   checks this). Do not copy the domain, Gateway IP, hostnames, project, region,
   cluster or Cloud SQL instance here - they come from the shared cluster file,
   and anything set here would be overridden.

4. Register `<env>` in every repository's `deploy/helmfile/helmfile.yaml.gotmpl`,
   copying its `gcp-greenfield-example` entry and changing the cluster-file path
   (`shared/clusters/<env>.yaml.gotmpl`). In this repository and
   `forwardmeasure-data-streaming` / `forwardmeasure-openworkflow`, also add the
   matching line to the per-environment file list below the `environments:` block,
   and add `<env>` to this repository's `scripts/environment-files.sh`,
   `forwardmeasure-openworkflow`'s `scripts/environment-files.sh` and the
   `production_environments` list in its `validate.sh`.

Registering the environment in five places by hand is recorded as deployment
tooling work in `k8s-infrastructure/TODO.md`, as is making this configuration's
output match the shared cluster file directly.

## Step 10: Validate and install the software

From the `forwardmeasure-platform` repository root, run:

Select product image tags in their committed image inventories and shared tags in
`deploy/helmfile/shared/image-versions.yaml`. No `OPENWORKFLOW_VERSION` export is required.

```bash
./deploy/validate-platform.sh <env>
```

This renders and validates the ForwardMeasure platform - shared services plus
every product built on them - without installing anything.

When validation passes, install (or update) it:

```bash
./deploy/install-platform.sh <env>
```

The installer deploys, in order:

1. the shared platform services;
2. OpenWorkflow (including the entity-intelligence and decision-engine database
   migrations, which share its tenant databases);
3. the data-streaming launcher;
4. the Entity Intelligence services; and
5. decision-engine (installed only once its database and credentials exist - see
   `forwardmeasure-decision-engine/docs/handover-2026-09-29-deployment-readiness.md`).

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
