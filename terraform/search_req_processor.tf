resource "google_cloud_run_v2_job" "search_job" {
  name     = "tiira-watcher-search-job"
  location = var.location_full

  template {
    template {
      timeout = "1200s"  # 20 minutes
      containers {
        image = "${var.location_full}-docker.pkg.dev/${var.project}/tiira-watcher-repo/tiira-watcher:${local.server_version}"
        args = ["search-reqs"]
        env {
          name = "FIRESTORE_PROJECT_ID"
          value = var.project
        }
        env {
          name = "TIIRA_USERNAME"
          value = var.tiira_username
        }
        env {
          name = "TIIRA_PASSWORD"
          value = var.tiira_password
        }
      }
      service_account = google_service_account.service_account_trigger.email
    }
  }

  lifecycle {
    ignore_changes = [
      launch_stage,
    ]
  }
}

resource "google_service_account" "service_account_trigger" {
  account_id   = "trigger-sa"
  display_name = "Cloud Run to Pub/Sub Trigger Service Account"
}

resource "google_project_iam_member" "trigger_sa_pub_sub" {
  provider = google-beta
  project  = var.project
  role     = "roles/pubsub.publisher"
  member   = "serviceAccount:${google_service_account.service_account_trigger.email}"
}

resource "google_service_account_key" "trigger_sa_key" {
  service_account_id = google_service_account.service_account_trigger.id
}

resource "google_secret_manager_secret" "trigger_json_key_secret" {
  secret_id = "trigger-sa-json-key-secret"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "trigger_json_key_secret_version" {
  secret = google_secret_manager_secret.trigger_json_key_secret.id
  # Base64 encoded json
  secret_data = google_service_account_key.trigger_sa_key.private_key
}

resource "google_secret_manager_secret_iam_member" "trigger_json_key_secret_binding" {
  secret_id = google_secret_manager_secret.trigger_json_key_secret.secret_id
  member = "serviceAccount:${google_service_account.service_account_trigger.email}"
  role = "roles/secretmanager.secretAccessor"
}

data "google_iam_policy" "trigger_sa_invoke_right_policy" {
  binding {
    role = "roles/run.invoker"
    members = [
      "serviceAccount:${google_service_account.service_account_trigger.email}",
    ]
  }
}

resource "google_cloud_run_v2_job_iam_policy" "trigger_sa_invoke_right" {
  location = var.location_full
  name = google_cloud_run_v2_job.search_job.name
  project = var.project
  policy_data = data.google_iam_policy.trigger_sa_invoke_right_policy.policy_data
}

resource "google_cloudfunctions_function" "search_request_trigger" {
  name        = "search-request-trigger"
  # NOTE, Cloud functions not supported in europe-north1 it seems, using gw_location instead
  region      = var.gw_location_full
  description = "When new search request is inserted, automatically invokes search job in Cloud Run."
  runtime     = "nodejs16"
  event_trigger {
    event_type = "providers/cloud.firestore/eventTypes/document.create"
    resource   = "projects/${var.project}/databases/(default)/documents/search_requests/{id}"
  }
  source_archive_bucket        = "${var.project}-terraform-backend"
  source_archive_object        = "search-request-trigger/search-request-trigger.zip"
  available_memory_mb          = 128
  timeout                      = 60
  entry_point                  = "searchRequestWriteTrigger"
  service_account_email = google_service_account.service_account_trigger.email

  environment_variables = {
    PROJECT = var.project
    LOCATION = var.location_full
    SEARCH_JOB = google_cloud_run_v2_job.search_job.name
    SECRET_NAME = google_secret_manager_secret.trigger_json_key_secret.secret_id
  }

  secret_volumes {
    mount_path = "/secrets"
    secret     = google_secret_manager_secret.trigger_json_key_secret.secret_id
  }
}