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
ENVIRONMENT="${1:?Usage: $0 <configured-environment> [--full]}"
FULL="${2:-}"

for stage in dashboard analytics ml-serving search messaging identity configuration; do
  helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" \
    --environment "${ENVIRONMENT}" --selector "stage=${stage}" destroy
done

if [[ "${FULL}" == "--full" ]]; then
  # istio-base, istiod, cert-manager, external-secrets - the mesh/cert-
  # manager/external-secrets operators and their CRDs, not anything they
  # configure (that's already gone above). Kept as a separate, explicit
  # opt-in rather than the default: a real teardown-and-reinstall test
  # needs this too, but most callers re-running uninstall/install in a
  # loop don't want to re-pull/reinstall CRDs every time.
  helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" \
    --environment "${ENVIRONMENT}" --selector "stage=foundation" destroy
  echo "Foundation releases destroyed for ${ENVIRONMENT}. Namespaces were retained."
else
  echo "Foundation releases and namespaces were retained intentionally. Pass --full to also tear down istio/cert-manager/external-secrets."
fi
