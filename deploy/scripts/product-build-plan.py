#!/usr/bin/env python3
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
"""Select container-producing modules for the configured three-product deployment."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--framework', choices=('quarkus', 'spring', 'micronaut'), required=True)
parser.add_argument('--engine', choices=('kafka-streams', 'pekko-postgresql', 'pekko-cassandra'), required=True)
parser.add_argument('--repository', choices=('forwardmeasure-openworkflow', 'forwardmeasure-data-streaming', 'forwardmeasure-entity-intelligence'))
args = parser.parse_args()
workspace = Path(__file__).resolve().parents[3]
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
result = {}
for name in ('forwardmeasure-openworkflow', 'forwardmeasure-data-streaming', 'forwardmeasure-entity-intelligence'):
    repo = workspace / name
    selected = []
    # Follow the reactor, not arbitrary target/generated POMs or cached artifacts.
    pending = [repo / 'pom.xml']
    while pending:
        pom = pending.pop()
        root = ET.parse(pom).getroot()
        for module in root.findall('./m:modules/m:module', ns):
            pending.append(pom.parent / module.text / 'pom.xml')
        image = root.findtext('./m:properties/m:container-image.name', namespaces=ns)
        if not image:
            continue
        if any(image.endswith('-' + framework) for framework in ('quarkus', 'spring', 'micronaut')):
            if not image.endswith('-' + args.framework):
                continue
        if name == 'forwardmeasure-openworkflow':
            engine = 'kafka-streams' if args.engine == 'kafka-streams' else 'pekko'
            if ('-engine-' in image or '-operation-adapter-' in image) and '-' + engine + '-' not in image:
                continue
        selected.append({'module': str(pom.parent.relative_to(repo)), 'image': image})
    if not selected:
        raise SystemExit(f'No image modules selected in {repo}')
    result[name] = sorted(selected, key=lambda entry: entry['module'])
if args.repository:
    print(','.join(entry['module'] for entry in result[args.repository]))
else:
    print(json.dumps({'framework': args.framework, 'engine': args.engine, 'products': result}, indent=2))
