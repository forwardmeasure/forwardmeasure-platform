#!/usr/bin/env python3
"""Reload controllers after identity reconciliation, without fetching Secret data.

Rerun after an interruption: the pod-template fingerprint makes updates idempotent; rollout status
is checked even when an earlier invocation patched the template and then died. Jobs are never
replayed: drain running Jobs before credential rotation.

--defer-missing is for the early pass that runs before product installs. A controller whose pod
template requires a Secret that does not exist yet (e.g. a product's own release still references a
Secret that platform-secrets has since renamed) is left alone: restarting it now would only roll out
pods that cannot start, and its product stage re-renders it. The final pass runs without the flag.
"""
import argparse
import hashlib
import json
import subprocess
import time

ANNOTATION = "platform.forwardmeasure.com/secret-versions"
DATA_HASH = "reconcile.external-secrets.io/data-hash"


# The API server occasionally answers a request with a transient failure ("the server is currently
# unable to handle the request", a 5xx, a dropped connection) during control-plane maintenance or
# load. Such a request is retried with back-off; anything else (NotFound, Forbidden, a rollout that
# timed out, invalid input) fails immediately.
TRANSIENT = ("the server is currently unable to handle the request", "(InternalError)",
             "(ServiceUnavailable)", "(Timeout)", "TLS handshake timeout", "connection refused",
             "connection reset by peer", "i/o timeout", "client connection lost",
             "etcdserver: request timed out", "unexpected EOF")
RETRY_DELAYS = (2, 4, 8, 16)


def run_kubectl(args, sleep=time.sleep, run=subprocess.run):
    for delay in (*RETRY_DELAYS, None):
        result = run(["kubectl", *args], text=True, capture_output=True, check=False)
        if not result.returncode or delay is None or not any(m in result.stderr for m in TRANSIENT):
            return result
        print(f"kubectl {' '.join(args[:3])}: transient API server error, retrying in {delay}s: "
              f"{result.stderr.strip()[:200]}")
        sleep(delay)


def kubectl(*args):
    result = run_kubectl(args)
    if result.returncode:
        raise RuntimeError(f"kubectl {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def references(spec, required_only=False):
    names = set()

    def add(name, ref):
        if name and not (required_only and ref.get("optional")):
            names.add(name)

    for container in spec.get("containers", []) + spec.get("initContainers", []):
        for env in container.get("env", []):
            ref = env.get("valueFrom", {}).get("secretKeyRef", {})
            add(ref.get("name"), ref)
        for env in container.get("envFrom", []):
            ref = env.get("secretRef", {})
            add(ref.get("name"), ref)
    for volume in spec.get("volumes", []):
        ref = volume.get("secret", {})
        add(ref.get("secretName"), ref)
        for source in volume.get("projected", {}).get("sources", []):
            ref = source.get("secret", {})
            add(ref.get("name"), ref)
    return names


def existing_secrets(command):
    # Table output: names only, no Secret data crosses the wire.
    rows = command("get", "secrets", "--all-namespaces", "--no-headers")
    return {tuple(line.split()[:2]) for line in rows.splitlines() if line.strip()}


def reload(command=kubectl, timeout=300, defer_missing=False):
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
    present = existing_secrets(command) if defer_missing else None
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
        if present is not None:
            missing = sorted(secret for secret in references(template["spec"], required_only=True)
                             if (namespace, secret) not in present)
            if missing:
                print(f"Deferring {kind} {namespace}/{name}: requires missing Secret(s) {', '.join(missing)}; "
                      "its own release re-renders it and the final reload checks it.")
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
    parser.add_argument("--defer-missing", action="store_true",
                        help="skip controllers whose pod template requires a Secret that does not exist yet")
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    try:
        count = reload(timeout=args.timeout, defer_missing=args.defer_missing)
    except (RuntimeError, KeyError, ValueError) as failure:
        parser.exit(1, f"{failure}\n")
    print(f"Checked {count} controllers consuming platform secrets.")


if __name__ == "__main__":
    main()
