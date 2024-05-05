terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
    }
  }

  backend "gcs" {
    bucket = "${var.project}-terraform-backend"
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

variable "ui_server_cors" {
  type = string
}

# Note: this is only known after API GW has been created in gateway.tf,
# so a placeholder value needs to be in the variables on the initial run
variable "api_gateway" {
  type = string
}

locals {
  server_version = file("server_version.txt")
}

provider "google" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
}

provider "google-beta" {
  credentials = file(var.gcloud_creds_file)

  project = var.project
  region  = var.location_full
}

resource "google_firestore_database" "database" {
  project     = var.project
  name        = "(default)"
  location_id = var.location_full
  type        = "FIRESTORE_NATIVE"
}

resource "google_firestore_index" "search_request_index" {
  project    = var.project
  collection = "search_request"

  fields {
    field_path = "timestamp"
    order      = "ASCENDING"
  }

  fields {
    field_path = "search_status"
    order      = "ASCENDING"
  }
}

