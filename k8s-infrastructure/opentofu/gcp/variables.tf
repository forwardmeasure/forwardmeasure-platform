#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
variable "project_id" {
  description = "Existing GCP project in which the platform is provisioned."
  type        = string
}

variable "region" {
  description = "Primary GCP region."
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Environment name used in labels and generated deployment values."
  type        = string
  default     = "production"
}

variable "platform_name" {
  description = "Stable platform resource-name prefix."
  type        = string
  default     = "forwardmeasure"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,28}[a-z0-9]$", var.platform_name))
    error_message = "platform_name must be a lower-case GCP-compatible name between 3 and 30 characters."
  }
}

variable "labels" {
  description = "Additional labels applied to supported GCP resources."
  type        = map(string)
  default     = {}
}

variable "enabled_project_services" {
  description = "Google APIs required by the platform."
  type        = set(string)
  default = [
    "artifactregistry.googleapis.com",
    "cloudresourcemanager.googleapis.com",
    "compute.googleapis.com",
    "container.googleapis.com",
    "dns.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "logging.googleapis.com",
    "monitoring.googleapis.com",
    "secretmanager.googleapis.com",
    "servicenetworking.googleapis.com",
    "serviceusage.googleapis.com",
    "sqladmin.googleapis.com",
    "storage.googleapis.com",
  ]
}

variable "network" {
  description = "VPC, GKE secondary ranges, private-service range and Cloud NAT configuration."
  type = object({
    subnet_cidr              = optional(string, "10.20.0.0/20")
    pods_cidr                = optional(string, "10.24.0.0/14")
    services_cidr            = optional(string, "10.28.0.0/20")
    private_services_prefix  = optional(number, 16)
    nat_min_ports_per_vm     = optional(number, 64)
    nat_enable_dynamic_ports = optional(bool, true)
    nat_log_filter           = optional(string, "ERRORS_ONLY")
    gateway_ip_address       = optional(string, null)
  })
  default = {}
}

variable "gke" {
  description = "Regional GKE cluster configuration."
  type = object({
    name                        = optional(string, null)
    deletion_protection         = optional(bool, true)
    release_channel             = optional(string, "STABLE")
    min_master_version          = optional(string, null)
    private_nodes               = optional(bool, true)
    private_endpoint            = optional(bool, false)
    master_ipv4_cidr            = optional(string, "172.16.0.0/28")
    master_authorized_networks  = optional(map(string), {})
    enable_dns_cache            = optional(bool, true)
    enable_gcs_fuse_csi         = optional(bool, true)
    enable_binary_authorization = optional(bool, false)
    security_posture_mode       = optional(string, "BASIC")
    vulnerability_mode          = optional(string, "VULNERABILITY_BASIC")
    maintenance_start_time      = optional(string, "2026-01-04T06:00:00Z")
    maintenance_end_time        = optional(string, "2026-01-04T10:00:00Z")
    maintenance_recurrence      = optional(string, "FREQ=WEEKLY;BYDAY=SU")
    node_service_account_roles = optional(set(string), [
      "roles/artifactregistry.reader",
      "roles/container.defaultNodeServiceAccount",
      "roles/logging.logWriter",
      "roles/monitoring.metricWriter",
      "roles/monitoring.viewer",
      "roles/stackdriver.resourceMetadata.writer",
    ])
  })
  default = {}
}

