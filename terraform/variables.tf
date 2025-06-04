# Variables para configuración básica
variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "East US"
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
  default     = "rg-ecommerce-microservices"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "student"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ecommerce-microservices"
}
