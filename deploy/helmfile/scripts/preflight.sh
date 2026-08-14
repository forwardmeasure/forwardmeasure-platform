#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"
HELMFILE_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

for command in kubectl helm helmfile yq jq gcloud; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

ENVIRONMENT_FILES=("${HELMFILE_DIR}/environments/base.yaml")
if [[ "${ENVIRONMENT}" == "gcp-greenfield-example" ]]; then
  ENVIRONMENT_FILES+=("${HELMFILE_DIR}/environments/gcp-greenfield.example.yaml")
elif [[ "${ENVIRONMENT}" != "base" ]]; then
  ENVIRONMENT_FILES+=("${HELMFILE_DIR}/environments/${ENVIRONMENT}.yaml")
fi

MERGED_JSON="$(yq ea -o=json \
  '. as $item ireduce ({}; . * $item)' "${ENVIRONMENT_FILES[@]}")"
printf '%s' "${MERGED_JSON}" | jq -e '
  (.tenants | type == "array" and length > 0)
  and (.identity.serviceClients | type == "array")
  and ((.tenants | map(.did) | sort)
       == (.identity.serviceClients | map(.claims.tenant_did) | sort))
  and (.identity.serviceClients | all(. as $client |
    (.clientId | type == "string" and length > 0)
    and (.secretKey | test("^[A-Z0-9_]+$"))
    and (.secretRemoteKey | type == "string" and length > 0)
    and (.claims.actor_type == "SERVICE")
    and ($client.claims.actor_did
      | startswith($client.claims.tenant_did + ":actors:"))))
' >/dev/null || {
  echo "Every tenant must have exactly one tenant-bound SERVICE identity client." >&2
  exit 1
}

printf '%s' "${MERGED_JSON}" | jq -e '
  .platformDashboard.image.digest
  | test("^sha256:[0-9a-f]{64}$")
    and . != "sha256:0000000000000000000000000000000000000000000000000000000000000000"
' >/dev/null || {
  echo "The production Platform Dashboard image must have a non-placeholder sha256 digest." >&2
  exit 1
}

value() {
  "${SCRIPT_DIR}/environment-value.sh" "${ENVIRONMENT}" "$1"
}

PROJECT_ID="$(value gcp.projectId)"
CLUSTER_NAME="$(value gcp.clusterName)"
MODEL_BUCKET="$(value gcp.modelCacheBucket)"
GATEWAY_IP="$(value gateway.loadBalancerIp)"
BOOTSTRAP_TENANT_HOST="$(value bootstrapIdentity.tenantHost)"
BOOTSTRAP_TENANT_DID="$(value bootstrapIdentity.tenantDid)"
BOOTSTRAP_ACTOR_DID="$(value bootstrapIdentity.actorDid)"
BOOTSTRAP_ACTOR_TYPE="$(value bootstrapIdentity.actorType)"

for required in "${PROJECT_ID}" "${CLUSTER_NAME}" "${MODEL_BUCKET}" "${GATEWAY_IP}" \
  "${BOOTSTRAP_TENANT_HOST}" "${BOOTSTRAP_TENANT_DID}" "${BOOTSTRAP_ACTOR_DID}" "${BOOTSTRAP_ACTOR_TYPE}"; do
  case "${required}" in
    ""|*example*|replace-with-*|192.0.2.*|198.51.100.*|203.0.113.*)
      echo "Environment ${ENVIRONMENT} still contains a placeholder value: ${required}" >&2
      exit 1
      ;;
  esac
done

[[ "${BOOTSTRAP_TENANT_HOST}" != *"*"* && "${BOOTSTRAP_TENANT_HOST}" == *.* ]] || {
  echo "bootstrapIdentity.tenantHost must be one concrete DNS hostname: ${BOOTSTRAP_TENANT_HOST}" >&2
  exit 1
}

[[ "${BOOTSTRAP_TENANT_DID}" == did:* ]] || {
  echo "bootstrapIdentity.tenantDid must be a DID: ${BOOTSTRAP_TENANT_DID}" >&2
  exit 1
}
[[ "${BOOTSTRAP_ACTOR_DID}" == did:* ]] || {
  echo "bootstrapIdentity.actorDid must be a DID: ${BOOTSTRAP_ACTOR_DID}" >&2
  exit 1
}
[[ "${BOOTSTRAP_ACTOR_TYPE}" =~ ^(HUMAN|SERVICE|SYSTEM)$ ]] || {
  echo "bootstrapIdentity.actorType must be HUMAN, SERVICE, or SYSTEM: ${BOOTSTRAP_ACTOR_TYPE}" >&2
  exit 1
}

ACTIVE_PROJECT="$(gcloud config get-value project 2>/dev/null)"
if [[ "${ACTIVE_PROJECT}" != "${PROJECT_ID}" ]]; then
  echo "Active gcloud project ${ACTIVE_PROJECT} does not match ${PROJECT_ID}." >&2
  exit 1
fi

while IFS= read -r secret_name; do
  gcloud secrets describe "${secret_name}" --project "${PROJECT_ID}" >/dev/null
done < <(printf '%s' "${MERGED_JSON}" | jq -r '.secretStore.remoteKeys[]')

while IFS= read -r secret_name; do
  gcloud secrets describe "${secret_name}" --project "${PROJECT_ID}" >/dev/null
done < <(printf '%s' "${MERGED_JSON}" \
  | jq -r '.identity.serviceClients[].secretRemoteKey')

gcloud storage buckets describe "gs://${MODEL_BUCKET}" --project "${PROJECT_ID}" >/dev/null
kubectl cluster-info >/dev/null

echo "Greenfield infrastructure prerequisites validated for ${ENVIRONMENT}."
