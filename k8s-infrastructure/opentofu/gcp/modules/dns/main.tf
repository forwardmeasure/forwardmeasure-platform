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
resource "google_dns_managed_zone" "platform" {
  count = var.create_managed_zone ? 1 : 0

  project     = var.project_id
  name        = var.managed_zone_name
  dns_name    = "${trimsuffix(var.root_domain, ".")}."
  description = "Public DNS zone for the ForwardMeasure Kubernetes platform"
  labels      = var.labels
}

data "google_dns_managed_zone" "platform" {
  count = var.create_managed_zone ? 0 : 1

  project = var.project_id
  name    = var.managed_zone_name
}

locals {
  zone_name    = var.create_managed_zone ? google_dns_managed_zone.platform[0].name : data.google_dns_managed_zone.platform[0].name
  name_servers = var.create_managed_zone ? google_dns_managed_zone.platform[0].name_servers : data.google_dns_managed_zone.platform[0].name_servers
}

resource "google_dns_record_set" "records" {
  for_each = var.records

  project      = var.project_id
  managed_zone = local.zone_name
  name         = each.value
  type         = "A"
  ttl          = var.ttl
  rrdatas      = [var.address]
}
