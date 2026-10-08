#!/usr/bin/env python3
"""Render identity manifests offline, using public base values and synthetic remote keys only."""
import argparse
from pathlib import Path
import subprocess
import tempfile
import yaml

ROOT = Path(__file__).resolve().parents[3]
WORKSPACE = ROOT.parent


def run(*args):
    result = subprocess.run(args, cwd=ROOT, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr)
    return result.stdout


def render(decision_engine_enabled=None):
    base = list(yaml.safe_load_all(run("helmfile", "--file", str(ROOT / "deploy/helmfile/helmfile.yaml.gotmpl"),
                                       "--environment", "base", "build", "--embed-values")))
    values = base[0]["renderedvalues"]
    if decision_engine_enabled is not None:
        values["platform"]["products"]["decisionEngine"]["enabled"] = decision_engine_enabled
    values["secretStore"]["remoteKeys"].update({
        "openworkflowRuntimeDatabasePassword": "fixture-fowf-runtime",
        "openworkflowAdministratorDatabasePassword": "fixture-admin",
        "entityIntelligenceRuntimeDatabasePassword": "fixture-fei-runtime"})
    chart = WORKSPACE / "helm-charts/charts/platform-secrets-helm-chart/helm-chart-sources"
    with tempfile.TemporaryDirectory(prefix="identity-render-") as directory:
        directory = Path(directory)
        (directory / "values.yaml").write_text(yaml.safe_dump(values))
        state = {"environments": {"default": {"values": [str(directory / "values.yaml")]}},
                 "releases": [{"name": "identity-fixture", "chart": str(chart), "values": [
                     str(ROOT / "deploy/helmfile/releases/platform-secrets/gcp/base.yaml.gotmpl")]}]}
        (directory / "helmfile.yaml").write_text(yaml.safe_dump({"environments": state["environments"]}) + "---\n" + yaml.safe_dump({"releases": state["releases"]}))
        rendered = list(yaml.safe_load_all(run("helmfile", "--file", str(directory / "helmfile.yaml"),
                                               "build", "--embed-values")))[0]
        secret_values = rendered["releases"][0]["values"][0]
        (directory / "secrets.yaml").write_text(yaml.safe_dump(secret_values))
        manifests = run("helm", "template", "identity-fixture", str(chart), "--values", str(directory / "secrets.yaml"))
    return values, secret_values, list(yaml.safe_load_all(manifests))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    _, _, manifests = render()
    args.output.write_text(yaml.safe_dump_all(manifests))
    print(f"Rendered {len(manifests)} resources using synthetic configuration to {args.output}")
