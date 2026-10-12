#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Validates the same ForwardMeasure platform composition install-platform.sh
# installs/updates - see that script for the scope note.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:-gcp-greenfield-example}"
source "${SCRIPT_DIR}/scripts/product-selection.sh"

# Every check below is independent (each renders on its own), so they run in parallel; each one's
# output is kept apart and printed once it finishes, failures last with their full output.
LOGS="$(mktemp -d)"
trap 'rm -rf "${LOGS}"' EXIT
pids=()
names=()
check() {
  local name="$1"
  shift
  "$@" >"${LOGS}/${#names[@]}.log" 2>&1 &
  pids+=($!)
  names+=("${name}")
}

check "platform validation" "${SCRIPT_DIR}/helmfile/validate.sh" "${ENVIRONMENT}"
check "OpenWorkflow validation" "${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/validate.sh" "${ENVIRONMENT}"

# Every product's render must reference our images by digest (check-image-digests.sh). For the
# products with no validate.sh of their own, rendering is also the whole check (it fails on an
# undeclared environment or a missing shared value).
render_and_check_digests() {
  helmfile --file "$1" --environment "${ENVIRONMENT}" template \
    | "${SCRIPT_DIR}/scripts/check-image-digests.sh" "$2 (${ENVIRONMENT})"
}
helmfiles=("${SCRIPT_DIR}/helmfile/helmfile.yaml.gotmpl")
for product in "${PRODUCT_REPOSITORIES[@]}"; do
  helmfiles+=("${WORKSPACE_DIR}/${product}/deploy/helmfile/helmfile.yaml.gotmpl")
done
for helmfile in "${helmfiles[@]}"; do
  product="$(basename "$(dirname "$(dirname "$(dirname "${helmfile}")")")")"
  check "${product} image digests" render_and_check_digests "${helmfile}" "${product}"
done

failed=()
for index in "${!pids[@]}"; do
  if wait "${pids[${index}]}"; then
    echo "${names[${index}]}: passed"
  else
    failed+=("${index}")
  fi
done
for index in "${failed[@]}"; do
  echo "${names[${index}]} failed:" >&2
  cat "${LOGS}/${index}.log" >&2
done
((${#failed[@]} == 0)) || exit 1

echo "ForwardMeasure platform (${PRODUCT_SUMMARY}) validated for ${ENVIRONMENT}."
