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
resource "google_sql_database_instance" "platform" {
  project             = var.project_id
  region              = var.region
  name                = var.name
  database_version    = var.database_version
  deletion_protection = var.deletion_protection

  settings {
    tier                        = var.tier
    edition                     = "ENTERPRISE"
    availability_type           = var.availability_type
    disk_type                   = var.disk_type
    disk_size                   = var.disk_size_gb
    disk_autoresize             = true
    disk_autoresize_limit       = var.disk_autoresize_limit_gb
    deletion_protection_enabled = var.deletion_protection
    user_labels                 = var.labels

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = var.network_id
      enable_private_path_for_google_cloud_services = true
      ssl_mode                                      = var.ssl_mode
    }

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = true
      start_time                     = var.backup_start_time
      transaction_log_retention_days = var.transaction_log_retention_days

      backup_retention_settings {
        retained_backups = var.backup_retained_count
        retention_unit   = "COUNT"
      }
    }

    insights_config {
      query_insights_enabled  = var.query_insights_enabled
      query_plans_per_minute  = 5
      query_string_length     = 4500
      record_application_tags = true
      record_client_address   = false
    }

    maintenance_window {
      day          = var.maintenance_day
      hour         = var.maintenance_hour
      update_track = "stable"
    }
  }
}

resource "google_sql_database" "databases" {
  for_each = var.databases

  project   = var.project_id
  instance  = google_sql_database_instance.platform.name
  name      = each.value
  charset   = "UTF8"
  collation = "en_US.UTF8"
}

resource "google_sql_user" "users" {
  for_each = var.users

  project             = var.project_id
  instance            = google_sql_database_instance.platform.name
  name                = each.key
  type                = "BUILT_IN"
  password_wo         = lookup(var.user_passwords, each.key, null)
  password_wo_version = each.value.password_version
  deletion_policy     = each.value.deletion_policy

  lifecycle {
    precondition {
      condition     = contains(keys(var.user_passwords), each.key)
      error_message = "Every configured Cloud SQL user requires an ephemeral value in cloudsql_user_passwords."
    }
  }

  depends_on = [google_sql_database.databases]
}
