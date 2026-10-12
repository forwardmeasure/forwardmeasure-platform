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
from types import SimpleNamespace
import unittest

spec = importlib.util.spec_from_file_location(
    "sync_platform_secrets", Path(__file__).parents[1] / "sync-platform-secrets.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def platform_item(namespace, name="client-credentials", owner="platform-secrets",
                  store="fake-secret-store", kind="ClusterSecretStore"):
    return {"metadata": {"namespace": namespace, "name": name,
                         "labels": {"app.kubernetes.io/managed-by": "Helm"},
                         "annotations": {"meta.helm.sh/release-name": owner,
                                         "meta.helm.sh/release-namespace": "external-secrets"}},
            "spec": {"secretStoreRef": {"name": store, "kind": kind}}}


class FakeCluster:
    def __init__(self, accept=False):
        self.accept = accept
        self.markers = {}
        self.clock = 0
        self.reads = []
        self.deleted = []
        self.reported = []
        self.items = [platform_item(ns) for ns in ("keycloak", "data-streaming", "entity-intelligence")]
        # What `helm get manifest platform-secrets` returns: the deployed revision's objects.
        self.released = {(ns, "client-credentials") for ns in ("keycloak", "data-streaming", "entity-intelligence")}

    def helm(self, *args):
        assert args[:3] == ("get", "manifest", "platform-secrets"), args
        assert args[3:] == ("--namespace", "external-secrets"), args
        documents = ["apiVersion: v1\nkind: ConfigMap\nmetadata: {name: unrelated}"]
        documents += [f"apiVersion: external-secrets.io/v1\nkind: ExternalSecret\n"
                      f"metadata: {{name: {name}, namespace: {ns}}}" for ns, name in sorted(self.released)]
        return "\n---\n".join(documents)

    def command(self, *args, optional=False):
        if args[:2] == ("get", "externalsecrets"):
            items = self.items
            if "-l" in args:
                for selector in args[args.index("-l") + 1].split(","):
                    key, value = selector.split("=", 1)
                    items = [item for item in items if item["metadata"]["labels"].get(key) == value]
            return json.dumps({"items": items})
        ns = args[1]
        if args[2] == "delete":
            self.deleted.append((ns, args[4]))
            self.items = [item for item in self.items
                          if (item["metadata"]["namespace"], item["metadata"]["name"]) != (ns, args[4])]
            return "deleted"
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
            cluster.items.append(platform_item(namespace, owner=owner, store=store, kind=kind))
        self.assertEqual(3, module.sync("fake-secret-store", command=cluster.command,
                                        helm_command=cluster.helm, report=cluster.reported.append))
        self.assertEqual({"keycloak", "data-streaming", "entity-intelligence"}, set(cluster.markers))
        self.assertEqual([], cluster.deleted)

    def test_old_ready_condition_does_not_satisfy_the_barrier(self):
        cluster = FakeCluster()
        with self.assertRaisesRegex(RuntimeError, "did not apply"):
            module.sync("fake-secret-store", 3, cluster.command, lambda: cluster.clock, cluster.sleep,
                        cluster.helm, cluster.reported.append)
        self.assertEqual({"keycloak", "data-streaming", "entity-intelligence"}, set(cluster.markers))
        self.assertTrue(all(".metadata.annotations" in args[-1] for args in cluster.reads))

    def test_interrupted_synchronization_can_resume_with_a_new_request(self):
        cluster = FakeCluster()
        with self.assertRaises(RuntimeError):
            module.sync("fake-secret-store", 1, cluster.command, lambda: cluster.clock, cluster.sleep,
                        cluster.helm, cluster.reported.append)
        old = dict(cluster.markers)
        cluster.accept = True
        self.assertEqual(3, module.sync("fake-secret-store", 3, cluster.command,
                                        lambda: cluster.clock, cluster.sleep,
                                        cluster.helm, cluster.reported.append))
        self.assertTrue(all(old[ns] != marker for ns, marker in cluster.markers.items()))


    def test_orphan_from_a_failed_upgrade_is_removed_and_not_waited_on(self):
        # A failed upgrade created this ExternalSecret; the next successful revision no longer
        # renders it, so Helm never deleted it. It still carries the release annotation.
        cluster = FakeCluster(accept=True)
        cluster.items.append(platform_item("entity-intelligence", "entity-intelligence-opensearch-ca"))
        self.assertEqual(3, module.sync("fake-secret-store", 3, cluster.command,
                                        lambda: cluster.clock, cluster.sleep,
                                        cluster.helm, cluster.reported.append))
        self.assertEqual([("entity-intelligence", "entity-intelligence-opensearch-ca")], cluster.deleted)
        self.assertEqual(1, len(cluster.reported))
        self.assertIn("entity-intelligence/entity-intelligence-opensearch-ca", cluster.reported[0])
        self.assertEqual({"keycloak", "data-streaming", "entity-intelligence"}, set(cluster.markers))

    def test_objects_of_other_releases_are_never_removed(self):
        cluster = FakeCluster(accept=True)
        cluster.items.append(platform_item("entity-intelligence", "entity-intelligence-opensearch-ca",
                                           owner="entity-intelligence-opensearch-ca"))
        module.sync("fake-secret-store", 3, cluster.command, lambda: cluster.clock, cluster.sleep,
                    cluster.helm, cluster.reported.append)
        self.assertEqual([], cluster.deleted)

    def test_an_empty_release_manifest_removes_nothing(self):
        cluster = FakeCluster(accept=True)
        cluster.released = set()
        with self.assertRaisesRegex(RuntimeError, "has no ExternalSecrets"):
            module.sync("fake-secret-store", 3, cluster.command, lambda: cluster.clock, cluster.sleep,
                        cluster.helm, cluster.reported.append)
        self.assertEqual([], cluster.deleted)
        self.assertEqual({}, cluster.markers)

