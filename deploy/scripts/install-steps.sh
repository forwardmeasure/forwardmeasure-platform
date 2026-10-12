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
# Shared steps for every install.sh (sourced, not run):
#
#   install_step <label> <command> [args...]
#       Runs the command and prints a timestamped start and finish with the elapsed time, so a
#       slow install shows where its time goes.
#
#   mark_helmfile_prepared <helmfile>
#       Records that a helmfile's chart repositories have been added and its chart dependencies
#       built during this install (any helmfile command run without --skip-deps does both, e.g. the
#       validation render). install-platform.sh and every nested install.sh share the record
#       through FORWARDMEASURE_PREPARED_HELMFILES; later calls for that helmfile pass --skip-deps.
#       Deliberately not `helmfile deps`: that also writes *.lock files, which then freeze chart
#       versions and reject every later pin change in chart-versions.yaml (the pins are already exact).
#
#   reconcile_releases <helmfile> <environment> <selector-or-empty> [extra flags...]
#       Brings the selected releases to the desired state. When every selected release that Helm
#       already knows about is in status "deployed", it runs `helmfile apply`: only releases whose
#       rendered manifests differ from what is deployed are upgraded, so their hooks (migrations,
#       identity reconciliation, workflow publication) run when something they depend on changes,
#       and unchanged releases cost one diff instead of a full upgrade and rollout wait.
#       When any selected release is failed or mid-operation (pending-install, pending-upgrade,
#       pending-rollback, uninstalling, ...), it runs `helmfile sync` instead, the previous
#       behaviour: every selected release is upgraded in order, including unchanged failed ones,
#       so a failed release is retried after its prerequisites rather than skipped as "no change".
#
#   install_prepared
#       True when install-platform.sh has already resolved image digests and validated every
#       repository for this run (FORWARDMEASURE_INSTALL_PREPARED=true); nested install.sh scripts
#       then skip repeating that work. Run on their own, they still do it themselves.

INSTALL_STEPS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

