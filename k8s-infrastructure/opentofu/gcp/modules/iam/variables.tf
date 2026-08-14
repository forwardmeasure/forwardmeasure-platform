variable "project_id" { type = string }
variable "cluster_name" { type = string }
variable "workload_pool" { type = string }

variable "node_service_account_roles" {
  type    = set(string)
  default = []
}

variable "bucket_names" {
  type    = map(string)
  default = {}
}

variable "workload_service_accounts" {
  type = map(object({
    account_id                = string
    display_name              = string
    project_roles             = optional(set(string), [])
    workload_identity_members = optional(set(string), [])
    bucket_roles              = optional(map(set(string)), {})
  }))
  default = {}
}
