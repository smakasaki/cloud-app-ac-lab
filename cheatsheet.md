# Cloud App — Cheatsheet

## Local Development

```bash
# Start only postgres
docker compose up -d postgres

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Full stack locally (app + postgres + prometheus + loki + alloy + grafana)
docker compose up --build

# Stop
docker compose down
docker compose down -v  # with volume removal
```

---

## Docker

```bash
# Build image
docker build -t cloud-app:local .

# Run image locally
docker run --rm \
  --link local-postgres:postgres \
  -e DB_URL=jdbc:postgresql://postgres:5432/cloudapp \
  -e DB_USER=cloudapp \
  -e DB_PASSWORD=cloudapp \
  -p 8080:8080 \
  cloud-app:local

# Push image manually to DockerHub
docker build -t YOUR_USERNAME/cloud-app:latest .
docker push YOUR_USERNAME/cloud-app:latest
docker push YOUR_USERNAME/cloud-app:$(git rev-parse --short HEAD)
```

---

## GCloud — Cluster Management

```bash
# Authenticate
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# Enable GKE API
gcloud services enable container.googleapis.com

# Create cluster (Standard, pd-standard to avoid consuming SSD quota)
gcloud container clusters create cloud-app-cluster \
  --zone us-central1-a \
  --num-nodes 2 \
  --machine-type e2-standard-2 \
  --disk-type pd-standard \
  --disk-size 50 \
  --no-enable-autoupgrade

# Connect to cluster
gcloud container clusters get-credentials cloud-app-cluster \
  --zone us-central1-a

# List clusters
gcloud container clusters list

# Add a node
gcloud container clusters resize cloud-app-cluster \
  --node-pool default-pool \
  --num-nodes 3 \
  --zone us-central1-a

# Delete cluster
gcloud container clusters delete cloud-app-cluster \
  --zone us-central1-a --quiet
```

---

## Application Deployment

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/app/config.yml
kubectl apply -f k8s/postgres/postgres.yml
kubectl apply -f k8s/app/deployment.yml
kubectl apply -f k8s/app/service.yml
kubectl apply -f k8s/app/hpa.yml

# Install ingress-nginx
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace

# Install monitoring
bash k8s/monitoring/install-monitoring.sh
```

---

## Updating the Application

### Via CI/CD (recommended)

On every `git push` to `main`, GitHub Actions automatically:
1. Runs tests
2. Builds a Docker image
3. Pushes to DockerHub with `:latest` and `:<git-sha>` tags

```bash
git add .
git commit -m "feat: your changes"
git push origin main
# CI will build and push the image automatically
```

Once CI finishes — pull the new image into the cluster:

```bash
# Restart pods — they will pull :latest from DockerHub
kubectl rollout restart deployment/cloud-app -n cloud-app

# Watch progress
kubectl rollout status deployment/cloud-app -n cloud-app
```

### Deploy a specific tag

```bash
# Update image to a specific git sha
kubectl set image deployment/cloud-app \
  cloud-app=YOUR_USERNAME/cloud-app:GIT_SHA \
  -n cloud-app

# Watch progress
kubectl rollout status deployment/cloud-app -n cloud-app
```

### Force pull latest

```bash
# If imagePullPolicy: Always — restart is enough
kubectl rollout restart deployment/cloud-app -n cloud-app

# Check which image is currently in use
kubectl get deployment cloud-app -n cloud-app \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
```

---

## Rollback

```bash
# View deployment history
kubectl rollout history deployment/cloud-app -n cloud-app

# View details of a specific revision
kubectl rollout history deployment/cloud-app -n cloud-app --revision=2

# Roll back to the previous version
kubectl rollout undo deployment/cloud-app -n cloud-app

# Roll back to a specific revision
kubectl rollout undo deployment/cloud-app -n cloud-app --to-revision=1

# Watch rollback progress
kubectl rollout status deployment/cloud-app -n cloud-app

# Verify rollback succeeded
kubectl get pods -n cloud-app
curl http://EXTERNAL_IP/
```

### Add a description to a deployment (for history)

In `k8s/app/deployment.yml` add an annotation:

```yaml
metadata:
  name: cloud-app
  namespace: cloud-app
  annotations:
    kubernetes.io/change-cause: "feat: add quotes endpoint v1.2"
```

Then `kubectl rollout history` will show descriptions instead of `<none>`.

---

## Kubectl — Common Commands

```bash
# Pods
kubectl get pods -n cloud-app
kubectl get pods -n cloud-app -w                     # watch
kubectl logs -n cloud-app deployment/cloud-app
kubectl logs -f -n cloud-app deployment/cloud-app    # follow
kubectl describe pod -n cloud-app <pod-name>

# Deployments and services
kubectl get deployments -n cloud-app
kubectl get hpa -n cloud-app
kubectl get pvc -n cloud-app
kubectl get ingress -n cloud-app
kubectl get svc -n cloud-app
kubectl get all -n cloud-app                         # everything at once

