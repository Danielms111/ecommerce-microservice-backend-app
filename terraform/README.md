# Instrucciones para desplegar la infraestructura de microservicios con Terraform

## Prerrequisitos

1. **Azure CLI** instalado y configurado
2. **Terraform** instalado
3. **Cuenta de Azure Student** (para costos bajos)

## Pasos para el despliegue

### 1. Configurar Azure CLI
```powershell
# Hacer login en Azure
az login

# Verificar que tienes acceso a tu suscripción de estudiante
az account show
```

### 2. Inicializar y aplicar Terraform
```powershell
# Navegar al directorio de terraform
cd terraform

# Inicializar Terraform
terraform init

# Ver qué recursos se van a crear (opcional)
terraform plan

# Aplicar la configuración
terraform apply
```

### 3. Obtener las URLs de acceso
Después del despliegue, Terraform mostrará las URLs importantes:
- **API Gateway**: Puerto principal de acceso (8080)
- **Service Discovery**: Para monitorear servicios registrados (8761)
- **Cloud Config**: Servidor de configuración centralizada (9296)

## Características de esta infraestructura BÁSICA y ECONÓMICA

### ✅ Optimizada para estudiantes
- **SKU Basic** para Container Registry (la más barata)
- **Container Instances** con recursos mínimos (0.5 CPU, 1GB RAM)
- **Solo servicios esenciales** desplegados inicialmente
- **East US** (región económica)

### 🏗️ Servicios incluidos
1. **Service Discovery** (Eureka) - Registro de servicios
2. **Cloud Config** - Configuración centralizada
3. **API Gateway** - Punto de entrada principal
4. **Product Service** - Gestión de productos
5. **User Service** - Gestión de usuarios

### 💰 Estimación de costos (aproximada)
- Container Registry Basic: ~$5/mes
- Container Instances (5 contenedores pequeños): ~$15-25/mes
- **Total estimado: $20-30/mes**

## Comandos útiles

### Ver estado de los recursos
```powershell
# Ver todos los recursos creados
terraform show

# Ver solo los outputs
terraform output
```

### Destruir la infraestructura
```powershell
# CUIDADO: Esto eliminará todos los recursos
terraform destroy
```

### Monitorear los contenedores
```powershell
# Ver logs de un container group
az container logs --resource-group rg-ecommerce-microservices --name api-gateway

# Ver estado de todos los container groups
az container list --resource-group rg-ecommerce-microservices --output table
```

## Próximos pasos (opcional)

Si quieres agregar más servicios, puedes duplicar el bloque de `azurerm_container_group` para:
- Order Service (Puerto 8300)
- Payment Service (Puerto 8400)
- Shipping Service (Puerto 8500)
- Favourite Service (Puerto 8600)

## Notas importantes

1. **Los contenedores usan imágenes públicas** ya disponibles en Docker Hub
2. **La configuración es mínima** para mantener costos bajos
3. **No incluye bases de datos persistentes** (para ahorrar costos)
4. **Algunos servicios son privados** (sin IP pública) para ahorrar costos
5. **Perfect para demos y desarrollo**, no para producción real

## Troubleshooting

Si algún contenedor no inicia:
1. Verifica los logs: `az container logs --resource-group rg-ecommerce-microservices --name [nombre-contenedor]`
2. Verifica el estado: `az container show --resource-group rg-ecommerce-microservices --name [nombre-contenedor]`
3. Los servicios pueden tardar 2-3 minutos en estar completamente disponibles
