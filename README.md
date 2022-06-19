# tiira-watcher
Cloud service to prefetch bird sightings from Tiira service and present them nicely

## Features / Doc Links

* Convert Tiira coordinates to WGS84 used by OpenStreetMap using [geo-conversion](https://github.com/lupapiste/geo-conversion) lib by Lupapiste
* Save interesting sightings into Firestore DB (with [firestore-clj](https://github.com/lurodrigo/firestore-clj) library)
* Using [Camel-snake-kebab](https://clj-commons.org/camel-snake-kebab/) for keyword translations
* Build running environment with [Terraform](https://www.terraform.io/)

## Running in Google Cloud

(For most of the links you need to copy URL, edit project ID and only then open)

1. Set up a project in [cloud console](https://console.cloud.google.com/)
2. Enable [App Engine](https://console.cloud.google.com/apis/library/appengine.googleapis.com?project=<projectid>)
3. Set up GCP according to [Terraform tutorial](https://learn.hashicorp.com/tutorials/terraform/google-cloud-platform-build?in=terraform/gcp-get-started)
4. Create [Firebase project](https://console.firebase.google.com/) for the Google Cloud project
5. Enable [IAM API](https://console.developers.google.com/apis/api/iam.googleapis.com/overview?project=<projectid>) 
6. Enable [Cloud Run Admin API](https://console.developers.google.com/apis/api/run.googleapis.com/overview?project=<projectid>>)
7. Enable [Service Control API](https://console.cloud.google.com/marketplace/product/google/servicecontrol.googleapis.com?q=search&referrer=search&project=<projectid>)
8. Enable [API Gateway](https://console.developers.google.com/apis/api/apigateway.googleapis.com/overview?project=<projectid>)
9. Enable email+password [authentication in Firebase](https://console.firebase.google.com/u/1/project/<project-id>/authentication/providers)
10. Generate admin key for [Firebase Admin SDK](https://console.firebase.google.com/u/0/project/<projectid>/settings/serviceaccounts/adminsdk)
11. Go to terraform directory of the checked out project, and `terraform init` and `terraform apply`

## Terraform issues

Currently it seems impossible to set IAM policy for Cloud Run services in a single terraform run. Explained here: https://stackoverflow.com/questions/70797574/terraform-cloud-run-error-forbidden-your-client-does-not-have-permission-to

So, first terraform run will fail with IAM policy issues. After that, to add the role Cloud Run Admin, go to Cloud Run service in Google Cloud Console, select desired containers, show info panel, add principal, select terraform service account and Cloud Run Admin role

After that, terraform should be able to complete the configuration.
