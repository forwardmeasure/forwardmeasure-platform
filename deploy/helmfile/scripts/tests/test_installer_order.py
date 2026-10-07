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
"""Installer CLI contracts with fake tools; never connects to a cluster.

The fake Helmfile models waiting/failure for the execution releases in the real nested-file
order. Separate show-dag rendering verifies Helmfile's actual dependency interpretation.
"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

WORKSPACE = Path(__file__).resolve().parents[5]

TOOL = r'''#!/usr/bin/env python3
import json, os, re, sys
from pathlib import Path
name = Path(sys.argv[0]).name
args = sys.argv[1:]
trace = Path(os.environ['INSTALLER_TRACE'])
def record(event):
    with trace.open('a') as out:
        out.write(json.dumps(event) + '\n')
record({'tool': name, 'args': args})
if name == 'helm':
    # The old installer would pick the failed adapter before updating the engine.
    if 'list' in args:
        print('openworkflow-operation-adapter')
    sys.exit(0)
if name != 'helmfile':
    sys.exit(0)
selector = args[args.index('--selector') + 1] if '--selector' in args else ''
if 'name=openworkflow-operation-adapter' in selector:
    sys.exit(41)
if 'stage=execution' in selector:
    if 'sync' not in args:
        sys.exit(43)
    root = Path(args[args.index('--file') + 1])
    releases = {
        'execution-services.yaml.gotmpl': 'execution-management',
        'tenant-administration.yaml.gotmpl': 'tenant-administration',
        'execution-engines.yaml.gotmpl': 'engine',
        'operation-adapters.yaml.gotmpl': 'adapter',
    }
    for child in re.findall(r'^  - path: (\S+)', root.read_text(), re.M):
        release = releases.get(Path(child).name)
        if not release:
            continue
        record({'release': release})
        if release == 'engine' and os.environ.get('FAIL_ENGINE') == '1':
            sys.exit(42)
        if release == 'engine':
            Path(os.environ['ENGINE_READY']).touch()
        if release == 'adapter' and not Path(os.environ['ENGINE_READY']).exists():
            sys.exit(41)
'''


class InstallerOrderTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        for name in ('kubectl', 'helm', 'helmfile', 'yq', 'jq'):
            self.executable(self.bin / name, TOOL)
        self.trace = self.root / 'trace.jsonl'
        self.env = dict(os.environ, PATH=f'{self.bin}:{os.environ["PATH"]}',
                        INSTALLER_TRACE=str(self.trace), ENGINE_READY=str(self.root / 'ready'))

    @staticmethod
    def executable(path, contents):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents)
        path.chmod(0o755)

    def run_installer(self, repository, stage=None, fail_engine=False):
        source = WORKSPACE / repository / 'deploy/helmfile'
        destination = self.root / repository / 'deploy/helmfile'
        destination.mkdir(parents=True, exist_ok=True)
        for name in ('install.sh', 'helmfile.yaml.gotmpl'):
            shutil.copyfile(source / name, destination / name)
        # Bypass unrelated validation, credentials, digest resolution and cluster setup.
        for name in ('validate.sh', 'scripts/preflight.sh', 'scripts/resolve-image-digests.sh',
                     'scripts/readiness.sh'):
            self.executable(destination / name, '#!/usr/bin/env bash\nexit 0\n')
        self.executable(destination / 'scripts/environment-value.sh',
                        '#!/usr/bin/env bash\nprintf "v1.0.0\\n"\n')
        self.env['FAIL_ENGINE'] = '1' if fail_engine else '0'
        args = ['bash', str(destination / 'install.sh'), 'fixture']
        if stage:
            args.append(stage)
        result = subprocess.run(args, env=self.env, text=True, capture_output=True)
        events = [json.loads(line) for line in self.trace.read_text().splitlines()]
        return result, events

    def assert_ordered_sync(self, events):
        self.assertFalse(any(event.get('tool') == 'helm' for event in events),
                         'Failed releases must not be enumerated/retried ahead of prerequisites')
        calls = [event['args'] for event in events if event.get('tool') == 'helmfile']
        self.assertTrue(calls)
        for args in calls:
            self.assertIn('sync', args)
            self.assertNotIn('apply', args)
            self.assertIn('--skip-needs=false', args)
            self.assertIn('--wait', args)
            self.assertIn('--wait-for-jobs', args)
        return calls

    def test_failed_adapter_waits_for_engine_update_in_execution_stage(self):
        result, events = self.run_installer('forwardmeasure-openworkflow', 'execution')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assert_ordered_sync(events)
        self.assertEqual(['execution-management', 'tenant-administration', 'engine', 'adapter'],
                         [event['release'] for event in events if 'release' in event])

    def test_failed_engine_stops_before_adapter_and_later_stages(self):
        result, events = self.run_installer('forwardmeasure-openworkflow', fail_engine=True)
        self.assertEqual(42, result.returncode, result.stderr)
        calls = self.assert_ordered_sync(events)
        self.assertEqual('stage=execution', calls[-1][calls[-1].index('--selector') + 1])
        self.assertNotIn('adapter', [event.get('release') for event in events])

    def test_full_fowf_install_keeps_stage_order_and_allows_empty_stages(self):
        result, events = self.run_installer('forwardmeasure-openworkflow')
        self.assertEqual(0, result.returncode, result.stderr)
        calls = self.assert_ordered_sync(events)
        self.assertEqual(
            ['core-infrastructure', 'cassandra', 'foundation', 'migrations', 'security',
             'definitions', 'execution', 'human-task', 'studio', 'acceptance', 'routing'],
            [args[args.index('--selector') + 1].removeprefix('stage=') for args in calls])
        self.assertTrue(all('--allow-no-matching-release' in args for args in calls))

    def test_platform_selected_stage_preserves_dependency_graph(self):
        result, events = self.run_installer('forwardmeasure-platform', 'messaging')
        self.assertEqual(0, result.returncode, result.stderr)
        calls = self.assert_ordered_sync(events)
        self.assertEqual(1, len(calls))
        self.assertIn('stage=messaging', calls[0])

    def test_each_product_uses_one_ordered_pass(self):
        for repository in ('forwardmeasure-data-streaming', 'forwardmeasure-entity-intelligence',
                           'forwardmeasure-decision-engine'):
            with self.subTest(repository=repository):
                self.trace.write_text('')
                result, events = self.run_installer(repository)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(1, len(self.assert_ordered_sync(events)))


if __name__ == '__main__':
    unittest.main()
