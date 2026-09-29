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

"${SCRIPT_DIR}/helmfile/validate.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/validate.sh" "${ENVIRONMENT}"

# The remaining products install-platform.sh deploys have no validate.sh of their own - rendering
# their helmfile for this environment is the check (it fails on an undeclared environment or a
# missing shared value).
for product in forwardmeasure-data-streaming forwardmeasure-entity-intelligence forwardmeasure-decision-engine; do
  helmfile --file "${WORKSPACE_DIR}/${product}/deploy/helmfile/helmfile.yaml.gotmpl" \
    --environment "${ENVIRONMENT}" template >/dev/null
done

echo "ForwardMeasure platform (shared services + OpenWorkflow + data-streaming + entity-intelligence + decision-engine) validated for ${ENVIRONMENT}."
