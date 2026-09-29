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
# Today that's OpenWorkflow plus forwardmeasure-data-streaming's own launcher (added 2026-09-26,
# gcp-openworkflow-prod only so far - see forwardmeasure-data-streaming's own deploy/helmfile) plus
# forwardmeasure-entity-intelligence's own REST services (added 2026-09-27 - fei needs both
# OpenWorkflow and data-streaming's WorkflowIngestionLauncher at runtime, so it runs last; its own
# schema migration/workflow-definition-publish Jobs already run earlier, bundled into
# OpenWorkflow's own deploy/helmfile - see forwardmeasure-entity-intelligence's own deploy/helmfile
# for the full rationale). forwardmeasure-decision-engine runs last (added 2026-09-29).
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
WORKSPACE_DIR="$(cd -- "${PLATFORM_DIR}/.." && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"

# FDS/FEI/FDE keep their imageVersions in their own environments/base.yaml(.gotmpl), not a separate
# image-versions.yaml - sync-image-digests.sh only reads the top-level .imageVersions key, so the
# file name doesn't matter.
"${SCRIPT_DIR}/scripts/sync-image-digests.sh" \
  "${SCRIPT_DIR}/helmfile/shared/image-versions.yaml" \
  "${SCRIPT_DIR}/helmfile/environments/image-versions.yaml" \
  "${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/environments/image-versions.yaml" \
  "${WORKSPACE_DIR}/forwardmeasure-data-streaming/deploy/helmfile/environments/base.yaml" \
  "${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/environments/base.yaml" \
  "${WORKSPACE_DIR}/forwardmeasure-decision-engine/deploy/helmfile/environments/base.yaml.gotmpl"

# Cross-repository values (domain, gateway, endpoints, shared namespaces, shared chart/image pins)
# are not copied into the product repositories: every helmfile loads helmfile/shared/ directly from
# this sibling checkout (see helmfile/shared/common.yaml.gotmpl).

"${SCRIPT_DIR}/helmfile/install.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-openworkflow/deploy/helmfile/install.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-data-streaming/deploy/helmfile/install.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-entity-intelligence/deploy/helmfile/install.sh" "${ENVIRONMENT}"
"${WORKSPACE_DIR}/forwardmeasure-decision-engine/deploy/helmfile/install.sh" "${ENVIRONMENT}"

echo "ForwardMeasure platform (shared services + OpenWorkflow + data-streaming + entity-intelligence + decision-engine) installed/updated for ${ENVIRONMENT}."
