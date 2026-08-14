output "cluster" {
  description = "GKE cluster coordinates."
  value = {
    name     = module.gke.name
    location = module.gke.location
    endpoint = nonsensitive(module.gke.endpoint)
  }
}

output "gateway_ip_address" {
  description = "Regional external address reserved for the Gateway."
  value       = module.network.gateway_ip_address
}

output "cloudsql" {
  description = "Cloud SQL connection metadata."
  value = {
    instance_name   = module.cloudsql.name
    connection_name = module.cloudsql.connection_name
    private_ip      = module.cloudsql.private_ip_address
    databases       = module.cloudsql.database_names
    users           = module.cloudsql.user_names
  }
}

output "dns" {
  description = "Managed-zone and platform DNS records."
  value = {
    managed_zone_name = module.dns.managed_zone_name
    name_servers      = module.dns.name_servers
    records           = module.dns.record_names
  }
}

output "buckets" {
  description = "Logical bucket key to actual GCS bucket name."
  value       = module.storage.bucket_names
}

output "service_account_emails" {
  description = "Platform service-account email addresses."
  value = merge(
    module.iam.workload_service_account_emails,
    { gke_nodes = module.iam.node_service_account_email }
  )
}

output "secret_ids" {
  description = "Secret Manager containers created for the platform."
  value       = module.secret_manager.secret_ids
}

locals {
  helmfile_environment = {
    platform = {
      domain      = local.root_domain
      environment = var.environment
    }
    gcp = {
      projectId                          = var.project_id
      region                             = var.region
      clusterName                        = module.gke.name
      dnsServiceAccountEmail             = module.iam.workload_service_account_emails["cert-manager"]
      externalSecretsServiceAccountEmail = module.iam.workload_service_account_emails["external-secrets"]
      modelServiceAccountEmail           = module.iam.workload_service_account_emails["model-serving"]
      modelCacheBucket                   = module.storage.bucket_names["model-cache"]
    }
    gateway = {
      loadBalancerIp = module.network.gateway_ip_address
      rootHost       = trimsuffix(local.dns_records.platform, ".")
      tenantHost     = trimsuffix(local.dns_records.tenants, ".")
      authHost       = trimsuffix(local.dns_records.auth, ".")
      registryHost   = trimsuffix(local.dns_records.registry, ".")
      searchHost     = trimsuffix(local.dns_records.search, ".")
      analyticsHost  = trimsuffix(local.dns_records.analytics, ".")
    }
  }
}

output "helmfile_environment" {
  description = "Non-secret values that overlay deploy/helmfile/environments/base.yaml."
  value       = local.helmfile_environment
}

output "helmfile_environment_yaml" {
  description = "YAML form of helmfile_environment. Redirect this output to a private environment overlay."
  value       = yamlencode(local.helmfile_environment)
}
