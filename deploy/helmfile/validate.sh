#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-gcp-greenfield-example}"
OUTPUT="$(mktemp)"
trap 'rm -f "${OUTPUT}"' EXIT

for command in helm helmfile yq; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

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
if grep -q '\$2y\$12\$MaV7oeL162lpD1QHjf/FyukinWOtOef7maOZCitvvnT3hxpUaW9SS' "${OUTPUT}"; then
  echo "Static legacy OpenSearch credential hash leaked into the render." >&2
  exit 1
fi

echo "ForwardMeasure shared-platform manifests validated for ${ENVIRONMENT}."
