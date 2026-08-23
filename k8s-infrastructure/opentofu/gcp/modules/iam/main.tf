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
resource "google_service_account" "gke_nodes" {
  project      = var.project_id
  account_id   = "${substr(var.cluster_name, 0, 18)}-${substr(sha256(var.cluster_name), 0, 5)}-nodes"
  display_name = "${var.cluster_name} GKE nodes"
  description  = "Node identity for the ${var.cluster_name} GKE cluster"
}

resource "google_project_iam_member" "gke_node_roles" {
  for_each = var.node_service_account_roles

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.gke_nodes.email}"
}

resource "google_service_account" "workloads" {
  for_each = var.workload_service_accounts

  project      = var.project_id
  account_id   = each.value.account_id
  display_name = each.value.display_name
  description  = "Workload Identity account managed by the ForwardMeasure platform"
}

locals {
  project_role_bindings = merge([
    for service_account_key, config in var.workload_service_accounts : {
      for role in config.project_roles : "${service_account_key}:${role}" => {
        service_account_key = service_account_key
        role                = role
      }
    }
  ]...)

  workload_identity_bindings = merge([
    for service_account_key, config in var.workload_service_accounts : {
      for member in config.workload_identity_members : "${service_account_key}:${member}" => {
        service_account_key = service_account_key
        member              = member
      }
    }
  ]...)

  bucket_role_bindings = merge(flatten([
    for service_account_key, config in var.workload_service_accounts : [
      for bucket_key, roles in config.bucket_roles : {
        for role in roles : "${service_account_key}:${bucket_key}:${role}" => {
          service_account_key = service_account_key
          bucket_key          = bucket_key
          role                = role
        }
      }
    ]
  ])...)
}

resource "google_project_iam_member" "workload_project_roles" {
  for_each = local.project_role_bindings

  project = var.project_id
  role    = each.value.role
  member  = "serviceAccount:${google_service_account.workloads[each.value.service_account_key].email}"
}

resource "google_service_account_iam_member" "workload_identity" {
  for_each = local.workload_identity_bindings

  service_account_id = google_service_account.workloads[each.value.service_account_key].name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.workload_pool}[${each.value.member}]"
}

resource "google_storage_bucket_iam_member" "workload_bucket_roles" {
  for_each = local.bucket_role_bindings

  bucket = var.bucket_names[each.value.bucket_key]
  role   = each.value.role
  member = "serviceAccount:${google_service_account.workloads[each.value.service_account_key].email}"

  lifecycle {
    precondition {
      condition     = contains(keys(var.bucket_names), each.value.bucket_key)
      error_message = "Every workload bucket_roles key must refer to a configured platform bucket."
    }
  }
}
