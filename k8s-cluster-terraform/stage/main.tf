module "staging_cluster" {
    source     = "../main"
    env_name   = "staging"
    project_id = "${var.project_id}"
    instance_type = "e2-medium"
    min_count      = 1
    max_count      = 2
    cluster_name = "k8s-cluster"
}

variable "project_id" {
  description = "The project ID to host the cluster in"
  default = "banded-pager-462814-v0"
}

resource "random_id" "default" {
  byte_length = 8
}

resource "google_storage_bucket" "default" {
  name     = "${random_id.default.hex}-terraform-remote-backend"
  location = "US"
  project  = var.project_id


  force_destroy               = false
  public_access_prevention    = "enforced"
  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }
}

resource "local_file" "default" {
  file_permission = "0644"
  filename        = "${path.module}/backend.tf"


  content = <<-EOT
  terraform {
    backend "gcs" {
      bucket = "${google_storage_bucket.default.name}"
    }
  }
  EOT
}
