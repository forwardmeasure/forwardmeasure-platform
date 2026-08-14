#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:-gcp-greenfield}"
echo "Verifying deployed platform readiness for ${ENVIRONMENT}"

kubectl --namespace istio-system rollout status deployment/istiod --timeout=10m
kubectl --namespace keycloak rollout status statefulset/keycloak --timeout=15m
kubectl --namespace kafka wait --for=condition=Ready kafka/kafka-cluster --timeout=20m
kubectl --namespace apicurio-registry wait --for=condition=Ready apicurioregistry3/platform-registry --timeout=15m
kubectl --namespace opensearch-cluster rollout status statefulset/opensearch-cluster-master --timeout=30m
kubectl --namespace knative-serving wait --for=condition=Ready knativeserving/knative-serving --timeout=30m
kubectl --namespace knative-eventing wait --for=condition=Ready knativeeventing/knative-eventing --timeout=30m
kubectl --namespace kserve rollout status deployment/kserve-controller-manager --timeout=15m
kubectl --namespace kserve-serving wait --for=condition=Ready --timeout=30m \
  inferenceservice/ner-gliner-medium-v2-1
kubectl --namespace docling-serve rollout status deployment/docling-docling-serve --timeout=30m
kubectl --namespace valkey rollout status statefulset/valkey --timeout=15m
kubectl --namespace superset rollout status deployment/superset --timeout=15m
kubectl --namespace forwardmeasure-platform rollout status \
  deployment/forwardmeasure-platform-dashboard --timeout=10m

echo "All mandatory shared-platform readiness gates passed."
