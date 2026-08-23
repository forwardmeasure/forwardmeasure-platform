#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
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
