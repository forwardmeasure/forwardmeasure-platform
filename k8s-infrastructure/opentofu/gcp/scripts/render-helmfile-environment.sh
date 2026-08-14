#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GCP_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_FILE="${1:-${GCP_ROOT}/generated/terraform-values.yaml}"

mkdir -p "$(dirname -- "${OUTPUT_FILE}")"
tofu -chdir="${GCP_ROOT}" output -raw helmfile_environment_yaml >"${OUTPUT_FILE}"
printf 'Rendered Helmfile environment values to %s\n' "${OUTPUT_FILE}"
