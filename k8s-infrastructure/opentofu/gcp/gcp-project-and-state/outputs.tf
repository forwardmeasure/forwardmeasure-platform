output "state_bucket_name" {
  value = google_storage_bucket.state.name
}

output "project_id" {
  value = var.project_id
}

output "backend_configuration" {
  value = <<-EOT
    bucket = "${google_storage_bucket.state.name}"
    prefix = "forwardmeasure-platform/gcp"
  EOT
}
