#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REGION="${AWS_REGION:-us-east-1}"
FOUNDATION_STACK="${FOUNDATION_STACK:-uniplay-foundation}"
APPLICATION_STACK="${APPLICATION_STACK:-uniplay-application}"
INSTANCE_PROFILE="${INSTANCE_PROFILE:-LabInstanceProfile}"
BUILDER_INSTANCE_TYPE="${BUILDER_INSTANCE_TYPE:-t3.large}"
ENABLE_CLOUDFRONT="${ENABLE_CLOUDFRONT:-false}"
IMAGE_TAG="${IMAGE_TAG:-$(date -u +%Y%m%d%H%M%S)}"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY_HOST="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
REGISTRY_PREFIX="${REGISTRY_HOST}/uniplay"

stack_output() {
  local stack="$1"
  local key="$2"
  aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$stack" \
    --query "Stacks[0].Outputs[?OutputKey=='$key'].OutputValue | [0]" \
    --output text
}

cleanup_builder() {
  if [ -n "${BUILDER_ID:-}" ]; then
    aws ec2 terminate-instances --region "$REGION" --instance-ids "$BUILDER_ID" >/dev/null 2>&1 || true
  fi
  if [ -n "${BUILDER_SG:-}" ]; then
    for attempt in $(seq 1 30); do
      if aws ec2 delete-security-group --region "$REGION" --group-id "$BUILDER_SG" >/dev/null 2>&1; then
        break
      fi
      sleep 5
    done
  fi
}

trap cleanup_builder EXIT

echo "Validating CloudFormation templates"
aws cloudformation validate-template \
  --region "$REGION" \
  --template-body "file://$ROOT_DIR/infra/aws/cloudformation/foundation.yml" >/dev/null
aws cloudformation validate-template \
  --region "$REGION" \
  --template-body "file://$ROOT_DIR/infra/aws/cloudformation/application.yml" >/dev/null

echo "Deploying $FOUNDATION_STACK"
aws cloudformation deploy \
  --region "$REGION" \
  --stack-name "$FOUNDATION_STACK" \
  --template-file "$ROOT_DIR/infra/aws/cloudformation/foundation.yml" \
  --parameter-overrides ProjectName=uniplay EnvironmentName=prod \
  --no-fail-on-empty-changeset

FRONTEND_BUCKET="$(stack_output "$FOUNDATION_STACK" FrontendBucketName)"
VPC_ID="$(stack_output "$FOUNDATION_STACK" VpcId)"
PUBLIC_SUBNET="$(stack_output "$FOUNDATION_STACK" PublicSubnetAId)"

echo "Uploading bootstrap and observability configuration"
aws s3 sync "$ROOT_DIR/infra/aws/user-data" \
  "s3://$FRONTEND_BUCKET/deploy/user-data" \
  --delete
aws s3 cp "$ROOT_DIR/infra/aws/builder/build-images.sh" \
  "s3://$FRONTEND_BUCKET/deploy/builder/build-images.sh"
aws s3 cp "$ROOT_DIR/infra/aws/observability/prometheus.yml" \
  "s3://$FRONTEND_BUCKET/deploy/observability/prometheus.yml"
aws s3 cp "$ROOT_DIR/infra/observability/prometheus/alerts.yml" \
  "s3://$FRONTEND_BUCKET/deploy/observability/alerts.yml"
aws s3 sync "$ROOT_DIR/infra/observability/grafana/provisioning" \
  "s3://$FRONTEND_BUCKET/deploy/observability/grafana/provisioning" \
  --delete
aws s3 sync "$ROOT_DIR/infra/observability/grafana/dashboards" \
  "s3://$FRONTEND_BUCKET/deploy/observability/grafana/dashboards" \
  --delete

SOURCE_KEY="deploy/source-$IMAGE_TAG.zip"
STATUS_KEY="deploy/build-$IMAGE_TAG.status"
LOG_KEY="deploy/build-$IMAGE_TAG.log"

echo "Packaging source"
cd "$ROOT_DIR"
rm -f /tmp/uniplay-source.zip
zip -qr /tmp/uniplay-source.zip . \
  -x '.git/*' '*/target/*' '*/node_modules/*' '*/dist/*' 'outputs/*' 'tmp/*' '*.log'
aws s3 cp /tmp/uniplay-source.zip "s3://$FRONTEND_BUCKET/$SOURCE_KEY"

BUILDER_SG="$(aws ec2 create-security-group \
  --region "$REGION" \
  --group-name "uniplay-builder-$IMAGE_TAG" \
  --description "Temporary UniPlay image builder" \
  --vpc-id "$VPC_ID" \
  --query GroupId \
  --output text)"
aws ec2 create-tags \
  --region "$REGION" \
  --resources "$BUILDER_SG" \
  --tags Key=Project,Value=UniPlay Key=Temporary,Value=true

AMI_ID="$(aws ec2 describe-images \
  --region "$REGION" \
  --owners amazon \
  --filters 'Name=name,Values=al2023-ami-2023.*-x86_64' Name=state,Values=available \
  --query 'sort_by(Images,&CreationDate)[-1].ImageId' \
  --output text)"

