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
# Installs or updates the ForwardMeasure platform as a whole - shared
# platform services plus every product built on them. Safe to re-run against
# an existing environment: each step below is a `helmfile ... apply`, which
# reconciles to desired state whether or not anything already exists, so
# this is not a one-time bootstrap script.
#
# FDE is opt-in while FOWF, FDS and FEI complete deployment rehabilitation.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"

source "${SCRIPT_DIR}/scripts/product-selection.sh"

image_files=(
  "${SCRIPT_DIR}/helmfile/shared/image-versions.yaml"
  "${SCRIPT_DIR}/helmfile/environments/image-versions.yaml"
  "${WORKSPACE_DIR}/forwardmeasure-data-streaming/deploy/helmfile/environments/base.yaml"
  "${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/environments/base.yaml"
)
if [[ "${FORWARDMEASURE_ENABLE_FDE}" == true ]]; then
  image_files+=("${WORKSPACE_DIR}/forwardmeasure-decision-engine/deploy/helmfile/environments/base.yaml.gotmpl")
fi
framework="$("${SCRIPT_DIR}/helmfile/scripts/environment-value.sh" "${ENVIRONMENT}" platform.framework)"
"${SCRIPT_DIR}/scripts/sync-image-digests.sh" --framework "${framework}" "${DIGEST_SELECTION_ARGS[@]}" "${image_files[@]}"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/scripts/resolve-image-digests.sh" "${ENVIRONMENT}"

# Before touching the cluster: everything renders, and every product references our images by
# digest (a template that drops a digest fails here, not on a stale pull).
"${SCRIPT_DIR}/validate-platform.sh" "${ENVIRONMENT}"

# Cross-repository values (domain, gateway, endpoints, shared namespaces, shared chart/image pins)
# are not copied into the product repositories: every helmfile loads helmfile/shared/ directly from
# this sibling checkout (see helmfile/shared/common.yaml.gotmpl).

"${SCRIPT_DIR}/helmfile/install.sh" "${ENVIRONMENT}"
for product in "${PRODUCT_REPOSITORIES[@]}"; do
  "${WORKSPACE_DIR}/${product}/deploy/helmfile/install.sh" "${ENVIRONMENT}"
done

echo "ForwardMeasure platform (${PRODUCT_SUMMARY}) installed/updated for ${ENVIRONMENT}."
