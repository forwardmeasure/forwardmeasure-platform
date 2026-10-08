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
"""Offline contracts for the actual central values and ExternalSecret chart templates."""
import copy
import importlib.util
import json
import itertools
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import yaml

SCRIPTS = Path(__file__).parents[1]
spec = importlib.util.spec_from_file_location("identity_fixture", SCRIPTS / "render-identity-fixture.py")
fixture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixture)

EXPECTED_KEYS = {
    "openworkflow-service-client-credentials": {"OPENWORKFLOW_SERVICE_CLIENT_CREDENTIALS"},
    "openworkflow-platform-clients": {
        "OPENWORKFLOW_PLATFORM_OPERATOR_CLIENT_ID", "OPENWORKFLOW_PLATFORM_OPERATOR_CLIENT_SECRET",
        "OPENWORKFLOW_WORKFLOW_PUBLISHER_CLIENT_ID", "OPENWORKFLOW_WORKFLOW_PUBLISHER_CLIENT_SECRET",
        "OPENWORKFLOW_DEFINITION_RESOURCE_AUTH_SECRETS"},
    "data-streaming-launcher-credentials": {
        "authorization-client-secret", "fowf-keycloak-client-id", "fowf-keycloak-client-secret"},
    "entity-intelligence-credentials": {
        "keycloak-client-secret", "fowf-keycloak-client-secret",
        "ENTITY_INTELLIGENCE_INGESTION_WORKER_KEYCLOAK_CLIENT_ID",
        "ENTITY_INTELLIGENCE_INGESTION_WORKER_KEYCLOAK_CLIENT_SECRET",
        "ENTITY_INTELLIGENCE_SCREENING_WORKER_KEYCLOAK_CLIENT_ID",
        "ENTITY_INTELLIGENCE_SCREENING_WORKER_KEYCLOAK_CLIENT_SECRET"}}


class ManifestTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.values, cls.secret_values, cls.manifests = fixture.render()

    def valid(self, values):
        result = subprocess.run(["jq", "-e", "-f", str(SCRIPTS / "validate-service-clients.jq")],
                                input=json.dumps(values), text=True, capture_output=True)
        return result.returncode == 0

    def test_inventory_accepts_false_but_rejects_strings_duplicates_and_reserved_keys(self):
        self.assertTrue(self.valid(self.values))
        for field, value in (("authorizationServicesEnabled", "false"), ("enabled", "true"),
                             ("clientId", "openworkflow"), ("secretKey", "OPENWORKFLOW_CLIENT_SECRET"),
                             ("secretKey", "FORWARDMEASURE_ADMIN_CONFIDENTIAL_CLIENT_SECRET")):
            values = copy.deepcopy(self.values)
            values["platform"]["identity"]["serviceClients"]["workflowPublisher"][field] = value
            self.assertFalse(self.valid(values), (field, value))

    def test_composed_secrets_enumerate_every_final_consumer_key(self):
        found = set()
        for manifest in self.manifests:
            if not manifest or manifest["kind"] != "ExternalSecret":
                continue
            name = manifest["metadata"]["name"]
            if name in EXPECTED_KEYS:
                found.add(name)
                self.assertEqual(EXPECTED_KEYS[name], set(manifest["spec"]["target"]["template"]["data"]))
        self.assertEqual(set(EXPECTED_KEYS), found)

    def test_operation_callback_tokens_render_with_and_without_decision_engine(self):
        for fde in (True, False):
            with self.subTest(decision_engine=fde):
                values, _, manifests = fixture.render(decision_engine_enabled=fde)
                secret = next(item for item in manifests if item and item["kind"] == "ExternalSecret"
                              and item["metadata"]["name"] == "openworkflow-service-client-credentials")
                self.assertEqual(values["namespaces"]["openworkflow"], secret["metadata"]["namespace"])
                data = secret["spec"]["target"]["template"]["data"]
                # Evaluate the actual ESO Go template with synthetic remote-secret values. This
                # catches invalid nesting/escaping and verifies the JSON the adapter will receive.
                with tempfile.TemporaryDirectory(prefix="operation-clients-render-") as directory:
                    chart = Path(directory)
                    (chart / "templates").mkdir()
                    (chart / "Chart.yaml").write_text("apiVersion: v2\nname: fixture\nversion: 1.0.0\n")
                    (chart / "values.yaml").write_text(yaml.safe_dump({"credentialTemplate":
                        data["OPENWORKFLOW_SERVICE_CLIENT_CREDENTIALS"]}))
                    (chart / "templates/clients.yaml").write_text(
                        'apiVersion: v1\nkind: ConfigMap\nmetadata: {name: fixture}\ndata:\n'
                        '  clients: {{ tpl .Values.credentialTemplate (merge (dict '
                        '"ingestionWorkerSecret" "fixture-ingestion-secret" '
                        '"evaluatorSecret" "fixture-evaluator-secret") .) | quote }}\n')
                    rendered = yaml.safe_load(fixture.run("helm", "template", "fixture", str(chart)))
                clients = json.loads(rendered["data"]["clients"])
                expected = {"entity-intelligence-ingestion-token"}
                if fde:
                    expected.add("decision-engine-token")
                self.assertEqual(expected, set(clients))
                ingestion = clients["entity-intelligence-ingestion-token"]
                self.assertEqual("entity-intelligence-ingestion-worker", ingestion["clientId"])
                self.assertEqual("fixture-ingestion-secret", ingestion["clientSecret"])
                self.assertEqual(values["platform"]["endpoints"]["keycloak"]["tokenUrl"], ingestion["tokenUrl"])
                refs = {item["secretKey"]: item["remoteRef"]["key"] for item in secret["spec"]["data"]}
                self.assertEqual(values["platform"]["identity"]["serviceClients"]
                                 ["entityIntelligenceIngestionWorker"]["secretRemoteKey"], refs["ingestionWorkerSecret"])
                self.assertEqual(fde, "evaluatorSecret" in refs)

    def test_all_operation_adapter_variants_consume_the_composed_service_credentials(self):
        for framework, engine, fde in itertools.product(
                ("quarkus", "spring", "micronaut"),
                ("kafka-streams", "pekko-postgresql", "pekko-cassandra"), (True, False)):
            with self.subTest(framework=framework, engine=engine, decision_engine=fde):
                env = dict(os.environ)
                env.pop("FORWARDMEASURE_RESOLVED_SELECTION", None)
                env.update(FORWARDMEASURE_FRAMEWORK=framework, FORWARDMEASURE_ENGINE=engine,
                           FORWARDMEASURE_ENABLE_FDE=str(fde).lower())
                result = subprocess.run(
                    ["helmfile", "-f", "deploy/helmfile/helmfile.yaml.gotmpl", "-e", "local",
                     "build", "--embed-values"], cwd=fixture.WORKSPACE / "forwardmeasure-openworkflow",
                    env=env, capture_output=True, text=True, timeout=60)
                self.assertEqual(0, result.returncode, result.stderr)
                releases = [release for doc in yaml.safe_load_all(result.stdout) if doc
                            for release in doc.get("releases", [])
                            if release["name"] == "openworkflow-operation-adapter"]
                self.assertEqual(1, len(releases))
                service = next(value["services"]["openworkflow-operation-adapter"]
                               for value in releases[0]["values"] if isinstance(value, dict)
                               and "services" in value)
                self.assertEqual(framework, service["framework"])
                self.assertEqual(fde, "OPENWORKFLOW_PLATFORM_GRPC_ENDPOINTS" in service["env"])
                credentials = [item for item in service["secrets"]
                               if item["envVar"] == "OPENWORKFLOW_SERVICE_CLIENT_CREDENTIALS"]
                self.assertEqual([{"envVar": "OPENWORKFLOW_SERVICE_CLIENT_CREDENTIALS",
                                   "existingSecretName": "openworkflow-service-client-credentials",
                                   "secretKey": "OPENWORKFLOW_SERVICE_CLIENT_CREDENTIALS"}], credentials)

    def test_all_central_clients_are_delivered_to_keycloak(self):
        secret = next(value for value in self.secret_values["secrets"] if value["name"] == "keycloak-realm-client-secrets")
        keys = {value["secretKey"] for value in secret["remoteRefs"]}
        expected = {value["secretKey"] for value in self.values["platform"]["identity"]["serviceClients"].values()
                    if value["enabled"]}
        self.assertTrue(expected <= keys)

    def test_human_task_signing_key_is_delivered_to_runtime_secret(self):
        secret = next(value for value in self.manifests if value and value["kind"] == "ExternalSecret"
                      and value["metadata"]["name"] == self.values["identity"]["openworkflowCredentialsSecret"])
        self.assertEqual(self.values["namespaces"]["openworkflow"], secret["metadata"]["namespace"])
        refs = {value["secretKey"]: value["remoteRef"]["key"] for value in secret["spec"]["data"]}
        self.assertEqual("platform-openworkflow-human-task-review-secret",
                         refs["OPENWORKFLOW_HUMAN_TASK_REVIEW_SECRET"])
        self.assertNotEqual(refs["OPENWORKFLOW_CLIENT_SECRET"], refs["OPENWORKFLOW_HUMAN_TASK_REVIEW_SECRET"])

    def test_generated_review_key_replaces_stale_store_entry_without_duplicate_on_rerender(self):
        # Exercise the real composition template with an offline fetch substitute. No operator
        # credentials or cloud secrets are read; all files used by readFile are synthetic.
        template = (fixture.ROOT / "deploy/helmfile/releases/platform-secrets/gcp/with-client-secrets.yaml.gotmpl").read_text()
        # Helmfile also expands ref+ URLs after rendering. Replacing only the fetch function
        # still contacts the provider; synthetic output must contain no resolvable reference.
        template = template.replace("ref+gcpsecrets://", "synthetic-gcp://")
        template = template.replace("fetchSecretValue", 'printf "fixture:%s"')
        values = copy.deepcopy(self.values)
        values["platform"]["cloud"].setdefault("gcp", {}).update(projectId="fixture-project", clusterName="fixture-cluster")
        review_key = values["secretStore"]["remoteKeys"]["openworkflowHumanTaskReviewSecret"]
        settings = {"clusterSecretStore": {"provider": {"fake": {"data": [
            {"key": review_key, "value": "stale"}, {"key": "unrelated", "value": "preserved"}]}}}}
        with tempfile.TemporaryDirectory(prefix="review-secret-fixture-") as directory:
            directory = Path(directory)
            credentials = directory / "fixture.credentials.yaml.gotmpl"
            (directory / "compose.yaml.gotmpl").write_text(template)
            (directory / "values.yaml").write_text(yaml.safe_dump(values))
            (directory / "helmfile.yaml").write_text(yaml.safe_dump({"environments": {
                "gcp-fixture": {"values": [str(directory / "values.yaml")]}}}) + "---\n" +
                yaml.safe_dump({"releases": [{"name": "fixture", "chart": "./unused-chart",
                                              "values": [str(directory / "compose.yaml.gotmpl")]}]}))
            for _ in range(2):
                credentials.write_text(yaml.safe_dump(settings))
                rendered = list(yaml.safe_load_all(fixture.run("helmfile", "--file", str(directory / "helmfile.yaml"),
                                                               "--environment", "gcp-fixture", "build", "--embed-values")))[0]
                settings = rendered["releases"][0]["values"][0]
                data = settings["clusterSecretStore"]["provider"]["fake"]["data"]
                matches = [entry["value"] for entry in data if entry["key"] == review_key]
                self.assertEqual(["fixture:synthetic-gcp://fixture-project/fixture-cluster-" + review_key], matches)
                self.assertIn({"key": "unrelated", "value": "preserved"}, data)
