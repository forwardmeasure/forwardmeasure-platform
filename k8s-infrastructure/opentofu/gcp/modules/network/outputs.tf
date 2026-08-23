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
output "network_id" { value = google_compute_network.platform.id }
output "network_name" { value = google_compute_network.platform.name }
output "subnetwork_id" { value = google_compute_subnetwork.platform.id }
output "pods_range_name" { value = local.pods_range_name }
output "services_range_name" { value = local.services_range_name }
output "gateway_ip_address" { value = google_compute_address.gateway.address }
output "private_services_connection" { value = google_service_networking_connection.private_services.peering }