variable "cpu_node_pools" {
  description = "CPU node pools. All meaningful fields remain managed by OpenTofu."
  type = map(object({
    machine_type         = string
    node_locations       = optional(list(string), [])
    image_type           = optional(string, "COS_CONTAINERD")
    disk_type            = optional(string, "pd-balanced")
    disk_size_gb         = optional(number, 200)
    initial_node_count   = optional(number, 1)
    total_min_node_count = optional(number, 1)
    total_max_node_count = optional(number, 6)
    spot                 = optional(bool, false)
    local_ssd_count      = optional(number, 0)
    tags                 = optional(list(string), [])
    labels               = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
  default = {
    general = {
      machine_type = "e2-standard-8"
    }
  }
}

variable "gpu_node_pools" {
  description = "Optional GPU node pools."
  type = map(object({
    machine_type         = string
    accelerator_type     = string
    accelerator_count    = number
    gpu_driver_version   = optional(string, "DEFAULT")
    node_locations       = optional(list(string), [])
    image_type           = optional(string, "COS_CONTAINERD")
    disk_type            = optional(string, "pd-balanced")
    disk_size_gb         = optional(number, 500)
    initial_node_count   = optional(number, 0)
    total_min_node_count = optional(number, 0)
    total_max_node_count = optional(number, 3)
    spot                 = optional(bool, true)
    local_ssd_count      = optional(number, 0)
    tags                 = optional(list(string), [])
    labels               = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [{ key = "nvidia.com/gpu", value = "present", effect = "NO_SCHEDULE" }])
  }))
  default = {}
}

variable "cloudsql" {
  description = "Private regional Cloud SQL for PostgreSQL configuration."
  type = object({
    name                           = optional(string, null)
    database_version               = optional(string, "POSTGRES_18")
    tier                           = optional(string, "db-custom-4-16384")
    availability_type              = optional(string, "REGIONAL")
    disk_type                      = optional(string, "PD_SSD")
    disk_size_gb                   = optional(number, 250)
    disk_autoresize_limit_gb       = optional(number, 2000)
    deletion_protection            = optional(bool, true)
    backup_start_time              = optional(string, "03:00")
    backup_retained_count          = optional(number, 14)
    transaction_log_retention_days = optional(number, 7)
    maintenance_day                = optional(number, 7)
    maintenance_hour               = optional(number, 5)
    ssl_mode                       = optional(string, "ENCRYPTED_ONLY")
    query_insights_enabled         = optional(bool, true)
  })
  default = {}
}

variable "cloudsql_databases" {
  description = "Databases created on the platform Cloud SQL instance."
  type        = set(string)
  default     = ["entity_intelligence", "keycloak", "openworkflow", "superset"]
}

variable "cloudsql_users" {
  description = "Built-in PostgreSQL users and write-only password revision numbers."
  type = map(object({
    password_version = optional(number, 1)
    deletion_policy  = optional(string, "ABANDON")
  }))
  default = {
    entity_intelligence = {}
    keycloak            = {}
    openworkflow        = {}
    superset            = {}
  }
}

variable "cloudsql_user_passwords" {
  description = "Passwords keyed by cloudsql_users. Supply only at plan/apply time; values are write-only and are not persisted in state."
  type        = map(string)
  sensitive   = true
  ephemeral   = true
  default     = {}
}

variable "cloudsql_extensions" {
  description = "Extensions installed by a Kubernetes Job after databases and users exist."
  type = map(object({
    username   = string
    extensions = set(string)
  }))
  default = {}

  validation {
    condition = alltrue(flatten([
      for config in values(var.cloudsql_extensions) : [
        for extension in config.extensions : can(regex("^[A-Za-z][A-Za-z0-9_-]*$", extension))
      ]
    ]))
    error_message = "PostgreSQL extension names may contain only letters, digits, underscores and hyphens and must begin with a letter."
  }
}

variable "postgres_bootstrap_image" {
  description = "Immutable PostgreSQL client image used by extension bootstrap jobs."
  type        = string
  default     = "docker.io/library/postgres:18.0"
}

variable "postgres_bootstrap_password_revision" {
  description = "Revision passed to Kubernetes write-only Secret data. Increment after rotating a database password."
  type        = number
  default     = 1
}

variable "dns" {
  description = "Public platform DNS zone and host configuration."
  type = object({
    root_domain         = string
    managed_zone_name   = optional(string, null)
    create_managed_zone = optional(bool, true)
    ttl                 = optional(number, 300)
    platform_host       = optional(string, "platform")
    auth_host           = optional(string, "auth")
    registry_host       = optional(string, "registry")
    search_host         = optional(string, "search")
    analytics_host      = optional(string, "analytics")
  })
}

variable "buckets" {
  description = "Platform GCS buckets keyed by logical purpose."
  type = map(object({
    name                     = optional(string, null)
    storage_class            = optional(string, "STANDARD")
    versioning               = optional(bool, true)
    force_destroy            = optional(bool, false)
    retention_period_seconds = optional(number, null)
    labels                   = optional(map(string), {})
  }))
  default = {
    entity-intelligence = {}
    model-cache         = {}
  }
}

variable "workload_service_accounts" {
  description = "GCP service accounts, project roles and Kubernetes Workload Identity principals."
  type = map(object({
    account_id                = string
    display_name              = string
    project_roles             = optional(set(string), [])
    workload_identity_members = optional(set(string), [])
    bucket_roles              = optional(map(set(string)), {})
  }))
  default = {
    cert-manager = {
      account_id                = "cert-manager"
      display_name              = "cert-manager DNS solver"
      project_roles             = ["roles/dns.admin"]
      workload_identity_members = ["cert-manager/cert-manager"]
    }
    external-secrets = {
      account_id                = "external-secrets"
      display_name              = "External Secrets operator"
      project_roles             = ["roles/secretmanager.secretAccessor"]
      workload_identity_members = ["external-secrets/external-secrets"]
    }
    model-serving = {
      account_id                = "model-serving"
      display_name              = "Shared model serving"
      workload_identity_members = ["kserve-serving/model-cache-sa"]
      bucket_roles = {
        model-cache = ["roles/storage.objectViewer"]
      }
    }
    entity-intelligence = {
      account_id                = "entity-intelligence"
      display_name              = "Entity Intelligence workloads"
      workload_identity_members = ["entity-intelligence/entity-intelligence"]
      bucket_roles = {
        entity-intelligence = ["roles/storage.objectAdmin"]
      }
    }
  }
}

variable "secret_ids" {
  description = "Secret Manager containers required by the greenfield Helmfile. Secret versions are populated separately."
  type        = set(string)
  default = [
    "platform-container-registry-dockerconfigjson",
    "platform-huggingface-token",
    "platform-keycloak-admin-password",
    "platform-keycloak-admin-username",
    "platform-keycloak-bootstrap-password",
    "platform-keycloak-confidential-client-secret",
    "platform-keycloak-database-password",
    "platform-keycloak-database-url",
    "platform-keycloak-database-username",
    "platform-opensearch-admin-cookie",
    "platform-opensearch-admin-password",
    "platform-opensearch-admin-password-hash",
    "platform-opensearch-admin-username",
    "platform-superset-admin-email",
    "platform-superset-admin-password",
    "platform-superset-admin-username",
    "platform-superset-database-host",
    "platform-superset-database-name",
    "platform-superset-database-password",
    "platform-superset-database-port",
    "platform-superset-database-uri",
    "platform-superset-database-username",
    "platform-superset-secret-key",
    "platform-valkey-password",
    "entity-intelligence-database-password",
    "entity-intelligence-database-username",
    "openworkflow-kafka-streams-database-password",
    "openworkflow-kafka-streams-database-username",
  ]
}

variable "additional_secret_ids" {
  description = "Environment- or tenant-specific Secret Manager containers, such as OKS adapter client secrets."
  type        = set(string)
  default     = []
}
