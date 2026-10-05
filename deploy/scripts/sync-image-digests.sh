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
# entry's repository:tag. Queried via `docker buildx imagetools inspect` (registry
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
# Usage: sync-image-digests.sh [--check] [--exclude-entry name] <image-versions.yaml> [...]
#   --check  report what would change and what is missing; write nothing.
#   --framework quarkus|spring|micronaut  resolve only this framework's split entries.
#   --exclude-entry path  omit an imageVersions subtree, e.g. engines.pekko (repeatable).
#   --resolved-values file  use rendered image coordinates while updating the base inventory.
set -euo pipefail

for command in docker yq jq; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

check_only=false
excluded_entries=()
selected_framework=""
resolved_values_file=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --check) check_only=true; shift ;;
    --framework)
      [[ $# -ge 2 && "$2" =~ ^(quarkus|spring|micronaut)$ ]] || {
        echo "--framework requires quarkus, spring or micronaut" >&2; exit 1;
      }
      selected_framework="$2"; shift 2 ;;
    --resolved-values)
      [[ $# -ge 2 && -f "$2" ]] || {
        echo "--resolved-values requires a rendered values file" >&2; exit 1;
      }
      resolved_values_file="$2"; shift 2 ;;
    --exclude-entry)
      [[ $# -ge 2 && "$2" =~ ^[A-Za-z][A-Za-z0-9_.-]*$ ]] || {
        echo "--exclude-entry requires an imageVersions entry path" >&2; exit 1;
      }
      excluded_entries+=("$2"); shift 2 ;;
    --*) echo "Unknown option: $1" >&2; exit 1 ;;
    *) break ;;
  esac
done
[[ $# -ge 1 ]] || {
  echo "Usage: $0 [--check] [--framework name] [--exclude-entry path] [--resolved-values file] <image-versions.yaml> [...]" >&2
  exit 1
}
excluded_json="$(printf '%s\n' "${excluded_entries[@]}" | jq -Rsc 'split("\n") | map(select(length > 0))')"

resolved_values='{}'
if [[ -n "${resolved_values_file}" ]]; then
  resolved_values="$(yq -o=json '.imageVersions' "${resolved_values_file}")"
fi

OURS='^(docker\.io/)?forwardmeasure/'
failures=()

docker buildx version >/dev/null 2>&1 || {
  echo "Docker Buildx is required to resolve architecture-independent image digests." >&2
  exit 1
}

# Resolve the top-level manifest digest, preserving the platform index for multi-architecture
# images. Selecting the first child manifest can silently pin the wrong node architecture.
resolve_digest() {
  local repository="$1" tag="$2" attempt digest
  for attempt in 1 2 3; do
    digest="$(docker buildx imagetools inspect "${repository}:${tag}" --format '{{json .Manifest}}' 2>/dev/null \
      | jq -er '.digest | select(test("^sha256:[0-9a-f]{64}$"))' 2>/dev/null || true)"
    if [[ "${digest}" =~ ^sha256:[0-9a-f]{64}$ ]]; then
      echo "${digest}"
      return 0
    fi
    [[ "${attempt}" == 3 ]] || sleep 2
  done
  return 1
}

sync_file() {
  local file="$1" rows
  echo "Syncing digests in ${file}"

  # Capture parser failures before registry access or inventory mutation.
  rows="$(yq -o=json '.imageVersions' "${file}" | jq -r --argjson excluded "${excluded_json}" --arg framework "${selected_framework}" --argjson resolved "${resolved_values}" '
    # The single-repository and per-framework branches must stay independent
    # generators - chaining one behind the other silently drops every entry
    # of the second shape (found the hard way: every repositories/digests
    # entry was once skipped).
    def entries_at($segs; $obj):
      ( if ($obj | has("repository")) then
          [ ($segs + ["digest"]), ($obj.repository // ""), ($obj.tag // ""),
            ($obj.digest // ""), (if ($obj | has("digest")) then "pin" else "nofield" end) ]
        else empty end ),
      ( ($obj.repositories // {}) | to_entries[] | select($framework == "" or .key == $framework) | .key as $k | .value as $repository |
          [ ($segs + ["digests", $k]), ($repository // ""), ($obj.tag // ""),
            ($obj.digests[$k]? // ""),
            (if (($obj.digests // {}) | has($k)) then "pin" else "nofield" end) ] );
    def walk_node($segs; $obj):
      if ($obj | type) != "object" or ($excluded | index($segs[1:] | join("."))) != null then empty
      else
        entries_at($segs; $obj),
        ( $obj | to_entries[]
          | select(.key as $k | ["digest","digests","repository","repositories","tag","pullPolicy"] | index($k) | not)
          | walk_node($segs + [.key]; .value) )
      end;
    to_entries[]
    | walk_node(["imageVersions", .key]; ($resolved[.key] // .value))
    | [ ( .[0] | map("[\"" + . + "\"]") | join("") ), .[1], .[2], .[3], .[4] ]
    | join("\u001f")
  ')"

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
  done <<<"${rows}"
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
