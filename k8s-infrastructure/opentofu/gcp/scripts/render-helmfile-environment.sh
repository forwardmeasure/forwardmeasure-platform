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
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GCP_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_FILE="${1:-${GCP_ROOT}/generated/terraform-values.yaml}"

mkdir -p "$(dirname -- "${OUTPUT_FILE}")"
tofu -chdir="${GCP_ROOT}" output -raw helmfile_environment_yaml >"${OUTPUT_FILE}"
printf 'Rendered Helmfile environment values to %s\n' "${OUTPUT_FILE}"
