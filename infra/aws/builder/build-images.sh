#!/usr/bin/env bash
set -euo pipefail

exec > >(tee -a /var/log/uniplay-image-build.log | logger -t uniplay-builder -s 2>/dev/console) 2>&1
source /etc/uniplay/builder.env

report_status() {
  local status="$1"
  printf '%s\n' "$status" >/tmp/build-status.txt
  aws s3 cp /tmp/build-status.txt "s3://$ARTIFACT_BUCKET/$STATUS_KEY"
  aws s3 cp /var/log/uniplay-image-build.log "s3://$ARTIFACT_BUCKET/$LOG_KEY" || true
}

trap 'report_status FAILED' ERR

dnf install -y docker unzip awscli
systemctl enable --now docker

aws s3 cp "s3://$ARTIFACT_BUCKET/$SOURCE_KEY" /tmp/uniplay-source.zip
rm -rf /workspace/uniplay
mkdir -p /workspace/uniplay
unzip -q /tmp/uniplay-source.zip -d /workspace/uniplay
cd /workspace/uniplay

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY_HOST"

for service in api-gateway room-service game-service realtime-service voice-service metrics-service; do
  echo "Building $service"
  docker build \
    --tag "$REGISTRY_PREFIX/$service:$IMAGE_TAG" \
    "$service"
  docker push "$REGISTRY_PREFIX/$service:$IMAGE_TAG"
done

echo "Building static frontend"
docker build --tag uniplay-frontend-build:$IMAGE_TAG frontend
frontend_container="$(docker create uniplay-frontend-build:$IMAGE_TAG)"
rm -rf /tmp/frontend-dist
mkdir -p /tmp/frontend-dist
docker cp "$frontend_container:/usr/share/nginx/html/." /tmp/frontend-dist
docker rm "$frontend_container"

aws s3 sync /tmp/frontend-dist "s3://$FRONTEND_BUCKET/" \
  --delete \
  --exclude 'deploy/*'

report_status SUCCESS
shutdown -h now
