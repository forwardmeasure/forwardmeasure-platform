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
"""Real ESO contract. Requires IDENTITY_TEST_CONTEXT pointing to a disposable cluster with ESO 2.0.1.

Only synthetic values are used. This suite creates isolated namespaces and a fake ClusterSecretStore;
it does not read a credential file or contact Secret Manager. Run explicitly, never against production.
"""
import base64
import copy
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import time
import unittest
import uuid

SCRIPTS = Path(__file__).parents[1]


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


fixture = load("identity_fixture", SCRIPTS / "render-identity-fixture.py")
barrier = load("secret_barrier", SCRIPTS / "sync-platform-secrets.py")
contracts = load("identity_contracts", SCRIPTS / "tests/test_identity_manifests.py")


class ExternalSecretIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.context = os.environ.get("IDENTITY_TEST_CONTEXT")
        if not cls.context:
            raise RuntimeError("Set IDENTITY_TEST_CONTEXT to an explicitly selected disposable ESO test cluster")
        cls.prefix = "identity-test-" + uuid.uuid4().hex[:10]
        cls.store = cls.prefix + "-store"
        cls.namespaces = set()
        cls.targets = []
        cls.values = {}
        cls.addClassCleanup(cls.cleanup)
        _, _, manifests = fixture.render()
        for manifest in manifests:
            if not manifest or manifest.get("kind") != "ExternalSecret":
                continue
            name = manifest["metadata"]["name"]
            if name not in contracts.EXPECTED_KEYS:
                continue
            ns = cls.prefix + "-" + str(len(cls.namespaces))
            cls.command("create", "namespace", ns)
            cls.namespaces.add(ns)
            manifest["metadata"]["namespace"] = ns
            manifest["spec"]["secretStoreRef"] = {"kind": "ClusterSecretStore", "name": cls.store}
            manifest["spec"]["target"]["creationPolicy"] = "Owner"
            manifest["spec"]["refreshInterval"] = "1s"
            for entry in manifest["spec"]["data"]:
                cls.values[entry["remoteRef"]["key"]] = 'synthetic-quote"-slash\\-newline\n-v1'
            cls.targets.append(manifest)
        cls.apply_store()
        for manifest in cls.targets:
            ns, name = manifest["metadata"]["namespace"], manifest["metadata"]["name"]
            # Simulates the old product Job's ownerless Secret, including obsolete extra keys.
            cls.apply({"apiVersion": "v1", "kind": "Secret", "metadata": {"namespace": ns, "name": name},
                       "stringData": {"obsolete-job-key": "fixture-only"}})
            cls.apply(manifest)

    @classmethod
    def command(cls, *args, optional=False, payload=None):
        result = subprocess.run(["kubectl", "--context", cls.context, *args], input=payload,
                                text=True, capture_output=True)
        if result.returncode:
            if optional and "(NotFound)" in result.stderr:
                return ""
            raise RuntimeError(result.stderr)
        # Limit the production helper's discovery to this fixture's namespaces.
        if args[:2] == ("get", "externalsecrets"):
            data = json.loads(result.stdout)
            data["items"] = [item for item in data["items"] if item["metadata"]["namespace"] in cls.namespaces]
            return json.dumps(data)
        return result.stdout.strip()

    @classmethod
    def apply(cls, value):
        return cls.command("apply", "-f", "-", payload=json.dumps(value))

    @classmethod
    def apply_store(cls):
        cls.apply({"apiVersion": "external-secrets.io/v1", "kind": "ClusterSecretStore",
                   "metadata": {"name": cls.store}, "spec": {"provider": {"fake": {"data": [
                       {"key": key, "value": value} for key, value in cls.values.items()]}}}})

    @classmethod
    def cleanup(cls):
        if getattr(cls, "context", None):
            for ns in cls.namespaces:
                cls.command("delete", "namespace", ns, "--ignore-not-found", "--wait=false")
            cls.command("delete", "clustersecretstore", cls.store, "--ignore-not-found")

    def check_outputs(self, suffix):
        for manifest in self.targets:
            ns, name = manifest["metadata"]["namespace"], manifest["metadata"]["name"]
            secret = json.loads(self.command("-n", ns, "get", "secret", name, "-o", "json"))
            data = {key: base64.b64decode(value).decode() for key, value in secret["data"].items()}
            self.assertEqual(contracts.EXPECTED_KEYS[name], set(data))
            self.assertTrue(any(owner["kind"] == "ExternalSecret" for owner in secret["metadata"]["ownerReferences"]))
            if name == "openworkflow-platform-clients":
                composed = json.loads(data["OPENWORKFLOW_DEFINITION_RESOURCE_AUTH_SECRETS"])
                self.assertEqual('synthetic-quote"-slash\\-newline\n-' + suffix,
                                 composed["apicurio-registry-reader"]["clientSecret"])
            for key, value in data.items():
                if key.lower().endswith("secret"):
                    self.assertTrue(value.endswith(suffix), key)

    def test_adoption_exact_keys_json_escaping_rotation_and_reentry(self):
        self.assertEqual(3, barrier.sync(self.store, 90, self.command))
        self.check_outputs("v1")
        for key in self.values:
            self.values[key] = self.values[key][:-2] + "v2"
        self.apply_store()
        # Repeat the same intended version, as after a caller dies following the provider update.
        self.apply_store()
        self.assertEqual(3, barrier.sync(self.store, 90, self.command))
        self.check_outputs("v2")
        self.assertEqual(3, barrier.sync(self.store, 90, self.command))
        self.check_outputs("v2")

    def test_foreign_owned_secret_is_not_deleted_or_taken_over(self):
        manifest = copy.deepcopy(self.targets[0])
        ns = manifest["metadata"]["namespace"]
        name = "foreign-owned-credentials"
        manifest["metadata"]["name"] = name
        manifest["spec"]["target"]["name"] = name
        self.apply({"apiVersion": "v1", "kind": "ConfigMap", "metadata": {"namespace": ns, "name": "foreign-owner"}})
        owner = json.loads(self.command("-n", ns, "get", "configmap", "foreign-owner", "-o", "json"))
        self.apply({"apiVersion": "v1", "kind": "Secret", "metadata": {"namespace": ns, "name": name,
                    "ownerReferences": [{"apiVersion": "v1", "kind": "ConfigMap", "name": "foreign-owner",
                                         "uid": owner["metadata"]["uid"], "controller": True}]},
                    "stringData": {"foreign": "do-not-delete"}})
        self.apply(manifest)
        try:
            deadline = time.monotonic() + 45
            while True:
                state = json.loads(self.command("-n", ns, "get", "externalsecret", name, "-o", "json"))
                if any(condition.get("type") == "Ready" and condition.get("status") == "False"
                       for condition in state.get("status", {}).get("conditions", [])):
                    break
                if time.monotonic() >= deadline:
                    self.fail("ESO did not report the ownership conflict")
                time.sleep(1)
            secret = json.loads(self.command("-n", ns, "get", "secret", name, "-o", "json"))
            self.assertEqual({"foreign"}, set(secret["data"]))
            self.assertEqual(owner["metadata"]["uid"], secret["metadata"]["ownerReferences"][0]["uid"])
        finally:
            self.command("-n", ns, "delete", "externalsecret", name, "--ignore-not-found")
