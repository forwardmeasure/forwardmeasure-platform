#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENVIRONMENT="${1:?environment is required}"
KEY="${2:?value key is required}"

FILES=("${SCRIPT_DIR}/environments/base.yaml")
if [[ "${ENVIRONMENT}" == "gcp-greenfield-example" ]]; then
  FILES+=("${SCRIPT_DIR}/environments/gcp-greenfield.example.yaml")
elif [[ "${ENVIRONMENT}" != "base" ]]; then
  FILES+=("${SCRIPT_DIR}/environments/${ENVIRONMENT}.yaml")
fi

yq ea '. as $item ireduce ({}; . * $item)' "${FILES[@]}" \
  | yq -r ".${KEY} // \"\""
