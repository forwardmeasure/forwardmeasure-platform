import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("reload_consumers", Path(__file__).parents[1] / "reload-secret-consumers.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Cluster:
    def __init__(self):
        self.hash = "original"
        self.calls = []
        self.interrupt = False
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
