#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <environment>\n' "$0" >&2
  exit 2
fi

ENVIRONMENT="$1"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GCP_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
BACKEND_FILE="${GCP_ROOT}/environments/${ENVIRONMENT}.backend.hcl"
VARIABLE_FILE="${GCP_ROOT}/environments/${ENVIRONMENT}.tfvars"
PLAN_DIRECTORY="${GCP_ROOT}/plans"
PLAN_FILE="${PLAN_DIRECTORY}/${ENVIRONMENT}.tfplan"

for required_file in "${BACKEND_FILE}" "${VARIABLE_FILE}"; do
  if [[ ! -f "${required_file}" ]]; then
    printf 'Required environment file does not exist: %s\n' "${required_file}" >&2
    exit 1
  fi
done

if [[ -z "${TF_VAR_cloudsql_user_passwords:-}" ]]; then
  printf 'TF_VAR_cloudsql_user_passwords must contain the ephemeral Cloud SQL password map.\n' >&2
  exit 1
fi

mkdir -p "${PLAN_DIRECTORY}"
tofu -chdir="${GCP_ROOT}" init -reconfigure -backend-config="${BACKEND_FILE}"
tofu -chdir="${GCP_ROOT}" plan -var-file="${VARIABLE_FILE}" -out="${PLAN_FILE}"
printf 'Saved reviewed-plan candidate to %s\n' "${PLAN_FILE}"
