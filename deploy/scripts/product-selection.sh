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
# Resolve once in the top-level process. Nested installs/validation reuse this snapshot.
_selection_script="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/deployment-selection.py"
_selection_was_resolved="${FORWARDMEASURE_RESOLVED_SELECTION+x}"
FORWARDMEASURE_RESOLVED_SELECTION="$(python3 "$_selection_script" --environment "${ENVIRONMENT:?environment is required}")" || return 1
export FORWARDMEASURE_RESOLVED_SELECTION
FORWARDMEASURE_FRAMEWORK="$(jq -r '.framework' <<< "$FORWARDMEASURE_RESOLVED_SELECTION")"
FORWARDMEASURE_ENGINE="$(jq -r '.engine' <<< "$FORWARDMEASURE_RESOLVED_SELECTION")"
FORWARDMEASURE_ENABLE_FDE="$(jq -r '.enableFde' <<< "$FORWARDMEASURE_RESOLVED_SELECTION")"
export FORWARDMEASURE_FRAMEWORK FORWARDMEASURE_ENGINE FORWARDMEASURE_ENABLE_FDE
if [[ -z "$_selection_was_resolved" ]]; then
  echo "Deployment selection (${ENVIRONMENT}): ${FORWARDMEASURE_RESOLVED_SELECTION}"
fi
unset _selection_script _selection_was_resolved
PRODUCT_REPOSITORIES=(forwardmeasure-openworkflow forwardmeasure-data-streaming forwardmeasure-entity-intelligence)
PRODUCT_SUMMARY="shared services + OpenWorkflow + data-streaming + entity-intelligence"
DIGEST_SELECTION_ARGS=(--exclude-entry decisionEngineMigrations)
if [[ "${FORWARDMEASURE_ENABLE_FDE}" == true ]]; then
  PRODUCT_REPOSITORIES+=(forwardmeasure-decision-engine)
  PRODUCT_SUMMARY+=" + decision-engine"
  DIGEST_SELECTION_ARGS=()
fi
