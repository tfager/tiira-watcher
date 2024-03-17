terraform {
  required_providers {
    google-beta = {
      source = "hashicorp/google"
    }
  }

  backend "gcs" {
    prefix = "terraform-ci/state"
  }
}

provider "google-beta" {
  project = var.project
  region  = var.location_full
}

variable "location_full" {
  type = string
}

variable "project" {
  type = string
}


resource "google_artifact_registry_repository" "tiira-docker-repo" {
  provider      = google-beta
  location      = var.location_full
  repository_id = "tiira-watcher-repo"
  description   = "Repository for tiira-watcher images"
  format        = "DOCKER"
}

resource "google_service_account" "repo-account" {
  provider = google-beta

  account_id   = "tiira-repo-account"
  display_name = "Service account for pushing docker images"
}

resource "google_artifact_registry_repository_iam_member" "repo-iam-read" {
  provider = google-beta

  location   = google_artifact_registry_repository.tiira-docker-repo.location
  repository = google_artifact_registry_repository.tiira-docker-repo.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.repo-account.email}"
}
