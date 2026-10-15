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

"""Render real FEI dispatch and FOWF policy templates; no cluster or registry calls."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

import yaml

WORKSPACE = Path(__file__).resolve().parents[3]
FEI = "forwardmeasure-entity-intelligence"
FOWF = "forwardmeasure-openworkflow"
PLATFORM = "forwardmeasure-platform"
FRAMEWORKS = ("quarkus", "spring", "micronaut")
WORKERS = {
    "ENTITY_INTELLIGENCE_INGESTION_SIMPLE_SOURCE_WORKER_IMAGE": "entityIntelligenceIngestionWorker",
    "ENTITY_INTELLIGENCE_INGESTION_CORRELATED_SOURCE_WORKER_IMAGE": "entityIntelligenceIngestionWorkerSpark",
    "ENTITY_INTELLIGENCE_INGESTION_RESOLUTION_DELIVERY_WORKER_IMAGE": "entityIntelligenceResolutionDeliveryWorker",
    "ENTITY_INTELLIGENCE_INGESTION_REFERENCE_POPULATION_DELTA_APPLY_WORKER_IMAGE": "entityIntelligenceReferencePopulationDeltaApplyWorker",
    "ENTITY_INTELLIGENCE_SCREENING_POPULATION_SCREENING_WORKER_IMAGE": "entityIntelligenceScreeningWorker",
}


class FeiWorkerImageCoordinatesTest(unittest.TestCase):
    def test_inventory_coordinates_survive_dispatch_and_authorization_unchanged(self):
        for framework in FRAMEWORKS:
            for registry in (None, "registry.example.test:5443/team/nested"):
                with self.subTest(framework=framework, registry=registry):
                    self.check_coordinates(framework, registry)

    def check_coordinates(self, framework, registry):
        with tempfile.TemporaryDirectory() as directory:
            workspace = Path(directory)
            shared = Path(PLATFORM) / "deploy/helmfile/shared"
            shutil.copytree(WORKSPACE / shared, workspace / shared)
            files = (
                f"{PLATFORM}/deploy/scripts/deployment-selection.py",
                f"{FEI}/deploy/helmfile/environments/base.yaml",
                f"{FEI}/deploy/helmfile/environments/gcp-platform-cluster.yaml.gotmpl",
                f"{FEI}/deploy/helmfile/releases/entity-intelligence-services/base.yaml.gotmpl",
                f"{FOWF}/deploy/helmfile/environments/platform-derived.yaml.gotmpl",
                "forwardmeasure-data-streaming/deploy/helmfile/environments/base.yaml",
            )
            for name in files:
                target = workspace / name
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(WORKSPACE / name, target)
            inventory = workspace / FEI / "deploy/helmfile/environments/base.yaml"
            values = yaml.safe_load(inventory.read_text())
            expected = {}
            for number, (variable, key) in enumerate(WORKERS.items(), start=1):
                image = values["imageVersions"][key]
                # Synthetic pins exercise frameworks not built/pushed on this workstation.
                digest = "sha256:" + f"{number:064x}"
                if "repositories" in image:
                    repository = image["repositories"][framework]
                    if registry:
                        repository = registry + "/" + repository.rsplit("/", 1)[1]
                    image["repositories"][framework] = repository
                    image["digests"][framework] = digest
                else:
                    repository = image["repository"]
                    if registry:
                        repository = registry + "/" + repository.rsplit("/", 1)[1]
                    image["repository"] = repository
                    image["digest"] = digest
                expected[variable] = repository + "@" + digest
            inventory.write_text(yaml.safe_dump(values))
            env = {k: v for k, v in os.environ.items() if not k.startswith("FORWARDMEASURE_")}
            env["FORWARDMEASURE_FRAMEWORK"] = framework
            shared_prefix = "../../../forwardmeasure-platform/deploy/helmfile/shared/"
            fei = self.render(workspace, FEI, [
                "environments/base.yaml",
                "environments/gcp-platform-cluster.yaml.gotmpl",
                shared_prefix + "common.yaml.gotmpl",
                shared_prefix + "image-versions.yaml",
                shared_prefix + "clusters/gcp-openworkflow-prod.yaml.gotmpl",
            ], "      - releases/entity-intelligence-services/base.yaml.gotmpl\n", env)
            fowf = self.render(workspace, FOWF,
                               ["environments/platform-derived.yaml.gotmpl"],
                               "      - job: {{ .Values.operations.kubernetesJob.imageAllowlist | quote }}\n"
                               "        deployment: {{ .Values.operations.kubernetesDeployment.imageAllowlist | quote }}\n", env)
            actual = {}
            for service in fei["services"].values():
                actual.update({k: v for k, v in service.get("env", {}).items() if k in WORKERS})
            self.assertEqual(expected, actual)
            for policy in (fowf["job"], fowf["deployment"]):
                self.assertTrue(policy)
                for tenant_policy in policy.split(";"):
                    images = set(tenant_policy.split("=", 1)[1].split("|"))
                    self.assertTrue(set(expected.values()).issubset(images), tenant_policy)
                    self.assertEqual(5, sum("entity-intelligence-" in image for image in images))

    def render(self, workspace, repo, values, release_values, env):
        directory = workspace / repo / "deploy/helmfile"
        state = directory / "inspection.yaml.gotmpl"
        state.write_text(
            yaml.safe_dump({"environments": {"gcp-openworkflow-prod": {"values": values}}})
            + "---\nreleases:\n  - name: inspect\n    chart: unused-local-chart\n    values:\n"
            + release_values)
        result = subprocess.run(
            ["helmfile", "-f", str(state), "-e", "gcp-openworkflow-prod", "build", "--embed-values"],
            cwd=directory, env=env, capture_output=True, text=True, timeout=60)
        self.assertEqual(0, result.returncode, result.stderr)
        documents = [d for d in yaml.safe_load_all(result.stdout) if d and d.get("releases")]
        self.assertEqual(1, len(documents))
        return documents[0]["releases"][0]["values"][0]


if __name__ == "__main__":
    unittest.main()
