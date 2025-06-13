variable "project_id" {
  description = "The project ID to host the cluster in"
  default = "banded-pager-462814-v0"
}
variable "cluster_name" {
  description = "The name for the GKE cluster"
  default     = "learnk8s-cluster"
}
variable "env_name" {
  description = "The environment for the GKE cluster"
  default     = "prod"
}
variable "region" {
  description = "The region to host the cluster in"
  default     = "us-central1"
}
variable "network" {
  description = "The VPC network created to host the cluster in"
  default     = "gke-network"
}
variable "subnetwork" {
  description = "The subnetwork created to host the cluster in"
  default     = "gke-subnet"
}
variable "ip_range_pods_name" {
  description = "The secondary ip range to use for pods"
  default     = "ip-range-pods"
}
variable "ip_range_services_name" {
  description = "The secondary ip range to use for services"
  default     = "ip-range-services"
}
variable "instance_type" {
  default = "e2-medium"
}
variable "min_count" {
  description = "Minimum number of nodes in the pool"
  type        = number
  default     = 1
}
variable "max_count" {
  description = "Maximum number of nodes in the pool"
  type        = number
  default     = 3
}
