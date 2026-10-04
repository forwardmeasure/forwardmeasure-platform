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
# Reads rendered manifests on stdin and fails if any image of ours
# ([docker.io/]forwardmeasure/...) is referenced without a digest - in an
# image: field or anywhere else (e.g. a worker image passed as an env value).
# sync-image-digests.sh pins the version files; this catches a template that
# drops a digest it was given. Third-party images may float by tag.
#
# Usage: helmfile ... template | check-image-digests.sh <label>
set -euo pipefail

label="${1:-rendered manifests}"
# Comments (a # at line start or after whitespace - YAML, or a script held in a
# ConfigMap) are dropped first: they may name an image without deploying it.
# A reference must start a value (line start, whitespace, quote, = or :), so
# a URL path segment such as .../realms/forwardmeasure/... never matches.
references="$(sed -E 's/(^|[[:space:]])#.*$//' | grep -oE "(^|[[:space:]\"'=:])(docker\.io/)?forwardmeasure/[a-z0-9][a-z0-9._-]*(:[A-Za-z0-9._-]+)?(@sha256:[0-9a-f]{64})?" \
  | sed -E "s/^[[:space:]\"'=:]//" | sort -u || true)"
unpinned="$(grep -v -e '@sha256:' -e '^$' <<<"${references}" || true)"
if [[ -n "${unpinned}" ]]; then
  echo "${label}: our images must be referenced by digest; these are not:" >&2
  sed 's/^/  /' <<<"${unpinned}" >&2
  exit 1
fi
echo "${label}: every image of ours is referenced by digest ($(grep -c . <<<"${references}" || true) distinct)."
