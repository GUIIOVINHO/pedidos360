# Demo del administrador de Kafka y RabbitMQ con JWT (para grabar el video).
# Uso:
#   .\demo-admin.ps1 -Corto        -> solo 4 pasos de Kafka (401 sin token, crear, modificar y eliminar un topico)
#   .\demo-admin.ps1 -SoloKafka    -> todos los pasos de Kafka (sin RabbitMQ)
#   .\demo-admin.ps1               -> Kafka y RabbitMQ
# Pide el correo y la clave de un usuario de Firebase (correo/clave) y ejecuta cada paso cuando presionas Enter.
# Las credenciales NO se guardan en el archivo.
param([switch]$Corto, [switch]$SoloKafka)

$apiKey = "AIzaSyDfUbd-Uc9teZ13mQe2WTYh1BT160UKyAA"   # apiKey publica del frontend (environment.ts)

function Paso($n, $titulo) {
    if ($Corto -and ($n -notin 1, 4, 5, 8)) { return $false }
    if (($Corto -or $SoloKafka) -and $n -ge 9) { return $false }
    Write-Host ""
    Write-Host "=== Paso $n. $titulo ===" -ForegroundColor Cyan
    Read-Host "Enter para ejecutar" | Out-Null
    return $true
}

$email = Read-Host "Correo del usuario de Firebase"
$passSecure = Read-Host "Clave" -AsSecureString
$pass = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($passSecure))

$body = @{ email = $email; password = $pass; returnSecureToken = $true } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -ContentType "application/json" -Body $body `
    -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
$token = $login.idToken
$h = @{ Authorization = "Bearer $token" }
Write-Host "Token JWT obtenido ($($token.Length) caracteres)" -ForegroundColor Green

if (Paso 1 "Sin token la API responde 401") {
    curl.exe -s -i http://localhost:8085/api/topics
}

if (Paso 2 "Kafka: brokers del cluster") {
    Invoke-RestMethod -Uri http://localhost:8085/api/cluster -Headers $h | ConvertTo-Json -Depth 5
}

if (Paso 3 "Kafka: lista de topicos") {
    Invoke-RestMethod -Uri http://localhost:8085/api/topics -Headers $h | Format-Table
}

if (Paso 4 "Kafka: crear el topico demo.topic (2 particiones, 2 replicas, retencion 1 hora)") {
    Invoke-RestMethod -Method Post -Uri http://localhost:8085/api/topics -Headers $h -ContentType "application/json" `
        -Body '{"name":"demo.topic","partitions":2,"replicationFactor":2,"retentionMs":3600000,"cleanupPolicy":"delete","minInsyncReplicas":1}' | ConvertTo-Json -Depth 6
}

if (Paso 5 "Kafka: modificar la retencion de demo.topic a 2 horas") {
    Invoke-RestMethod -Method Patch -Uri http://localhost:8085/api/topics/demo.topic/config -Headers $h -ContentType "application/json" `
        -Body '{"set":{"retention.ms":"7200000"}}' | ConvertTo-Json -Depth 6
}

if (Paso 6 "Kafka: consumer group analytics-metricas (miembros, particiones y lag)") {
    Invoke-RestMethod -Uri http://localhost:8085/api/consumer-groups/analytics-metricas -Headers $h | ConvertTo-Json -Depth 6
}

if (Paso 7 "Kafka: validacion (nombre vacio y 0 particiones -> 400)") {
    curl.exe -s -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"name":"","partitions":0,"replicationFactor":2}' http://localhost:8085/api/topics
}

if (Paso 8 "Kafka: eliminar demo.topic") {
    Invoke-RestMethod -Method Delete -Uri http://localhost:8085/api/topics/demo.topic -Headers $h
    Write-Host "demo.topic eliminado"
}

if (Paso 9 "RabbitMQ: crear cola, exchange y binding") {
    Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/queues -Headers $h -ContentType "application/json" -Body '{"name":"demo.queue","deadLetterExchange":"pedidos.dlx","deadLetterRoutingKey":"demo.dead"}'
    Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/exchanges -Headers $h -ContentType "application/json" -Body '{"name":"demo.exchange","type":"topic"}'
    Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/bindings -Headers $h -ContentType "application/json" -Body '{"queue":"demo.queue","exchange":"demo.exchange","routingKey":"demo.key"}'
}

if (Paso 10 "RabbitMQ: eliminar binding, exchange y cola") {
    Invoke-RestMethod -Method Delete -Headers $h -Uri "http://localhost:8083/api/bindings?queue=demo.queue&exchange=demo.exchange&routingKey=demo.key"
    Invoke-RestMethod -Method Delete -Headers $h -Uri http://localhost:8083/api/exchanges/demo.exchange
    Invoke-RestMethod -Method Delete -Headers $h -Uri http://localhost:8083/api/queues/demo.queue
}

Write-Host ""
Write-Host "Listo" -ForegroundColor Green
