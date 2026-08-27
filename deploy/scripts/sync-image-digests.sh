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
# Refreshes every already-pinned `digest`/`digests.<framework>` field in one
# or more image-versions.yaml files to whatever digest the registry
# currently serves for that entry's repository:tag. Queried via `docker
# manifest inspect -v` (registry API only, no layer pull - fast enough to
# run on every install).
#
# Entries left at digest: "" are deliberately unpinned (floating tag, e.g.
# postgresql/cassandra/redpanda track upstream directly) and are never
# touched - this only re-resolves pins that already exist, so a rebuild
# can't silently start pinning something nobody asked to pin. This is what
# closes the gap found the hard way: a full image rebuild + push updates the
# registry, but nothing previously updated these files to match, so Helm
# kept deploying the old pinned digest (see
# forwardmeasure-openworkflow's openworkflow-tenant-reconciliation Job
# failing against a stale pre-fix image despite a clean rebuild).
#
# Usage: sync-image-digests.sh <image-versions.yaml> [<image-versions.yaml> ...]
set -euo pipefail

for command in docker yq jq; do
  command -v "${command}" >/dev/null || {
    echo "Required command is unavailable: ${command}" >&2
    exit 1
  }
done

[[ $# -ge 1 ]] || {
  echo "Usage: $0 <image-versions.yaml> [<image-versions.yaml> ...]" >&2
  exit 1
}

# Registry API digest lookup for one repository:tag - the manifest's own
# digest (what a repo@sha256:... reference resolves to), not the config or
# layer digests also present in the response. -v returns a single object for
# a single-platform manifest and an array for a multi-platform manifest
# list; every image built by this workspace's docker-maven-plugin is
# single-platform (amd64), but the [0] fallback keeps this correct either
# way.
resolve_digest() {
  local repository="$1" tag="$2"
  docker manifest inspect -v "${repository}:${tag}" 2>/dev/null \
    | jq -r 'if type == "array" then .[0].Descriptor.digest else .Descriptor.digest end'
}

sync_file() {
  local file="$1"
  echo "Syncing digests in ${file}"

  # Walks .imageVersions to arbitrary depth (most entries are flat, engines.*
  # is one level deeper) and emits one row per already-pinned digest field:
  # yq path to the digest field, its repository, its tag, its current value.
  while IFS=$'\t' read -r digest_path repository tag current_digest; do
    # Empty tag means the entry is pinned by digest alone with no floating
    # tag behind it (e.g. nginxUnprivileged/grpcbin/nats) - nothing to
    # re-resolve against, so it's left as a deliberate one-time pin.
    [[ -n "${current_digest}" && -n "${repository}" && -n "${tag}" ]] || continue
    new_digest="$(resolve_digest "${repository}" "${tag}")"
    if [[ -z "${new_digest}" ]]; then
      echo "  FAILED to resolve digest for ${repository}:${tag}" >&2
      exit 1
    elif [[ "${new_digest}" == "${current_digest}" ]]; then
      echo "  up to date: ${repository}:${tag}"
    else
      echo "  updating ${repository}:${tag}: ${current_digest} -> ${new_digest}"
      yq -i ".${digest_path} = \"${new_digest}\"" "${file}"
    fi
  done < <(yq -o=json '.imageVersions' "${file}" | jq -r '
    # The flat and nested (per-framework) branches must stay independent
    # generators at the top level - chaining the nested branch behind the
    # flat branch as$d binding would silently drop every nested-only entry,
    # since (null // empty) as $d produces zero outputs and nothing
    # downstream of it runs. Confirmed the hard way: an earlier version of
    # this scoped both branches under one `as $d`, so definitionManagement,
    # executionManagement, engines.*, operationAdapter, and studio - every
    # entry using the repositories/digests map shape - were silently skipped.
    def entries_at($segs; $obj):
      ( ($obj.digest // "") as $d | select($d != "") |
        [($segs + ["digest"]), ($obj.repository // ""), ($obj.tag // ""), $d] ),
      ( ($obj.digests // {}) | to_entries[] | select((.value // "") != "") |
          [ ($segs + ["digests", .key]),
            ($obj.repositories[.key]? // ""),
            ($obj.tag // ""),
            .value ] );
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
    | [ ( .[0] | map("[\"" + . + "\"]") | join("") ), .[1], .[2], .[3] ]
    | @tsv
  ')
}

for file in "$@"; do
  sync_file "${file}"
done
