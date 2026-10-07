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
"""Render the gateway's AuthZEN request-ID contract without credentials or a cluster."""
from pathlib import Path
import subprocess
import tempfile
import unittest

import yaml

CHART = Path(__file__).resolve().parents[2] / "charts/platform-bootstrap"


class GatewayRequestIdTest(unittest.TestCase):
    def render_filters(self, enabled=True, class_name="istio", preserve=True):
        values = {"gateway": {"enabled": enabled, "className": class_name,
                              "name": "fixture-gateway", "namespace": "fixture-istio",
                              "listeners": [
                                  {"name": "auth-https", "hostname": "auth.example.test",
                                   "tlsSecret": "auth-tls", "preserveRequestId": preserve},
                                  {"name": "registry-https", "hostname": "registry.example.test",
                                   "tlsSecret": "registry-tls"}]}}
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "values.yaml"
            path.write_text(yaml.safe_dump(values))
            result = subprocess.run(["helm", "template", "fixture", str(CHART), "-f", str(path)],
                                    capture_output=True, text=True, check=True)
        return [item for item in yaml.safe_load_all(result.stdout)
                if item and item["kind"] == "EnvoyFilter"]

    def test_only_selected_https_listener_preserves_exact_request_ids(self):
        filters = self.render_filters()
        self.assertEqual(1, len(filters))
        spec = filters[0]["spec"]
        self.assertEqual("fixture-istio", filters[0]["metadata"]["namespace"])
        self.assertEqual({"gateway.networking.k8s.io/gateway-name": "fixture-gateway"},
                         spec["workloadSelector"]["labels"])
        patch = spec["configPatches"][0]
        self.assertEqual("NETWORK_FILTER", patch["applyTo"])
        self.assertEqual("GATEWAY", patch["match"]["context"])
        self.assertEqual(443, patch["match"]["listener"]["portNumber"])
        self.assertEqual("auth.example.test", patch["match"]["listener"]["filterChain"]["sni"])
        self.assertEqual("MERGE", patch["patch"]["operation"])
        config = patch["patch"]["value"]["typed_config"]
        self.assertTrue(config["preserve_external_request_id"])
        uuid_config = config["request_id_extension"]["typed_config"]
        self.assertFalse(uuid_config["pack_trace_reason"])
        self.assertFalse(uuid_config["use_request_id_for_trace_sampling"])

    def test_no_filter_for_disabled_gateway_other_controller_or_unselected_listener(self):
        self.assertEqual([], self.render_filters(enabled=False))
        self.assertEqual([], self.render_filters(class_name="another-controller"))
        self.assertEqual([], self.render_filters(preserve=False))
