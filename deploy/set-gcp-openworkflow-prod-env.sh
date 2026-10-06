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
# No environment setup is required for gcp-openworkflow-prod.
# Image tags/digests: forwardmeasure-openworkflow/deploy/helmfile/environments/image-versions.yaml
# Shared images: forwardmeasure-platform/deploy/helmfile/shared/image-versions.yaml
# Cluster facts: forwardmeasure-platform/deploy/helmfile/shared/clusters/gcp-openworkflow-prod.yaml.gotmpl
# Optional product selection: export FORWARDMEASURE_ENABLE_FDE=true
# Optional operator-owned egress policy (unset means no extra destinations):
# export OPENWORKFLOW_EXTERNAL_EGRESS_CIDR=<approved-cidr>
# export OPENWORKFLOW_HTTP_EGRESS_ALLOWLIST='<tenant-identity>=<approved-host>'
# The platform's registry/identity resource hosts are derived from shared configuration.
