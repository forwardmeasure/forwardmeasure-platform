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

if [[ $# -lt 2 || $# -gt 3 ]]; then
  printf 'Usage: %s <gcp-project-id> <secret-id> [value-file|-]\n' "$0" >&2
  exit 2
fi

PROJECT_ID="$1"
SECRET_ID="$2"
VALUE_FILE="${3:--}"

gcloud secrets describe "${SECRET_ID}" --project "${PROJECT_ID}" >/dev/null

if [[ "${VALUE_FILE}" == "-" ]]; then
  if [[ ! -t 0 ]]; then
    gcloud secrets versions add "${SECRET_ID}" \
      --project "${PROJECT_ID}" --data-file=-
  else
    printf 'Enter the value for %s, then press Ctrl-D:\n' "${SECRET_ID}" >&2
    gcloud secrets versions add "${SECRET_ID}" \
      --project "${PROJECT_ID}" --data-file=-
  fi
else
  gcloud secrets versions add "${SECRET_ID}" \
    --project "${PROJECT_ID}" --data-file="${VALUE_FILE}"
fi
