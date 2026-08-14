provider "google" {
  project = var.project_id
  region  = var.region

  default_labels = local.labels
}

provider "kubernetes" {
  host                   = "https://${module.gke.endpoint}"
  cluster_ca_certificate = base64decode(module.gke.cluster_ca_certificate)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "gke-gcloud-auth-plugin"
  }
}
