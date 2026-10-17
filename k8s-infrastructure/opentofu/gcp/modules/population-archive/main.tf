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
# Archives have an independent bucket and key. No database setting is managed here.
locals {
  bucket_name = "${trim(substr("${var.project_id}-${var.cluster_id}-fei-archives", 0, 54), "-")}-${substr(sha256("${var.project_id}/${var.cluster_id}/fei-archives"), 0, 8)}"
}

resource "google_project_service" "kms" {
  project            = var.project_id
  service            = "cloudkms.googleapis.com"
  disable_on_destroy = false
}

data "google_storage_project_service_account" "archive" {
  project = var.project_id
}

resource "google_kms_key_ring" "archive" {
  project  = var.project_id
  name     = "fei-archives-${substr(sha256(var.cluster_id), 0, 16)}"
  location = var.region

  depends_on = [google_project_service.kms]
}

resource "google_kms_crypto_key" "archive" {
  name     = "population-archive"
  key_ring = google_kms_key_ring.archive.id
  purpose  = "ENCRYPT_DECRYPT"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_kms_crypto_key_iam_member" "storage_service_agent" {
  crypto_key_id = google_kms_crypto_key.archive.id
  role          = "roles/cloudkms.cryptoKeyEncrypterDecrypter"
  member        = "serviceAccount:${data.google_storage_project_service_account.archive.email_address}"
}

# Services and worker Jobs use the same Workload Identity for application envelope keys.
resource "google_kms_crypto_key_iam_member" "runtime_envelope" {
  crypto_key_id = google_kms_crypto_key.archive.id
  role          = "roles/cloudkms.cryptoKeyEncrypterDecrypter"
  member        = "serviceAccount:${var.runtime_service_account}"
}

resource "google_storage_bucket" "archive" {
  project                     = var.project_id
  name                        = local.bucket_name
  location                    = var.region
  storage_class               = var.storage_class
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = false

  versioning {
    enabled = true
  }

  # The outbox deletes exact versions. A provider soft-delete window would retain erased bytes.
  soft_delete_policy {
    retention_duration_seconds = 0
  }

  encryption {
    default_kms_key_name = google_kms_crypto_key.archive.id
  }

  labels = { purpose = "fei-population-archive", managed_by = "opentofu" }

  depends_on = [google_kms_crypto_key_iam_member.storage_service_agent]
}

resource "google_storage_bucket_iam_member" "runtime" {
  bucket = google_storage_bucket.archive.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${var.runtime_service_account}"
}
