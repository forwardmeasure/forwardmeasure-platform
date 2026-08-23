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
ENVIRONMENT="${1:-gcp-greenfield-example}"
OUTPUT="$(mktemp)"
trap 'rm -f "${OUTPUT}"' EXIT

for command in helm helmfile yq jq rg; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

"${SCRIPT_DIR}/scripts/environment-files.sh" "${ENVIRONMENT}" >/dev/null

yq -e '.chartVersions | type == "!!map" and length > 0' \
  "${SCRIPT_DIR}/environments/chart-versions.yaml" >/dev/null
yq -e 'has("versions") | not' "${SCRIPT_DIR}/environments/base.yaml.gotmpl" >/dev/null
yq -e 'has("chartVersions") | not' "${SCRIPT_DIR}/environments/base.yaml.gotmpl" >/dev/null
yq -e 'has("imageVersions") | not' "${SCRIPT_DIR}/environments/base.yaml.gotmpl" >/dev/null
yq -o=json '.imageVersions' "${SCRIPT_DIR}/environments/image-versions.yaml" \
  | jq -e '
      [.. | objects | select(has("repository"))]
      | length > 0
        and all(
          (.repository | type == "string" and length > 0)
          and (((.tag // "") | length > 0) or ((.digest // "") | test("^sha256:[0-9a-f]{64}$")))
          and (((.digest // "") == "") or ((.digest // "") | test("^sha256:[0-9a-f]{64}$")))
          and (((.tag // "") != "latest") or ((.digest // "") | test("^sha256:[0-9a-f]{64}$")))
        )' >/dev/null
yq -o=json '.chartSources' "${SCRIPT_DIR}/environments/base.yaml.gotmpl" \
  | jq -e 'all(.[]; (.mode == "local" or .mode == "repository") and (.chart | length > 0))' >/dev/null

while IFS= read -r chart_key; do
  rg -q "\\.Values\\.chartVersions\\.${chart_key}([^A-Za-z0-9_]|$)" \
    "${SCRIPT_DIR}/helmfiles" || {
      echo "Unused platform chart version: ${chart_key}" >&2
      exit 1
    }
done < <(yq -r '.chartVersions | keys | .[]' "${SCRIPT_DIR}/environments/chart-versions.yaml")

if rg -n 'version:[[:space:]]+\{\{[[:space:]]+\.Values\.' "${SCRIPT_DIR}/helmfiles" \
    | grep -v '\.Values\.chartVersions\.'; then
  echo "A platform Helm release version is not sourced from chartVersions." >&2
  exit 1
fi

yq eval-all --exit-status \
  '[.] | map(select(.apiVersion != "v1" or .kind != "Namespace" or .metadata.name == null)) | length == 0' \
  "${SCRIPT_DIR}/manifests/namespaces.yaml" >/dev/null
helm lint "${SCRIPT_DIR}/charts/platform-bootstrap"
helm lint "${SCRIPT_DIR}/charts/platform-dashboard"
helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" \
  --environment "${ENVIRONMENT}" template >"${OUTPUT}"

if grep -qi 'open-webui' "${OUTPUT}"; then
  echo "Open WebUI unexpectedly appears in the mandatory platform render." >&2
  exit 1
fi

# Gateway/ClusterIssuer/ExternalSecret and the cloud-only releases below
# (platform-certificates, platform-routing, platform-secrets, opensearch-
# cluster, model-cache) only render once a cloud is selected - see the
# installed: {{ if $cloud }} gating added across helmfiles/*.yaml.gotmpl.
# "base" is the one registered environment name with no cloud prefix, kept
# deliberately reduced (a structural/chart-version sanity tier, same role
# data-fabric's own "base" environment plays), so these content assertions
# only apply to real, cloud-selected environments.
if [[ "${ENVIRONMENT}" != "base" ]]; then
  for kind in Gateway ClusterIssuer ExternalSecret StatefulSet Deployment Kafka KafkaTopic ApicurioRegistry3 KnativeServing ClusterServingRuntime InferenceService; do
    grep -q "kind: ${kind}" "${OUTPUT}" || {
      echo "Expected Kubernetes kind is absent from render: ${kind}" >&2
      exit 1
    }
  done

  grep -q 'name: opensearch-cluster-opensearch-dashboards' "${OUTPUT}"
  grep -q 'name: kserve-controller-manager' "${OUTPUT}"
  grep -q 'name: ner-gliner-medium-v2-1' "${OUTPUT}"
  grep -q 'forwardmeasure.com/ner-client: "true"' "${OUTPUT}"
  grep -q 'kubernetes.io/metadata.name: knative-serving' "${OUTPUT}"
  grep -q 'name: platform-registry-app-service' "${OUTPUT}"
  grep -q 'name: forwardmeasure-platform-dashboard' "${OUTPUT}"
  grep -q 'value: /platform' "${OUTPUT}"
  grep -q 'forwardmeasure/forwardmeasure-keycloak' "${OUTPUT}"
  grep -q 'name: REALM_LOGIN_THEME' "${OUTPUT}"
  grep -q 'value: "forwardmeasure"' "${OUTPUT}"
  grep -Eq 'value: ["]?/apis/registry["]?' "${OUTPUT}"
  grep -q 'SQLALCHEMY_DATABASE_URI' "${OUTPUT}"
  grep -q 'secretKey: username' "${OUTPUT}"
  grep -q 'secretKey: password' "${OUTPUT}"
  grep -q 'secretKey: passwordHash' "${OUTPUT}"
fi
if grep -q '\$2y\$12\$MaV7oeL162lpD1QHjf/FyukinWOtOef7maOZCitvvnT3hxpUaW9SS' "${OUTPUT}"; then
  echo "Static legacy OpenSearch credential hash leaked into the render." >&2
  exit 1
fi

echo "ForwardMeasure shared-platform manifests validated for ${ENVIRONMENT}."
