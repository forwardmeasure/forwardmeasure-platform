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
output "name" { value = google_container_cluster.platform.name }
output "location" { value = google_container_cluster.platform.location }
output "endpoint" {
  value     = google_container_cluster.platform.endpoint
  sensitive = true
}
output "cluster_ca_certificate" {
  value     = google_container_cluster.platform.master_auth[0].cluster_ca_certificate
  sensitive = true
}
output "cpu_node_pool_names" { value = keys(google_container_node_pool.cpu) }
output "gpu_node_pool_names" { value = keys(google_container_node_pool.gpu) }
