#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:-gcp-greenfield-example}"

INTEGRATION_DIR="$(mktemp -d)"
trap 'rm -rf -- "${INTEGRATION_DIR}"' EXIT
OKS_INTEGRATION_VALUES="${INTEGRATION_DIR}/entity-intelligence-oks-values.yaml"

"${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/render-oks-integration-values.sh" \
  "${ENVIRONMENT}" "${OKS_INTEGRATION_VALUES}"

"${SCRIPT_DIR}/helmfile/validate.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/openworkflow-kafka-streams/deploy/helmfile/validate.sh" \
  "${ENVIRONMENT}" "${OKS_INTEGRATION_VALUES}"
"${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/validate.sh" "${ENVIRONMENT}"

echo "All three greenfield deployment tiers validated for ${ENVIRONMENT}."
