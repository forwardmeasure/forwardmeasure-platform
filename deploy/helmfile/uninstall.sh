#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-gcp-greenfield}"

for stage in dashboard analytics ml-serving search messaging identity configuration; do
  helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" \
    --environment "${ENVIRONMENT}" --selector "stage=${stage}" destroy
done

echo "Foundation releases and namespaces were retained intentionally."
