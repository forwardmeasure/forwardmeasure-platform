provider "google" {
  region = var.region
}

resource "google_project" "platform" {
  count = var.create_project ? 1 : 0

  project_id      = var.project_id
  name            = var.project_name
  billing_account = var.billing_account
  folder_id       = var.folder_id
  org_id          = var.folder_id == null ? var.organization_id : null
  deletion_policy = "PREVENT"

  lifecycle {
    precondition {
      condition     = var.billing_account != null && trimspace(var.billing_account) != ""
      error_message = "billing_account is required when create_project is true."
    }
  }
}

resource "google_project_service" "storage" {
  project            = var.project_id
  service            = "storage.googleapis.com"
  disable_on_destroy = false

  depends_on = [google_project.platform]
}

resource "google_storage_bucket" "state" {
  project                     = var.project_id
  name                        = var.state_bucket_name
  location                    = var.region
  storage_class               = "STANDARD"
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = false

  versioning {
    enabled = true
  }

  labels = merge({
    "managed-by" = "opentofu"
    "purpose"    = "opentofu-state"
  }, var.labels)

  depends_on = [google_project_service.storage]
}
