#!/usr/bin/env python3
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

"""Resolve non-secret deployment choices once; child processes consume the validated snapshot."""
import argparse
import json
import os
from pathlib import Path
import sys

import yaml

CONFIG = Path(__file__).resolve().parents[1] / "helmfile/shared/deployment.yaml"
SNAPSHOT = "FORWARDMEASURE_RESOLVED_SELECTION"
DEFAULTS = dict(framework="quarkus", engine="kafka-streams", enableFde=True,
                displayName="kriyagentic", namespaceLayout="standard")
OVERRIDES = dict(framework="FORWARDMEASURE_FRAMEWORK", engine="FORWARDMEASURE_ENGINE",
                 enableFde="FORWARDMEASURE_ENABLE_FDE", displayName="FORWARDMEASURE_DISPLAY_NAME",
                 namespaceLayout="FORWARDMEASURE_NAMESPACE_LAYOUT")
CHOICES = dict(framework=("quarkus", "spring", "micronaut"),
               engine=("kafka-streams", "pekko-postgresql", "pekko-cassandra"),
               namespaceLayout=("standard", "legacy"))


def validate(selection):
    if not isinstance(selection, dict):
        raise ValueError("Deployment selection must be an object")
    for key, choices in CHOICES.items():
        if selection.get(key) not in choices:
            raise ValueError(f"{key} must be one of {', '.join(choices)}")
    if type(selection.get("enableFde")) is not bool:
        raise ValueError("enableFde must be true or false")
    name = selection.get("displayName")
    if not isinstance(name, str) or not name.strip() or len(name) > 80 or not name.isprintable():
        raise ValueError("displayName must be 1–80 printable characters")
    return selection


def resolve(environment, environ, config=CONFIG):
    if SNAPSHOT in environ:
        selection = validate(json.loads(environ[SNAPSHOT]))
        if selection.get("environment") != environment:
            raise ValueError("Resolved selection belongs to a different environment; start a new installer invocation")
        return validate(selection)
    shared = yaml.safe_load(Path(config).read_text()) or {}
    if not isinstance(shared, dict) or set(shared) - set(DEFAULTS) - {"environments"}:
        raise ValueError("Unknown deployment configuration keys")
    environments = shared.get("environments", {})
    if not isinstance(environments, dict):
        raise ValueError("environments must be a mapping")
    specific = environments.get(environment, {})
    if not isinstance(specific, dict) or set(specific) - set(DEFAULTS):
        raise ValueError(f"Invalid deployment configuration for {environment}")
    selection = {**DEFAULTS, **{k: v for k, v in shared.items() if k != "environments"}, **specific}
    for key, variable in OVERRIDES.items():
        if variable in environ:
            value = environ[variable]
            if key == "enableFde":
                if value not in ("true", "false"):
                    raise ValueError(f"{variable} must be true or false")
                value = value == "true"
            selection[key] = value
    selection["environment"] = environment
    return validate(selection)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--environment", required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(resolve(args.environment, os.environ), separators=(",", ":")))
    except (ValueError, OSError, yaml.YAMLError) as error:
        print(f"Invalid deployment selection: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
