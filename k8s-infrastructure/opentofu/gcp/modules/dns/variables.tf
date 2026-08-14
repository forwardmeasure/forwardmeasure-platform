variable "project_id" { type = string }
variable "create_managed_zone" { type = bool }
variable "managed_zone_name" { type = string }
variable "root_domain" { type = string }
variable "records" { type = map(string) }
variable "address" { type = string }
variable "ttl" { type = number }
variable "labels" { type = map(string) }
