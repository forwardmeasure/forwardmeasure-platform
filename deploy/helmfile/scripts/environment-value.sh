#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENVIRONMENT="${1:?environment is required}"
KEY="${2:?value key is required}"

mapfile -t FILES < <("${SCRIPT_DIR}/scripts/environment-files.sh" "${ENVIRONMENT}")

yq ea '. as $item ireduce ({}; . * $item)' "${FILES[@]}" \
  | yq -r ".${KEY} // \"\""
