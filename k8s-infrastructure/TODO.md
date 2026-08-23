# Kubernetes Infrastructure TODO

## GCP — current phase

- [x] Extract the GCP implementation from the retiring Data Fabric repository.
- [x] Support either an existing project or greenfield project creation and billing attachment.
- [x] Enable the required GCP project services declaratively.
- [x] Provision VPC-native networking, explicit pod/service ranges, Private Service Access and Cloud NAT.
- [x] Replace destructive IAM scripts with declarative service accounts, roles and Workload Identity bindings.
- [x] Provision hardened regional GKE with managed CPU and GPU node pools.
- [x] Stop ignoring node-pool configuration changes.
- [x] Provision private regional Cloud SQL PostgreSQL with deletion protection, backups and PITR.
- [x] Replace dummy and shell-managed database passwords with ephemeral/write-only password application.
- [x] Retain PostgreSQL extension bootstrap as an idempotent, versioned Kubernetes Job using write-only Secret data.
- [x] Provision GCS, Cloud DNS, Gateway IP and Secret Manager containers.
- [x] Generate the non-secret GCP-to-Helmfile environment contract.
- [x] Add provider validation and a mocked complete-stack plan test.
- [x] Provide a start-to-finish GCP operator guide with commands, explanations and expected results.
- [ ] Remove the manual Helmfile environment-registration step across the platform, OKS and Entity Intelligence repositories.
- [ ] Apply in a new GCP project and record the real infrastructure acceptance result.
- [ ] Run `deploy/validate-platform.sh` against the generated GCP environment.
- [ ] Install the platform, OKS and Entity Intelligence into that cluster and record browser/API acceptance.

## AWS — subsequent phase

- [ ] Implement the equivalent EKS platform under `opentofu/aws`.
- [ ] Provide VPC, managed node groups, RDS PostgreSQL, S3, Route 53, Secrets Manager and Pod Identity/IRSA.
- [ ] Generate the same logical Helmfile environment contract.
- [ ] Prove a complete greenfield AWS deployment.

## Azure — subsequent phase

- [ ] Implement the equivalent AKS platform under `opentofu/azure`.
- [ ] Provide virtual networking, managed node pools, Azure PostgreSQL, Blob Storage, Azure DNS, Key Vault and Workload Identity.
- [ ] Generate the same logical Helmfile environment contract.
- [ ] Prove a complete greenfield Azure deployment.

## Cross-cloud convergence

- [ ] Extract a provider-neutral schema for infrastructure-to-Helmfile outputs.
- [ ] Add contract tests proving that GCP, AWS and Azure emit equivalent logical capabilities.
- [ ] Keep cloud-specific resource vocabulary out of application configuration.
