resource "google_api_gateway_api" "api" {
  provider = google-beta
  api_id   = "my-api"
}

resource "google_api_gateway_api_config" "api_cfg" {
  provider      = google-beta
  api           = google_api_gateway_api.api.api_id
  api_config_id = "config1"

  openapi_documents {
    document {
      path     = "spec.yaml"
      contents = filebase64("./tiira_watcher_api_swagger.yml")
    }
  }

  lifecycle {
    create_before_destroy = false
  }

  gateway_config {
    backend_config {
      google_service_account = google_service_account.service_account_apigw.name
    }
  }
}

# API GW not available in europe-north1 as of this writing: https://cloud.google.com/api-gateway/docs/deployment-model
resource "google_api_gateway_gateway" "api_gw" {
  provider   = google-beta
  api_config = google_api_gateway_api_config.api_cfg.id
  gateway_id = "api-gw"
  region = "europe-west1"
  lifecycle {
    ignore_changes = [
      api_config
    ]
  }
}

output "apigw_endpoint" {
  value = google_api_gateway_gateway.api_gw.default_hostname
}