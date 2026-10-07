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
# Build the resolved product source tree. Publishing is an explicit --push choice.
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
workspace="$(cd -- "${script_dir}/../../.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment> [--push]}"
push=false
if [[ "${2:-}" == --push ]]; then push=true; elif [[ -n "${2:-}" ]]; then echo "Unknown option: $2" >&2; exit 2; fi
if (($# > 2)); then echo "Too many arguments" >&2; exit 2; fi
source "${script_dir}/product-selection.sh"
log_dir="${FORWARDMEASURE_BUILD_LOG_DIR:-/tmp/forwardmeasure-deployment-build/$(date -u +%Y%m%dT%H%M%SZ)}"
mkdir -p "${log_dir}"
python3 "${script_dir}/product-build-plan.py" --environment "${ENVIRONMENT}" >"${log_dir}/build-plan.json"
# Work around the observed Temurin 25 C2 augmentation crash in the build JVM only.
export MAVEN_OPTS="${MAVEN_OPTS:+${MAVEN_OPTS} }-XX:TieredStopAtLevel=1"
export NODE_OPTIONS="${NODE_OPTIONS:+${NODE_OPTIONS} }--max-old-space-size=2048"
bounded="${workspace}/forwardmeasure-openworkflow/scripts/build-bounded.sh"
# Preserve provenance for the dirty source batch as well as the selected image list.
for repository in forwardmeasure-platform forwardmeasure-database-migrations forwardmeasure-jpa forwardmeasure-object-storage forwardmeasure-authzen forwardmeasure-testcontainers forwardmeasure-entity-matching forwardmeasure-openworkflow forwardmeasure-data-streaming forwardmeasure-entity-intelligence forwardmeasure-decision-engine helm-charts openworkflow-k8s-setup; do
  {
    git -C "${workspace}/${repository}" rev-parse HEAD
    git -C "${workspace}/${repository}" status --short
    git -C "${workspace}/${repository}" diff HEAD --binary | sha256sum
  } >"${log_dir}/${repository}-source-status.txt"
done
# Historical reactor license-header debt is separate from deployable compilation.
args=(-B -ntp -Drat.skip=true -Djacoco.skip=true -DskipTests -DskipITs -Dmaven.test.skip=false -Dmaven.javadoc.skip=true)
for repository in forwardmeasure-platform forwardmeasure-testcontainers forwardmeasure-database-migrations forwardmeasure-jpa forwardmeasure-object-storage forwardmeasure-authzen forwardmeasure-entity-matching; do
  "${bounded}" -f "${workspace}/${repository}/pom.xml" "${args[@]}" clean install 2>&1 | tee "${log_dir}/${repository}.log"
done
# FOWF supplies the migrations library used by FEI; FDS supplies FEI worker APIs.
# FEI's migration image is then published before the combined installer starts FOWF's hooks.
for repository in "${PRODUCT_REPOSITORIES[@]}"; do
  modules="$(python3 "${script_dir}/product-build-plan.py" --environment "${ENVIRONMENT}" --repository "${repository}")"
  if [[ "${repository}" == forwardmeasure-data-streaming ]]; then
    modules+=",forwardmeasure-data-streaming-launcher-client"
  fi
  "${bounded}" -f "${workspace}/${repository}/pom.xml" "${args[@]}" -pl "${modules}" -am \
    -Pcontainer-image -Dcontainer-image.build=true -Dcontainer-image.push="${push}" \
    clean install 2>&1 | tee "${log_dir}/${repository}-images.log"
done
printf 'Build logs and selected image modules: %s\n' "${log_dir}"
