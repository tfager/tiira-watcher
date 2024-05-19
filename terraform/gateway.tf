resource "google_api_gateway_api" "api" {
  provider = google-beta
  api_id   = "tiira-watcher-api"
}

resource "google_service_account" "service_account_apigw" {
  account_id   = "apigw-sa"
  display_name = "API GW Service Account"
}

resource "google_api_gateway_api_config" "api_cfg" {
  provider             = google-beta
  api                  = google_api_gateway_api.api.api_id
  api_config_id_prefix = "tiira-api-config"

  openapi_documents {
    document {
      path = "spec.yaml"
      contents = base64encode(templatefile("./tiira_watcher_api_swagger.yml",
        { project      = var.project
          # TODO: Not sure if this works on initial run, since the API GW is not yet created
          api_endpoint = google_cloud_run_v2_service.tiira_watcher_api.uri
          api_gateway  = var.api_gateway
      }))
    }
    
  }

  lifecycle {
    create_before_destroy = true
  }

  gateway_config {
    backend_config {
      google_service_account = google_service_account.service_account_apigw.name
    }
  }
  depends_on = [google_cloud_run_v2_service.tiira_watcher_api]
}

# API GW not available in europe-north1 as of this writing: https://cloud.google.com/api-gateway/docs/deployment-model
# Thus separate variable gw_location_full
resource "google_api_gateway_gateway" "api_gw" {
  provider   = google-beta
  api_config = google_api_gateway_api_config.api_cfg.id
  gateway_id = "api-gw"
  region     = var.gw_location_full
}

output "apigw_endpoint" {
  value = google_api_gateway_gateway.api_gw.default_hostname
}