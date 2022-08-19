#!/usr/bin/env bash

# Add to Terraform the role `Cloud Run Admin` to add further IAM policies to other service accounts

# TODO: vars from JSON or somewhere
# TODO: Script SA creation and key JSON download
#  - see https://learn.hashicorp.com/tutorials/terraform/google-cloud-platform-build?in=terraform/gcp-get-started
TERRAFORM_SA=terraform-account@tiira-watcher-dev.iam.gserviceaccount.com
REGION=europe-north1
PROJECT=tiira-watcher-dev

# TODO: Maybe not needed, replaced by the latter?
#gcloud projects add-iam-policy-binding $PROJECT \
#    --member="serviceAccount:$TERRAFORM_SA" --role="roles/resourcemanager.projectIamAdmin"
gcloud projects add-iam-policy-binding $PROJECT \
    --member="serviceAccount:$TERRAFORM_SA" --role='roles/run.admin'