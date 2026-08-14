mock_provider "google" {}

mock_provider "kubernetes" {}

run "complete_greenfield_contract" {
  command = plan

  override_resource {
    target = module.network.google_compute_network.platform
    values = {
      id = "projects/forwardmeasure-test/global/networks/forwardmeasure-test"
    }
  }

  override_resource {
    target = module.iam.google_service_account.workloads
    values = {
      name  = "projects/forwardmeasure-test/serviceAccounts/platform-test@forwardmeasure-test.iam.gserviceaccount.com"
      email = "platform-test@forwardmeasure-test.iam.gserviceaccount.com"
    }
  }

  variables {
    project_id  = "forwardmeasure-test"
    environment = "test"

    dns = {
      root_domain = "platform.test"
    }

    gke = {
      master_authorized_networks = {
        test-runner = "192.0.2.10/32"
      }
    }

    cloudsql_user_passwords = {
      entity_intelligence = "test-only-entity-intelligence-password"
      keycloak            = "test-only-keycloak-password"
      openworkflow        = "test-only-openworkflow-password"
      superset            = "test-only-superset-password"
    }

    cloudsql_extensions = {
      superset = {
        username   = "superset"
        extensions = ["uuid-ossp"]
      }
    }
  }

  assert {
    condition     = output.helmfile_environment.gcp.projectId == "forwardmeasure-test"
    error_message = "The Helmfile handoff must contain the configured GCP project."
  }

  assert {
    condition     = output.helmfile_environment.gateway.tenantHost == "*.platform.test"
    error_message = "The Helmfile handoff must contain the tenant wildcard."
  }
}
