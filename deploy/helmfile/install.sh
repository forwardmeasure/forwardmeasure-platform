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

for namespace in keycloak apicurio-registry opensearch-cluster kserve-serving docling-serve valkey superset; do
  if kubectl --namespace "${namespace}" get externalsecret >/dev/null 2>&1; then
    kubectl --namespace "${namespace}" wait --for=condition=Ready --timeout=300s externalsecret --all
  fi
done

apply_stage identity
apply_stage messaging
apply_stage search
apply_stage ml-serving
apply_stage analytics
apply_stage dashboard

"${SCRIPT_DIR}/scripts/readiness.sh" "${ENVIRONMENT}"
