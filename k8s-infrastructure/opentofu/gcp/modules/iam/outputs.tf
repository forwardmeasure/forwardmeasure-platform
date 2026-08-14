output "node_service_account_email" {
  value = google_service_account.gke_nodes.email
}

output "workload_service_account_emails" {
  value = { for key, service_account in google_service_account.workloads : key => service_account.email }
}

output "workload_service_account_names" {
  value = { for key, service_account in google_service_account.workloads : key => service_account.name }
}
