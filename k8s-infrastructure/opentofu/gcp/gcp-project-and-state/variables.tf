variable "project_id" {
  type        = string
  description = "Existing GCP project that will own the OpenTofu state bucket."
}

variable "create_project" {
  type        = bool
  description = "Create and attach the GCP project instead of using an existing billed project."
  default     = false
}

variable "project_name" {
  type        = string
  description = "Display name used when create_project is true."
  default     = "ForwardMeasure Platform"
}

variable "billing_account" {
  type        = string
  description = "Billing account attached to a newly created project."
  default     = null
  nullable    = true
}

variable "folder_id" {
  type        = string
  description = "Optional GCP folder for a newly created project."
  default     = null
  nullable    = true
}

variable "organization_id" {
  type        = string
  description = "Optional GCP organization for a newly created project when folder_id is not used."
  default     = null
  nullable    = true
}

variable "region" {
  type        = string
  description = "Location of the state bucket."
  default     = "us-central1"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique GCS bucket name used by the primary GCP stack."
}

variable "labels" {
  type        = map(string)
  description = "Additional bucket labels."
  default     = {}
}
