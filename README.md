# tiira-watcher
Cloud service to prefetch bird sightings from Tiira service and present them nicely

## Features / Doc Links

* Convert Tiira coordinates to WGS84 used by OpenStreetMap using [geo-conversion](https://github.com/lupapiste/geo-conversion) lib by Lupapiste
* Save interesting sightings into Firestore DB (with [firestore-clj](https://github.com/lurodrigo/firestore-clj) library)
* Using [Camel-snake-kebab](https://clj-commons.org/camel-snake-kebab/) for keyword translations
* Build running environment with [Terraform](https://www.terraform.io/)
* Set up GHA build with [Setup Clojure](https://github.com/marketplace/actions/setup-clojure) action

## Running in Google Cloud

(For most of the links you need to copy URL, edit project ID and only then open)

1. Set up a project in [cloud console](https://console.cloud.google.com/)
2. Add to yourself the role `Service Account Token Creator` in [IAM](https://console.cloud.google.com/iam-admin/iam) (needed for local docker push, maybe not essential)
3. Init GCP project by running [gcloud-init.sh](gcloud-init.sh) - TODO: need ServiceAccount first
4. Enable [App Engine](https://console.cloud.google.com/apis/library/appengine.googleapis.com?project=<projectid>)
5. Set up GCP according to [Terraform tutorial](https://learn.hashicorp.com/tutorials/terraform/google-cloud-platform-build?in=terraform/gcp-get-started)
6. Create [Firebase project](https://console.firebase.google.com/) for the Google Cloud project
7. Enable [IAM API](https://console.developers.google.com/apis/api/iam.googleapis.com/overview?project=<projectid>) 
8. Enable [Cloud Run Admin API](https://console.developers.google.com/apis/api/run.googleapis.com/overview?project=<projectid>>)
9. Enable [Service Control API](https://console.cloud.google.com/marketplace/product/google/servicecontrol.googleapis.com?q=search&referrer=search&project=<projectid>)
10. Enable [API Gateway](https://console.developers.google.com/apis/api/apigateway.googleapis.com/overview?project=<projectid>)
11. Enable Container Registry API: `gcloud services enable artifactregistry.googleapis.com`
12. Enable email+password [authentication in Firebase](https://console.firebase.google.com/u/1/project/<project-id>/authentication/providers)
13. Generate admin key for [Firebase Admin SDK](https://console.firebase.google.com/u/0/project/<projectid>/settings/serviceaccounts/adminsdk)
14. Fill in CI terraform variables as in [template](terraform/ci/registry.tfvars.template) into `terraform/gcr.tfvars`
15. Go to `terraform/ci` directory of the checked out project, and `terraform init` and `terraform apply -var-file=registry.tfvars`
16. Fill in terraform variables as in [template](terraform/gcr.tfvars.template) into `terraform/gcr.tfvars`
17. Go to terraform directory of the checked out project, and `terraform init` and `terraform apply -var-file=gcr.tfvars`
16. 

(TODO: enable APIs with `gcloud services enable cloudbuild.googleapis.com compute.googleapis.com`)

## Github Actions Setup

1. Create & download service account key: ```gcloud iam service-accounts keys create gha-gcloud-sa.json 
   --iam-account=github@${PROJECT}.iam.gserviceaccount.com```
2. Add secret GCR_PROJECT (value is project ID)
3. Add secret GCR_SA_KEY (value is the entire JSON from step 1)

## Development

1. Make changes & test locally
2. Update version number in project.clj
3. Upon commit & push, GHA will build new docker image with given version, and update the version number into GCS
4. When running terraform-deploy GHA job (manually so far), it will take the image with saved version number

## Notes

For local docker push, do:
```gcloud auth configure-docker europe-north1-docker.pkg.dev```
(sets up $HOME/.docker/config.json)

Then push as seen in [build.sh](build.sh).

