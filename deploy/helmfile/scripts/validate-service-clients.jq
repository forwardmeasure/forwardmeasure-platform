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
(.tenants | type == "array" and length > 0)
  and (.platform.identity.serviceClients | type == "object" and length > 0)
  and (.platform.identity.serviceClients | all(.[]; .enabled | type == "boolean"))
  and (.platform.identity.serviceClients | [.[] | select(.enabled)] | . as $clients |
    (length > 0) and (map(.clientId) | length == (unique | length))
    and (map(.secretKey) | length == (unique | length))
    and all(.[];
      (.name | type == "string" and length > 0)
      and (.clientId | type == "string" and test("^[A-Za-z0-9._-]+$"))
      and (.secretKey | type == "string" and test("^[A-Z0-9_]+$")
        and . != "FORWARDMEASURE_ADMIN_CONFIDENTIAL_CLIENT_SECRET")
      and (.secretRemoteKey | type == "string" and length > 0)
      and (.authorizationServicesEnabled | type == "boolean")
      and (.organizationClaim | type == "boolean")))
