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
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"
HELMFILE_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

for command in kubectl helm helmfile yq jq gcloud; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

# Rendered via `helmfile build`, not a raw multi-file yq merge of the
# environment files directly - several of those are .gotmpl files with
# unrendered Go-template expressions in scalar values (e.g. tenants[].did),
# and yq parsing that raw text misreads `{{ }}` as YAML flow-mapping
# syntax, silently corrupting the value instead of erroring. Confirmed the
# hard way: this previously failed the tenant/service-client check below
# with garbage instead of the real (correct) rendered DIDs.
#
# select(di==0): `helmfile build` emits one document per top-level
# helmfiles: entry (9 of them), each carrying its own renderedvalues: block
# - without narrowing to one, this concatenates 9 identical JSON objects,
# which happened to not break the jq -e checks below (jq's exit status
# reflects only the last object in the stream, and all 9 are identical) but
# is fragile and was never actually correct - environment-value.sh's use of
# the same unnarrowed pattern for scalar lookups silently corrupted every
# value it returned (confirmed the hard way via the actorType check
# failing on "HUMAN\n---\nHUMAN\n---\n..."). Narrowing here too rather than
# relying on the objects staying identical.
MERGED_JSON="$(helmfile --file "${HELMFILE_DIR}/helmfile.yaml.gotmpl" \
  --environment "${ENVIRONMENT}" build 2>/dev/null | yq -o=json 'select(di==0) | .renderedvalues')"
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
  .imageVersions.platformDashboard.digest
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

# Every gcloud call below already passes --project explicitly, so gcloud's
# own ambient default project (`gcloud config get-value project`) is
# irrelevant to them - checking it was only ever a proxy for "did you set
# up kubectl correctly," since `gcloud container clusters get-credentials`
# normally sets both at once. That's the wrong thing to check directly:
# confirmed the hard way - a real run had kubectl's context already
# correctly pointed at gke_genai-llm-393115_us-central1_openworkflow-prod
# while the ambient gcloud project was stale from unrelated work in the
# same shell, and this blocked an otherwise-safe deploy on that
# irrelevant mismatch. Checking kubectl's actual context instead - the
# thing every subsequent kubectl/helmfile-apply call in this script and in
# install.sh actually depends on. gke_<project>_<zone-or-region>_<cluster>
# is GKE's fixed context-naming convention, not something invented here.
ACTIVE_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
case "${ACTIVE_CONTEXT}" in
  gke_"${PROJECT_ID}"_*_"${CLUSTER_NAME}") ;;
  *)
    echo "kubectl's current context (${ACTIVE_CONTEXT:-none}) does not target cluster ${CLUSTER_NAME} in project ${PROJECT_ID}." >&2
    exit 1
    ;;
esac

# Meaningless while this cluster stays on the fake ClusterSecretStore
# provider (a deliberate, standing decision - not being reopened here):
# remoteKey/secretRemoteKey values are then just internal keys matched
# against that provider's own literal data list, not real GCP Secret
# Manager secret names, so there's nothing real for gcloud to find. Only
# runs once secretStore.name says a real provider is actually in use.
SECRET_STORE_NAME="$(value secretStore.name)"
if [[ "${SECRET_STORE_NAME}" != "fake-secret-store" ]]; then
  while IFS= read -r secret_name; do
    gcloud secrets describe "${secret_name}" --project "${PROJECT_ID}" >/dev/null
  done < <(printf '%s' "${MERGED_JSON}" | jq -r '.secretStore.remoteKeys[]')

  while IFS= read -r secret_name; do
    gcloud secrets describe "${secret_name}" --project "${PROJECT_ID}" >/dev/null
  done < <(printf '%s' "${MERGED_JSON}" \
    | jq -r '.identity.serviceClients[].secretRemoteKey')
fi

gcloud storage buckets describe "gs://${MODEL_BUCKET}" --project "${PROJECT_ID}" >/dev/null
kubectl cluster-info >/dev/null

echo "Greenfield infrastructure prerequisites validated for ${ENVIRONMENT}."
