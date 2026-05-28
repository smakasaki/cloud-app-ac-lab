#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

echo "==> Installing kube-prometheus-stack"
helm upgrade --install kube-prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=admin \
  --set prometheus.prometheusSpec.podMonitorSelectorNilUsesHelmValues=false \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set nodeExporter.enabled=false \
  --set prometheusOperator.admissionWebhooks.patch.enabled=false \
  --set prometheusOperator.admissionWebhooks.enabled=false \
  --set prometheusOperator.tls.enabled=false \
  --set kubeStateMetrics.enabled=false \
  --set kubeEtcd.enabled=false \
  --set kubeScheduler.enabled=false \
  --set kubeControllerManager.enabled=false \
  --set kubeProxy.enabled=false \
  --set kubeDns.enabled=false \
  --set coreDns.enabled=false

echo "==> Installing Loki"
helm upgrade --install loki grafana/loki \
  --namespace monitoring \
  --set loki.commonConfig.replication_factor=1 \
  --set loki.storage.type=filesystem \
  --set singleBinary.replicas=1 \
  --set loki.auth_enabled=false \
  --set deploymentMode=SingleBinary \
  --set backend.replicas=0 \
  --set read.replicas=0 \
  --set write.replicas=0 \
  --set loki.useTestSchema=true

echo "==> Installing Grafana Alloy"
helm upgrade --install alloy grafana/alloy \
  --namespace monitoring \
  -f "$SCRIPT_DIR/alloy-values.yaml"

echo "==> Applying ServiceMonitor for cloud-app"
kubectl apply -f "$SCRIPT_DIR/servicemonitor.yml"

echo ""
echo "Prometheus: kubectl port-forward -n monitoring svc/kube-prometheus-kube-prome-prometheus 9090:9090"
echo "Grafana:    kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:80"
echo "Alloy:      kubectl port-forward -n monitoring svc/alloy 12345:12345"
echo "Loki URL (in Grafana datasource): http://loki-gateway.monitoring.svc.cluster.local"
echo "Creds:      admin / admin"