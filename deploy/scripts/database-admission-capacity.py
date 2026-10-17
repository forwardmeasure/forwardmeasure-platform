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

"""Compute one database-instance identity and supported lifecycle lock budget for deployment."""
import argparse
import json
import uuid


def capacity(identity, slots, buckets):
    if not identity or identity.isspace():
        raise ValueError("An explicit platform database instance identity is required")
    if not 1 <= slots <= 64 or not 1 <= buckets <= 65536:
        raise ValueError("Unsupported admission slot or guard bucket count")
    # These are application coordination settings. PostgreSQL capacity is observed at runtime;
    # this renderer never proposes or imposes database flags or connection limits.
    return {"instanceId": str(uuid.uuid5(uuid.NAMESPACE_URL, identity)),
            "slots": slots, "buckets": buckets}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--instance-identity", required=True)
    parser.add_argument("--slots", type=int, default=1)
    parser.add_argument("--buckets", type=int, default=4096)
    args = parser.parse_args()
    try:
        result = capacity(args.instance_identity, args.slots, args.buckets)
    except ValueError as error:
        parser.error(str(error))
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
