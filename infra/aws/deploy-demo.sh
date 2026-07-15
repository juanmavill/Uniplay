#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-us-west-2}"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REG="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
VPC="$(aws ec2 describe-vpcs --region "$REGION" --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
CIDR="$(aws ec2 describe-vpcs --region "$REGION" --vpc-ids "$VPC" --query 'Vpcs[0].CidrBlock' --output text)"
SUBNET="$(aws ec2 describe-subnets --region "$REGION" --filters Name=default-for-az,Values=true --query 'Subnets[0].SubnetId' --output text)"
AMI="$(aws ec2 describe-images --region "$REGION" --owners amazon --filters 'Name=name,Values=al2023-ami-2023.*-x86_64' Name=state,Values=available --query 'sort_by(Images,&CreationDate)[-1].ImageId' --output text)"
SG_NAME="uniplay-demo-20260715"
SG="$(aws ec2 describe-security-groups --region "$REGION" --filters Name=group-name,Values="$SG_NAME" Name=vpc-id,Values="$VPC" --query 'SecurityGroups[0].GroupId' --output text)"

if [ "$SG" = "None" ] || [ -z "$SG" ]; then
  SG="$(aws ec2 create-security-group --region "$REGION" --group-name "$SG_NAME" --description "UniPlay demo deployment" --vpc-id "$VPC" --query GroupId --output text)"
fi

aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG" --protocol -1 --cidr "$CIDR" >/dev/null 2>&1 || true
for port in 80 8080 3000 9090 7880 7881; do
  aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG" --protocol tcp --port "$port" --cidr 0.0.0.0/0 >/dev/null 2>&1 || true
done
aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG" --protocol udp --port 7882 --cidr 0.0.0.0/0 >/dev/null 2>&1 || true

cat >/tmp/uniplay-livekit-userdata.sh <<'EOF'
#!/bin/bash
set -euxo pipefail
exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
dnf install -y docker awscli
systemctl enable --now docker
TOKEN=$(curl -sS -X PUT -H "X-aws-ec2-metadata-token-ttl-seconds: 21600" http://169.254.169.254/latest/api/token)
PRIVATE_IP=$(curl -sS -H "X-aws-ec2-metadata-token:$TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)
docker run -d --restart unless-stopped --name livekit --network host livekit/livekit-server:latest --dev --bind 0.0.0.0 --node-ip "$PRIVATE_IP"
EOF

LIVEKIT_ID="$(aws ec2 run-instances --region "$REGION" --image-id "$AMI" --instance-type t3.small --count 1 --subnet-id "$SUBNET" --security-group-ids "$SG" --iam-instance-profile Name=LabInstanceProfile --associate-public-ip-address --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3,DeleteOnTermination=true}' --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=uniplay-livekit}]' --user-data fileb:///tmp/uniplay-livekit-userdata.sh --query 'Instances[0].InstanceId' --output text)"
aws ec2 wait instance-running --region "$REGION" --instance-ids "$LIVEKIT_ID"
for attempt in $(seq 1 30); do
  LIVEKIT_PRIVATE_IP="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$LIVEKIT_ID" --query 'Reservations[0].Instances[0].PrivateIpAddress' --output text)"
  LIVEKIT_PUBLIC_IP="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$LIVEKIT_ID" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"
  [ "$LIVEKIT_PUBLIC_IP" != "None" ] && break
  sleep 2
done

cat >/tmp/uniplay-core-userdata.sh <<EOF
#!/bin/bash
set -euxo pipefail
exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
dnf install -y docker awscli
systemctl enable --now docker
LIVEKIT_PRIVATE_IP="$LIVEKIT_PRIVATE_IP"
LIVEKIT_PUBLIC_IP="$LIVEKIT_PUBLIC_IP"
REG="$REG"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "\$REG"
mkdir -p /opt/uniplay
docker network create uniplay || true
docker run -d --restart unless-stopped --name redis --network uniplay -p 6379:6379 redis:7.4-alpine redis-server --appendonly yes
docker run -d --restart unless-stopped --name room-service --network uniplay -p 8081:8081 -e SPRING_PROFILES_ACTIVE=docker -e REDIS_HOST=redis -e REDIS_PORT=6379 -e ROOM_SERVICE_PORT=8081 "\$REG/uniplay/room-service:latest"
docker run -d --restart unless-stopped --name game-service --network uniplay -p 8082:8082 -e SPRING_PROFILES_ACTIVE=docker -e REDIS_HOST=redis -e REDIS_PORT=6379 -e GAME_SERVICE_PORT=8082 -e GAME_POINTS_PER_CORRECT_ANSWER=100 -e GAME_ROUND_DURATION=PT1M -e GAME_SESSION_TTL=PT2H "\$REG/uniplay/game-service:latest"
docker run -d --restart unless-stopped --name metrics-service --network uniplay -p 8084:8084 -e SPRING_PROFILES_ACTIVE=docker -e REDIS_HOST=redis -e REDIS_PORT=6379 -e METRICS_SERVICE_PORT=8084 "\$REG/uniplay/metrics-service:latest"
if aws ecr describe-images --region "$REGION" --repository-name uniplay/voice-service --image-ids imageTag=latest >/dev/null 2>&1; then
  docker run -d --restart unless-stopped --name voice-service --network uniplay -p 8085:8085 -e SPRING_PROFILES_ACTIVE=docker -e VOICE_SERVICE_PORT=8085 -e LIVEKIT_URL="ws://\$LIVEKIT_PRIVATE_IP:7880" -e LIVEKIT_PUBLIC_URL="ws://\$LIVEKIT_PUBLIC_IP:7880" -e LIVEKIT_API_KEY=devkey -e LIVEKIT_API_SECRET=secret -e VOICE_TOKEN_TTL=PT30M -e REDIS_HOST=redis -e REDIS_PORT=6379 "\$REG/uniplay/voice-service:latest"
else
  echo "voice-service:latest no esta disponible en ECR; se omite temporalmente"
fi
cat >/opt/uniplay/prometheus.yml <<'PROMEOF'
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: uniplay
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - room-service:8081
          - game-service:8082
          - metrics-service:8084
          - voice-service:8085
PROMEOF
docker run -d --restart unless-stopped --name prometheus --network uniplay -p 9090:9090 -v /opt/uniplay/prometheus.yml:/etc/prometheus/prometheus.yml:ro prom/prometheus:v3.5.0 --config.file=/etc/prometheus/prometheus.yml
docker run -d --restart unless-stopped --name grafana --network uniplay -p 3000:3000 -e GF_SECURITY_ADMIN_USER=admin -e GF_SECURITY_ADMIN_PASSWORD=uniplay -e GF_USERS_ALLOW_SIGN_UP=false grafana/grafana:12.1.0
EOF

CORE_ID="$(aws ec2 run-instances --region "$REGION" --image-id "$AMI" --instance-type t3.medium --count 1 --subnet-id "$SUBNET" --security-group-ids "$SG" --iam-instance-profile Name=LabInstanceProfile --associate-public-ip-address --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3,DeleteOnTermination=true}' --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=uniplay-core}]' --user-data fileb:///tmp/uniplay-core-userdata.sh --query 'Instances[0].InstanceId' --output text)"
aws ec2 wait instance-running --region "$REGION" --instance-ids "$CORE_ID"
CORE_PRIVATE_IP="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$CORE_ID" --query 'Reservations[0].Instances[0].PrivateIpAddress' --output text)"
CORE_PUBLIC_IP="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$CORE_ID" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"

