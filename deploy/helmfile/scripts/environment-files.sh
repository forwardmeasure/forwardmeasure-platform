#!/usr/bin/env bash
set -euo pipefail

HELMFILE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENVIRONMENT="${1:?environment is required}"

FILES=(
  environments/chart-versions.yaml
  environments/image-versions.yaml
  environments/base.yaml
)

if [[ "${ENVIRONMENT}" == "gcp-greenfield-example" ]]; then
  FILES+=("environments/gcp-greenfield.example.yaml")
elif [[ "${ENVIRONMENT}" != "base" ]]; then
  FILES+=("environments/${ENVIRONMENT}.yaml")
fi

for relative_path in "${FILES[@]}"; do
  absolute_path="${HELMFILE_DIR}/${relative_path}"
  [[ -f "${absolute_path}" ]] || {
    echo "Platform environment file is missing: ${absolute_path}" >&2
    exit 1
  }
  printf '%s\n' "${absolute_path}"
done
