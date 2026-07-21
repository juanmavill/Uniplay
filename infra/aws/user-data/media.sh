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
mkdir -p /opt/uniplay/caddy-data /opt/uniplay/caddy-config /srv/uniplay
aws s3 sync "s3://$FRONTEND_BUCKET" /srv/uniplay \
  --delete \
  --exclude 'deploy/*'

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

cat >/opt/uniplay/Caddyfile <<EOF
$APP_DOMAIN {
  encode gzip zstd

  @backend path /salas* /rooms* /games* /metrics* /voice* /ws* /actuator* /grafana*
  reverse_proxy @backend http://$APPLICATION_ALB {
    header_up X-UniPlay-Origin $ORIGIN_VERIFY_SECRET
  }

  root * /srv/uniplay
  try_files {path} /index.html
  file_server
}

$LIVEKIT_DOMAIN {
  reverse_proxy 127.0.0.1:7880
}
EOF

docker rm -f livekit >/dev/null 2>&1 || true
docker run -d --restart unless-stopped \
  --name livekit \
  --network host \
  -v /opt/uniplay/livekit.yaml:/etc/livekit.yaml:ro \
  livekit/livekit-server:v1.9.7 \
  --config /etc/livekit.yaml \
  --node-ip "$LIVEKIT_PUBLIC_IP"

docker rm -f caddy >/dev/null 2>&1 || true
docker run -d --restart unless-stopped \
  --name caddy \
  --network host \
  -v /opt/uniplay/Caddyfile:/etc/caddy/Caddyfile:ro \
  -v /opt/uniplay/caddy-data:/data \
  -v /opt/uniplay/caddy-config:/config \
  -v /srv/uniplay:/srv/uniplay:ro \
  caddy:2.8-alpine

for attempt in $(seq 1 60); do
  if curl -fsS http://localhost:7880 >/dev/null \
    && curl -fsS -H "Host: $APP_DOMAIN" http://localhost/index.html >/dev/null; then
    echo "LiveKit and Caddy are healthy"
    exit 0
  fi
  sleep 5
done

echo "LiveKit did not become healthy" >&2
exit 1
