terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
    }
  }

  backend "gcs" {
    bucket = "tiira-watcher-dev-terraform-backend"
    prefix = "terraform/state"
  }
}

variable "project" {
  type = string
}

variable "gcloud_creds_file" {
  type = string
}

variable "location_full" {
  type = string
}

variable "gw_location_full" {
  type = string
}

variable "tiira_password" {
  type      = string
  sensitive = true
}

variable "tiira_username" {
  type = string
}
provider "google" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
  #zone    = "europe-north1-b"
}

provider "google-beta" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
  #zone    = "europe-north1-b"
}

resource "google_firestore_index" "sightings_index" {
  project    = var.project
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

resource "google_firestore_index" "search_request_index" {
  project    = var.project
  collection = "search_request"

  fields {
    field_path = "id"
    order      = "DESCENDING"
  }

  fields {
    field_path = "timestamp"
    order      = "ASCENDING"
  }
}
