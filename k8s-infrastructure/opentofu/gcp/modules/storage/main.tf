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
locals {
  buckets = {
    for key, config in var.buckets : key => merge(config, {
      resolved_name = coalesce(config.name, substr("${var.prefix}-${key}-${substr(sha256(var.project_id), 0, 8)}", 0, 63))
    })
  }
}

resource "google_storage_bucket" "buckets" {
  for_each = local.buckets

  project                     = var.project_id
  name                        = each.value.resolved_name
  location                    = var.region
  storage_class               = each.value.storage_class
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = each.value.force_destroy

  versioning {
    enabled = each.value.versioning
  }

  dynamic "retention_policy" {
    for_each = each.value.retention_period_seconds == null ? [] : [each.value.retention_period_seconds]
    content {
      retention_period = retention_policy.value
      is_locked        = false
    }
  }

  labels = merge(var.labels, each.value.labels, { purpose = each.key })
}
