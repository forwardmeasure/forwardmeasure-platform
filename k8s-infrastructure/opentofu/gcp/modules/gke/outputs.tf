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
