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
variable "cluster_name" { type = string }
variable "workload_pool" { type = string }

variable "node_service_account_roles" {
  type    = set(string)
  default = []
}

variable "bucket_names" {
  type    = map(string)
  default = {}
}

variable "workload_service_accounts" {
  type = map(object({
    account_id                = string
    display_name              = string
    project_roles             = optional(set(string), [])
    workload_identity_members = optional(set(string), [])
    bucket_roles              = optional(map(set(string)), {})
  }))
  default = {}
}
