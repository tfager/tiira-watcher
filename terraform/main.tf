terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      version = "4.21.0"
    }
  }
}

provider "google" {
  credentials = file("gcp-tiira-watcher-dev-aa5c85e6df09.json")

  project = "tiira-watcher-dev"
  region  = "europe-north1"
  zone    = "europe-north1-b"
}

provider "google-beta" {
  credentials = file("gcp-tiira-watcher-dev-aa5c85e6df09.json")

  project = "tiira-watcher-dev"
  region  = "europe-north1"
  zone    = "europe-north1-b"
}

resource "google_firestore_index" "sightings_index" {
  project = "tiira-watcher-dev"

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