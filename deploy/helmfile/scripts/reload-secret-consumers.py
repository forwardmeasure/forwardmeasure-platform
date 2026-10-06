#!/usr/bin/env python3
"""Reload controllers after identity reconciliation, without fetching Secret data.

Rerun after an interruption: the pod-template fingerprint makes updates idempotent; rollout status
is checked even when an earlier invocation patched the template and then died. Jobs are never
replayed: drain running Jobs before credential rotation.
"""
import argparse
import hashlib
import json
import subprocess

ANNOTATION = "platform.forwardmeasure.com/secret-versions"
DATA_HASH = "reconcile.external-secrets.io/data-hash"


def kubectl(*args):
    result = subprocess.run(["kubectl", *args], text=True, capture_output=True, check=False)
    if result.returncode:
        raise RuntimeError(f"kubectl {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def references(spec):
    names = set()
    for container in spec.get("containers", []) + spec.get("initContainers", []):
        for env in container.get("env", []):
            name = env.get("valueFrom", {}).get("secretKeyRef", {}).get("name")
            if name:
                names.add(name)
        for env in container.get("envFrom", []):
            name = env.get("secretRef", {}).get("name")
            if name:
                names.add(name)
    for volume in spec.get("volumes", []):
        name = volume.get("secret", {}).get("secretName")
        if name:
            names.add(name)
        for source in volume.get("projected", {}).get("sources", []):
            name = source.get("secret", {}).get("name")
            if name:
                names.add(name)
    return names


def reload(command=kubectl, timeout=300):
    inventory = json.loads(command("get", "externalsecrets", "--all-namespaces", "-l",
                                   "platform.forwardmeasure.com/identity-client-credentials=true", "-o", "json"))
    # Helm overwrites managed-by; the release annotation survives installation.
    selected = [item for item in inventory["items"]
                if item["metadata"].get("annotations", {}).get("meta.helm.sh/release-name") == "platform-secrets"]
    if not selected:
        raise RuntimeError("No identity client ExternalSecrets found; install the updated platform-secrets chart first")
    versions = {}
    for item in selected:
        namespace = item["metadata"]["namespace"]
        name = item["spec"].get("target", {}).get("name") or item["metadata"]["name"]
        digest = command("-n", namespace, "get", "secret", name, "-o",
                         'go-template={{ index .metadata.annotations "' + DATA_HASH + '" }}')
        if not digest or digest == "<no value>":
            raise RuntimeError(f"No ESO data hash on {namespace}/{name}; synchronize secrets first")
        versions[(namespace, name)] = digest
    controllers = json.loads(command("get", "deployments,statefulsets,daemonsets,cronjobs,jobs",
                                     "--all-namespaces", "-o", "json"))
    desired = []
    for item in controllers["items"]:
        kind, meta = item["kind"].lower(), item["metadata"]
        namespace, name = meta["namespace"], meta["name"]
        template = item["spec"].get("template")
        if kind == "cronjob":
            template = item["spec"]["jobTemplate"]["spec"]["template"]
        if not template:
            continue
        used = {name: versions[(namespace, name)] for name in references(template["spec"])
                if (namespace, name) in versions}
        if not used:
            continue
        digest = hashlib.sha256(json.dumps(used, sort_keys=True).encode()).hexdigest()
        current = template.get("metadata", {}).get("annotations", {}).get(ANNOTATION)
        if kind == "job":
            if item.get("status", {}).get("active", 0) and current != digest:
                raise RuntimeError(f"Drain active Job {namespace}/{name} before rotating credentials; "
                                   "it will not be replayed automatically")
            continue
        if kind in ("statefulset", "daemonset") and item["spec"].get("updateStrategy", {}).get("type") == "OnDelete":
            if current != digest:
                raise RuntimeError(f"{namespace}/{name} uses OnDelete; explicit consumer reload required")
        desired.append((kind, namespace, name, digest, current))
    # Validate all controllers before the first mutation.
    for kind, namespace, name, digest, current in desired:
        if current != digest:
            patch = {"metadata": {"annotations": {ANNOTATION: digest}}}
            if kind == "cronjob":
                patch = {"spec": {"jobTemplate": {"spec": {"template": patch}}}}
            else:
                patch = {"spec": {"template": patch}}
            command("-n", namespace, "patch", kind, name, "--type=merge", "--patch", json.dumps(patch))
        if kind != "cronjob":
            command("-n", namespace, "rollout", "status", f"{kind}/{name}", f"--timeout={timeout}s")
    return len(desired)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    try:
        count = reload(timeout=args.timeout)
    except (RuntimeError, KeyError, ValueError) as failure:
        parser.exit(1, f"{failure}\n")
    print(f"Checked {count} controllers consuming platform secrets.")


if __name__ == "__main__":
    main()
