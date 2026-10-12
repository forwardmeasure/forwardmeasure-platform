#!/usr/bin/env python3
"""Synchronize platform ExternalSecrets without reading or printing secret values."""
import argparse
import json
import subprocess
import time
import uuid

import yaml

MARKER = "platform.forwardmeasure.com/secret-sync"
RELEASE = "platform-secrets"
RELEASE_NAME = "meta.helm.sh/release-name"
RELEASE_NAMESPACE = "meta.helm.sh/release-namespace"


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


def kubectl(*args, optional=False):
    result = run_kubectl(args)
    if result.returncode:
        if optional and "(NotFound)" in result.stderr:
            return ""
        raise RuntimeError(f"kubectl {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def helm(*args):
    result = subprocess.run(["helm", *args], text=True, capture_output=True, check=False)
    if result.returncode:
        raise RuntimeError(f"helm {' '.join(args[:3])} failed: {result.stderr.strip()}")
    return result.stdout


def released_externalsecrets(release_namespace, helm_command=helm):
    """(namespace, name) of every ExternalSecret in the release's current deployed manifest."""
    manifest = helm_command("get", "manifest", RELEASE, "--namespace", release_namespace)
    owned = set()
    for document in yaml.safe_load_all(manifest):
        if document and document.get("kind") == "ExternalSecret":
            metadata = document["metadata"]
            owned.add((metadata.get("namespace") or release_namespace, metadata["name"]))
    if not owned:
        # Never treat an empty or unreadable manifest as "everything is an orphan".
        raise RuntimeError(f"The {RELEASE} release in {release_namespace} has no ExternalSecrets")
    return owned


def sync(store, timeout=300, command=kubectl, now=time.monotonic, sleep=time.sleep,
         helm_command=helm, report=print):
    # Helm replaces managed-by with "Helm" at install time. Use release ownership,
    # not the chart's rendered managed-by label, to identify installed resources.
    inventory = json.loads(command("get", "externalsecrets", "--all-namespaces", "-o", "json"))
    labelled = [item for item in inventory["items"]
                if item["metadata"].get("annotations", {}).get(RELEASE_NAME) == RELEASE
                and item["spec"]["secretStoreRef"]["name"] == store
                and item["spec"]["secretStoreRef"].get("kind", "SecretStore") == "ClusterSecretStore"]
    if not labelled:
        raise RuntimeError(f"No platform ExternalSecrets reference {store}")
    release_namespaces = {item["metadata"]["annotations"].get(RELEASE_NAMESPACE) for item in labelled}
    if len(release_namespaces) != 1 or None in release_namespaces:
        raise RuntimeError(f"Cannot tell which namespace the {RELEASE} release is in: {release_namespaces}")
    owned = released_externalsecrets(release_namespaces.pop(), helm_command)
    # A failed Helm upgrade leaves the objects it created behind, still annotated with the release:
    # Helm only removes what was in the last successful revision. Such an orphan is not part of the
    # release any more, can wait forever on a key a later stage publishes, and blocks the release
    # that now owns that name from installing. Remove it, then synchronize only what the release owns.
    selected = []
    for item in labelled:
        namespace, name = item["metadata"]["namespace"], item["metadata"]["name"]
        if (namespace, name) in owned:
            selected.append(item)
            continue
        command("--namespace", namespace, "delete", "externalsecret", name, "--ignore-not-found")
        report(f"Removed {namespace}/{name}: labelled as {RELEASE} but not in its deployed manifest "
               "(left behind by a failed upgrade).")
    if not selected:
        raise RuntimeError(f"No ExternalSecrets of the deployed {RELEASE} release reference {store}")
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
    except (RuntimeError, ValueError, KeyError, yaml.YAMLError) as failure:
        parser.exit(1, f"{failure}\n")
    print(f"Synchronized {count} platform ExternalSecrets, including product namespaces.")


if __name__ == "__main__":
    main()
