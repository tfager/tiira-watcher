#!/usr/bin/env bash

ACCESS_TOKEN=$(gcloud auth print-access-token)
LOCATION=europe-north1
PROJECT=tiira-watcher-dev
JOB=tiira-watcher-search-job

echo $ACCESS_TOKEN

curl -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -X POST \
  -d '' \
  "https://${LOCATION}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${PROJECT}/jobs/${JOB}:run"