cat >/tmp/uniplay-edge-userdata.sh <<EOF
#!/bin/bash
set -euxo pipefail
exec > >(tee -a /var/log/uniplay-bootstrap.log | logger -t uniplay-bootstrap -s 2>/dev/console) 2>&1
dnf install -y docker awscli
systemctl enable --now docker
CORE_PRIVATE_IP="$CORE_PRIVATE_IP"
REG="$REG"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "\$REG"
docker network create uniplay || true
docker run -d --restart unless-stopped --name realtime-service --network uniplay -p 8083:8083 -e SPRING_PROFILES_ACTIVE=docker -e REDIS_HOST="\$CORE_PRIVATE_IP" -e REDIS_PORT=6379 -e REALTIME_SERVICE_PORT=8083 -e REALTIME_ALLOWED_ORIGINS="*" "\$REG/uniplay/realtime-service:latest"
docker run -d --restart unless-stopped --name api-gateway --network uniplay -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker -e API_GATEWAY_PORT=8080 -e ROOM_SERVICE_URI="http://\$CORE_PRIVATE_IP:8081" -e GAME_SERVICE_URI="http://\$CORE_PRIVATE_IP:8082" -e REALTIME_SERVICE_URI=http://realtime-service:8083 -e REALTIME_WEBSOCKET_URI=ws://realtime-service:8083 -e METRICS_SERVICE_URI="http://\$CORE_PRIVATE_IP:8084" -e VOICE_SERVICE_URI="http://\$CORE_PRIVATE_IP:8085" -e GATEWAY_ALLOWED_ORIGINS="*" -e GATEWAY_RATE_LIMIT_ENABLED=true -e GATEWAY_RATE_LIMIT_REQUESTS_PER_WINDOW=120 -e GATEWAY_RATE_LIMIT_WINDOW=PT1M "\$REG/uniplay/api-gateway:latest"
docker run -d --restart unless-stopped --name frontend --network uniplay -p 80:80 "\$REG/uniplay/frontend:latest"
EOF

EDGE_ID="$(aws ec2 run-instances --region "$REGION" --image-id "$AMI" --instance-type t3.medium --count 1 --subnet-id "$SUBNET" --security-group-ids "$SG" --iam-instance-profile Name=LabInstanceProfile --associate-public-ip-address --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=25,VolumeType=gp3,DeleteOnTermination=true}' --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=uniplay-edge}]' --user-data fileb:///tmp/uniplay-edge-userdata.sh --query 'Instances[0].InstanceId' --output text)"
aws ec2 wait instance-running --region "$REGION" --instance-ids "$EDGE_ID"
EDGE_PUBLIC_IP="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$EDGE_ID" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"

echo "LIVEKIT_ID=$LIVEKIT_ID LIVEKIT_PUBLIC_IP=$LIVEKIT_PUBLIC_IP"
echo "CORE_ID=$CORE_ID CORE_PRIVATE_IP=$CORE_PRIVATE_IP CORE_PUBLIC_IP=$CORE_PUBLIC_IP"
echo "EDGE_ID=$EDGE_ID EDGE_PUBLIC_IP=$EDGE_PUBLIC_IP"
echo "APP_URL=http://$EDGE_PUBLIC_IP"
echo "GRAFANA_URL=http://$CORE_PUBLIC_IP:3000"
