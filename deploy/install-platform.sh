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
# an existing environment: each step below is an ordered `helmfile ... sync`, which
# reconciles to desired state whether or not anything already exists, so
# this is not a one-time bootstrap script.
#
# Usage: install-platform.sh <environment>                 install/update everything
#        install-platform.sh <environment> <release>...    update only the named Helm releases
#        install-platform.sh <environment> --list          list every release name, per repository
#
# Naming releases updates just those components of an installed platform, e.g. after pushing a
# new image: entity-intelligence-services. Image digests are still refreshed and the whole
# platform still validates first; the named releases are then synced from whichever helmfile
# declares them (the platform's own or a selected product's). Their `needs` are not synced and the
# stage ordering, preflights and secret reloads of a full install are skipped, so name every
# release that has to move. A name no helmfile declares fails before the cluster is touched.
#
# Product/framework/engine choices are resolved once and inherited by every child process.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment> [--list | <release>...]}"
shift
LIST_ONLY=false
RELEASES=()
for argument in "$@"; do
  case "${argument}" in
    --list) LIST_ONLY=true ;;
    -*) echo "Unknown option: ${argument}" >&2; exit 1 ;;
    *) RELEASES+=("${argument}") ;;
  esac
done

source "${SCRIPT_DIR}/scripts/product-selection.sh"
python3 "${SCRIPT_DIR}/scripts/check-deployment-transition.py"

image_files=(
  "${SCRIPT_DIR}/helmfile/shared/image-versions.yaml"
  "${SCRIPT_DIR}/helmfile/environments/image-versions.yaml"
  "${WORKSPACE_DIR}/forwardmeasure-data-streaming/deploy/helmfile/environments/base.yaml"
  "${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/environments/base.yaml"
)
if [[ "${FORWARDMEASURE_ENABLE_FDE}" == true ]]; then
  image_files+=("${WORKSPACE_DIR}/forwardmeasure-decision-engine/deploy/helmfile/environments/base.yaml.gotmpl")
fi
framework="${FORWARDMEASURE_FRAMEWORK}"

# The platform's own helmfile, then each selected product's.
helmfile_files=("${SCRIPT_DIR}/helmfile/helmfile.yaml.gotmpl")
for product in "${PRODUCT_REPOSITORIES[@]}"; do
  helmfile_files+=("${WORKSPACE_DIR}/${product}/deploy/helmfile/helmfile.yaml.gotmpl")
done

# Prints "<name> <enabled>" for each release a helmfile declares in this environment.
release_names() {
  helmfile --file "$1" --environment "${ENVIRONMENT}" list --output json --allow-no-matching-release \
    | jq -r '.[] | "\(.name) \(.enabled)"'
}

if [[ "${LIST_ONLY}" == true ]]; then
  for file in "${helmfile_files[@]}"; do
    echo "${file#"${WORKSPACE_DIR}/"}:"
    release_names "${file}" | sed -e 's/^/  /' -e 's/ true$//' -e 's/ false$/  (disabled here)/'
  done
  exit 0
fi

# Which helmfile declares each named release; an unknown name stops here.
declare -A selected_by_file=()
if (( ${#RELEASES[@]} > 0 )); then
  declare -A declared_in=()
  declare -A disabled=()
  for file in "${helmfile_files[@]}"; do
    while read -r name enabled; do
      [[ -n "${name}" ]] || continue
      if [[ "${enabled}" == true ]]; then
        declared_in["${name}"]+="${file} "
      else
        disabled["${name}"]=1
      fi
    done < <(release_names "${file}")
  done
  unknown=()
  for release in "${RELEASES[@]}"; do
    if [[ -z "${declared_in[${release}]:-}" ]]; then
      if [[ -n "${disabled[${release}]:-}" ]]; then
        unknown+=("${release}(disabled in ${ENVIRONMENT})")
      else
        unknown+=("${release}")
      fi
      continue
    fi
    for file in ${declared_in[${release}]}; do
      selected_by_file["${file}"]+=" --selector name=${release}"
    done
  done
  if (( ${#unknown[@]} > 0 )); then
    echo "Cannot update: ${unknown[*]} (see $0 ${ENVIRONMENT} --list)" >&2
    exit 1
  fi
fi
# Render the workflow environment before any registry lookups. Invalid configuration must fail
# visibly here, rather than after resolving every other product's image inventory.
echo "Checking OpenWorkflow environment inputs (${ENVIRONMENT})"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/scripts/environment-value.sh" \
  "${ENVIRONMENT}" cloudProvider >/dev/null
echo "Resolving selected platform/product image digests"
"${SCRIPT_DIR}/scripts/sync-image-digests.sh" --framework "${framework}" "${DIGEST_SELECTION_ARGS[@]}" "${image_files[@]}"
echo "Resolving OpenWorkflow image digests"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/scripts/resolve-image-digests.sh" "${ENVIRONMENT}"

# Before touching the cluster: everything renders, and every product references our images by
# digest (a template that drops a digest fails here, not on a stale pull).
"${SCRIPT_DIR}/validate-platform.sh" "${ENVIRONMENT}"

# Cross-repository values (domain, gateway, endpoints, shared namespaces, shared chart/image pins)
# are not copied into the product repositories: every helmfile loads helmfile/shared/ directly from
# this sibling checkout (see helmfile/shared/common.yaml.gotmpl).

if (( ${#RELEASES[@]} > 0 )); then
  for file in "${helmfile_files[@]}"; do
    [[ -n "${selected_by_file[${file}]:-}" ]] || continue
    echo "Updating${selected_by_file[${file}]//--selector name=/} from ${file#"${WORKSPACE_DIR}/"}"
    # shellcheck disable=SC2086 # the selector flags are deliberately word-split
    helmfile --file "${file}" --environment "${ENVIRONMENT}" ${selected_by_file[${file}]} \
      sync --wait --wait-for-jobs
  done
  echo "Updated ${RELEASES[*]} for ${ENVIRONMENT}."
  exit 0
fi

"${SCRIPT_DIR}/helmfile/install.sh" "${ENVIRONMENT}"
for product in "${PRODUCT_REPOSITORIES[@]}"; do
  "${WORKSPACE_DIR}/${product}/deploy/helmfile/install.sh" "${ENVIRONMENT}"
done

python3 "${SCRIPT_DIR}/helmfile/scripts/reload-secret-consumers.py"

python3 "${SCRIPT_DIR}/scripts/selection-manifests.py" record | kubectl apply -f -

echo "ForwardMeasure platform (${PRODUCT_SUMMARY}) installed/updated for ${ENVIRONMENT}."
