# Outputs importantes para acceder a los servicios
output "api_gateway_url" {
  description = "URL pública del API Gateway"
  value       = "http://${azurerm_container_group.api_gateway.fqdn}:8080"
}

output "service_discovery_url" {
  description = "URL del Service Discovery (Eureka)"
  value       = "http://${azurerm_container_group.service_discovery.fqdn}:8761"
}

output "cloud_config_url" {
  description = "URL del Cloud Config Server"
  value       = "http://${azurerm_container_group.cloud_config.fqdn}:9296"
}

output "container_registry_login_server" {
  description = "Login server del Container Registry"
  value       = azurerm_container_registry.acr.login_server
}

output "resource_group_name" {
  description = "Nombre del grupo de recursos creado"
  value       = azurerm_resource_group.ecommerce_rg.name
}
