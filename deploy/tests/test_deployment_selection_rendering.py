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

"""Opt-in Helmfile build coverage for the complete selection matrix; never installs releases."""
import importlib.util
import itertools
import json
import os
from pathlib import Path
import subprocess
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("deployment_selection", ROOT / "deploy/scripts/deployment-selection.py")
resolver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resolver)


class DeploymentSelectionRenderingTest(unittest.TestCase):
    def test_every_product_consumes_the_same_snapshot_in_all_eighteen_combinations(self):
        environment = "gcp-greenfield-example"
        repos = ("forwardmeasure-platform", "forwardmeasure-openworkflow", "forwardmeasure-data-streaming",
                 "forwardmeasure-entity-intelligence", "forwardmeasure-decision-engine")
        for framework, engine, fde in itertools.product(resolver.CHOICES["framework"],
                                                        resolver.CHOICES["engine"], (True, False)):
            selection = resolver.resolve(environment, {"FORWARDMEASURE_FRAMEWORK": framework,
                "FORWARDMEASURE_ENGINE": engine, "FORWARDMEASURE_ENABLE_FDE": str(fde).lower()})
            # Contradictory inherited overrides must not change an already frozen invocation.
            env = {**os.environ, resolver.SNAPSHOT: json.dumps(selection),
                   "FORWARDMEASURE_FRAMEWORK": "ignored-after-resolution"}
            for repo in repos:
                with self.subTest(repo=repo, framework=framework, engine=engine, fde=fde):
                    result = subprocess.run(["helmfile", "-f", "deploy/helmfile/helmfile.yaml.gotmpl",
                                             "-e", environment, "build"], cwd=ROOT.parent / repo,
                                            env=env, capture_output=True, text=True, timeout=60)
                    self.assertEqual(0, result.returncode, result.stderr)
                    values = [document["renderedvalues"]["platform"] for document in yaml.safe_load_all(result.stdout)
                              if document and "renderedvalues" in document]
                    self.assertTrue(values)
                    for platform in values:
                        self.assertEqual(framework, platform["framework"])
                        self.assertEqual(engine, platform["engine"])
                        self.assertEqual(fde, platform["products"]["decisionEngine"]["enabled"])
                        self.assertEqual("forwardmeasure-entity-intelligence", platform["namespaces"]["entityIntelligence"])


if __name__ == "__main__":
    unittest.main()