class TransientRetryTest(unittest.TestCase):
    """The kubectl wrapper retries the API server's transient failures, and only those."""

    def run_with(self, stderrs):
        calls, delays = [], []

        def run(args, **_):
            calls.append(args)
            stderr = stderrs[min(len(calls), len(stderrs)) - 1]
            return SimpleNamespace(returncode=1 if stderr else 0, stdout="ok", stderr=stderr or "")

        result = module.run_kubectl(("-n", "fei", "rollout", "status", "deployment/x"), delays.append, run)
        return result, calls, delays

    def test_unavailable_api_server_is_retried_until_it_answers(self):
        unavailable = ('Error from server (InternalError): an error on the server ("Internal Server Error: '
                       '\\"/apis/apps/v1/namespaces/fei/deployments/x\\": the server is currently unable '
                       'to handle the request")')
        result, calls, delays = self.run_with([unavailable, unavailable, None])
        self.assertEqual(0, result.returncode)
        self.assertEqual(3, len(calls))
        self.assertEqual([2, 4], delays)

    def test_real_errors_fail_without_retrying(self):
        for stderr in ('Error from server (NotFound): deployments.apps "x" not found',
                       'Error from server (Forbidden): deployments.apps "x" is forbidden',
                       'error: timed out waiting for the condition'):
            result, calls, delays = self.run_with([stderr])
            self.assertEqual(1, result.returncode)
            self.assertEqual(1, len(calls), stderr)
            self.assertEqual([], delays)

    def test_a_server_that_stays_unavailable_fails_after_the_last_retry(self):
        result, calls, delays = self.run_with(["Error from server (ServiceUnavailable): the server is "
                                               "currently unable to handle the request"])
        self.assertEqual(1, result.returncode)
        self.assertEqual(len(module.RETRY_DELAYS) + 1, len(calls))
        self.assertEqual(list(module.RETRY_DELAYS), delays)

if __name__ == "__main__":
    unittest.main()
