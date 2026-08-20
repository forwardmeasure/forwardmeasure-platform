#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:?Usage: $0 <configured-environment>}"
echo "Verifying deployed platform readiness for ${ENVIRONMENT}"

value() {
  "${SCRIPT_DIR}/environment-value.sh" "${ENVIRONMENT}" "$1"
}

kubectl --namespace "$(value namespaces.istio)" rollout status deployment/istiod --timeout=10m
kubectl --namespace "$(value namespaces.keycloak)" rollout status statefulset/keycloak --timeout=15m
kubectl --namespace "$(value namespaces.kafka)" wait --for=condition=Ready \
  "kafka/$(value kafka.clusterName)" --timeout=20m
kubectl --namespace "$(value namespaces.registry)" wait --for=condition=Ready \
  "apicurioregistry3/$(value registry.name)" --timeout=15m
kubectl --namespace "$(value namespaces.openSearch)" rollout status \
  statefulset/opensearch-cluster-master --timeout=30m
kubectl --namespace "$(value namespaces.knativeServing)" wait --for=condition=Ready \
  knativeserving/knative-serving --timeout=30m
kubectl --namespace "$(value namespaces.knativeEventing)" wait --for=condition=Ready \
  knativeeventing/knative-eventing --timeout=30m
kubectl --namespace "$(value namespaces.kserve)" rollout status \
  deployment/kserve-controller-manager --timeout=15m
kubectl --namespace "$(value namespaces.modelServing)" wait --for=condition=Ready --timeout=30m \
  inferenceservice/ner-gliner-medium-v2-1
kubectl --namespace "$(value namespaces.docling)" rollout status \
  deployment/docling-docling-serve --timeout=30m
kubectl --namespace "$(value namespaces.valkey)" rollout status statefulset/valkey --timeout=15m
kubectl --namespace "$(value namespaces.superset)" rollout status deployment/superset --timeout=15m
kubectl --namespace "$(value namespaces.platform)" rollout status \
  deployment/forwardmeasure-platform-dashboard --timeout=10m

echo "All mandatory shared-platform readiness gates passed."
