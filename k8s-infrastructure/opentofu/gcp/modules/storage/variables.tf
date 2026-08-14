variable "project_id" { type = string }
variable "region" { type = string }
variable "prefix" { type = string }
variable "labels" { type = map(string) }
variable "buckets" {
  type = map(object({
    name                     = optional(string, null)
    storage_class            = optional(string, "STANDARD")
    versioning               = optional(bool, true)
    force_destroy            = optional(bool, false)
    retention_period_seconds = optional(number, null)
    labels                   = optional(map(string), {})
  }))
}
