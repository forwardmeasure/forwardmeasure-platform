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
# Today that's just OpenWorkflow; entity-intelligence is a planned addition,
# not wired in yet.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"

"${SCRIPT_DIR}/helmfile/install.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/install.sh" "${ENVIRONMENT}"

echo "ForwardMeasure platform (shared services + OpenWorkflow) installed/updated for ${ENVIRONMENT}."
