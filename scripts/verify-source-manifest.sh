#!/usr/bin/env bash
set -euo pipefail

mode="${1:-development}"
if [[ "$mode" != "development" && "$mode" != "release" ]]; then
  echo "Usage: $0 [development|release]" >&2
  exit 2
fi

platform_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
manifest="$platform_dir/platform-sources.json"

jq -e '.schemaVersion == 1 and (.sources | length > 0)' "$manifest" >/dev/null

while IFS= read -r source; do
  id="$(jq -r '.id' <<<"$source")"
  relative_path="$(jq -r '.path' <<<"$source")"
  expected_branch="$(jq -r '.branch' <<<"$source")"
  expected_revision="$(jq -r '.revision' <<<"$source")"
  expected_version="$(jq -r '.mavenVersion' <<<"$source")"
  source_dir="$(cd "$platform_dir/$relative_path" && pwd)"

  actual_branch="$(git -C "$source_dir" branch --show-current)"
  if [[ "$actual_branch" != "$expected_branch" ]]; then
    echo "$id: expected branch $expected_branch, found $actual_branch" >&2
    exit 1
  fi

  actual_version="$(mvn -q -f "$source_dir/pom.xml" help:evaluate \
    -Dexpression=project.version -DforceStdout </dev/null)"
  if [[ "$actual_version" != "$expected_version" ]]; then
    echo "$id: expected Maven version $expected_version, found $actual_version" >&2
    exit 1
  fi

  if [[ "$mode" == "release" ]]; then
    if [[ "$expected_revision" == "WORKTREE" ]]; then
      echo "$id: release manifest requires an immutable Git revision" >&2
      exit 1
    fi
    if [[ "$expected_version" == *-SNAPSHOT ]]; then
      echo "$id: release manifest cannot contain snapshot version $expected_version" >&2
      exit 1
    fi
    if [[ -n "$(git -C "$source_dir" status --porcelain)" ]]; then
      echo "$id: release source tree is dirty" >&2
      exit 1
    fi
    actual_revision="$(git -C "$source_dir" rev-parse HEAD)"
    if [[ "$actual_revision" != "$expected_revision" ]]; then
      echo "$id: expected revision $expected_revision, found $actual_revision" >&2
      exit 1
    fi
  fi

  echo "$id: $expected_version ($expected_revision)"
done < <(jq -c '.sources[]' "$manifest")
