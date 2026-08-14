resource "google_container_cluster" "platform" {
  project  = var.project_id
  location = var.region
  name     = var.cluster_name

  deletion_protection      = var.deletion_protection
  remove_default_node_pool = true
  initial_node_count       = 1
  min_master_version       = var.min_master_version

  network           = var.network_id
  subnetwork        = var.subnetwork_id
  networking_mode   = "VPC_NATIVE"
  datapath_provider = "ADVANCED_DATAPATH"

  ip_allocation_policy {
    cluster_secondary_range_name  = var.pods_range_name
    services_secondary_range_name = var.services_range_name
    stack_type                    = "IPV4"
  }

  private_cluster_config {
    enable_private_nodes    = var.private_nodes
    enable_private_endpoint = var.private_endpoint
    master_ipv4_cidr_block  = var.private_nodes ? var.master_ipv4_cidr : null

    master_global_access_config {
      enabled = false
    }
  }

  master_authorized_networks_config {
    gcp_public_cidrs_access_enabled = false

    dynamic "cidr_blocks" {
      for_each = var.master_authorized_networks
      content {
        display_name = cidr_blocks.key
        cidr_block   = cidr_blocks.value
      }
    }
  }

  master_auth {
    client_certificate_config {
      issue_client_certificate = false
    }
  }

  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  addons_config {
    dns_cache_config {
      enabled = var.enable_dns_cache
    }

    gce_persistent_disk_csi_driver_config {
      enabled = true
    }

    gcs_fuse_csi_driver_config {
      enabled = var.enable_gcs_fuse_csi
    }

    horizontal_pod_autoscaling {
      disabled = false
    }

    http_load_balancing {
      disabled = false
    }

    network_policy_config {
      disabled = false
    }
  }

  network_policy {
    enabled  = true
    provider = "PROVIDER_UNSPECIFIED"
  }

  enable_intranode_visibility = true
  enable_shielded_nodes       = true
  enable_legacy_abac          = false

  logging_service    = "logging.googleapis.com/kubernetes"
  monitoring_service = "monitoring.googleapis.com/kubernetes"

  logging_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS", "APISERVER", "SCHEDULER", "CONTROLLER_MANAGER"]
  }

  monitoring_config {
    enable_components = ["SYSTEM_COMPONENTS", "APISERVER", "SCHEDULER", "CONTROLLER_MANAGER", "STORAGE", "POD", "DEPLOYMENT", "STATEFULSET", "DAEMONSET", "HPA", "CADVISOR", "KUBELET"]

    managed_prometheus {
      enabled = true
    }
  }

  release_channel {
    channel = var.release_channel
  }

  maintenance_policy {
    recurring_window {
      start_time = var.maintenance_start_time
      end_time   = var.maintenance_end_time
      recurrence = var.maintenance_recurrence
    }
  }

  binary_authorization {
    evaluation_mode = var.enable_binary_authorization ? "PROJECT_SINGLETON_POLICY_ENFORCE" : "DISABLED"
  }

  security_posture_config {
    mode               = var.security_posture_mode
    vulnerability_mode = var.vulnerability_mode
  }

  resource_labels = var.resource_labels
}

resource "google_container_node_pool" "cpu" {
  for_each = var.cpu_node_pools

  project            = var.project_id
  location           = var.region
  name               = each.key
  cluster            = google_container_cluster.platform.id
  node_locations     = length(each.value.node_locations) == 0 ? null : each.value.node_locations
  initial_node_count = each.value.initial_node_count

  autoscaling {
    total_min_node_count = each.value.total_min_node_count
    total_max_node_count = each.value.total_max_node_count
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  upgrade_settings {
    strategy        = "SURGE"
    max_surge       = 1
    max_unavailable = 0
  }

  node_config {
    service_account = var.node_service_account_email
    machine_type    = each.value.machine_type
    image_type      = each.value.image_type
    disk_type       = each.value.disk_type
    disk_size_gb    = each.value.disk_size_gb
    spot            = each.value.spot
    tags            = each.value.tags
    labels          = each.value.labels

    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]

    metadata = {
      disable-legacy-endpoints = "true"
    }

    shielded_instance_config {
      enable_secure_boot          = true
      enable_integrity_monitoring = true
    }

    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    dynamic "taint" {
      for_each = each.value.taints
      content {
        key    = taint.value.key
        value  = taint.value.value
        effect = taint.value.effect
      }
    }

    dynamic "local_nvme_ssd_block_config" {
      for_each = each.value.local_ssd_count == 0 ? [] : [each.value.local_ssd_count]
      content {
        local_ssd_count = local_nvme_ssd_block_config.value
      }
    }
  }

}

resource "google_container_node_pool" "gpu" {
  for_each = var.gpu_node_pools

  project            = var.project_id
  location           = var.region
  name               = each.key
  cluster            = google_container_cluster.platform.id
  node_locations     = length(each.value.node_locations) == 0 ? null : each.value.node_locations
  initial_node_count = each.value.initial_node_count

  autoscaling {
    total_min_node_count = each.value.total_min_node_count
    total_max_node_count = each.value.total_max_node_count
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  upgrade_settings {
    strategy        = "SURGE"
    max_surge       = 1
    max_unavailable = 0
  }

  node_config {
    service_account = var.node_service_account_email
    machine_type    = each.value.machine_type
    image_type      = each.value.image_type
    disk_type       = each.value.disk_type
    disk_size_gb    = each.value.disk_size_gb
    spot            = each.value.spot
    tags            = each.value.tags
    labels          = each.value.labels

    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]

    metadata = {
      disable-legacy-endpoints = "true"
    }

    guest_accelerator {
      type  = each.value.accelerator_type
      count = each.value.accelerator_count

      gpu_driver_installation_config {
        gpu_driver_version = each.value.gpu_driver_version
      }
    }

    shielded_instance_config {
      enable_secure_boot          = true
      enable_integrity_monitoring = true
    }

    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    dynamic "taint" {
      for_each = each.value.taints
      content {
        key    = taint.value.key
        value  = taint.value.value
        effect = taint.value.effect
      }
    }

    dynamic "local_nvme_ssd_block_config" {
      for_each = each.value.local_ssd_count == 0 ? [] : [each.value.local_ssd_count]
      content {
        local_ssd_count = local_nvme_ssd_block_config.value
      }
    }
  }

}
