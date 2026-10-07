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

"""Reject implicit engine/namespace migration before any installer mutation."""
import json
import os
import subprocess
import sys

import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("deployment_selection", Path(__file__).with_name("deployment-selection.py"))
selection_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selection_module)
ENGINE_RELEASES = {f"openworkflow-engine-{engine}": engine for engine in selection_module.CHOICES["engine"]}
PRODUCT_RELEASES = {"decision-engine": "decision-engine", "entity-intelligence-services": "entity-intelligence",
                    "data-streaming-launcher": "data-streaming"}
MARKER = "forwardmeasure-deployment-selection"


def violations(selection, releases, previous):
    errors = []
    if previous:
        for key in ("engine", "namespaceLayout"):
            if previous.get(key) != selection[key]:
                errors.append(f"Recorded {key}={previous.get(key)} differs from requested {selection[key]}")
    prefix = "forwardmeasure-" if selection["namespaceLayout"] == "standard" else ""
    for release in releases:
        name, namespace = release["name"], release["namespace"]
        if name in ENGINE_RELEASES and ENGINE_RELEASES[name] != selection["engine"]:
            errors.append(f"Existing engine release {namespace}/{name} differs from requested {selection['engine']}")
        if name in PRODUCT_RELEASES and namespace != prefix + PRODUCT_RELEASES[name]:
            errors.append(f"Existing product release {namespace}/{name} requires namespace cutover")
        if name == "decision-engine" and not selection["enableFde"]:
            errors.append("FDE is installed; disabling selection alone cannot safely retire its running workflows")
    return errors


def main():
    selection = selection_module.validate(json.loads(os.environ[selection_module.SNAPSHOT]))
    # Helm 4 removed --all. Explicit live/recoverable states also avoid treating a release
    # uninstalled with retained history as an existing workload during a deliberate cutover.
    releases = json.loads(subprocess.check_output(
        ["helm", "list", "--deployed", "--failed", "--pending", "--uninstalling",
         "--all-namespaces", "--max", "10000", "-o", "json"], text=True))
    if len(releases) >= 10000:
        sys.exit("Deployment preflight stopped: Helm release listing may be truncated; no changes were made.")
    marker = subprocess.check_output(["kubectl", "-n", "forwardmeasure-platform", "get", "configmap", MARKER,
                                      "--ignore-not-found", "-o", "json"], text=True)
    previous = json.loads(json.loads(marker)["data"]["selection.json"]) if marker.strip() else None
    errors = violations(selection, releases, previous)
    if errors:
        sys.exit("Deployment transition blocked:\n- " + "\n- ".join(errors)
                 + "\nFollow docs/deployment-configuration-and-cutover.md; no automatic state migration is performed.")
    print("Deployment transition preflight passed")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(f"Deployment preflight failed: {error.cmd[0]} exited with status {error.returncode}; "
                 "resolve the command error above before retrying. No changes were made.")
    except (OSError, ValueError, KeyError) as error:
        sys.exit(f"Deployment preflight failed: {error}. No changes were made.")
