locals {
  cluster_name  = coalesce(var.gke.name, "${substr(var.platform_name, 0, 24)}-${substr(var.environment, 0, 10)}")
  cloudsql_name = coalesce(var.cloudsql.name, "${var.platform_name}-${var.environment}-postgres")
  dns_zone_name = coalesce(var.dns.managed_zone_name, replace(var.dns.root_domain, ".", "-"))
  root_domain   = trimsuffix(var.dns.root_domain, ".")
  workload_pool = "${var.project_id}.svc.id.goog"

  labels = merge({
    "environment" = var.environment
    "managed-by"  = "opentofu"
    "platform"    = var.platform_name
  }, var.labels)

  dns_records = {
    platform  = "${var.dns.platform_host}.${local.root_domain}."
    auth      = "${var.dns.auth_host}.${local.root_domain}."
    registry  = "${var.dns.registry_host}.${local.root_domain}."
    search    = "${var.dns.search_host}.${local.root_domain}."
    analytics = "${var.dns.analytics_host}.${local.root_domain}."
    tenants   = "*.${local.root_domain}."
    models    = "*.models.${local.root_domain}."
  }
}

module "project_services" {
  source = "./modules/project-services"

  project_id = var.project_id
  services   = var.enabled_project_services
}

module "network" {
  source = "./modules/network"

  project_id               = var.project_id
  region                   = var.region
  name                     = local.cluster_name
  subnet_cidr              = var.network.subnet_cidr
  pods_cidr                = var.network.pods_cidr
  services_cidr            = var.network.services_cidr
  private_services_prefix  = var.network.private_services_prefix
  nat_min_ports_per_vm     = var.network.nat_min_ports_per_vm
  nat_enable_dynamic_ports = var.network.nat_enable_dynamic_ports
  nat_log_filter           = var.network.nat_log_filter
  gateway_ip_address       = var.network.gateway_ip_address

  depends_on = [module.project_services]
}

module "iam" {
  source = "./modules/iam"

  project_id                 = var.project_id
  cluster_name               = local.cluster_name
  workload_pool              = local.workload_pool
  node_service_account_roles = var.gke.node_service_account_roles
  workload_service_accounts  = var.workload_service_accounts
  bucket_names               = module.storage.bucket_names

  depends_on = [module.project_services, module.storage]
}

module "gke" {
  source = "./modules/gke"

  project_id                  = var.project_id
  region                      = var.region
  cluster_name                = local.cluster_name
  network_id                  = module.network.network_id
  subnetwork_id               = module.network.subnetwork_id
  pods_range_name             = module.network.pods_range_name
  services_range_name         = module.network.services_range_name
  node_service_account_email  = module.iam.node_service_account_email
  deletion_protection         = var.gke.deletion_protection
  release_channel             = var.gke.release_channel
  min_master_version          = var.gke.min_master_version
  private_nodes               = var.gke.private_nodes
  private_endpoint            = var.gke.private_endpoint
  master_ipv4_cidr            = var.gke.master_ipv4_cidr
  master_authorized_networks  = var.gke.master_authorized_networks
  enable_dns_cache            = var.gke.enable_dns_cache
  enable_gcs_fuse_csi         = var.gke.enable_gcs_fuse_csi
  enable_binary_authorization = var.gke.enable_binary_authorization
  security_posture_mode       = var.gke.security_posture_mode
  vulnerability_mode          = var.gke.vulnerability_mode
  maintenance_start_time      = var.gke.maintenance_start_time
  maintenance_end_time        = var.gke.maintenance_end_time
  maintenance_recurrence      = var.gke.maintenance_recurrence
  cpu_node_pools              = var.cpu_node_pools
  gpu_node_pools              = var.gpu_node_pools
  resource_labels             = local.labels

  depends_on = [module.project_services, module.network, module.iam]
}

module "cloudsql" {
  source = "./modules/cloudsql"

  project_id                     = var.project_id
  region                         = var.region
  name                           = local.cloudsql_name
  network_id                     = module.network.network_id
  database_version               = var.cloudsql.database_version
  tier                           = var.cloudsql.tier
  availability_type              = var.cloudsql.availability_type
  disk_type                      = var.cloudsql.disk_type
  disk_size_gb                   = var.cloudsql.disk_size_gb
  disk_autoresize_limit_gb       = var.cloudsql.disk_autoresize_limit_gb
  deletion_protection            = var.cloudsql.deletion_protection
  backup_start_time              = var.cloudsql.backup_start_time
  backup_retained_count          = var.cloudsql.backup_retained_count
  transaction_log_retention_days = var.cloudsql.transaction_log_retention_days
  maintenance_day                = var.cloudsql.maintenance_day
  maintenance_hour               = var.cloudsql.maintenance_hour
  ssl_mode                       = var.cloudsql.ssl_mode
  query_insights_enabled         = var.cloudsql.query_insights_enabled
  databases                      = var.cloudsql_databases
  users                          = var.cloudsql_users
  user_passwords                 = var.cloudsql_user_passwords
  labels                         = local.labels

  depends_on = [module.project_services, module.network]
}

module "storage" {
  source = "./modules/storage"

  project_id = var.project_id
  region     = var.region
  prefix     = "${var.platform_name}-${var.environment}"
  buckets    = var.buckets
  labels     = local.labels

  depends_on = [module.project_services]
}

module "secret_manager" {
  source = "./modules/secret-manager"

  project_id = var.project_id
  secret_ids = setunion(var.secret_ids, var.additional_secret_ids)
  labels     = local.labels

  depends_on = [module.project_services]
}

module "dns" {
  source = "./modules/dns"

