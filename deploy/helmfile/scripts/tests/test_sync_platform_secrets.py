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
"""Contract tests for synchronization barriers; no cluster or credential access."""
import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    "sync_platform_secrets", Path(__file__).parents[1] / "sync-platform-secrets.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class FakeCluster:
    def __init__(self, accept=False):
        self.accept = accept
        self.markers = {}
        self.clock = 0
        self.reads = []
        self.items = [
            {"metadata": {"namespace": ns, "name": "client-credentials",
                          "labels": {"app.kubernetes.io/managed-by": "Helm"},
                          "annotations": {"meta.helm.sh/release-name": "platform-secrets"}},
             "spec": {"secretStoreRef": {"name": "fake-secret-store", "kind": "ClusterSecretStore"}}}
            for ns in ("keycloak", "data-streaming", "entity-intelligence")]

    def command(self, *args, optional=False):
        if args[:2] == ("get", "externalsecrets"):
            items = self.items
            if "-l" in args:
                for selector in args[args.index("-l") + 1].split(","):
                    key, value = selector.split("=", 1)
                    items = [item for item in items if item["metadata"]["labels"].get(key) == value]
            return json.dumps({"items": items})
        ns = args[1]
        if args[2] == "patch":
            patch = json.loads(args[-1])
            self.markers[ns] = patch["spec"]["target"]["template"]["metadata"]["annotations"][module.MARKER]
            return "patched"
        if args[3] == "secret":
            self.reads.append(args)
            return self.markers[ns] if self.accept else "previous-install"
        if args[3] == "externalsecret":
            return json.dumps({"status": {"conditions": [{"type": "Ready", "status": "True"}]}})
        raise AssertionError(args)

    def sleep(self, seconds):
        self.clock += seconds


class SynchronizationTest(unittest.TestCase):
    def test_helm_owned_resources_are_selected_and_other_owners_and_stores_are_excluded(self):
        cluster = FakeCluster(accept=True)
        for namespace, owner, store, kind in (
                ("foreign", "another-release", "fake-secret-store", "ClusterSecretStore"),
                ("other-store", "platform-secrets", "another-store", "ClusterSecretStore"),
                ("namespaced-store", "platform-secrets", "fake-secret-store", "SecretStore")):
            cluster.items.append({
                "metadata": {"namespace": namespace, "name": "client-credentials",
                             "labels": {"app.kubernetes.io/managed-by": "Helm"},
                             "annotations": {"meta.helm.sh/release-name": owner}},
                "spec": {"secretStoreRef": {"name": store, "kind": kind}}})
        self.assertEqual(3, module.sync("fake-secret-store", command=cluster.command))
        self.assertEqual({"keycloak", "data-streaming", "entity-intelligence"}, set(cluster.markers))

    def test_old_ready_condition_does_not_satisfy_the_barrier(self):
        cluster = FakeCluster()
        with self.assertRaisesRegex(RuntimeError, "did not apply"):
            module.sync("fake-secret-store", 3, cluster.command, lambda: cluster.clock, cluster.sleep)
        self.assertEqual({"keycloak", "data-streaming", "entity-intelligence"}, set(cluster.markers))
        self.assertTrue(all(".metadata.annotations" in args[-1] for args in cluster.reads))

    def test_interrupted_synchronization_can_resume_with_a_new_request(self):
        cluster = FakeCluster()
        with self.assertRaises(RuntimeError):
            module.sync("fake-secret-store", 1, cluster.command, lambda: cluster.clock, cluster.sleep)
        old = dict(cluster.markers)
        cluster.accept = True
        self.assertEqual(3, module.sync("fake-secret-store", 3, cluster.command,
                                        lambda: cluster.clock, cluster.sleep))
        self.assertTrue(all(old[ns] != marker for ns, marker in cluster.markers.items()))


if __name__ == "__main__":
    unittest.main()
