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
import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace
import unittest

spec = importlib.util.spec_from_file_location("reload_consumers", Path(__file__).parents[1] / "reload-secret-consumers.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Cluster:
    def __init__(self):
        self.hash = "original"
        self.calls = []
        self.interrupt = False
        self.secrets = {("fei", "credentials")}
        self.items = [{"metadata": {"namespace": "fei", "name": "credentials",
                                   "labels": {"app.kubernetes.io/managed-by": "Helm",
                                              "platform.forwardmeasure.com/identity-client-credentials": "true"},
                                   "annotations": {"meta.helm.sh/release-name": "platform-secrets"}},
                       "spec": {"data": [{"remoteRef": {"key": "fixture-client-secret"}}]}}]
        self.controller = {"kind": "Deployment", "metadata": {"namespace": "fei", "name": "ingestion"},
                           "spec": {"template": {"metadata": {}, "spec": {"containers": [{"env": [
                               {"valueFrom": {"secretKeyRef": {"name": "credentials", "key": "client-secret"}}}]}]}}}}

    def command(self, *args):
        self.calls.append(args)
        if args[:2] == ("get", "secrets"):
            assert "-o" not in args, "Secret listing must not request object data"
            return "\n".join(f"{namespace} {name}   Opaque   1   1d" for namespace, name in sorted(self.secrets))
        if args[:2] == ("get", "externalsecrets"):
            items = self.items
            if "-l" in args:
                for selector in args[args.index("-l") + 1].split(","):
                    key, value = selector.split("=", 1)
                    items = [item for item in items if item["metadata"]["labels"].get(key) == value]
            return json.dumps({"items": items})
        if args[0] == "get":
            return json.dumps({"items": [self.controller]})
        if args[2] == "get":
            assert ".metadata.annotations" in args[-1]
            return self.hash
        if args[2] == "patch":
            self.controller["spec"]["template"]["metadata"].update(json.loads(args[-1])["spec"]["template"]["metadata"])
            return "patched"
        if args[2] == "rollout":
            if self.interrupt:
                raise RuntimeError("interrupted after patch")
            return "ready"
        raise AssertionError(args)

    def patches(self):
        return sum(len(call) > 2 and call[2] == "patch" for call in self.calls)


class ReloadTest(unittest.TestCase):
    def test_another_helm_release_is_not_reloaded(self):
        cluster = Cluster()
        cluster.items[0]["metadata"]["annotations"]["meta.helm.sh/release-name"] = "another-release"
        with self.assertRaisesRegex(RuntimeError, "No identity client ExternalSecrets"):
            module.reload(cluster.command)
        self.assertEqual(0, cluster.patches())

    def test_repeated_install_does_not_roll_unchanged_secrets(self):
        cluster = Cluster()
        module.reload(cluster.command)
        module.reload(cluster.command)
        self.assertEqual(1, cluster.patches())
        cluster.hash = "rotated"
        module.reload(cluster.command)
        self.assertEqual(2, cluster.patches())

    def test_interrupted_rollout_is_waited_on_without_patching_again(self):
        cluster = Cluster()
        cluster.interrupt = True
        with self.assertRaisesRegex(RuntimeError, "interrupted"):
            module.reload(cluster.command)
        cluster.interrupt = False
        module.reload(cluster.command)
        self.assertEqual(1, cluster.patches())
        self.assertEqual(2, sum(len(call) > 2 and call[2] == "rollout" for call in cluster.calls))

    def test_running_jobs_are_never_replayed(self):
        cluster = Cluster()
        cluster.controller["kind"] = "Job"
        cluster.controller["status"] = {"active": 1}
        with self.assertRaisesRegex(RuntimeError, "Drain active Job"):
            module.reload(cluster.command)
        self.assertEqual(0, cluster.patches())

    def test_early_pass_defers_controller_requiring_renamed_secret(self):
        cluster = Cluster()
        containers = cluster.controller["spec"]["template"]["spec"]["containers"]
        containers[0]["env"].append({"valueFrom": {"secretKeyRef": {"name": "renamed-away", "key": "k"}}})
        self.assertEqual(0, module.reload(cluster.command, defer_missing=True))
        self.assertEqual(0, cluster.patches())
        self.assertFalse(any(len(call) > 2 and call[2] == "rollout" for call in cluster.calls))
        # The final pass stays strict: it patches and waits, so a Secret still missing fails rollout.
        module.reload(cluster.command)
        self.assertEqual(1, cluster.patches())

    def test_optional_missing_secret_does_not_defer(self):
        cluster = Cluster()
        cluster.controller["spec"]["template"]["spec"]["volumes"] = [
            {"name": "tenant", "secret": {"secretName": "tenant-secrets", "optional": True}}]
        self.assertEqual(1, module.reload(cluster.command, defer_missing=True))
        self.assertEqual(1, cluster.patches())


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
