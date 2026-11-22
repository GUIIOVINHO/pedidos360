# Libera los puertos de todos los servicios (backend y frontend).
# Para detener tambien los contenedores de Docker:
#   docker compose -f docker-compose.yml down
#   docker compose -f docker/docker-compose.kafka-ui.yml down
#   docker compose -f docker/docker-compose.kafka.yml down
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

Write-Host "Puertos 8080 a 8085 y 4200 liberados."
