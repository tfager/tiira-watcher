resource "google_service_account" "github_sa" {
  provider     = google-beta
  account_id   = "github"
  display_name = "Github Actions Service Account"
}

resource "google_project_iam_member" "github_sa_cloud_run_admin" {
  provider = google-beta
  project  = var.project
  role     = "roles/run.admin"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_cloud_build_editor" {
  provider = google-beta
  project  = var.project
  role     = "roles/cloudbuild.builds.editor"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_cloud_build_builder" {
  provider = google-beta
  project  = var.project
  role     = "roles/cloudbuild.builds.builder"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_viewer" {
  provider = google-beta
  project  = var.project
  role     = "roles/viewer"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_sa_user" {
  provider = google-beta
  project  = var.project
  role     = "roles/iam.serviceAccountUser"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}
