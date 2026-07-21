#!/usr/bin/env bash
set -euo pipefail

exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
source /etc/uniplay/bootstrap.env

dnf install -y docker awscli jq
systemctl enable --now docker

LIVEKIT_SECRET="$(aws secretsmanager get-secret-value \
  --region "$AWS_REGION" \
  --secret-id "$LIVEKIT_SECRET_ARN" \
  --query SecretString \
  --output text)"
LIVEKIT_API_KEY="$(jq -r .apiKey <<<"$LIVEKIT_SECRET")"
LIVEKIT_API_SECRET="$(jq -r .apiSecret <<<"$LIVEKIT_SECRET")"

mkdir -p /opt/uniplay
cat >/opt/uniplay/livekit.yaml <<EOF
port: 7880
rtc:
  tcp_port: 7881
  udp_port: 7882
redis:
  address: $REDIS_HOST:6379
keys:
  $LIVEKIT_API_KEY: $LIVEKIT_API_SECRET
EOF

docker rm -f livekit >/dev/null 2>&1 || true
docker run -d --restart unless-stopped \
  --name livekit \
  --network host \
  -v /opt/uniplay/livekit.yaml:/etc/livekit.yaml:ro \
  livekit/livekit-server:v1.9.7 \
  --config /etc/livekit.yaml \
  --node-ip "$LIVEKIT_PUBLIC_IP"

for attempt in $(seq 1 60); do
  if curl -fsS http://localhost:7880 >/dev/null; then
    echo "LiveKit is healthy"
    exit 0
  fi
  sleep 5
done

echo "LiveKit did not become healthy" >&2
exit 1
