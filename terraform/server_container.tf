resource "google_service_account" "cloud_run_sa" {
  account_id   = "cloud-run-sa"
  display_name = "Service Account for Cloud Run"
}

resource "google_project_iam_member" "cloud_run_sa_firestore_access" {
  provider = google-beta
  project  = var.project
  role     = "roles/datastore.user"
  member   = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

resource "google_cloud_run_v2_service" "tiira_watcher_api" {
  name     = "tiira-watcher"
  location = var.location_full

  template {
    containers {
      image = "europe-north1-docker.pkg.dev/${var.project}/tiira-watcher-repo/tiira-watcher:${local.server_version}"
      args = ["server"]
      env {
        name  = "FIRESTORE_PROJECT_ID"
        value = var.project
      }
      env {
        name  = "UI_SERVER_ADDRESS"
        value = var.ui_server_cors
      }
      env {
        name  = "TIIRA_USERNAME"
        value = var.tiira_username
      }
      env {
        name  = "TIIRA_PASSWORD"
        value = var.tiira_password
      }
    }
    service_account = google_service_account.cloud_run_sa.email
  }
}

output "api_url" {
  value = google_cloud_run_v2_service.tiira_watcher_api.traffic_statuses[0].uri
}

# Only allow the API Gateway service account to call your Cloud Run instance
resource "google_cloud_run_service_iam_member" "public_access" {
  service  = google_cloud_run_v2_service.tiira_watcher_api.name
  location = google_cloud_run_v2_service.tiira_watcher_api.location
  project  = google_cloud_run_v2_service.tiira_watcher_api.project
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.service_account_apigw.email}"
}
