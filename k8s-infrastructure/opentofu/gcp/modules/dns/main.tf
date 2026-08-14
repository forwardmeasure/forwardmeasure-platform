resource "google_dns_managed_zone" "platform" {
  count = var.create_managed_zone ? 1 : 0

  project     = var.project_id
  name        = var.managed_zone_name
  dns_name    = "${trimsuffix(var.root_domain, ".")}."
  description = "Public DNS zone for the ForwardMeasure Kubernetes platform"
  labels      = var.labels
}

data "google_dns_managed_zone" "platform" {
  count = var.create_managed_zone ? 0 : 1

  project = var.project_id
  name    = var.managed_zone_name
}

locals {
  zone_name    = var.create_managed_zone ? google_dns_managed_zone.platform[0].name : data.google_dns_managed_zone.platform[0].name
  name_servers = var.create_managed_zone ? google_dns_managed_zone.platform[0].name_servers : data.google_dns_managed_zone.platform[0].name_servers
}

resource "google_dns_record_set" "records" {
  for_each = var.records

  project      = var.project_id
  managed_zone = local.zone_name
  name         = each.value
  type         = "A"
  ttl          = var.ttl
  rrdatas      = [var.address]
}
