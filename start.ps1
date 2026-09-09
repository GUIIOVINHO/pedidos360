$ErrorActionPreference = "Stop"

$ports = @(8080, 4200)

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

Write-Host "Iniciando backend..."
Start-Process powershell -WorkingDirectory $backendPath -ArgumentList @('-NoExit', '-Command', 'mvn spring-boot:run')

Write-Host "Iniciando frontend..."
Start-Process powershell -WorkingDirectory $frontendPath -ArgumentList @('-NoExit', '-Command', 'npm install; npm start -- --host 0.0.0.0 --port 4200')

Write-Host ""
Write-Host "Aplicacion levantada."
Write-Host "Backend: http://localhost:8080"
Write-Host "Frontend: http://localhost:4200"
