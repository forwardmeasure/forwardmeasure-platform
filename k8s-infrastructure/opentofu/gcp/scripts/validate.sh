#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GCP_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PROJECT_AND_STATE_ROOT="${GCP_ROOT}/gcp-project-and-state"

tofu -chdir="${PROJECT_AND_STATE_ROOT}" fmt -check -recursive
tofu -chdir="${PROJECT_AND_STATE_ROOT}" init -backend=false
tofu -chdir="${PROJECT_AND_STATE_ROOT}" validate

tofu -chdir="${GCP_ROOT}" fmt -check -recursive
tofu -chdir="${GCP_ROOT}" init -backend=false
tofu -chdir="${GCP_ROOT}" validate
tofu -chdir="${GCP_ROOT}" test
