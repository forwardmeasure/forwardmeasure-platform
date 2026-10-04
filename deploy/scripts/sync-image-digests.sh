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
# Sets every `digest`/`digests.<framework>` field in one or more
# image-versions files to the digest the registry currently serves for that
# entry's repository:tag. Queried via `docker manifest inspect -v` (registry
# API only, no layer pull - fast enough to run on every install).
#
# The rule (user decision, 2026-10-01):
#   - Our images (docker.io/forwardmeasure/...) are always deployed by
#     digest. Every one of them is resolved, including entries whose digest
#     is still empty. If any can't be resolved (not pushed, wrong tag), this
#     fails after listing all of them, so the install stops before deploying
#     a stale or missing image. An entry of ours with no digest field at all
#     is a failure too - add `digest: ""` and make sure its template uses it.
#   - Third-party images may be pinned or float by tag. A pinned one is
#     refreshed; one left at digest: "" (e.g. postgres, redpanda) is left
#     alone.
# Only cloud installs run this (install-platform.sh, fowf's
# resolve-image-digests.sh for environments with a cloudProvider). kind and
# local environments load locally built images by tag and never come here.
#
# Usage: sync-image-digests.sh [--check] <image-versions.yaml> [...]
#   --check  report what would change and what is missing; write nothing.
set -euo pipefail

for command in docker yq jq; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

check_only=false
if [[ "${1:-}" == "--check" ]]; then
  check_only=true
  shift
fi
[[ $# -ge 1 ]] || {
  echo "Usage: $0 [--check] <image-versions.yaml> [<image-versions.yaml> ...]" >&2
  exit 1
}

OURS='^(docker\.io/)?forwardmeasure/'
failures=()

# Registry API digest lookup for one repository:tag - the manifest's own
# digest (what a repo@sha256:... reference resolves to), not the config or
# layer digests also present in the response. -v returns a single object for
# a single-platform manifest and an array for a multi-platform manifest
# list; the [0] fallback keeps this correct either way. Retried: Docker Hub's
# token endpoint occasionally refuses one of many back-to-back lookups.
resolve_digest() {
  local repository="$1" tag="$2" attempt digest
  for attempt in 1 2 3; do
    digest="$(docker manifest inspect -v "${repository}:${tag}" 2>/dev/null \
      | jq -r 'if type == "array" then .[0].Descriptor.digest else .Descriptor.digest end' 2>/dev/null || true)"
    if [[ -n "${digest}" && "${digest}" != "null" ]]; then
      echo "${digest}"
      return 0
    fi
    [[ "${attempt}" == 3 ]] || sleep 2
  done
  return 1
}

sync_file() {
  local file="$1"
  echo "Syncing digests in ${file}"

  # One row per digest field (pinned or empty), plus one per entry of any
  # repository that has no digest field: yq path to the field, repository,
  # tag, current digest, and "pin" or "nofield". Fields are separated by
  # \x1f, not a tab: read collapses consecutive tabs, which would shift an
  # empty field into the next one.
  while IFS=$'\x1f' read -r digest_path repository tag current_digest kind; do
    [[ -n "${repository}" ]] || continue
    if [[ ! "${repository}" =~ ${OURS} ]]; then
      # Third-party: refresh a pin, leave a floating tag alone.
      [[ "${kind}" == "pin" && -n "${current_digest}" && -n "${tag}" ]] || continue
    elif [[ "${kind}" == "nofield" ]]; then
      failures+=("${file}: ${repository} has no digest field (${digest_path})")
      continue
    elif [[ -z "${tag}" ]]; then
      # Pinned by digest alone, no tag to re-resolve - a deliberate fixed pin.
      [[ -n "${current_digest}" ]] || failures+=("${file}: ${repository} has neither tag nor digest")
      continue
    fi
    if ! new_digest="$(resolve_digest "${repository}" "${tag}")"; then
      failures+=("${file}: ${repository}:${tag} could not be resolved (not pushed?)")
      continue
    fi
    if [[ "${new_digest}" == "${current_digest}" ]]; then
      echo "  up to date: ${repository}:${tag}"
    elif [[ "${check_only}" == true ]]; then
      echo "  would update ${repository}:${tag}: ${current_digest:-<none>} -> ${new_digest}"
    else
      echo "  updating ${repository}:${tag}: ${current_digest:-<none>} -> ${new_digest}"
      yq -i ".${digest_path} = \"${new_digest}\"" "${file}"
    fi
  done < <(yq -o=json '.imageVersions' "${file}" | jq -r '
    # The single-repository and per-framework branches must stay independent
    # generators - chaining one behind the other silently drops every entry
    # of the second shape (found the hard way: every repositories/digests
    # entry was once skipped).
    def entries_at($segs; $obj):
      ( if ($obj | has("repository")) then
          [ ($segs + ["digest"]), ($obj.repository // ""), ($obj.tag // ""),
            ($obj.digest // ""), (if ($obj | has("digest")) then "pin" else "nofield" end) ]
        else empty end ),
      ( ($obj.repositories // {}) | to_entries[] | .key as $k | .value as $repository |
          [ ($segs + ["digests", $k]), ($repository // ""), ($obj.tag // ""),
            ($obj.digests[$k]? // ""),
            (if (($obj.digests // {}) | has($k)) then "pin" else "nofield" end) ] );
    def walk_node($segs; $obj):
      if ($obj | type) != "object" then empty
      else
        entries_at($segs; $obj),
        ( $obj | to_entries[]
          | select(.key as $k | ["digest","digests","repository","repositories","tag","pullPolicy"] | index($k) | not)
          | walk_node($segs + [.key]; .value) )
      end;
    to_entries[]
    | walk_node(["imageVersions", .key]; .value)
    | [ ( .[0] | map("[\"" + . + "\"]") | join("") ), .[1], .[2], .[3], .[4] ]
    | join("\u001f")
  ')
}

for file in "$@"; do
  sync_file "${file}"
done

if ((${#failures[@]} > 0)); then
  echo >&2
  echo "Our images must be deployed by digest; these could not be pinned:" >&2
  printf '  %s\n' "${failures[@]}" >&2
  exit 1
fi
