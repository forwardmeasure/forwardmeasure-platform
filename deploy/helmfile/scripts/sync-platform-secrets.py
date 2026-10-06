#!/usr/bin/env python3
"""Synchronize platform ExternalSecrets without reading or printing secret values."""
import argparse
import json
import subprocess
import time
import uuid

MARKER = "platform.forwardmeasure.com/secret-sync"


def kubectl(*args, optional=False):
    result = subprocess.run(["kubectl", *args], text=True, capture_output=True, check=False)
    if result.returncode:
        if optional and "(NotFound)" in result.stderr:
            return ""
        raise RuntimeError(f"kubectl {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def sync(store, timeout=300, command=kubectl, now=time.monotonic, sleep=time.sleep):
    # Helm replaces managed-by with "Helm" at install time. Use release ownership,
    # not the chart's rendered managed-by label, to identify installed resources.
    inventory = json.loads(command("get", "externalsecrets", "--all-namespaces", "-o", "json"))
    selected = [item for item in inventory["items"]
                if item["metadata"].get("annotations", {}).get("meta.helm.sh/release-name") == "platform-secrets"
                and item["spec"]["secretStoreRef"]["name"] == store
                and item["spec"]["secretStoreRef"].get("kind", "SecretStore") == "ClusterSecretStore"]
    if not selected:
        raise RuntimeError(f"No platform ExternalSecrets reference {store}")
    nonce = uuid.uuid4().hex
    pending = []
    for item in selected:
        namespace, name = item["metadata"]["namespace"], item["metadata"]["name"]
        target = item["spec"].get("target", {}).get("name") or name
        # A marker on the *target* proves ESO applied this request, even if Ready was already true.
        # A metadata-only template preserves ordinary spec.data with ESO's no-data-template path.
        patch = {"metadata": {"annotations": {"force-sync": nonce}},
                 "spec": {"target": {"template": {"metadata": {"annotations": {MARKER: nonce}}}}}}
        command("--namespace", namespace, "patch", "externalsecret", name,
                "--type=merge", "--patch", json.dumps(patch))
        pending.append((namespace, name, target))
    deadline = now() + timeout
    while pending:
        waiting = []
        for namespace, name, target in pending:
            observed = command("--namespace", namespace, "get", "secret", target, "-o",
                               'go-template={{ index .metadata.annotations "' + MARKER + '" }}',
                               optional=True)
            state = json.loads(command("--namespace", namespace, "get", "externalsecret", name,
                                       "-o", "json"))
            ready = any(c.get("type") == "Ready" and c.get("status") == "True"
                        for c in state.get("status", {}).get("conditions", []))
            if observed != nonce or not ready:
                waiting.append((namespace, name, target))
        pending = waiting
        if pending and now() >= deadline:
            names = ", ".join(f"{ns}/{name}" for ns, name, _ in pending)
            raise RuntimeError(f"ExternalSecrets did not apply this synchronization request: {names}")
        if pending:
            sleep(2)
    return len(selected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("store")
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    try:
        count = sync(args.store, args.timeout)
    except (RuntimeError, ValueError, KeyError) as failure:
        parser.exit(1, f"{failure}\n")
    print(f"Synchronized {count} platform ExternalSecrets, including product namespaces.")


if __name__ == "__main__":
    main()
