resource "google_service_account" "github_sa" {
  provider     = google-beta
  account_id   = "github"
  display_name = "Github Actions Service Account"
}

resource "google_project_iam_member" "github_sa_roles" {
  for_each = toset([
    "roles/run.admin",
    "roles/secretmanager.secretAccessor",
    "roles/viewer",
    "roles/iam.serviceAccountUser",
    "roles/cloudfunctions.developer",
    "roles/storage.objectUser",
    "roles/artifactregistry.writer",
    "roles/artifactregistry.reader"
  ])
  role = each.key
  provider = google-beta
  project  = var.project
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}
