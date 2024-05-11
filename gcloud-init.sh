#!/usr/bin/env bash

# Add to Terraform the role `Cloud Run Admin` to add further IAM policies to other service accounts

# TODO: vars from JSON or somewhere
#  - see https://learn.hashicorp.com/tutorials/terraform/google-cloud-platform-build?in=terraform/gcp-get-started
TERRAFORM_SA_SHORT=terraform-account
REGION=europe-north1
if [ $1 == "production" ]
then
    PROJECT=tiira-watcher-prod
else
    PROJECT=tiira-watcher-dev
fi
TERRAFORM_SA=$TERRAFORM_SA_SHORT@${PROJECT}.iam.gserviceaccount.com
TERRAFORM_SA_SECRETS_FILE=${PROJECT}-sa.json
BUCKET=${PROJECT}-terraform-backend

echo "Project = $PROJECT"

# Create terraform SA
#gcloud iam service-accounts --project $PROJECT create "$TERRAFORM_SA_SHORT" --display-name="Terraform Service Account"
gcloud iam service-accounts --project $PROJECT keys create $TERRAFORM_SA_SECRETS_FILE --iam-account=$TERRAFORM_SA

# Enable GCP Services
gcloud --project $PROJECT services enable \
    apigateway.googleapis.com \
    firestore.googleapis.com \
    eventarc.googleapis.com \
    logging.googleapis.com \
    pubsub.googleapis.com \
    run.googleapis.com \
    workflows.googleapis.com \
    cloudfunctions.googleapis.com \
    secretmanager.googleapis.com \
    iam.googleapis.com \
    servicecontrol.googleapis.com \
    cloudbuild.googleapis.com
    # Servicecontrol needed for cloud console, not sure if needed for anything else
    # Cloudbuild Needed for search req cloud function

ROLES=(
#   roles/resourcemanager.projectIamAdmin
#   roles/run.admin
#   roles/artifactregistry.admin
#   roles/datastore.indexAdmin
#   roles/datastore.owner
#   roles/secretmanager.secretAccessor
#   roles/secretmanager.admin
#   roles/cloudfunctions.admin
#   roles/storage.objectAdmin
#   roles/iam.serviceAccountAdmin
#   roles/iam.serviceAccountKeyAdmin
#   roles/iam.serviceAccountUser
#   roles/apigateway.admin
    )

for role in "${ROLES[@]}"
do
    echo "Adding $role"
    gcloud projects add-iam-policy-binding $PROJECT \
      --member="serviceAccount:$TERRAFORM_SA" --role="$role"
done

# Storage bucket for terraform backend
#gcloud storage buckets create gs://${BUCKET} \
#        --project $PROJECT \
#        --location=$REGION \
#        --pap
#gsutil versioning set on gs://${BUCKET}

