# Levanta todos los microservicios del backend + el frontend, cada uno en su propia ventana.
#   .\start.ps1           -> usa RabbitMQ y Kafka que ya tengas levantados
#   .\start.ps1 -Docker   -> ademas levanta con Docker: RabbitMQ, Kafka (Zookeeper + 2 brokers) y Kafka UI
param([switch]$Docker)

$ErrorActionPreference = "Stop"

$ports = @(8080, 8081, 8082, 8083, 8084, 8085, 4200)

foreach ($port in $ports) {
    $processes = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($processId in $processes) {
        try {
            Stop-Process -Id $processId -Force
            Write-Host "Puerto $port liberado. Proceso detenido: $processId"
        } catch {
            Write-Host "No se pudo detener el proceso $processId del puerto $port"
        }
    }
}

$backendPath = Join-Path $PSScriptRoot "backend"
$frontendPath = Join-Path $PSScriptRoot "frontend"

if (-not (Test-Path (Join-Path $backendPath "firebase-service-account.json"))) {
    Write-Host "ATENCION: falta backend\firebase-service-account.json (credenciales de Firebase). Pedidos, analytics y los administradores no podran validar el JWT." -ForegroundColor Yellow
}

function Wait-Port([int]$Port, [int]$TimeoutSeconds) {
    $limite = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $limite) {
        if (Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue) {
            return $true
        }
        Start-Sleep -Seconds 3
    }
    return $false
}

if ($Docker) {
    Set-Location $PSScriptRoot

    Write-Host "Preparando la red de Docker (pedidos360-net)..."
    $redes = docker network ls --format "{{.Name}}"
    if (-not ($redes -contains "pedidos360-net")) {
        docker network create pedidos360-net | Out-Null
    }

    Write-Host "Levantando RabbitMQ..."
    docker compose -f docker-compose.yml up -d

    Write-Host "Levantando Kafka (Zookeeper + 2 brokers)..."
    docker compose -f docker/docker-compose.kafka.yml up -d

    Write-Host "Esperando a que los brokers de Kafka acepten conexiones (puede tardar hasta 90 segundos)..."
    $kafkaListo = (Wait-Port 9092 90) -and (Wait-Port 9093 90)
    if ($kafkaListo) {
        Start-Sleep -Seconds 15
    } else {
        Write-Host "ATENCION: Kafka no respondio a tiempo. Revisa con: docker compose -f docker/docker-compose.kafka.yml logs" -ForegroundColor Yellow
    }

    Write-Host "Levantando Kafka UI..."
    docker compose -f docker/docker-compose.kafka-ui.yml up -d
}

# Orden: consumidores de RabbitMQ (declaran las colas), administradores, pedidos (declara el topico de eventos)
# y al final analytics (consume ese topico y declara el de metricas).
$services = @(
    "notificaciones-service",
    "tickets-service",
    "rabbit-admin-service",
    "kafka-admin-service",
    "pedidos-service",
    "analytics-service"
)

foreach ($service in $services) {
    Write-Host "Iniciando $service..."
    $servicePath = Join-Path $backendPath $service
    Start-Process powershell -WorkingDirectory $servicePath -ArgumentList @('-NoExit', '-Command', 'mvn spring-boot:run')
    Start-Sleep -Seconds 2
}

Write-Host "Iniciando frontend..."
Start-Process powershell -WorkingDirectory $frontendPath -ArgumentList @('-NoExit', '-Command', 'npm install; npm start -- --host 0.0.0.0 --port 4200')

Write-Host ""
Write-Host "Aplicacion levantada (los servicios tardan 1 o 2 minutos en estar listos)."
Write-Host "Frontend:         http://localhost:4200  (Metricas en vivo: /metricas)"
Write-Host "pedidos-service:  http://localhost:8080"
Write-Host "notificaciones:   puerto 8081 (consumidor RabbitMQ + alertas Kafka)"
Write-Host "tickets:          puerto 8082 (consumidor RabbitMQ)"
Write-Host "rabbit-admin:     http://localhost:8083"
Write-Host "analytics:        http://localhost:8084 (consumidor/productor Kafka + metricas)"
Write-Host "kafka-admin:      http://localhost:8085"
Write-Host "Panel RabbitMQ:   http://localhost:15672 (guest / guest)"
Write-Host "Kafka UI:         http://localhost:8090"