# Refuses to run when helm, helmfile or the helm-diff plugin differ from deploy/tool-versions.env,
# or when helm or helmfile is built for another CPU (an emulated ARM helmfile on x86-64 made every
# install several times slower without any error). Each install script calls it first.
check_install_tools() {
  local versions="${INSTALL_STEPS_DIR}/../tool-versions.env"
  # shellcheck source=../tool-versions.env
  source "${versions}"
  local problems=() binary actual
  for binary in helm helmfile; do
    command -v "${binary}" >/dev/null || { problems+=("${binary} is not installed"); continue; }
    actual="$(python3 - "$(command -v "${binary}")" <<'PY'
import platform, sys
machines = {0x3e: "x86_64", 0xb7: "aarch64"}
with open(sys.argv[1], "rb") as binary:
    header = binary.read(20)
built = machines.get(int.from_bytes(header[18:20], "little"), "unknown") if header[:4] == b"\x7fELF" else "not-elf"
host = {"amd64": "x86_64", "arm64": "aarch64"}.get(platform.machine().lower(), platform.machine().lower())
print("" if built in (host, "not-elf") else f"built for {built}, this machine is {host}")
PY
)"
    [[ -z "${actual}" ]] || problems+=("$(command -v "${binary}") is ${actual}")
  done
  actual="$(helm version --short 2>/dev/null)"
  [[ "${actual}" == "v${HELM_VERSION}+"* || "${actual}" == "v${HELM_VERSION}" ]] \
    || problems+=("helm is ${actual:-missing}, pinned ${HELM_VERSION}")
  actual="$(helmfile --version 2>/dev/null | awk '{print $NF}')"
  [[ "${actual}" == "${HELMFILE_VERSION}" ]] \
    || problems+=("helmfile is ${actual:-missing}, pinned ${HELMFILE_VERSION}")
  actual="$(helm plugin list 2>/dev/null | awk '$1 == "diff" { print $2 }')"
  [[ "${actual}" == "${HELM_DIFF_VERSION}" ]] \
    || problems+=("helm-diff plugin is ${actual:-missing}, pinned ${HELM_DIFF_VERSION}")
  if (( ${#problems[@]} > 0 )); then
    echo "Install tools differ from ${versions}:" >&2
    printf '  %s\n' "${problems[@]}" >&2
    return 1
  fi
}

install_step() {
  local label="$1"
  shift
  local started
  started=$(date +%s)
  echo "[$(date +%H:%M:%S)] ==> ${label}"
  "$@"
  echo "[$(date +%H:%M:%S)] <== ${label} ($(($(date +%s) - started))s)"
}

install_prepared() {
  [[ "${FORWARDMEASURE_INSTALL_PREPARED:-false}" == true ]]
}

helmfile_prepared() {
  case ":${FORWARDMEASURE_PREPARED_HELMFILES:-}:" in
    *":$1:"*) return 0 ;;
  esac
  return 1
}

mark_helmfile_prepared() {
  helmfile_prepared "$1" && return 0
  export FORWARDMEASURE_PREPARED_HELMFILES="${FORWARDMEASURE_PREPARED_HELMFILES:+${FORWARDMEASURE_PREPARED_HELMFILES}:}$1"
}

# Every enabled release of a helmfile as "<namespace>/<name> ,<labels>,", listed once per helmfile
# per install (rendering a large helmfile takes seconds; stages reuse the list). Call it in the
# install's own shell, not inside $( ... ): a subshell's assignment is discarded.
declare -A INSTALL_RELEASE_LISTS=()
load_release_list() {
  local file="$1" environment="$2"
  if [[ -z "${INSTALL_RELEASE_LISTS[${file}]+listed}" ]]; then
    local notices listed
    # Helmfile repeats advisory notices on every list; show them only if the list itself fails.
    notices="$(mktemp)"
    listed="$(helmfile --file "${file}" --environment "${environment}" \
      list --output json --allow-no-matching-release 2>"${notices}")" || {
        cat "${notices}" >&2
        rm -f "${notices}"
        return 1
      }
    rm -f "${notices}"
    INSTALL_RELEASE_LISTS["${file}"]="$(jq -r \
      '.[] | select(.enabled == true) | "\(.namespace)/\(.name) ,\(.labels),"' <<<"${listed}")"
  fi
}

# Every release Helm has, as "<namespace>/<name> <status>", read once per install script: listing
# all releases takes seconds, and within one install a status only changes because a stage of this
# install changed it (a failed stage stops the install), so the first snapshot stays valid for
# deciding later stages. Like load_release_list, call it in the install's own shell.
load_release_statuses() {
  if [[ -z "${INSTALL_RELEASE_STATUSES+loaded}" ]]; then
    # Helm 4 has no --all: name every state a release can be left in.
    INSTALL_RELEASE_STATUSES="$(helm list --all-namespaces --deployed --failed --pending --uninstalling \
      --output json | jq -r '.[] | "\(.namespace)/\(.name) \(.status)"')"
  fi
}

# Prints "<namespace>/<name> (<status>)" for each selected release Helm has, whose status is not
# "deployed". The selector is empty (every release) or one "key=value" label, as the stages use.
unsettled_releases() {
  local file="$1" environment="$2" selector="$3"
  local releases="${INSTALL_RELEASE_LISTS[${file}]?load_release_list must run first}" selected statuses
  if [[ -n "${selector}" ]]; then
    selected="$(awk -v label=",${selector%%=*}:${selector#*=}," 'index($2, label) { print $1 }' <<<"${releases}")"
  else
    selected="$(awk 'NF { print $1 }' <<<"${releases}")"
  fi
  [[ -n "${selected}" ]] || return 0
  statuses="${INSTALL_RELEASE_STATUSES?load_release_statuses must run first}"
  awk 'NR == FNR { status[$1] = $2; next }
       ($1 in status) && status[$1] != "deployed" { print $1 " (" status[$1] ")" }' \
    <(printf '%s\n' "${statuses}") <(printf '%s\n' "${selected}")
}

reconcile_releases() {
  local file="$1" environment="$2" selector="$3"
  shift 3
  # The first call for a helmfile in this install adds its repositories and builds its chart
  # dependencies; every later call skips that.
  local deps=(--skip-deps)
  helmfile_prepared "${file}" || deps=()
  local selection=()
  [[ -n "${selector}" ]] && selection=(--selector "${selector}")
  load_release_list "${file}" "${environment}"
  load_release_statuses
  local unsettled
  unsettled="$(unsettled_releases "${file}" "${environment}" "${selector}")"
  if [[ -n "${unsettled}" ]]; then
    echo "Not all selected releases are deployed, so every one is synchronized in order:"
    sed 's/^/  /' <<<"${unsettled}"
    helmfile --file "${file}" --environment "${environment}" "${selection[@]}" \
      sync "${deps[@]}" --skip-needs=false --wait --wait-for-jobs "$@"
  else
    helmfile --file "${file}" --environment "${environment}" "${selection[@]}" \
      apply "${deps[@]}" --skip-needs=false --wait --wait-for-jobs \
      --suppress-secrets --skip-diff-on-install --context 3 "$@"
  fi
  mark_helmfile_prepared "${file}"
}
