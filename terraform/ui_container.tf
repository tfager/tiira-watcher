locals {
  ui_version = file("ui_version.txt")
}

resource "google_cloud_run_service" "ui" {
  name     = "tiira-watcher-ui"
  location = "europe-north1"

  template {
    spec {
      containers {
        image = "europe-north1-docker.pkg.dev/${var.project}/tiira-watcher-repo/tiira-watcher-ui:${local.ui_version}"
      }
    }
  }
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
  provider = google
  location = google_cloud_run_service.ui.location
  project  = google_cloud_run_service.ui.project
  service  = google_cloud_run_service.ui.name

  policy_data = data.google_iam_policy.noauth.policy_data
}

output "ui_url" {
  value = google_cloud_run_service.ui.status[0].url
}