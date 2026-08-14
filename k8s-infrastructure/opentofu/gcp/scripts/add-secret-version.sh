#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  printf 'Usage: %s <gcp-project-id> <secret-id> [value-file|-]\n' "$0" >&2
  exit 2
fi

PROJECT_ID="$1"
SECRET_ID="$2"
VALUE_FILE="${3:--}"

gcloud secrets describe "${SECRET_ID}" --project "${PROJECT_ID}" >/dev/null

if [[ "${VALUE_FILE}" == "-" ]]; then
  if [[ ! -t 0 ]]; then
    gcloud secrets versions add "${SECRET_ID}" \
      --project "${PROJECT_ID}" --data-file=-
  else
    printf 'Enter the value for %s, then press Ctrl-D:\n' "${SECRET_ID}" >&2
    gcloud secrets versions add "${SECRET_ID}" \
      --project "${PROJECT_ID}" --data-file=-
  fi
else
  gcloud secrets versions add "${SECRET_ID}" \
    --project "${PROJECT_ID}" --data-file="${VALUE_FILE}"
fi
