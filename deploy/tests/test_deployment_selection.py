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

"""Selection/transition regression cases. No cluster or credentials are required."""
import importlib.util
import itertools
import json
from pathlib import Path
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"


def module(name):
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), SCRIPTS / (name + ".py"))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


selection = module("deployment-selection")
transition = module("check-deployment-transition")


class DeploymentSelectionTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.config = Path(self.directory.name) / "deployment.yaml"
        self.config.write_text("{}")

    def resolve(self, env=None):
        return selection.resolve("fixture", env or {}, self.config)

    def test_defaults_and_all_eighteen_combinations(self):
        self.assertEqual(("quarkus", "kafka-streams", True),
                         tuple(self.resolve()[key] for key in ("framework", "engine", "enableFde")))
        for framework, engine, fde in itertools.product(selection.CHOICES["framework"],
                                                        selection.CHOICES["engine"], (True, False)):
            with self.subTest(framework=framework, engine=engine, fde=fde):
                actual = self.resolve({"FORWARDMEASURE_FRAMEWORK": framework, "FORWARDMEASURE_ENGINE": engine,
                                       "FORWARDMEASURE_ENABLE_FDE": str(fde).lower()})
                self.assertEqual((framework, engine, fde), tuple(actual[key] for key in ("framework", "engine", "enableFde")))

    def test_environment_then_shared_then_default(self):
        self.config.write_text("framework: spring\nenableFde: false\nenvironments:\n  fixture:\n    engine: pekko-cassandra\n")
        actual = self.resolve({"FORWARDMEASURE_FRAMEWORK": "micronaut"})
        self.assertEqual("micronaut", actual["framework"])
        self.assertFalse(actual["enableFde"])
        self.assertEqual("pekko-cassandra", actual["engine"])
        self.assertEqual("kriyagentic", actual["displayName"])

    def test_snapshot_is_stable_when_files_or_environment_change(self):
        initial = self.resolve()
        self.config.write_text("invalid configuration")
        self.assertEqual(initial, self.resolve({selection.SNAPSHOT: json.dumps(initial),
                                               "FORWARDMEASURE_FRAMEWORK": "spring"}))
        with self.assertRaises(ValueError):
            selection.resolve("different-cluster", {selection.SNAPSHOT: json.dumps(initial)}, self.config)

    def test_invalid_values_fail_without_falling_back(self):
        for variable, value in (("FORWARDMEASURE_ENABLE_FDE", "yes"), ("FORWARDMEASURE_ENGINE", "pekko"),
                                ("FORWARDMEASURE_FRAMEWORK", ""), ("FORWARDMEASURE_NAMESPACE_LAYOUT", "other")):
            with self.subTest(variable=variable), self.assertRaises(ValueError):
                self.resolve({variable: value})

    def test_shared_false_is_not_replaced_with_default_true(self):
        self.config.write_text("enableFde: false")
        self.assertFalse(self.resolve()["enableFde"])

    def test_malformed_snapshot_and_nonprintable_brand_fail_validation(self):
        for snapshot in ("null", "[]", '"value"'):
            with self.subTest(snapshot=snapshot), self.assertRaises(ValueError):
                self.resolve({selection.SNAPSHOT: snapshot})
        with self.assertRaises(ValueError):
            self.resolve({"FORWARDMEASURE_DISPLAY_NAME": "brand\x7f"})

    def test_engine_and_namespace_changes_are_blocked(self):
        current = self.resolve()
        self.assertEqual([], transition.violations(current, [], None))
        releases = [{"name": "openworkflow-engine-pekko-postgresql", "namespace": "forwardmeasure-openworkflow"},
                    {"name": "entity-intelligence-services", "namespace": "entity-intelligence"}]
        self.assertEqual(2, len(transition.violations(current, releases, None)))
        self.assertTrue(transition.violations(current, [], {**current, "engine": "pekko-cassandra"}))

    def test_disabled_selection_does_not_silently_abandon_live_fde(self):
        current = self.resolve({"FORWARDMEASURE_ENABLE_FDE": "false"})
        errors = transition.violations(current, [{"name": "decision-engine", "namespace": "forwardmeasure-decision-engine"}], None)
        self.assertEqual(1, len(errors))


if __name__ == "__main__":
    unittest.main()