# Nodes
kubectl get nodes
kubectl describe nodes | grep -A8 "Allocated resources"

# Events (useful for debugging)
kubectl get events -n cloud-app --sort-by='.lastTimestamp'

# Exec into a pod
kubectl exec -it -n cloud-app deployment/cloud-app -- sh
```

---

## Monitoring — Port-Forward

```bash
# Prometheus
kubectl port-forward -n monitoring svc/kube-prometheus-kube-prome-prometheus 9090:9090
# http://localhost:9090

# Grafana
kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:80
# http://localhost:3000  (admin / admin)

# Alloy
kubectl port-forward -n monitoring svc/alloy 12345:12345
# http://localhost:12345
```

---

## Requirements Checklist

### Req 1 — Docker image

```bash
docker build -t cloud-app:local .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/cloudapp \
  -e DB_USER=cloudapp \
  -e DB_PASSWORD=cloudapp \
  cloud-app:local
curl http://localhost:8080/
```

### Req 2 — Docker registry

```bash
# Verify the image exists on DockerHub
docker pull YOUR_USERNAME/cloud-app:latest
# or open https://hub.docker.com/r/YOUR_USERNAME/cloud-app
```

### Req 4 — Cloud cluster

```bash
gcloud container clusters list
kubectl get nodes
```

### Req 5 — Accessible from the internet

```bash
kubectl get ingress -n cloud-app
EXTERNAL_IP=$(kubectl get ingress -n cloud-app \
  -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}')
curl http://$EXTERNAL_IP/
curl http://$EXTERNAL_IP/quotes/stats
```

### Req 6 — Scaling

```bash
kubectl scale deployment cloud-app --replicas=4 -n cloud-app
kubectl get pods -n cloud-app -w
kubectl scale deployment cloud-app --replicas=2 -n cloud-app
```

### Req 7 — Zero-downtime update

```bash
# Terminal 1 — continuous requests
EXTERNAL_IP=$(kubectl get ingress -n cloud-app \
  -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}')
while true; do curl -s http://$EXTERNAL_IP/ ; sleep 0.5; done

# Terminal 2 — deploy new version
kubectl rollout restart deployment/cloud-app -n cloud-app
kubectl rollout status deployment/cloud-app -n cloud-app
# Requests in terminal 1 should not be interrupted
```

### Req 8 — Rollback

```bash
kubectl rollout history deployment/cloud-app -n cloud-app
kubectl rollout undo deployment/cloud-app -n cloud-app
kubectl rollout status deployment/cloud-app -n cloud-app
kubectl get pods -n cloud-app
```

### Req 9 — Monitoring

```bash
kubectl get pods -n monitoring
kubectl port-forward -n monitoring svc/kube-prometheus-kube-prome-prometheus 9090:9090
# http://localhost:9090 → query: http_server_requests_seconds_count
```

### Req 10 — Autoscaling

```bash
kubectl get hpa -n cloud-app
kubectl describe hpa cloud-app-hpa -n cloud-app
# Shows: minReplicas, maxReplicas, current CPU/memory metrics
```

### Req 11 — Logs in Loki

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-grafana 3000:80
# http://localhost:3000 → Explore → Loki → {namespace="cloud-app"}
```

### Req 12 — Metrics in Prometheus

```bash
EXTERNAL_IP=$(kubectl get ingress -n cloud-app \
  -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}')
curl http://$EXTERNAL_IP/actuator/prometheus | grep http_server
# Prometheus UI: http://localhost:9090 → http_server_requests_seconds_count
```

### Req 13 — DB in a separate container

```bash
kubectl get pods -n cloud-app
kubectl get deployment postgres -n cloud-app
# Shows two separate deployments: cloud-app and postgres
```

### Req 14 — Storage mounted to DB

```bash
kubectl get pvc -n cloud-app
kubectl describe pod -n cloud-app -l app=postgres | grep -A5 Mounts
# Shows: /var/lib/postgresql/data from postgres-storage (rw)
```

---

## Application Endpoints

```bash
EXTERNAL_IP=$(kubectl get ingress -n cloud-app \
  -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}')

curl http://$EXTERNAL_IP/
curl http://$EXTERNAL_IP/quotes
curl http://$EXTERNAL_IP/quotes/random
curl http://$EXTERNAL_IP/quotes/stats
curl http://$EXTERNAL_IP/quotes/author/linus
curl http://$EXTERNAL_IP/actuator/health
curl http://$EXTERNAL_IP/actuator/prometheus | head -20

curl -X POST http://$EXTERNAL_IP/quotes \
  -H "Content-Type: application/json" \
  -d '{"text": "Talk is cheap. Show me the code.", "author": "Linus Torvalds"}'

curl -X DELETE http://$EXTERNAL_IP/quotes/1
```
