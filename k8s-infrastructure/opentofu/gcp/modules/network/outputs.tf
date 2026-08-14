output "network_id" { value = google_compute_network.platform.id }
output "network_name" { value = google_compute_network.platform.name }
output "subnetwork_id" { value = google_compute_subnetwork.platform.id }
output "pods_range_name" { value = local.pods_range_name }
output "services_range_name" { value = local.services_range_name }
output "gateway_ip_address" { value = google_compute_address.gateway.address }
output "private_services_connection" { value = google_service_networking_connection.private_services.peering }
