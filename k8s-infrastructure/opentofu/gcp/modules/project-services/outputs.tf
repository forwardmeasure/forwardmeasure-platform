output "enabled_services" {
  value = sort(keys(google_project_service.services))
}
