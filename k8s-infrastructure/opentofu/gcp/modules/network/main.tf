locals {
  pods_range_name     = "${var.name}-pods"
  services_range_name = "${var.name}-services"
}

resource "google_compute_network" "platform" {
  project                 = var.project_id
  name                    = "${var.name}-vpc"
  description             = "Network for the ${var.name} Kubernetes platform"
  auto_create_subnetworks = false
  routing_mode            = "REGIONAL"
}

resource "google_compute_subnetwork" "platform" {
  project                  = var.project_id
  region                   = var.region
  name                     = "${var.name}-subnet"
  network                  = google_compute_network.platform.id
  ip_cidr_range            = var.subnet_cidr
  private_ip_google_access = true
  stack_type               = "IPV4_ONLY"

  secondary_ip_range {
    range_name    = local.pods_range_name
    ip_cidr_range = var.pods_cidr
  }

  secondary_ip_range {
    range_name    = local.services_range_name
    ip_cidr_range = var.services_cidr
  }

  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 0.5
    metadata             = "INCLUDE_ALL_METADATA"
  }
}

resource "google_compute_global_address" "private_services" {
  project       = var.project_id
  name          = "${var.name}-private-services"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = var.private_services_prefix
  network       = google_compute_network.platform.id
}

resource "google_service_networking_connection" "private_services" {
  network                 = google_compute_network.platform.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_services.name]
  update_on_creation_fail = true
}

resource "google_compute_router" "platform" {
  project = var.project_id
  region  = var.region
  name    = "${var.name}-router"
  network = google_compute_network.platform.id
}

resource "google_compute_router_nat" "platform" {
  project                            = var.project_id
  region                             = var.region
  name                               = "${var.name}-nat"
  router                             = google_compute_router.platform.name
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"
  min_ports_per_vm                   = var.nat_min_ports_per_vm
  enable_dynamic_port_allocation     = var.nat_enable_dynamic_ports

  subnetwork {
    name                    = google_compute_subnetwork.platform.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }

  log_config {
    enable = true
    filter = var.nat_log_filter
  }
}

resource "google_compute_address" "gateway" {
  project      = var.project_id
  region       = var.region
  name         = "${var.name}-gateway"
  description  = "Regional external address for the Kubernetes Gateway"
  address_type = "EXTERNAL"
  address      = var.gateway_ip_address
  network_tier = "PREMIUM"
}
