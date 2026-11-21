# Pedidos360 - EP3 DSY1107: mensajeria desacoplada con RabbitMQ

Al sistema de la EP1 (Angular + Spring Boot + Firebase Auth) se le incorporo RabbitMQ.
El backend dejo de ser un solo proyecto y ahora son **4 microservicios Spring Boot**:

| Microservicio | Puerto | Rol |
|---|---|---|
| `pedidos-service` | 8080 | API REST de pedidos (la misma de la EP1) + **productor**: publica un evento al crear/actualizar un pedido |
| `notificaciones-service` | 8081 | **Consumidor**: envia (simula en el log) la notificacion al cliente |
| `tickets-service` | 8082 | **Consumidor**: genera el ticket del pedido (archivo `.txt`) |
| `rabbit-admin-service` | 8083 | **Administrador**: API REST para crear/eliminar colas, exchanges y bindings (protegida con JWT) |

El frontend Angular **no cambia**.

## Arquitectura

```
Angular --JWT--> pedidos-service (8080) --guarda en H2--> responde 201 al tiro
                       |
                       | publica evento (async)
                       v
              pedidos.exchange (topic)
               |  pedido.creado / pedido.actualizado          |  pedido.creado
               v                                              v
      notificaciones.queue                              tickets.queue
               |  (consumidor, ACK manual)                    |  (consumidor, ACK manual)
               v                                              v
      notificaciones-service                           tickets-service
               |                                              |
   rechazo / reintentos agotados                  rechazo / reintentos agotados
               v                                              v
        pedidos.dlx (direct) --> notificaciones.dlq     tickets.dlq   --> se registra en el log
```

Idea clave para explicar: **pedidos-service no espera a nadie**. Guarda el pedido, publica el evento y responde.
Si RabbitMQ o un consumidor estan caidos, el pedido igual se crea (el error de publicacion solo queda en el log).

## 1) Actualizar tu repositorio

Desde la carpeta raiz del repo (`pedidos360-main`), en PowerShell:

```powershell
# 1. La credencial de Firebase pasa a una sola copia compartida por todos los microservicios
Move-Item backend\src\main\resources\firebase-service-account.json backend\firebase-service-account.json

# 2. Borrar el backend viejo (monolito de la EP1)
Remove-Item -Recurse -Force backend\src, backend\pom.xml, backend\.gitignore, backend\bin, backend\target -ErrorAction SilentlyContinue

# 3. Descomprimir este zip en la raiz del repo (reemplaza start.ps1, stop.ps1 y .gitignore)
Expand-Archive -Path .\pedidos360-ep3.zip -DestinationPath . -Force
```

Antes de hacer commit revisa con `git status` que **no aparezca** `firebase-service-account.json`.

## 2) RabbitMQ: elige una opcion

**A. Docker (si el computador lo tiene):**
```powershell
.\start.ps1 -Docker
```
Panel de administracion: http://localhost:15672 (usuario `guest`, clave `guest`).

**B. RabbitMQ en la nube (si NO hay Docker, ej. en el instituto):** crea una instancia gratuita en CloudAMQP
(o cualquier broker AMQP), copia host / usuario / clave y define las variables ANTES de correr el script:

```powershell
$env:RABBITMQ_HOST  = "tu-host.rmq.cloudamqp.com"
$env:RABBITMQ_PORT  = "5671"
$env:RABBITMQ_USER  = "tu-usuario"
$env:RABBITMQ_PASS  = "tu-clave"
$env:RABBITMQ_VHOST = "tu-usuario"
$env:RABBITMQ_SSL   = "true"
.\start.ps1
```
(En CloudAMQP el vhost normalmente es igual al usuario.) Todos los servicios leen esas variables; sin ellas usan `localhost`.

## 3) Levantar todo

```powershell
.\start.ps1          # abre 5 ventanas: notificaciones, tickets, admin, pedidos y frontend
.\stop.ps1           # libera los puertos
```
Requisitos: Java 17+, Maven, Node/npm, y `backend\firebase-service-account.json` (el mismo de la EP1).
Los consumidores se levantan primero porque son quienes crean las colas.

## 4) Guion de demostracion para la presentacion

Entra a http://localhost:4200, inicia sesion y crea pedidos desde la pantalla. Mira las ventanas de logs.

1. **Flujo normal:** crea un pedido cualquiera. En `notificaciones-service` aparece `[NOTIFICACION] ...`, en `tickets-service`
   aparece `[TICKET] ...` y se crea `backend\tickets-service\tickets\ticket-<id>.txt`.
2. **Reintentos (recuperable):** pon `FALLO-TEMPORAL` dentro del campo *producto*. Los logs muestran
   `reintento 1/3`, `reintento 2/3` y al tercer intento se procesa bien (ACK).
3. **Reintentos agotados -> DLQ:** producto `FALLO-DLQ`. Falla 3 veces, luego `-> DLQ` y aparece `[DLQ] Mensaje NO entregado ...` en el log.
4. **Error no recuperable -> DLQ directa:** cantidad `5000`. No hay reintentos: va directo a la DLQ.
5. **Desacoplamiento:** cierra la ventana de `notificaciones-service`, crea un pedido -> la API igual responde 201 y el mensaje queda
   esperando en `notificaciones.queue` (verlo en el panel). Al volver a levantar el servicio, lo procesa.
