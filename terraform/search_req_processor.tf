resource "google_service_account" "service_account_trigger" {
  account_id   = "trigger-sa"
  display_name = "Cloud Run to Pub/Sub Trigger Service Account"
}

# TODO: Delete old, create with this: https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloud_run_v2_job
resource "google_project_iam_member" "trigger_sa_pub_sub" {
  provider = google-beta
  project  = var.project
  role     = "roles/pubsub.publisher"
  member   = "serviceAccount:${google_service_account.service_account_trigger.email}"
}

# TODO: Maybe not needed?
resource "google_project_iam_member" "trigger_sa_event_receiver" {
  provider = google-beta
  project  = var.project
  role     = "roles/eventarc.eventReceiver"
  member   = "serviceAccount:${google_service_account.service_account_trigger.email}"
}

resource "google_service_account_key" "trigger_sa_key" {
  service_account_id = google_service_account.service_account_trigger.id
}

resource "google_secret_manager_secret" "trigger_json_key_secret" {
  secret_id = "trigger-sa-json-key-secret"
  replication {
    automatic = true
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
  # TODO get properly
  name = "tiira-watcher-search-job"
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
    resource   = "projects/tiira-watcher-dev/databases/(default)/documents/search_requests/{id}"
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
    # TODO Get properly
    SEARCH_JOB = "tiira-watcher-search-job"
    SECRET_NAME = google_secret_manager_secret.trigger_json_key_secret.secret_id
  }

  secret_volumes {
    mount_path = "/secrets"
    secret     = google_secret_manager_secret.trigger_json_key_secret.secret_id
  }
}