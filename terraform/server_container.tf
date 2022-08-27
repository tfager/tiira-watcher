resource "google_service_account" "service_account_apigw" {
  account_id   = "apigw-sa"
  display_name = "API GW Service Account"
}

resource "google_cloud_run_service" "tiira_watcher_api" {
  name     = "tiira-watcher"
  location = "europe-north1"

  template {
    spec {
      containers {
          image = "europe-north1-docker.pkg.dev/tiira-watcher-dev/tiira-watcher-repo/tiira-watcher:${var.server_version}"
		  env {
			name = "FIRESTORE_PROJECT_ID"
			value = "tiira-watcher-dev"
		  }
		  env {
		    name = "UI_SERVER_ADDRESS"
		    value = "https://tiira-watcher-ui-vodgsvsqja-lz.a.run.app"
		    # TODO: From TF outputs
		  }
		  env {
		    name = "TIIRA_USERNAME"
		    value = var.tiira_username
		  }
		  env {
		    name = "TIIRA_PASSWORD"
		    value = var.tiira_password
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
        image = "europe-north1-docker.pkg.dev/tiira-watcher-dev/tiira-watcher-repo/tiira-watcher-ui:${var.ui_version}"
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