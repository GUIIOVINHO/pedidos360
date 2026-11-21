# Levanta los 4 microservicios del backend + el frontend, cada uno en su propia ventana.
#   .\start.ps1           -> usa el RabbitMQ que ya tengas (o el de la nube, via variables RABBITMQ_*)
#   .\start.ps1 -Docker   -> ademas levanta RabbitMQ local con docker compose
param([switch]$Docker)

$ErrorActionPreference = "Stop"

$ports = @(8080, 8081, 8082, 8083, 4200)

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
    Write-Host "ATENCION: falta backend\firebase-service-account.json (credenciales de Firebase). Pedidos y admin no podran validar el JWT." -ForegroundColor Yellow
}

if ($Docker) {
    Write-Host "Levantando RabbitMQ con Docker..."
    docker compose up -d
    Write-Host "Esperando a que RabbitMQ arranque..."
    Start-Sleep -Seconds 12
}

# Primero los consumidores (declaran las colas), despues admin y pedidos.
$services = @("notificaciones-service", "tickets-service", "rabbit-admin-service", "pedidos-service")

foreach ($service in $services) {
    Write-Host "Iniciando $service..."
    $servicePath = Join-Path $backendPath $service
    Start-Process powershell -WorkingDirectory $servicePath -ArgumentList @('-NoExit', '-Command', 'mvn spring-boot:run')
    Start-Sleep -Seconds 2
}

Write-Host "Iniciando frontend..."
Start-Process powershell -WorkingDirectory $frontendPath -ArgumentList @('-NoExit', '-Command', 'npm install; npm start -- --host 0.0.0.0 --port 4200')

Write-Host ""
Write-Host "Aplicacion levantada (los servicios tardan un minuto en estar listos)."
Write-Host "Frontend:        http://localhost:4200"
Write-Host "pedidos-service: http://localhost:8080"
Write-Host "notificaciones:  puerto 8081 (consumidor)"
Write-Host "tickets:         puerto 8082 (consumidor)"
Write-Host "rabbit-admin:    http://localhost:8083"
Write-Host "Panel RabbitMQ:  http://localhost:15672 (guest / guest si usas Docker)"
