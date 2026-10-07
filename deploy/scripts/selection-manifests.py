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

"""Produce non-secret Kubernetes resources from the already resolved selection."""
import json
import os
import sys

selection = json.loads(os.environ["FORWARDMEASURE_RESOLVED_SELECTION"])
if sys.argv[1:] == ["namespaces"]:
    prefix = "forwardmeasure-" if selection["namespaceLayout"] == "standard" else ""
    names = [prefix + name for name in ("data-streaming", "entity-intelligence", "decision-engine")]
    print(json.dumps(dict(apiVersion="v1", kind="List", items=[
        dict(apiVersion="v1", kind="Namespace", metadata=dict(name=name)) for name in names])))
elif sys.argv[1:] == ["record"]:
    print(json.dumps(dict(apiVersion="v1", kind="ConfigMap",
                         metadata=dict(name="forwardmeasure-deployment-selection", namespace="forwardmeasure-platform"),
                         data={"selection.json": json.dumps(selection, sort_keys=True)})))
else:
    sys.exit("Usage: selection-manifests.py namespaces|record")
