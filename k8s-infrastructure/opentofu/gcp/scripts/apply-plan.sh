#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'Usage: %s <environment>\n' "$0" >&2
  exit 2
fi

ENVIRONMENT="$1"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GCP_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PLAN_FILE="${GCP_ROOT}/plans/${ENVIRONMENT}.tfplan"

if [[ ! -f "${PLAN_FILE}" ]]; then
  printf 'Saved plan does not exist: %s\n' "${PLAN_FILE}" >&2
  exit 1
fi

if [[ -z "${TF_VAR_cloudsql_user_passwords:-}" ]]; then
  printf 'TF_VAR_cloudsql_user_passwords must match the values used to create the saved plan.\n' >&2
  exit 1
fi

tofu -chdir="${GCP_ROOT}" apply "${PLAN_FILE}"
