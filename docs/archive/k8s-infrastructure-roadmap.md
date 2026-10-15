# Kubernetes Infrastructure Roadmap

## Phase 1 — GCP

The first implementation lives in `k8s-infrastructure/opentofu/gcp` and owns
the complete GCP substrate required by the ForwardMeasure platform:

- project service enablement;
- VPC-native regional GKE, private nodes, Cloud NAT and explicit pod/service ranges;
- managed CPU and GPU node pools;
- declarative IAM and Workload Identity;
- regional private Cloud SQL PostgreSQL and database bootstrap;
- GCS buckets;
- Cloud DNS and the regional Gateway address;
- Secret Manager containers; and
- generated non-secret values for the Helmfile deployment.

The Phase 1 acceptance gate is a plan and apply in a clean GCP project,
followed by the greenfield Helmfile validation and installation.

## Phase 2 — AWS

Add `k8s-infrastructure/opentofu/aws` with the same platform-facing outputs:

- VPC, private subnets, egress and load-balancer addresses;
- regional EKS and managed CPU/GPU node groups;
- IRSA/Pod Identity equivalents of the workload identities;
- RDS PostgreSQL, databases and extension bootstrap;
- S3 buckets;
- Route 53 records and hosted zone support;
- Secrets Manager containers; and
- a generated AWS Helmfile environment overlay.

The implementation must preserve the logical output contract rather than
copying GCP resource vocabulary into the Helmfile.

## Phase 3 — Azure

Add `k8s-infrastructure/opentofu/azure` with the same platform-facing outputs:

- virtual network, subnets, outbound egress and public Gateway address;
- regional AKS and managed CPU/GPU node pools;
- Azure Workload Identity;
- Azure Database for PostgreSQL, databases and extension bootstrap;
- Blob Storage containers;
- Azure DNS records and zone support;
- Key Vault secret containers; and
- a generated Azure Helmfile environment overlay.

## Cross-cloud convergence

After all three providers exist, extract the shared environment-output schema
and validation suite. The Kubernetes application layer must consume logical
capabilities—database, object storage, DNS, identity and secrets—without
embedding GCP, AWS or Azure resource names into application configuration.
