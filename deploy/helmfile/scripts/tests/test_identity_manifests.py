"""Offline contracts for the actual central values and ExternalSecret chart templates."""
import copy
import importlib.util
import json
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
        template = template.replace("fetchSecretValue", 'printf "fixture:%s"')
        values = copy.deepcopy(self.values)
        values["platform"]["cloud"]["gcp"].update(projectId="fixture-project", clusterName="fixture-cluster")
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
                self.assertEqual(["fixture:ref+gcpsecrets://fixture-project/fixture-cluster-" + review_key], matches)
                self.assertIn({"key": "unrelated", "value": "preserved"}, data)
