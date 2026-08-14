variable "project_id" { type = string }
variable "region" { type = string }
variable "name" { type = string }
variable "network_id" { type = string }
variable "database_version" { type = string }
variable "tier" { type = string }
variable "availability_type" { type = string }
variable "disk_type" { type = string }
variable "disk_size_gb" { type = number }
variable "disk_autoresize_limit_gb" { type = number }
variable "deletion_protection" { type = bool }
variable "backup_start_time" { type = string }
variable "backup_retained_count" { type = number }
variable "transaction_log_retention_days" { type = number }
variable "maintenance_day" { type = number }
variable "maintenance_hour" { type = number }
variable "ssl_mode" { type = string }
variable "query_insights_enabled" { type = bool }
variable "labels" { type = map(string) }
variable "databases" { type = set(string) }
variable "users" {
  type = map(object({
    password_version = optional(number, 1)
    deletion_policy  = optional(string, "ABANDON")
  }))
}
variable "user_passwords" {
  type      = map(string)
  sensitive = true
  ephemeral = true
}
