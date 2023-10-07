#!/usr/bin/env bash

# Until this PR is merged, terraform can't create cloud run jobs, so need to do it here
# https://github.com/GoogleCloudPlatform/magic-modules/pull/6750

LOCATION=europe-north1
PROJECT=tiira-watcher-dev
VERSION=$(cat terraform/server_version.txt)

if [ -z $TIIRA_USERNAME ]
then
  echo "TIIRA_USERNAME variable required"
  exit 1
fi

if [ -z $TIIRA_PASSWORD ]
then
  echo "TIIRA_PASSWORD variable required"
  exit 1
fi

# TODO: https://cloud.google.com/run/docs/configuring/secrets
gcloud beta run jobs create tiira-watcher-search-job \
      --image "${LOCATION}-docker.pkg.dev/${PROJECT}/tiira-watcher-repo/tiira-watcher:${VERSION}" \
      --set-env-vars "FIRESTORE_PROJECT_ID=${PROJECT},TIIRA_USERNAME=${TIIRA_USERNAME},TIIRA_PASSWORD=${TIIRA_PASSWORD}" \
      --args "search-reqs"
