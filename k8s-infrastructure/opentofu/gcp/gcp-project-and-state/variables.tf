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
  type        = string
  description = "Existing GCP project that will own the OpenTofu state bucket."
}

variable "create_project" {
  type        = bool
  description = "Create and attach the GCP project instead of using an existing billed project."
  default     = false
}

variable "project_name" {
  type        = string
  description = "Display name used when create_project is true."
  default     = "ForwardMeasure Platform"
}

variable "billing_account" {
  type        = string
  description = "Billing account attached to a newly created project."
  default     = null
  nullable    = true
}

variable "folder_id" {
  type        = string
  description = "Optional GCP folder for a newly created project."
  default     = null
  nullable    = true
}

variable "organization_id" {
  type        = string
  description = "Optional GCP organization for a newly created project when folder_id is not used."
  default     = null
  nullable    = true
}

variable "region" {
  type        = string
  description = "Location of the state bucket."
  default     = "us-central1"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique GCS bucket name used by the primary GCP stack."
}

variable "labels" {
  type        = map(string)
  description = "Additional bucket labels."
  default     = {}
}
