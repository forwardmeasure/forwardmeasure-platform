#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# https://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Refresh derived inline schemas without depending on cross-file codegen naming.

Only the platform JSON is authoritative. JSON flow objects are valid YAML, so this
maintainer tool needs only Python's standard library. Product contract tests compare
these derived components against the dependency artifact and detect stale copies.
"""
import argparse
import copy
import json
from pathlib import Path
import re

PLATFORM = Path(__file__).resolve().parents[1]
SOURCE = PLATFORM / "forwardmeasure-platform-api-contracts/src/main/resources/META-INF/forwardmeasure/openapi/problem-v1.json"
TARGETS = (
    "forwardmeasure-openworkflow/openworkflow-api-specifications/src/main/resources/META-INF/openapi/common-definitions.yaml",
    "forwardmeasure-entity-intelligence/forwardmeasure-entity-intelligence-api-specifications/src/main/resources/META-INF/openapi/entity-intelligence-api.yaml",
    "forwardmeasure-data-streaming/forwardmeasure-data-streaming-api/src/main/resources/openapi/launcher.yaml",
)


def dialect_30(value):
    if isinstance(value, dict):
        kind = value.get("type")
        if isinstance(kind, list) and "null" in kind:
            nonnull = [item for item in kind if item != "null"]
            if len(nonnull) != 1:
                raise ValueError("A shared schema has a union not representable in OpenAPI 3.0")
            value["type"] = nonnull[0]
            value["nullable"] = True
        for child in value.values():
            dialect_30(child)
    elif isinstance(value, list):
        for child in value:
            dialect_30(child)


def replace_component(text, name, schema):
    begin = f"    # BEGIN GENERATED platform problem-v1: {name}\n"
    end = f"    # END GENERATED platform problem-v1: {name}\n"
    encoded = json.dumps(schema, indent=2, ensure_ascii=False).splitlines()
    block = begin + f"    {name}: " + encoded[0] + "\n"
    block += "\n".join("    " + line for line in encoded[1:]) + "\n" + end
    if begin in text:
        start = text.index(begin)
        stop = text.index(end, start) + len(end)
    else:
        schemas = re.search(r"^  schemas:\s*$", text, re.M)
        if not schemas:
            raise ValueError("Missing components.schemas")
        match = re.search(r"^    " + re.escape(name) + r":.*$", text[schemas.end():], re.M)
        if not match:
            return text.rstrip() + "\n" + block
        start = schemas.end() + match.start()
        next_line = text.index("\n", start) + 1
        following = re.search(r"^ {0,4}\S", text[next_line:], re.M)
        stop = next_line + following.start() if following else len(text)
    return text[:start] + block + text[stop:]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail if a derived component is stale")
    parser.add_argument("--workspace", type=Path, default=PLATFORM.parent)
    args = parser.parse_args()
    schemas = json.loads(SOURCE.read_text())["components"]["schemas"]
    dirty = []
    for relative in TARGETS:
        path = args.workspace / relative
        original = path.read_text()
        derived = copy.deepcopy(schemas)
        if re.search(r"^openapi:\s*3\.0\.", original, re.M):
            dialect_30(derived)
        updated = original
        for name, schema in derived.items():
            updated = replace_component(updated, name, schema)
        if updated != original:
            dirty.append(relative)
            if not args.check:
                path.write_text(updated)
    for relative in dirty:
        print(("STALE " if args.check else "UPDATED ") + relative)
    return 1 if args.check and dirty else 0


if __name__ == "__main__":
    raise SystemExit(main())