cat >/tmp/uniplay-builder-user-data.sh <<EOF
#!/bin/bash
set -euo pipefail
mkdir -p /etc/uniplay
cat >/etc/uniplay/builder.env <<'ENVEOF'
AWS_REGION=$REGION
ARTIFACT_BUCKET=$FRONTEND_BUCKET
FRONTEND_BUCKET=$FRONTEND_BUCKET
SOURCE_KEY=$SOURCE_KEY
STATUS_KEY=$STATUS_KEY
LOG_KEY=$LOG_KEY
REGISTRY_HOST=$REGISTRY_HOST
REGISTRY_PREFIX=$REGISTRY_PREFIX
IMAGE_TAG=$IMAGE_TAG
ENVEOF
aws s3 cp s3://$FRONTEND_BUCKET/deploy/builder/build-images.sh /tmp/uniplay-build-images.sh
chmod +x /tmp/uniplay-build-images.sh
/tmp/uniplay-build-images.sh
EOF

echo "Launching temporary image builder"
BUILDER_ID="$(aws ec2 run-instances \
  --region "$REGION" \
  --image-id "$AMI_ID" \
  --instance-type "$BUILDER_INSTANCE_TYPE" \
  --count 1 \
  --subnet-id "$PUBLIC_SUBNET" \
  --security-group-ids "$BUILDER_SG" \
  --iam-instance-profile "Name=$INSTANCE_PROFILE" \
  --associate-public-ip-address \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=60,VolumeType=gp3,DeleteOnTermination=true,Encrypted=true}' \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=uniplay-builder},{Key=Project,Value=UniPlay},{Key=Temporary,Value=true}]" \
  --user-data fileb:///tmp/uniplay-builder-user-data.sh \
  --query 'Instances[0].InstanceId' \
  --output text)"

echo "Builder: $BUILDER_ID"
build_status=""
for attempt in $(seq 1 120); do
  build_status="$(aws s3 cp "s3://$FRONTEND_BUCKET/$STATUS_KEY" - 2>/dev/null || true)"
  if [ "$build_status" = SUCCESS ]; then
    break
  fi
  if [ "$build_status" = FAILED ]; then
    aws s3 cp "s3://$FRONTEND_BUCKET/$LOG_KEY" - || true
    echo "Image build failed" >&2
    exit 1
  fi
  sleep 20
done

if [ "$build_status" != SUCCESS ]; then
  aws s3 cp "s3://$FRONTEND_BUCKET/$LOG_KEY" - || true
  echo "Image build timed out" >&2
  exit 1
fi

cleanup_builder
BUILDER_ID=""
BUILDER_SG=""

ORIGIN_SECRET="$(openssl rand -hex 24)"
GRAFANA_PASSWORD="$(openssl rand -base64 18 | tr -d '/+=')"

echo "Deploying $APPLICATION_STACK"
aws cloudformation deploy \
  --region "$REGION" \
  --stack-name "$APPLICATION_STACK" \
  --template-file "$ROOT_DIR/infra/aws/cloudformation/application.yml" \
  --parameter-overrides \
    FoundationStackName="$FOUNDATION_STACK" \
    InstanceProfileName="$INSTANCE_PROFILE" \
    ImageTag="$IMAGE_TAG" \
    OriginVerifySecret="$ORIGIN_SECRET" \
    GrafanaAdminPassword="$GRAFANA_PASSWORD" \
    EnableCloudFront="$ENABLE_CLOUDFRONT" \
  --no-fail-on-empty-changeset

APPLICATION_URL="$(stack_output "$APPLICATION_STACK" ApplicationUrl)"
GRAFANA_URL="$(stack_output "$APPLICATION_STACK" GrafanaUrl)"
LIVEKIT_URL="$(stack_output "$APPLICATION_STACK" LiveKitUrl)"

if [ "$ENABLE_CLOUDFRONT" = true ]; then
  aws cloudfront create-invalidation \
    --distribution-id "$(aws cloudformation describe-stack-resource --region "$REGION" --stack-name "$APPLICATION_STACK" --logical-resource-id MainDistribution --query StackResourceDetail.PhysicalResourceId --output text)" \
    --paths '/*' >/dev/null
fi

cat >"$HOME/uniplay-deployment.txt" <<EOF
IMAGE_TAG=$IMAGE_TAG
APPLICATION_URL=$APPLICATION_URL
GRAFANA_URL=$GRAFANA_URL
GRAFANA_USER=admin
GRAFANA_PASSWORD=$GRAFANA_PASSWORD
LIVEKIT_URL=$LIVEKIT_URL
EOF
chmod 600 "$HOME/uniplay-deployment.txt"

aws s3 rm "s3://$FRONTEND_BUCKET/$SOURCE_KEY" >/dev/null
printf '\nUniPlay deployment complete\nAPP: %s\nGRAFANA: %s\nLIVEKIT: %s\nCredentials: %s\n' \
  "$APPLICATION_URL" "$GRAFANA_URL" "$LIVEKIT_URL" "$HOME/uniplay-deployment.txt"
