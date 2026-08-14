variable "project_id" { type = string }
variable "region" { type = string }
variable "cluster_name" { type = string }
variable "network_id" { type = string }
variable "subnetwork_id" { type = string }
variable "pods_range_name" { type = string }
variable "services_range_name" { type = string }
variable "node_service_account_email" { type = string }
variable "deletion_protection" { type = bool }
variable "release_channel" { type = string }
variable "min_master_version" {
  type     = string
  nullable = true
}
variable "private_nodes" { type = bool }
variable "private_endpoint" { type = bool }
variable "master_ipv4_cidr" { type = string }
variable "master_authorized_networks" { type = map(string) }
variable "enable_dns_cache" { type = bool }
variable "enable_gcs_fuse_csi" { type = bool }
variable "enable_binary_authorization" { type = bool }
variable "security_posture_mode" { type = string }
variable "vulnerability_mode" { type = string }
variable "maintenance_start_time" { type = string }
variable "maintenance_end_time" { type = string }
variable "maintenance_recurrence" { type = string }
variable "resource_labels" { type = map(string) }

variable "cpu_node_pools" {
  type = map(object({
    machine_type         = string
    node_locations       = optional(list(string), [])
    image_type           = optional(string, "COS_CONTAINERD")
    disk_type            = optional(string, "pd-balanced")
    disk_size_gb         = optional(number, 200)
    initial_node_count   = optional(number, 1)
    total_min_node_count = optional(number, 1)
    total_max_node_count = optional(number, 6)
    spot                 = optional(bool, false)
    local_ssd_count      = optional(number, 0)
    tags                 = optional(list(string), [])
    labels               = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
}

variable "gpu_node_pools" {
  type = map(object({
    machine_type         = string
    accelerator_type     = string
    accelerator_count    = number
    gpu_driver_version   = optional(string, "DEFAULT")
    node_locations       = optional(list(string), [])
    image_type           = optional(string, "COS_CONTAINERD")
    disk_type            = optional(string, "pd-balanced")
    disk_size_gb         = optional(number, 500)
    initial_node_count   = optional(number, 0)
    total_min_node_count = optional(number, 0)
    total_max_node_count = optional(number, 3)
    spot                 = optional(bool, true)
    local_ssd_count      = optional(number, 0)
    tags                 = optional(list(string), [])
    labels               = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
}
