terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      version = "4.21.0"
    }
  }
}

variable project {
    type = string
}

variable gcloud_creds_file {
    type = string
}

variable location_full {
    type = string
}

provider "google" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
  zone    = "europe-north1-b"
}

provider "google-beta" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
  zone    = "europe-north1-b"
}

resource "google_firestore_index" "sightings_index" {
  project = var.project
  collection = "sightings"

  fields {
    field_path = "id"
    order      = "DESCENDING"
  }

  fields {
    field_path = "timestamp"
    order      = "DESCENDING"
  }

}

resource "google_service_account" "github_sa" {
  account_id   = "github"
  display_name = "Github Actions Service Account"
}

resource "google_project_iam_member" "github_sa_cloud_run_admin" {
  project = var.project
  role    = "roles/run.admin"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_cloud_build_editor" {
  project = var.project
  role    = "roles/cloudbuild.builds.editor"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_cloud_build_builder" {
  project = var.project
  role    = "roles/cloudbuild.builds.builder"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_viewer" {
  project = var.project
  role    = "roles/viewer"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}

resource "google_project_iam_member" "github_sa_sa_user" {
  project = var.project
  role    = "roles/iam.serviceAccountUser"
  member   = "serviceAccount:${google_service_account.github_sa.email}"
}
