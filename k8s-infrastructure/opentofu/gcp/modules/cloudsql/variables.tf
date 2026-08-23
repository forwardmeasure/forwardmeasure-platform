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
variable "project_id" { type = string }
variable "region" { type = string }
variable "name" { type = string }
variable "network_id" { type = string }
variable "database_version" { type = string }
variable "tier" { type = string }
variable "availability_type" { type = string }
variable "disk_type" { type = string }
variable "disk_size_gb" { type = number }
variable "disk_autoresize_limit_gb" { type = number }
variable "deletion_protection" { type = bool }
variable "backup_start_time" { type = string }
variable "backup_retained_count" { type = number }
variable "transaction_log_retention_days" { type = number }
variable "maintenance_day" { type = number }
variable "maintenance_hour" { type = number }
variable "ssl_mode" { type = string }
variable "query_insights_enabled" { type = bool }
variable "labels" { type = map(string) }
variable "databases" { type = set(string) }
variable "users" {
  type = map(object({
    password_version = optional(number, 1)
    deletion_policy  = optional(string, "ABANDON")
  }))
}
variable "user_passwords" {
  type      = map(string)
  sensitive = true
  ephemeral = true
}
