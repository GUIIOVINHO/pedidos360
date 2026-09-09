$ErrorActionPreference = "Stop"

$ports = @(8080, 4200)

foreach ($port in $ports) {
    $processes = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($pid in $processes) {
        try {
            Stop-Process -Id $pid -Force
            Write-Host "Puerto $port liberado. Proceso detenido: $pid"
        } catch {
            Write-Host "No se pudo detener el proceso $pid del puerto $port"
        }
    }
}

Write-Host "Puertos 8080 y 4200 liberados."
