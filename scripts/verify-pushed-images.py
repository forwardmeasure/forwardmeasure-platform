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
"""Fail when a pushing build created one of our images but never published it.

A module can build its image and silently skip publication: a Quarkus image build without
quarkus.container-image.push, or a docker-maven-plugin module with a build goal but no push goal.
Deploys resolve images by registry digest, so such an image never reaches a cluster, and nothing
reports it. Docker records a registry digest (RepoDigests) on an image only once it has been
pushed, so every image of ours created during this build must have one.

Usage: verify-pushed-images.py <build-start-epoch-seconds> [repository-prefix]
"""
import json
import subprocess
import sys
from datetime import datetime, timezone

PREFIX = "forwardmeasure/"


def docker(*args):
    result = subprocess.run(["docker", *args], text=True, capture_output=True, check=False)
    if result.returncode:
        raise RuntimeError(f"docker {' '.join(args[:2])} failed: {result.stderr.strip()}")
    return result.stdout


def created(value):
    # Docker prints nanoseconds and either Z or an offset; Python parses microseconds.
    value = value.replace("Z", "+00:00")
    if "." in value:
        whole, rest = value.split(".", 1)
        digits = rest[:len(rest) - len(rest.lstrip("0123456789"))]
        offset = rest[len(digits):]
        value = f"{whole}.{digits[:6]}{offset}"
    return datetime.fromisoformat(value)


def unpublished(since, images):
    """Tags of our images created at or after `since` that carry no registry digest."""
    missing = []
    for image in images:
        tags = [tag for tag in image.get("RepoTags") or []
                if tag.removeprefix("docker.io/").startswith(PREFIX)]
        if not tags or created(image["Created"]) < since:
            continue
        if not image.get("RepoDigests"):
            missing.extend(tags)
    return sorted(missing)


def main():
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    global PREFIX
    if len(sys.argv) == 3:
        PREFIX = sys.argv[2]
    since = datetime.fromtimestamp(int(sys.argv[1]), tz=timezone.utc)
    ids = docker("image", "ls", "--quiet", "--no-trunc", "--filter", f"reference={PREFIX}*").split()
    if not ids:
        print("No images of ours were found locally; nothing to verify.")
        return
    images = json.loads(docker("image", "inspect", *sorted(set(ids))))
    missing = unpublished(since, images)
    if missing:
        print("These images were built by this build but never pushed to the registry, so no "
              "deploy will pick them up:", file=sys.stderr)
        for tag in missing:
            print(f"  {tag}", file=sys.stderr)
        print("Their modules build an image without publishing it: give a docker-maven-plugin "
              "module a push goal, or set quarkus.container-image.push to ${container-image.push} "
              "for a Quarkus image build.", file=sys.stderr)
        sys.exit(1)
    print("Every image this build created was pushed to the registry.")


if __name__ == "__main__":
    main()
