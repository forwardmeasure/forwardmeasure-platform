#
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
#
"""Release regression source. Run after deployment alongside the acceptance fixtures."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ReleasePreparationTest(unittest.TestCase):
    def test_selected_build_omits_fde_other_frameworks_and_inactive_engine(self):
        result = subprocess.run([
            'python3', str(ROOT / 'deploy/scripts/product-build-plan.py'),
            '--framework', 'quarkus', '--engine', 'kafka-streams'],
            check=True, capture_output=True, text=True)
        plan = json.loads(result.stdout)
        self.assertEqual(set(plan['products']), {
            'forwardmeasure-openworkflow', 'forwardmeasure-data-streaming',
            'forwardmeasure-entity-intelligence'})
        images = [item['image'] for items in plan['products'].values() for item in items]
        self.assertIn('openworkflow-operation-adapter-kafka-streams-quarkus', images)
        self.assertIn('entity-intelligence-ingestion-worker-spark', images)
        self.assertFalse(any('decision-engine' in name or 'engine-pekko' in name
                             or name.endswith(('-spring', '-micronaut')) for name in images))

    def test_fei_studio_uses_public_login_and_private_upstreams(self):
        import yaml
        fei = ROOT.parent / 'forwardmeasure-entity-intelligence'
        env = {key: os.environ[key] for key in ('PATH', 'HOME')}
        env.update(OPENWORKFLOW_VERSION='1.1.0', FORWARDMEASURE_ENABLE_FDE='false')
        rendered = subprocess.run([
            'helmfile', '--file', 'deploy/helmfile/helmfile.yaml.gotmpl',
            '--environment', 'gcp-openworkflow-prod', 'template', '--skip-deps'],
            cwd=fei, env=env, check=True, capture_output=True, text=True)
        resources = [item for item in yaml.safe_load_all(rendered.stdout) if item]
        studio = next(item for item in resources if item.get('kind') == 'Deployment'
                      and item['metadata']['name'] == 'entity-intelligence-studio-service')
        issuers = {entry['value'] for item in resources if item.get('kind') == 'Deployment'
                   for container in item['spec']['template']['spec']['containers']
                   for entry in container.get('env', [])
                   if entry['name'] == 'ENTITY_INTELLIGENCE_KEYCLOAK_ISSUER'}
        self.assertEqual({'https://auth.kriyagentic.com:443/realms/forwardmeasure'}, issuers)
        containers = studio['spec']['template']['spec']['containers']
        self.assertEqual(1, len(containers))
        config = {entry['name']: entry.get('value') for entry in containers[0]['env']}
        self.assertEqual('https://auth.kriyagentic.com', config['ENTITY_INTELLIGENCE_STUDIO_OIDC_URL'])
        for service in ('INGESTION', 'RESOLUTION', 'SCREENING', 'FOWF_EXECUTIONS'):
            self.assertIn('.svc.cluster.local', config[f'ENTITY_INTELLIGENCE_STUDIO_{service}_UPSTREAM_URL'])
        self.assertFalse(any('DATABASE_PASSWORD' in key or 'CLIENT_SECRET' in key for key in config))
        route = next(item for item in resources if item.get('kind') == 'HTTPRoute')
        self.assertEqual(['entity-intelligence.kriyagentic.com'], route['spec']['hostnames'])
        self.assertEqual('tenant-https', route['spec']['parentRefs'][0]['sectionName'])

    def test_digest_sync_preserves_multiarchitecture_index(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            docker = directory / 'docker'
            docker.write_text('''#!/usr/bin/env python3
import json, sys
if sys.argv[1:3] == ['buildx', 'version']:
    print('test buildx')
elif sys.argv[1:4] == ['buildx', 'imagetools', 'inspect']:
    print(json.dumps({'digest': 'sha256:' + 'a' * 64}))
else:
    # A manifest-list child's digest must never replace the index digest.
    print(json.dumps([{'Descriptor': {'digest': 'sha256:' + 'b' * 64}}]))
''')
            docker.chmod(0o755)
            inventory = directory / 'images.yaml'
            inventory.write_text('imageVersions:\n  worker:\n    repository: docker.io/forwardmeasure/test-worker\n    tag: "1.1.0"\n    digest: ""\n')
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ['PATH'])
            subprocess.run([str(ROOT / 'deploy/scripts/sync-image-digests.sh'), str(inventory)],
                           env=env, check=True, capture_output=True, text=True)
            self.assertIn('sha256:' + 'a' * 64, inventory.read_text())
            self.assertNotIn('sha256:' + 'b' * 64, inventory.read_text())


if __name__ == '__main__':
    unittest.main()
