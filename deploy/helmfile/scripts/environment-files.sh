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

HELMFILE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENVIRONMENT="${1:?environment is required}"

# Mirrors helmfile.yaml.gotmpl's $layers dict exactly - keep both in sync by
# hand whenever an environment is added, renamed, or removed. There's no way
# to derive one from the other given how helmfile's own templating works
# (same trade-off data-fabric's reference implementation accepts).
FILES=(
  environments/chart-versions.yaml
  environments/image-versions.yaml
  environments/base.yaml.gotmpl
)

case "${ENVIRONMENT}" in
  base) ;;
  gcp)
    FILES+=(environments/gcp.yaml)
    ;;
  gcp-openworkflow-prod)
    FILES+=(environments/gcp.yaml environments/gcp-openworkflow-prod.yaml.gotmpl)
    ;;
  gcp-greenfield-example)
    FILES+=(environments/gcp.yaml environments/gcp-greenfield-example.yaml)
    ;;
  *)
    echo "Unknown environment: ${ENVIRONMENT}" >&2
    exit 1
    ;;
esac

for relative_path in "${FILES[@]}"; do
  absolute_path="${HELMFILE_DIR}/${relative_path}"
  [[ -f "${absolute_path}" ]] || {
    echo "Platform environment file is missing: ${absolute_path}" >&2
    exit 1
  }
  printf '%s\n' "${absolute_path}"
done
