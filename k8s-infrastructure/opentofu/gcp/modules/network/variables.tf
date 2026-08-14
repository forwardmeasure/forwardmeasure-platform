variable "project_id" { type = string }
variable "region" { type = string }
variable "name" { type = string }
variable "subnet_cidr" { type = string }
variable "pods_cidr" { type = string }
variable "services_cidr" { type = string }
variable "private_services_prefix" { type = number }
variable "nat_min_ports_per_vm" { type = number }
variable "nat_enable_dynamic_ports" { type = bool }
variable "nat_log_filter" { type = string }
variable "gateway_ip_address" {
  type     = string
  nullable = true
}
