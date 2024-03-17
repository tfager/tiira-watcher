# tiira-watcher
Cloud service to prefetch bird sightings from Tiira service and present them nicely

## Features / Doc Links

* Convert Tiira coordinates to WGS84 used by OpenStreetMap using [geo-conversion](https://github.com/lupapiste/geo-conversion) lib by Lupapiste
* Save interesting sightings into Firestore DB (with [firestore-clj](https://github.com/lurodrigo/firestore-clj) library)
* Using [Camel-snake-kebab](https://clj-commons.org/camel-snake-kebab/) for keyword translations
* Build running environment with [Terraform](https://www.terraform.io/)
* Set up GHA build with [Setup Clojure](https://github.com/marketplace/actions/setup-clojure) action

## Setup in Google Cloud

1. Set up a project in [cloud console](https://console.cloud.google.com/)
1. Init GCP project by running [gcloud-init.sh](gcloud-init.sh)
1. Fill in CI terraform variables as in [template](terraform/ci/registry.tfvars.template) into `terraform/gcr.tfvars`
1. Go to `terraform/ci` directory of the checked out project, and `terraform init --backend-config=../config/dev.config` and `terraform apply -var-file=registry.tfvars`
1. Run terraform apply --var-file=registry.tfvars
1. Fill in terraform variables as in [template](terraform/ci/registry.tfvars.template) into `terraform/ci/registry.tfvars`
1. Complete github actions setup as below
1. Go to terraform directory of the checked out project, and `terraform init` and `terraform apply -var-file=gcr.tfvars`
1. Fill in terraform variables as in [template](terraform/gcr.tfvars.template) into `terraform/gcr.tfvars`

## User Setup

1. Create [Firebase project](https://console.firebase.google.com/) for the Google Cloud project
1. Enable email+password [authentication in Firebase](https://console.firebase.google.com/u/0/project/<project-id>/authentication/providers)
1. Generate admin key for [Firebase Admin SDK](https://console.firebase.google.com/u/0/project/<projectid>/settings/serviceaccounts/adminsdk)
1. Find API key in [Firebase settings](https://console.firebase.google.com/u/0/project/tiira-watcher-prod/settings/general)
1. Set up python for user admin
 * For the first time: `pyenv install 3.10.4; pyenv virtualenv 3.10.4 tiira-watcher; pip install -r requirements.txt`
 * Later simply: `pyenv activate tiira-watcher`
1. Set `export FIREBASE_CREDS_FILE=./firebase_admin_key.json`
1. Run `python ./user_admin.py` and add username+password as prompted

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

