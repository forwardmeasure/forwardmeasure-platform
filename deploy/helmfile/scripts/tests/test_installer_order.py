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

The fake Helmfile models waiting/failure for the execution releases in the real nested-file order,
and the fake Helm reports release statuses. Each install script decides per stage: `helmfile apply`
(upgrade what changed) when every selected release Helm knows is deployed, otherwise an ordered
`helmfile sync` of the whole stage, so a failed release is retried after its prerequisites.
Separate show-dag rendering verifies Helmfile's actual dependency interpretation.
"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

WORKSPACE = Path(__file__).resolve().parents[5]
PLATFORM_SCRIPTS = WORKSPACE / 'forwardmeasure-platform/deploy/scripts'


def pinned_versions():
    versions = {}
    for line in (WORKSPACE / 'forwardmeasure-platform/deploy/tool-versions.env').read_text().splitlines():
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            versions[key] = value
    return versions


TOOL = r"""#!/usr/bin/env python3
import json, os, re, sys
from pathlib import Path
name = Path(sys.argv[0]).name
args = sys.argv[1:]
trace = Path(os.environ['INSTALLER_TRACE'])
def record(event):
    with trace.open('a') as out:
        out.write(json.dumps(event) + '\n')
if name == 'helm':
    if args[:1] == ['version']:
        print('v' + os.environ['PIN_HELM_VERSION'] + '+gtest'); sys.exit(0)
    if args[:2] == ['plugin', 'list']:
        print('NAME\tVERSION\ndiff\t' + os.environ['PIN_HELM_DIFF_VERSION']); sys.exit(0)
    record({'tool': name, 'args': args})
    if 'list' in args:
        failed = os.environ.get('FAILED_RELEASE')
        print(json.dumps([{'namespace': 'owf', 'name': failed, 'status': 'failed'}] if failed else []))
    sys.exit(0)
if name != 'helmfile':
    sys.exit(0)
if args == ['--version']:
    print('helmfile version ' + os.environ['PIN_HELMFILE_VERSION']); sys.exit(0)
if 'list' in args:
    # The releases that matter to these contracts, with their stage labels.
    print(json.dumps([
        {'name': 'openworkflow-execution-management', 'namespace': 'owf', 'enabled': True, 'labels': 'stage:execution'},
        {'name': 'openworkflow-engine', 'namespace': 'owf', 'enabled': True, 'labels': 'stage:execution'},
        {'name': 'openworkflow-operation-adapter', 'namespace': 'owf', 'enabled': True, 'labels': 'stage:execution'},
        {'name': 'kafka', 'namespace': 'kafka', 'enabled': True, 'labels': 'stage:messaging'},
    ]))
    sys.exit(0)
record({'tool': name, 'args': args})
selector = args[args.index('--selector') + 1] if '--selector' in args else ''
if 'name=openworkflow-operation-adapter' in selector:
    sys.exit(41)
if 'stage=execution' in selector:
    if 'sync' not in args and 'apply' not in args:
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
"""


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
        self.trace.write_text('')
        versions = pinned_versions()
        self.env = dict(os.environ, PATH=f'{self.bin}:{os.environ["PATH"]}',
                        INSTALLER_TRACE=str(self.trace), ENGINE_READY=str(self.root / 'ready'),
                        PIN_HELM_VERSION=versions['HELM_VERSION'],
                        PIN_HELMFILE_VERSION=versions['HELMFILE_VERSION'],
                        PIN_HELM_DIFF_VERSION=versions['HELM_DIFF_VERSION'])
        self.env.pop('FORWARDMEASURE_PREPARED_HELMFILES', None)
        self.env.pop('FORWARDMEASURE_INSTALL_PREPARED', None)
        # The shared platform scripts every install.sh loads: the real install steps and tool pins,
        # stubs for deployment selection and the transition check.
        scripts = self.root / 'forwardmeasure-platform/deploy/scripts'
        scripts.mkdir(parents=True)
        shutil.copyfile(PLATFORM_SCRIPTS / 'install-steps.sh', scripts / 'install-steps.sh')
        shutil.copyfile(WORKSPACE / 'forwardmeasure-platform/deploy/tool-versions.env',
                        scripts.parent / 'tool-versions.env')
        (scripts / 'product-selection.sh').write_text('PRODUCT_REPOSITORIES=()\n')
        (scripts / 'check-deployment-transition.py').write_text('')
        (scripts / 'selection-manifests.py').write_text('')
        # A real jq is needed by install-steps.sh; the stubbed one above is only for the scripts.
        (self.bin / 'jq').unlink()

    @staticmethod
    def executable(path, contents):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents)
        path.chmod(0o755)

    def run_installer(self, repository, stage=None, fail_engine=False, failed_release=None):
        source = WORKSPACE / repository / 'deploy/helmfile'
        destination = self.root / repository / 'deploy/helmfile'
        destination.mkdir(parents=True, exist_ok=True)
        for name in ('install.sh', 'helmfile.yaml.gotmpl'):
            shutil.copyfile(source / name, destination / name)
        # Bypass unrelated validation, credentials, digest resolution, topic readiness and cluster setup.
        for name in ('validate.sh', 'scripts/preflight.sh', 'scripts/resolve-image-digests.sh',
                     'scripts/readiness.sh'):
            self.executable(destination / name, '#!/usr/bin/env bash\nexit 0\n')
        for name in ('scripts/openworkflow-topics.py', 'scripts/sync-platform-secrets.py',
                     'scripts/reload-secret-consumers.py'):
            self.executable(destination / name, '')
        self.executable(destination / 'scripts/environment-value.sh',
                        '#!/usr/bin/env bash\nprintf "v1.0.0\\n"\n')
        self.env['FAIL_ENGINE'] = '1' if fail_engine else '0'
        self.env['FAILED_RELEASE'] = failed_release or ''
        args = ['bash', str(destination / 'install.sh'), 'fixture']
        if stage:
            args.append(stage)
        result = subprocess.run(args, env=self.env, text=True, capture_output=True)
        events = [json.loads(line) for line in self.trace.read_text().splitlines()]
        return result, events

    def stage_calls(self, events, command):
        """The helmfile stage reconciliations, each checked to use `command` and wait for readiness."""
        calls = [event['args'] for event in events
                 if event.get('tool') == 'helmfile' and ('sync' in event['args'] or 'apply' in event['args'])]
        self.assertTrue(calls)
        for args in calls:
            self.assertIn(command, args)
            self.assertNotIn('apply' if command == 'sync' else 'sync', args)
            self.assertIn('--skip-needs=false', args)
            self.assertIn('--wait', args)
            self.assertIn('--wait-for-jobs', args)
            # Never a separate per-release retry pass ahead of the stage's prerequisites.
            self.assertNotIn('name=openworkflow-operation-adapter', args)
        return calls

    def test_failed_adapter_waits_for_engine_update_in_execution_stage(self):
        result, events = self.run_installer('forwardmeasure-openworkflow', 'execution',
                                            failed_release='openworkflow-operation-adapter')
        self.assertEqual(0, result.returncode, result.stderr)
        self.stage_calls(events, 'sync')
        self.assertIn('owf/openworkflow-operation-adapter (failed)', result.stdout)
        self.assertEqual(['execution-management', 'tenant-administration', 'engine', 'adapter'],
                         [event['release'] for event in events if 'release' in event])

    def test_a_deployed_stage_upgrades_only_what_changed(self):
        result, events = self.run_installer('forwardmeasure-openworkflow', 'execution')
        self.assertEqual(0, result.returncode, result.stderr)
        calls = self.stage_calls(events, 'apply')
        self.assertIn('--suppress-secrets', calls[0])
        self.assertEqual(['execution-management', 'tenant-administration', 'engine', 'adapter'],
                         [event['release'] for event in events if 'release' in event])

    def test_failed_engine_stops_before_adapter_and_later_stages(self):
        result, events = self.run_installer('forwardmeasure-openworkflow', fail_engine=True)
        self.assertEqual(42, result.returncode, result.stderr)
        calls = self.stage_calls(events, 'apply')
        self.assertEqual('stage=execution', calls[-1][calls[-1].index('--selector') + 1])
        self.assertNotIn('adapter', [event.get('release') for event in events])

    def test_full_fowf_install_keeps_stage_order_and_allows_empty_stages(self):
        result, events = self.run_installer('forwardmeasure-openworkflow')
        self.assertEqual(0, result.returncode, result.stderr)
        calls = self.stage_calls(events, 'apply')
        self.assertEqual(
            ['core-infrastructure', 'cassandra', 'messaging', 'foundation', 'migrations', 'security',
             'definitions', 'execution', 'human-task', 'studio', 'acceptance', 'routing'],
            [args[args.index('--selector') + 1].removeprefix('stage=') for args in calls])
        self.assertTrue(all('--allow-no-matching-release' in args for args in calls))
        # Repositories and chart dependencies once, on the first call; skipped afterwards.
        self.assertNotIn('--skip-deps', calls[0])
        self.assertTrue(all('--skip-deps' in args for args in calls[1:]))

    def test_platform_selected_stage_preserves_dependency_graph(self):
        result, events = self.run_installer('forwardmeasure-platform', 'messaging')
        self.assertEqual(0, result.returncode, result.stderr)
        calls = self.stage_calls(events, 'apply')
        self.assertEqual(1, len(calls))
        self.assertIn('stage=messaging', calls[0])

    def test_each_product_uses_one_ordered_pass(self):
        for repository in ('forwardmeasure-data-streaming', 'forwardmeasure-entity-intelligence',
                           'forwardmeasure-decision-engine'):
            with self.subTest(repository=repository):
                self.trace.write_text('')
                result, events = self.run_installer(repository)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(1, len(self.stage_calls(events, 'apply')))

    def test_install_refuses_tools_that_differ_from_the_pins(self):
        self.env['PIN_HELMFILE_VERSION'] = '0.0.1'
        result, events = self.run_installer('forwardmeasure-openworkflow', 'execution')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('helmfile is 0.0.1, pinned', result.stderr)
        self.assertEqual([], [event for event in events if event.get('tool') == 'helmfile'])


if __name__ == '__main__':
    unittest.main()
