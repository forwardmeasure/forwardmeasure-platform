#!/usr/bin/env bash
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
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# One level above this repo's own root - where install-platform.sh already
# finds forwardmeasure-openworkflow as a sibling checkout. Needed below to
# run its migrations stage before Keycloak/Superset exist.
WORKSPACE_DIR="$(cd -- "${SCRIPT_DIR}/../../.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment> [stage]}"
REQUESTED_STAGE="${2:-}"

for command in kubectl helm helmfile yq; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

"${SCRIPT_DIR}/validate.sh" "${ENVIRONMENT}"
"${SCRIPT_DIR}/scripts/preflight.sh" "${ENVIRONMENT}"

kubectl apply -f "${SCRIPT_DIR}/manifests/namespaces.yaml"
GATEWAY_API_VERSION="$(${SCRIPT_DIR}/scripts/environment-value.sh "${ENVIRONMENT}" gatewayApi.version)"
kubectl apply -f "https://github.com/kubernetes-sigs/gateway-api/releases/download/${GATEWAY_API_VERSION}/standard-install.yaml"

apply_stage() {
  local stage="$1"
  echo "Applying ForwardMeasure platform stage: ${stage}"
  helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" \
    --environment "${ENVIRONMENT}" --selector "stage=${stage}" apply
}

if [[ -n "${REQUESTED_STAGE}" ]]; then
  apply_stage "${REQUESTED_STAGE}"
  exit 0
fi

apply_stage foundation

kubectl wait --for=condition=Established --timeout=180s \
  crd/certificates.cert-manager.io \
  crd/externalsecrets.external-secrets.io \
  crd/clustersecretstores.external-secrets.io

apply_stage configuration

SECRET_STORE="$(${SCRIPT_DIR}/scripts/environment-value.sh "${ENVIRONMENT}" secretStore.name)"
kubectl wait --for=condition=Ready --timeout=180s "clustersecretstore/${SECRET_STORE}"

OPENWORKFLOW_NAMESPACE="$(${SCRIPT_DIR}/scripts/environment-value.sh "${ENVIRONMENT}" namespaces.openworkflow)"

for namespace in keycloak apicurio-registry opensearch-cluster kserve-serving docling-serve valkey superset "${OPENWORKFLOW_NAMESPACE}"; do
  if kubectl --namespace "${namespace}" get externalsecret >/dev/null 2>&1; then
    kubectl --namespace "${namespace}" wait --for=condition=Ready --timeout=300s externalsecret --all
  fi
done

# Keycloak's/Superset's Postgres roles don't exist until OpenWorkflow's
# migrations Job creates them (OpenWorkflowTenantMigrator.ensureRuntimeRole) -
# Terraform only ever generates their passwords in Secret Manager, never the
# role. Must run before apply_stage identity/analytics below: otherwise their
# own helm --wait blocks forever on a pod that can't start without a role
# nothing has created yet. Single stage, not the full openworkflow install.sh -
# stage=migrations now also carries openworkflow-identity (see
# forwardmeasure-openworkflow's helmfiles/foundation.yaml.gotmpl), the
# ServiceAccount its Cloud SQL Auth Proxy sidecar needs - deliberately not
# stage=foundation, whose other release has its own Keycloak-readiness hook
# and would reintroduce this same deadlock. install-platform.sh's own final,
# unconditional full install.sh call re-runs this stage again later - both
# operations it performs are idempotent, so that's safe, not wasted work.
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/install.sh" "${ENVIRONMENT}" migrations

apply_stage identity
apply_stage messaging
apply_stage search
apply_stage ml-serving
apply_stage analytics
apply_stage dashboard

"${SCRIPT_DIR}/scripts/readiness.sh" "${ENVIRONMENT}"
