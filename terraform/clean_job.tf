resource "google_cloud_run_v2_job" "clean_job" {
  name     = "tiira-watcher-clean-job"
  location = var.location_full

  template {
    template {
      timeout = "60s"
      containers {
        image = "${var.location_full}-docker.pkg.dev/${var.project}/tiira-watcher-repo/tiira-watcher:${local.server_version}"
        args = ["clean"]
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
resource "google_cloud_run_v2_job_iam_policy" "allow_invoke_clean_job" {
  location = var.location_full
  name = google_cloud_run_v2_job.clean_job.name
  project = var.project
  # From search_req_processor.tf
  policy_data = data.google_iam_policy.trigger_sa_invoke_right_policy.policy_data
}

# Scheduler docs: https://cloud.google.com/run/docs/execute/jobs-on-schedule#terraform

resource "google_cloud_scheduler_job" "job" {
  provider         = google-beta
  name             = "schedule-clean-job"
  description      = "Clean up old search reqs and sightings every night"
  schedule         = "0 5 * * *"
  attempt_deadline = "320s"
  # Use alternate location as cloud scheduler not available in Finland
  region           = var.gw_location_full
  project          = var.project

  retry_config {
    retry_count = 3
  }

  http_target {
    http_method = "POST"
    uri         = "https://${google_cloud_run_v2_job.clean_job.location}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_number}/jobs/${google_cloud_run_v2_job.clean_job.name}:run"

    oauth_token {
      service_account_email = google_service_account.service_account_trigger.email
    }
  }

  depends_on = [resource.google_cloud_run_v2_job.clean_job, resource.google_cloud_run_v2_job_iam_policy.allow_invoke_clean_job]
}


