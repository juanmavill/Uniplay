#!/usr/bin/env bash
set -euo pipefail

exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
source /etc/uniplay/bootstrap.env

dnf install -y docker awscli
systemctl enable --now docker

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY_HOST"

docker network create uniplay >/dev/null 2>&1 || true
docker rm -f realtime-service api-gateway >/dev/null 2>&1 || true

docker run -d --restart unless-stopped \
  --name realtime-service \
  --network uniplay \
  -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT=6379 \
  -e REALTIME_SERVICE_PORT=8083 \
  -e REALTIME_ALLOWED_ORIGINS='*' \
  "$REGISTRY_PREFIX/realtime-service:$IMAGE_TAG"

docker run -d --restart unless-stopped \
  --name api-gateway \
  --network uniplay \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e API_GATEWAY_PORT=8080 \
  -e ROOM_SERVICE_URI=http://core.uniplay.internal:8081 \
  -e GAME_SERVICE_URI=http://core.uniplay.internal:8082 \
  -e REALTIME_SERVICE_URI=http://realtime-service:8083 \
  -e REALTIME_WEBSOCKET_URI=ws://realtime-service:8083 \
  -e METRICS_SERVICE_URI=http://core.uniplay.internal:8084 \
  -e VOICE_SERVICE_URI=http://core.uniplay.internal:8085 \
  -e GATEWAY_ALLOWED_ORIGINS='*' \
  -e GATEWAY_RATE_LIMIT_ENABLED=true \
  -e GATEWAY_RATE_LIMIT_REQUESTS_PER_WINDOW=600 \
  -e GATEWAY_RATE_LIMIT_WINDOW=PT1M \
  "$REGISTRY_PREFIX/api-gateway:$IMAGE_TAG"

TOKEN="$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 21600' http://169.254.169.254/latest/api/token)"
PRIVATE_IP="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)"

cat >/tmp/edge-dns.json <<EOF
{
  "Changes": [{
    "Action": "UPSERT",
    "ResourceRecordSet": {
      "Name": "edge.uniplay.internal",
      "Type": "A",
      "TTL": 30,
      "ResourceRecords": [{"Value": "$PRIVATE_IP"}]
    }
  }]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --change-batch file:///tmp/edge-dns.json >/dev/null

for attempt in $(seq 1 60); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null; then
    echo "Edge is healthy"
    exit 0
  fi
  sleep 5
done

echo "Edge did not become healthy" >&2
exit 1
