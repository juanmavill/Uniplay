#!/usr/bin/env bash
set -euo pipefail

exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
source /etc/uniplay/bootstrap.env

dnf install -y docker awscli
systemctl enable --now docker

mkdir -p /opt/uniplay/observability
aws s3 cp \
  "s3://$FRONTEND_BUCKET/deploy/observability/" \
  /opt/uniplay/observability/ \
  --recursive

docker network create uniplay >/dev/null 2>&1 || true
docker rm -f prometheus grafana >/dev/null 2>&1 || true

docker run -d --restart unless-stopped \
  --name prometheus \
  --network uniplay \
  -p 9090:9090 \
  -v /opt/uniplay/observability/prometheus.yml:/etc/prometheus/prometheus.yml:ro \
  -v /opt/uniplay/observability/alerts.yml:/etc/prometheus/alerts.yml:ro \
  prom/prometheus:v3.5.0 \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --web.enable-lifecycle

docker run -d --restart unless-stopped \
  --name grafana \
  --network uniplay \
  -p 3000:3000 \
  -e GF_SECURITY_ADMIN_USER="$GRAFANA_ADMIN_USER" \
  -e GF_SECURITY_ADMIN_PASSWORD="$GRAFANA_ADMIN_PASSWORD" \
  -e GF_USERS_ALLOW_SIGN_UP=false \
  -e GF_SERVER_ROOT_URL='%(protocol)s://%(domain)s/grafana/' \
  -e GF_SERVER_SERVE_FROM_SUB_PATH=true \
  -v /opt/uniplay/observability/grafana/provisioning:/etc/grafana/provisioning:ro \
  -v /opt/uniplay/observability/grafana/dashboards:/var/lib/grafana/dashboards:ro \
  grafana/grafana:12.1.0

for attempt in $(seq 1 60); do
  if curl -fsS http://localhost:3000/api/health >/dev/null; then
    echo "Observability is healthy"
    exit 0
  fi
  sleep 5
done

echo "Observability did not become healthy" >&2
exit 1
