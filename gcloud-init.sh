#!/usr/bin/env bash

# Add to Terraform the role `Cloud Run Admin` to add further IAM policies to other service accounts

# TODO: vars from JSON or somewhere
#  - see https://learn.hashicorp.com/tutorials/terraform/google-cloud-platform-build?in=terraform/gcp-get-started
TERRAFORM_SA=terraform-account@tiira-watcher-dev.iam.gserviceaccount.com
REGION=europe-north1
PROJECT=tiira-watcher-dev
BUCKET=${PROJECT}-terraform-backend

# SA creation - untested
#gcloud iam service-accounts keys create gha-gcloud-sa.json --iam-account=$TERRAFORM_SA
# TODO: write to file etc.

#gcloud projects add-iam-policy-binding $PROJECT \
#    --member="serviceAccount:$TERRAFORM_SA" --role="roles/resourcemanager.projectIamAdmin"
#gcloud projects add-iam-policy-binding $PROJECT \
#    --member="serviceAccount:$TERRAFORM_SA" --role='roles/run.admin'
#gcloud projects add-iam-policy-binding $PROJECT \
#    --member="serviceAccount:$TERRAFORM_SA" --role="roles/artifactregistry.admin"

# Storage bucket for terraform backend
#gcloud storage buckets create gs://${BUCKET} \
#        --location=$REGION \
#        --pap
#gsutil versioning set on gs://${BUCKET}

