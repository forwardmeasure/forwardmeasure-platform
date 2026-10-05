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
# Sourced by the umbrella entrypoints. Direct Helmfile invocations read the same
# flag in shared/common.yaml.gotmpl. Default to the three-product recovery scope.
case "${FORWARDMEASURE_ENABLE_FDE:-false}" in
  true|false) export FORWARDMEASURE_ENABLE_FDE="${FORWARDMEASURE_ENABLE_FDE:-false}" ;;
  *) echo "FORWARDMEASURE_ENABLE_FDE must be true or false" >&2; return 1 ;;
esac
PRODUCT_REPOSITORIES=(forwardmeasure-openworkflow forwardmeasure-data-streaming forwardmeasure-entity-intelligence)
PRODUCT_SUMMARY="shared services + OpenWorkflow + data-streaming + entity-intelligence"
DIGEST_SELECTION_ARGS=(--exclude-entry decisionEngineMigrations)
if [[ "${FORWARDMEASURE_ENABLE_FDE}" == true ]]; then
  PRODUCT_REPOSITORIES+=(forwardmeasure-decision-engine)
  PRODUCT_SUMMARY+=" + decision-engine"
  DIGEST_SELECTION_ARGS=()
fi
