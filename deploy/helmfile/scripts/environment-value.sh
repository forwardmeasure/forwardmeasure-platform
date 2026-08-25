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

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
ENVIRONMENT="${1:?environment is required}"
KEY="${2:?value key is required}"

# Rendered via `helmfile build`, not a raw multi-file yq merge of the
# environment files directly - several are .gotmpl files with unrendered
# Go-template expressions in scalar values, and yq parsing that raw text
# misreads `{{ }}` as YAML flow-mapping syntax, silently corrupting the
# value instead of erroring. Confirmed the hard way via preflight.sh's own
# use of the same pattern (see its fix for the concrete failure mode).
#
# `helmfile build` emits one YAML document per top-level helmfiles: entry
# (9 of them for this repo, one per release group), each with its own
# renderedvalues: block - not a single merged document. Piping all of them
# through yq without narrowing to one produces N copies of the answer
# joined by `---` separators, silently corrupting every value into garbage
# that only certain kinds of checks (anchored regex) will actually catch -
# confirmed the hard way: bootstrapIdentity.actorType failed a `^...$`
# match on "HUMAN\n---\nHUMAN\n---\n..." while several looser checks
# elsewhere (glob/contains) passed on the same corruption undetected.
# select(di==0) takes only the first document - safe because every
# top-level entry shares the same environment: layer, so renderedvalues is
# identical across all of them (verified: md5-identical across all 9).
helmfile --file "${SCRIPT_DIR}/helmfile.yaml.gotmpl" --environment "${ENVIRONMENT}" build 2>/dev/null \
  | yq 'select(di==0) | .renderedvalues' \
  | yq -r ".${KEY} // \"\""
