resource "google_service_account" "service_account_apigw" {
  account_id   = "apigw-sa"
  display_name = "API GW Service Account"
}

resource "google_cloud_run_service" "tiira_watcher_api" {
  name     = "tiira-watcher-api"
  location = "europe-north1"

  template {
    spec {
      containers {
          image = "europe-north1-docker.pkg.dev/tiira-watcher-dev/tiira-watcher-repo/tiira-watcher:0.9.0"
		  env {
			name = "FIRESTORE_PROJECT_ID"
			value = "tiira-watcher-dev"
		  }
		  env {
		    name = "UI_SERVER_ADDRESS"
		    value = "https://tiira-watcher-ui-vodgsvsqja-lz.a.run.app"
		    # TODO: From TF outputs
		  }
      }
    }
  }
}

resource "google_cloud_run_service" "ui" {
  name     = "tiira-watcher-ui"
  location = "europe-north1"

  template {
    spec {
      containers {
        image = "europe-north1-docker.pkg.dev/tiira-watcher-dev/tiira-watcher-repo/tiira-watcher-ui:b9dd7dce399f07e05d142d2431ae18916dd49d18"
      }
    }
  }
}

output "cloudrun_endpoint" {
  value = google_cloud_run_service.tiira_watcher_api.status[0].url
}

# Only allow the API Gateway service account to call your Cloud Run instance
resource "google_cloud_run_service_iam_member" "public_access" {
  provider = google
  service  = google_cloud_run_service.tiira_watcher_api.name
  location = google_cloud_run_service.tiira_watcher_api.location
  project  = google_cloud_run_service.tiira_watcher_api.project
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.service_account_apigw.email}"
}

data "google_iam_policy" "noauth" {
  binding {
    role = "roles/run.invoker"
    members = [
      "allUsers",
    ]
  }
}

# Allow anyone to call the UI (which then asks for the login)
resource "google_cloud_run_service_iam_policy" "noauth" {
  location    = google_cloud_run_service.ui.location
  project     = google_cloud_run_service.ui.project
  service     = google_cloud_run_service.ui.name

  policy_data = data.google_iam_policy.noauth.policy_data
}