6. **Administrador REST (con JWT):** obten un token de Firebase y llama al servicio de administracion:

```powershell
# Token (usa el apiKey de frontend\src\environments\environment.ts y un usuario email/clave ya creado)
$body  = @{ email = "test@pedidos360.cl"; password = "123456"; returnSecureToken = $true } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -ContentType "application/json" -Body $body `
         -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=TU_API_KEY"
$h = @{ Authorization = "Bearer $($login.idToken)" }

# Crear cola, exchange y binding
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/queues    -Headers $h -ContentType "application/json" -Body '{"name":"demo.queue","deadLetterExchange":"pedidos.dlx","deadLetterRoutingKey":"demo.dead"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/exchanges -Headers $h -ContentType "application/json" -Body '{"name":"demo.exchange","type":"topic"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/bindings  -Headers $h -ContentType "application/json" -Body '{"queue":"demo.queue","exchange":"demo.exchange","routingKey":"demo.key"}'

# Consultar y eliminar
Invoke-RestMethod -Uri http://localhost:8083/api/queues/demo.queue -Headers $h
Invoke-RestMethod -Method Delete -Headers $h -Uri "http://localhost:8083/api/bindings?queue=demo.queue&exchange=demo.exchange&routingKey=demo.key"
Invoke-RestMethod -Method Delete -Headers $h -Uri http://localhost:8083/api/exchanges/demo.exchange
Invoke-RestMethod -Method Delete -Headers $h -Uri http://localhost:8083/api/queues/demo.queue

# Validacion: nombre vacio -> 400 con mensaje claro; sin token -> 401
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/queues -Headers $h -ContentType "application/json" -Body '{"name":""}'
```
No borres ni recrees `notificaciones.queue` / `tickets.queue` con otros argumentos: RabbitMQ rechaza redeclarar una cola con configuracion distinta.

## 5) Donde esta cada cosa de la rubrica

| Indicador (peso) | Donde verlo |
|---|---|
| Nombres centralizados (12%) | `application.yml` de cada servicio (`app.rabbit.*`) y `config/RabbitNamesProperties`. En el codigo Java no hay ningun nombre de cola/exchange escrito |
| Beans Queue/Exchange/Binding (13%) | `config/RabbitConfig` de notificaciones y tickets (un bloque por caso de uso) y de pedidos (exchange) |
| Sin mezclar negocio y mensajeria (10%) | Configuracion en `config/`, logica en `service/`, adaptadores en `consumer/`. Los `*Service` no importan nada de RabbitMQ |
| `@RabbitListener` por dominio (15%) | `notificaciones/consumer/` y `tickets/consumer/` (cada dominio en su microservicio y paquete) |
| ACK y manejo de errores (20%) | `messaging/MessageAckSupport` (ACK manual, 4 rutas: ACK, NACK->DLQ, reintento, DLQ por reintentos agotados). Excepciones `NonRecoverable...` / `Recoverable...` |
| Endpoints REST del administrador (13%) | `rabbit-admin-service/controller/RabbitAdminController` (`/api/queues`, `/api/exchanges`, `/api/bindings`) |
| Logica encapsulada en un servicio (10%) | `rabbit-admin-service/service/RabbitAdminService` (el controlador no toca RabbitMQ) |
| Validacion de parametros (7%) | DTOs con anotaciones (`QueueRequest`, `ExchangeRequest`, `BindingRequest`) + `GlobalExceptionHandler` con mensajes claros |

Ademas se mantiene lo de la EP1: filtro JWT (`FirebaseAuthFilter`) en `pedidos-service` y `rabbit-admin-service`, login con Firebase en Angular.

## 6) Preguntas probables y respuesta corta

- **Por que RabbitMQ y no llamar directo al otro servicio?** Desacopla: pedidos no depende de que notificaciones o tickets esten vivos, y si fallan el mensaje espera en la cola.
- **Que es una DLQ?** Cola a donde RabbitMQ manda los mensajes que no se pudieron procesar (rechazados o con reintentos agotados) para no perderlos ni bloquear la cola principal.
- **Por que ACK manual?** Para confirmar solo cuando el procesamiento termino bien; si no, decidimos entre reintentar o mandar a la DLQ.
- **Por que nunca `requeue=true`?** Un mensaje malo volveria a la cola infinitamente (bucle).
- **Tipo de exchange usado?** `topic` para eventos de pedidos (routing key `pedido.creado`, `pedido.actualizado`) y `direct` para el dead letter exchange.

## Nota

Este codigo se escribio sin poder compilarlo ni probarlo contra un RabbitMQ real. Haz `.\start.ps1` una vez con tiempo
(idealmente un dia antes) y si algun servicio no compila o falla al arrancar, copia el error completo y se corrige.
`mvn spring-boot:run` no ejecuta los tests; `mvn test` si, y con Java 25 Mockito puede quejarse del JDK (no es un error del codigo).