  project_id          = var.project_id
  create_managed_zone = var.dns.create_managed_zone
  managed_zone_name   = local.dns_zone_name
  root_domain         = local.root_domain
  records             = local.dns_records
  address             = module.network.gateway_ip_address
  ttl                 = var.dns.ttl
  labels              = local.labels

  depends_on = [module.project_services, module.network]
}

locals {
  extension_bootstrap = {
    for database, config in var.cloudsql_extensions : database => {
      username   = config.username
      extensions = sort(tolist(config.extensions))
      digest = substr(sha256(jsonencode({
        database   = database
        username   = config.username
        extensions = sort(tolist(config.extensions))
        image      = var.postgres_bootstrap_image
      })), 0, 12)
    }
    if length(config.extensions) > 0
  }
}

resource "kubernetes_namespace_v1" "database_bootstrap" {
  count = length(local.extension_bootstrap) > 0 ? 1 : 0

  metadata {
    name = "platform-database-bootstrap"
  }

  depends_on = [module.gke]
}

resource "kubernetes_secret_v1" "database_bootstrap" {
  for_each = local.extension_bootstrap

  metadata {
    name      = "postgres-${each.key}-${each.value.digest}"
    namespace = kubernetes_namespace_v1.database_bootstrap[0].metadata[0].name
  }

  data_wo = {
    username = each.value.username
    password = var.cloudsql_user_passwords[each.value.username]
  }

  data_wo_revision = var.postgres_bootstrap_password_revision
  type             = "Opaque"

  lifecycle {
    precondition {
      condition     = contains(keys(var.cloudsql_user_passwords), each.value.username)
      error_message = "Every cloudsql_extensions username must have a value in the ephemeral cloudsql_user_passwords map."
    }
  }

  depends_on = [module.gke, module.cloudsql]
}

resource "kubernetes_job_v1" "database_bootstrap" {
  for_each = local.extension_bootstrap

  metadata {
    name      = "postgres-${each.key}-${each.value.digest}"
    namespace = kubernetes_namespace_v1.database_bootstrap[0].metadata[0].name
    labels = {
      "app.kubernetes.io/component"  = "database-bootstrap"
      "app.kubernetes.io/managed-by" = "opentofu"
      "app.kubernetes.io/name"       = "${var.platform_name}-postgres-bootstrap"
    }
  }

  wait_for_completion = true

  spec {
    backoff_limit              = 6
    active_deadline_seconds    = 1200
    completions                = 1
    parallelism                = 1
    ttl_seconds_after_finished = null

    template {
      metadata {
        labels = {
          "app.kubernetes.io/component" = "database-bootstrap"
          "app.kubernetes.io/name"      = "${var.platform_name}-postgres-bootstrap"
        }
      }

      spec {
        restart_policy = "Never"

        container {
          name              = "postgres-bootstrap"
          image             = var.postgres_bootstrap_image
          image_pull_policy = "IfNotPresent"
          command           = ["/bin/bash", "-ceu"]
          args = [<<-EOT
            for attempt in $(seq 1 60); do
              if pg_isready -h "$DB_HOST" -p 5432 -d "$DB_NAME" -U "$DB_USER" >/dev/null 2>&1; then
                break
              fi
              if [ "$attempt" -eq 60 ]; then
                echo "PostgreSQL did not become ready" >&2
                exit 1
              fi
              sleep 5
            done

            ${join("\n", [for extension in each.value.extensions : "psql -h \"$DB_HOST\" -p 5432 -d \"$DB_NAME\" -U \"$DB_USER\" -v ON_ERROR_STOP=1 -c 'CREATE EXTENSION IF NOT EXISTS \"${extension}\";' "])}
          EOT
          ]

          env {
            name  = "DB_HOST"
            value = module.cloudsql.private_ip_address
          }

          env {
            name  = "DB_NAME"
            value = each.key
          }

          env {
            name = "DB_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.database_bootstrap[each.key].metadata[0].name
                key  = "username"
              }
            }
          }

          env {
            name = "PGPASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.database_bootstrap[each.key].metadata[0].name
                key  = "password"
              }
            }
          }

          env {
            name  = "PGSSLMODE"
            value = "require"
          }

          resources {
            requests = {
              cpu    = "50m"
              memory = "64Mi"
            }
            limits = {
              memory = "256Mi"
            }
          }

          security_context {
            allow_privilege_escalation = false
            capabilities {
              drop = ["ALL"]
            }
            read_only_root_filesystem = true
            run_as_non_root           = true
          }
        }

        security_context {
          run_as_non_root = true
          seccomp_profile {
            type = "RuntimeDefault"
          }
        }
      }
    }
  }

  timeouts {
    create = "25m"
    update = "25m"
  }

  depends_on = [module.cloudsql, module.gke, kubernetes_secret_v1.database_bootstrap]
}

check "control_plane_access" {
  assert {
    condition     = var.gke.private_endpoint || length(var.gke.master_authorized_networks) > 0
    error_message = "A cluster with a public control-plane endpoint requires at least one master_authorized_networks entry."
  }
}

check "helmfile_identity_contract" {
  assert {
    condition = alltrue([
      contains(keys(var.workload_service_accounts), "cert-manager"),
      contains(keys(var.workload_service_accounts), "external-secrets"),
      contains(keys(var.workload_service_accounts), "model-serving"),
      contains(keys(var.buckets), "model-cache"),
    ])
    error_message = "The Helmfile contract requires cert-manager, external-secrets and model-serving service accounts plus the model-cache bucket."
  }
}

check "extension_bootstrap_contract" {
  assert {
    condition = alltrue([
      for database, config in var.cloudsql_extensions :
      contains(var.cloudsql_databases, database) && contains(keys(var.cloudsql_users), config.username)
    ])
    error_message = "Every extension bootstrap entry must refer to a configured Cloud SQL database and user."
  }
}
