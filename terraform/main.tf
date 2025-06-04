# Terraform básico para desplegar microservicios en Azure Container Instances
# Configuración mínima para estudiantes (COSTO MUY BAJO)

terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Grupo de recursos
resource "azurerm_resource_group" "ecommerce_rg" {
  name     = "rg-ecommerce-microservices"
  location = "East US" # Región económica
}

# Container Registry básico (SKU básico para estudiantes)
resource "azurerm_container_registry" "acr" {
  name                = "acrecommercestudent"
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  location            = azurerm_resource_group.ecommerce_rg.location
  sku                 = "Basic" # La opción más económica
  admin_enabled       = true
}

# Comentamos la red virtual para simplificar y evitar conflictos
resource "azurerm_virtual_network" "vnet" {
  name                = "vnet-ecommerce"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
}

resource "azurerm_subnet" "subnet" {
  name                 = "subnet-containers"
  resource_group_name  = azurerm_resource_group.ecommerce_rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = ["10.0.1.0/24"]
}

# Service Discovery (Puerto 8761)
resource "azurerm_container_group" "service_discovery" {
  name                = "service-discovery-v2"
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  ip_address_type     = "Public"
  dns_name_label      = "service-discovery-ecommerce-v2"
  os_type             = "Linux"

  container {
    name   = "service-discovery"
    image  = "danielm11/service-discovery-ecommerce-boot:0.1.0"
    cpu    = "0.5" # Mínimo para ahorrar costos
    memory = "1.0"

    ports {
      port     = 8761
      protocol = "TCP"
    }

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "prod"
    }
  }

  tags = {
    Environment = "student"
    Project     = "ecommerce-microservices"
  }
}

# Cloud Config (Puerto 9296)
resource "azurerm_container_group" "cloud_config" {
  name                = "cloud-config-v2"
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  ip_address_type     = "Public"
  dns_name_label      = "cloud-config-ecommerce-v2"
  os_type             = "Linux"

  container {
    name   = "cloud-config"
    image  = "danielm11/cloud-config-ecommerce-boot:0.1.0"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = 9296
      protocol = "TCP"
    }

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "prod"
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://${azurerm_container_group.service_discovery.fqdn}:8761/eureka/"
    }
  }

  tags = {
    Environment = "student"
    Project     = "ecommerce-microservices"
  }
}

# API Gateway (Puerto 8080)
resource "azurerm_container_group" "api_gateway" {
  name                = "api-gateway-v2"
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  ip_address_type     = "Public"
  dns_name_label      = "api-gateway-ecommerce-v2"
  os_type             = "Linux"

  container {
    name   = "api-gateway"
    image  = "danielm11/api-gateway-ecommerce-boot:0.1.0"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = 8080
      protocol = "TCP"
    }

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "prod"
      SPRING_CONFIG_IMPORT = "optional:configserver:http://${azurerm_container_group.cloud_config.fqdn}:9296/"
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://${azurerm_container_group.service_discovery.fqdn}:8761/eureka/"
    }
  }

  tags = {
    Environment = "student"
    Project     = "ecommerce-microservices"
  }
}

# Product Service (Puerto 8100)
resource "azurerm_container_group" "product_service" {
  name                = "product-service-v2"
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  ip_address_type     = "Public"  # Cambiamos a público para evitar problemas de networking
  dns_name_label      = "product-service-ecommerce-v2"
  os_type             = "Linux"

  container {
    name   = "product-service"
    image  = "danielm11/product-service-ecommerce-boot:0.1.0"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = 8100
      protocol = "TCP"
    }

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "prod"
      SPRING_CONFIG_IMPORT = "optional:configserver:http://${azurerm_container_group.cloud_config.fqdn}:9296/"
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://${azurerm_container_group.service_discovery.fqdn}:8761/eureka/"
    }
  }

  tags = {
    Environment = "student"
    Project     = "ecommerce-microservices"
  }
}

# User Service (Puerto 8200)
resource "azurerm_container_group" "user_service" {
  name                = "user-service-v2"
  location            = azurerm_resource_group.ecommerce_rg.location
  resource_group_name = azurerm_resource_group.ecommerce_rg.name
  ip_address_type     = "Public"  # Cambiamos a público para simplificar
  dns_name_label      = "user-service-ecommerce-v2"
  os_type             = "Linux"

  container {
    name   = "user-service"
    image  = "danielm11/user-service-ecommerce-boot:0.1.0"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = 8200
      protocol = "TCP"
    }

    environment_variables = {
      SPRING_PROFILES_ACTIVE = "prod"
      SPRING_CONFIG_IMPORT = "optional:configserver:http://${azurerm_container_group.cloud_config.fqdn}:9296/"
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://${azurerm_container_group.service_discovery.fqdn}:8761/eureka/"
    }
  }

  tags = {
    Environment = "student"
    Project     = "ecommerce-microservices"
  }
}
