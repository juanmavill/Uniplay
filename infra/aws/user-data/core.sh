#!/usr/bin/env bash
set -euo pipefail

exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
source /etc/uniplay/bootstrap.env

dnf install -y docker awscli jq
systemctl enable --now docker

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY_HOST"

LIVEKIT_SECRET="$(aws secretsmanager get-secret-value \
  --region "$AWS_REGION" \
  --secret-id "$LIVEKIT_SECRET_ARN" \
  --query SecretString \
  --output text)"
LIVEKIT_API_KEY="$(jq -r .apiKey <<<"$LIVEKIT_SECRET")"
LIVEKIT_API_SECRET="$(jq -r .apiSecret <<<"$LIVEKIT_SECRET")"

docker network create uniplay >/dev/null 2>&1 || true
docker rm -f room-service game-service metrics-service voice-service >/dev/null 2>&1 || true

docker run -d --restart unless-stopped \
  --name room-service \
  --network uniplay \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT=6379 \
  -e ROOM_SERVICE_PORT=8081 \
  "$REGISTRY_PREFIX/room-service:$IMAGE_TAG"

docker run -d --restart unless-stopped \
  --name game-service \
  --network uniplay \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT=6379 \
  -e GAME_SERVICE_PORT=8082 \
  -e GAME_POINTS_PER_CORRECT_ANSWER=100 \
  -e GAME_DRAWER_MAJORITY_BONUS=50 \
  -e GAME_ROUND_DURATION=PT1M \
  -e GAME_SESSION_TTL=PT2H \
  "$REGISTRY_PREFIX/game-service:$IMAGE_TAG"

docker run -d --restart unless-stopped \
  --name metrics-service \
  --network uniplay \
  -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT=6379 \
  -e METRICS_SERVICE_PORT=8084 \
  "$REGISTRY_PREFIX/metrics-service:$IMAGE_TAG"

docker run -d --restart unless-stopped \
  --name voice-service \
  --network uniplay \
  -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT=6379 \
  -e VOICE_SERVICE_PORT=8085 \
  -e LIVEKIT_URL="$LIVEKIT_PUBLIC_URL" \
  -e LIVEKIT_PUBLIC_URL="$LIVEKIT_PUBLIC_URL" \
  -e LIVEKIT_API_KEY="$LIVEKIT_API_KEY" \
  -e LIVEKIT_API_SECRET="$LIVEKIT_API_SECRET" \
  -e VOICE_TOKEN_TTL=PT30M \
  "$REGISTRY_PREFIX/voice-service:$IMAGE_TAG"

TOKEN="$(curl -fsS -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 21600' http://169.254.169.254/latest/api/token)"
PRIVATE_IP="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)"

cat >/tmp/core-dns.json <<EOF
{
  "Changes": [{
    "Action": "UPSERT",
    "ResourceRecordSet": {
      "Name": "core.uniplay.internal",
      "Type": "A",
      "TTL": 30,
      "ResourceRecords": [{"Value": "$PRIVATE_IP"}]
    }
  }]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --change-batch file:///tmp/core-dns.json >/dev/null

for port in 8081 8082 8084 8085; do
  for attempt in $(seq 1 60); do
    if curl -fsS "http://localhost:$port/actuator/health" >/dev/null; then
      break
    fi
    if [ "$attempt" -eq 60 ]; then
      echo "Service on port $port did not become healthy" >&2
      exit 1
    fi
    sleep 5
  done
done

echo "Core is healthy"
