#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
FOUNDATION_STACK="${FOUNDATION_STACK:-uniplay-foundation}"
APPLICATION_STACK="${APPLICATION_STACK:-uniplay-application}"

stack_exists() {
  aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$1" >/dev/null 2>&1
}

if stack_exists "$APPLICATION_STACK"; then
  echo "Deleting $APPLICATION_STACK"
  aws cloudformation delete-stack --region "$REGION" --stack-name "$APPLICATION_STACK"
  aws cloudformation wait stack-delete-complete --region "$REGION" --stack-name "$APPLICATION_STACK"
fi

if stack_exists "$FOUNDATION_STACK"; then
  bucket="$(aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$FOUNDATION_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='FrontendBucketName'].OutputValue | [0]" \
    --output text)"
  if [ -n "$bucket" ] && [ "$bucket" != None ]; then
    aws s3 rm "s3://$bucket" --recursive || true
  fi
  echo "Deleting $FOUNDATION_STACK"
  aws cloudformation delete-stack --region "$REGION" --stack-name "$FOUNDATION_STACK"
  aws cloudformation wait stack-delete-complete --region "$REGION" --stack-name "$FOUNDATION_STACK"
fi

echo "UniPlay AWS resources deleted"
