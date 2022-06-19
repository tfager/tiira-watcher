resource "google_service_account" "service_account_apigw" {
  account_id   = "apigw-sa"
  display_name = "API GW Service Account"
}

resource "google_cloud_run_service" "hello" {
  name     = "hello"
  location = "europe-north1"

  template {
    spec {
      containers {
        image = "gcr.io/cloudrun/hello"

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
        image = "gcr.io/tiira-watcher-dev/tiira-watcher-ui"

      }
    }
  }
}

output "cloudrun_endpoint" {
  value = google_cloud_run_service.hello.status[0].url
}

# Only allow the API Gateway service account to call your Cloud Run instance
resource "google_cloud_run_service_iam_member" "public_access" {
  provider = google
  service  = google_cloud_run_service.hello.name
  location = google_cloud_run_service.hello.location
  project  = google_cloud_run_service.hello.project
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