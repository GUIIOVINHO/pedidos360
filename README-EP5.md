# Pedidos360 - EP5 DSY1107: streaming y analitica con Apache Kafka

A lo que ya existia (Angular + Spring Boot + Firebase + RabbitMQ) se le agrego **Apache Kafka**, sin tocar el flujo de RabbitMQ.

## Que se agrego

| Pieza | Puerto | Que hace |
|---|---|---|
| `pedidos-service` (modificado) | 8080 | Ademas de publicar en RabbitMQ, **produce** cada evento de pedido al topico Kafka `pedidos.eventos` |
| `analytics-service` (**nuevo**) | 8084 | **Consume** `pedidos.eventos` (grupo `analytics-metricas`), calcula metricas en tiempo real y las **produce** al topico `pedidos.metricas`. Expone `GET /api/metricas` (JWT) |
| `notificaciones-service` (modificado) | 8081 | Sigue consumiendo RabbitMQ igual que antes y ahora **consume** `pedidos.metricas` (grupo `notificaciones-alertas`) para emitir alertas de alta demanda |
| `kafka-admin-service` (**nuevo**) | 8085 | API REST (JWT) para administrar el cluster: crear/listar/describir/modificar/eliminar topicos, configuraciones, particiones y consumer groups (con lag) |
| Frontend | 4200 | Nueva vista **Metricas en vivo** (`/metricas`) que muestra el flujo de eventos y las metricas |
| `docker/docker-compose.kafka.yml` | 9092/9093 | Zookeeper + 2 brokers Kafka (replicacion real) |
| `docker/docker-compose.kafka-ui.yml` | 8090 | Kafka UI para ver brokers, topicos, mensajes y consumer groups |
| `docker-compose.yml` (ya existia) | 5672 / 15672 | RabbitMQ (EP3) |

## Arquitectura

```
Angular --JWT--> pedidos-service (8080) --guarda--> H2 --> responde 201 al tiro
                       |                      |
                       | RabbitMQ (EP3)       | Kafka (EP5)
                       v                      v
        notificaciones.queue          topico pedidos.eventos  (3 particiones, 2 replicas, clave = id del pedido)
        tickets.queue                          |
        (tareas: notificar, ticket)            v  grupo analytics-metricas (3 hilos = 1 por particion)
                                       analytics-service (8084) --calcula metricas en memoria
                                               |                       |
                                               | produce               | GET /api/metricas (JWT)
                                               v                       v
                                  topico pedidos.metricas        Angular: /metricas (cada 3 s)
                                  (compactado, 1 particion)
                                               |
                                               v  grupo notificaciones-alertas
                                  notificaciones-service --> [ALERTA] alta demanda

   errores Kafka: reintentos (3) -> topico DLT (pedidos.eventos.analytics.DLT / pedidos.metricas.notificaciones.DLT)
   kafka-admin-service (8085): administra topicos, configuraciones y consumer groups del cluster
```

## Por que RabbitMQ Y Kafka (para la presentacion)

- **RabbitMQ = tareas.** "Envia esta notificacion", "genera este ticket": cada mensaje lo procesa un consumidor, se confirma (ACK) y desaparece. Si falla, reintento y DLQ.
- **Kafka = flujo de eventos.** Cada evento de negocio queda registrado en un log ordenado y **se puede releer**. Varios grupos independientes leen el mismo flujo (analitica y alertas) sin quitarse mensajes. Sirve para metricas en tiempo real.
- Conviven sin afectarse: `pedidos-service` publica en ambos y, si uno de los dos falla, **solo se registra el error** y el pedido igual se crea.
- Decisiones de diseno: clave = id del pedido (los eventos de un pedido llegan en orden a la misma particion), 3 particiones (hasta 3 consumidores en paralelo), 2 replicas (tolera la caida de un broker), `acks=all` + idempotencia, topico de metricas compactado (solo importa la ultima foto), retencion de 7 dias en eventos.

## 1) Aplicar los cambios a tu repo

Desde la raiz del repo (`Desktop\pedidos360`), en PowerShell:

```powershell
Expand-Archive -Path .\pedidos360-ep5.zip -DestinationPath .\tmp -Force
Copy-Item .\tmp\* . -Recurse -Force
Remove-Item -Recurse -Force .\tmp
Remove-Item .\pedidos360-ep5.zip
```
Esto agrega las carpetas nuevas y reemplaza `start.ps1`, `stop.ps1`, los `pom.xml`/`application.yml` de pedidos y notificaciones, `PedidoController`, y 3 archivos del frontend (`environment.ts`, `app.routes.ts`, `pedidos-list.component.ts`; en `environment.ts` solo se agrego `analyticsUrl`).

## 2) Levantar todo

Requisitos: Docker Desktop encendido (idealmente con **4 GB de RAM o mas**), Java 17, Maven, Node y `backend\firebase-service-account.json`.

```powershell
.\start.ps1 -Docker
```
Crea la red `pedidos360-net`, levanta RabbitMQ, Kafka (Zookeeper + 2 brokers) y Kafka UI, espera a que Kafka responda y abre una ventana por cada microservicio mas el frontend. Tarda 2 a 3 minutos.

Sin el script (manual):
```powershell
docker network create pedidos360-net
docker compose -f docker-compose.yml up -d
docker compose -f docker/docker-compose.kafka.yml up -d
docker compose -f docker/docker-compose.kafka-ui.yml up -d
```

Paneles: RabbitMQ http://localhost:15672 (guest/guest) - **Kafka UI http://localhost:8090** - App http://localhost:4200

Para apagar: `.\stop.ps1` y luego `docker compose -f docker/docker-compose.kafka-ui.yml down`, `docker compose -f docker/docker-compose.kafka.yml down` y `docker compose down`.

## 3) Guion de demostracion

1. **Kafka UI** (http://localhost:8090): Brokers (2), Topics: `pedidos.eventos` (3 particiones, replicas 2), `pedidos.metricas` (1 particion, `cleanup.policy=compact`) y las DLT. Consumers: `analytics-metricas` y `notificaciones-alertas`.
2. **Flujo normal:** entra a la app, abre **Metricas en vivo** y en otra pestana crea pedidos. Se ven subir los contadores y aparecer los eventos. En los logs: `pedidos-service` -> `[KAFKA] PEDIDO_CREADO publicado (... particion=, offset=)`, `analytics-service` -> `[KAFKA] Evento recibido ...` y `Metricas publicadas ...`.
3. **Mensajes en Kafka UI:** Topics > `pedidos.eventos` > Messages (se ve la clave = id del pedido y el JSON). En `pedidos.metricas` queda la ultima foto con clave `global`.
4. **Alerta (segundo consumidor/otro grupo):** crea 5 pedidos en menos de un minuto -> `notificaciones-service` muestra `[ALERTA] ALTA DEMANDA ...` y el recuadro de la vista de metricas se pone rojo.
5. **Errores y DLT:**
   - cantidad `5000` -> evento invalido: `analytics-service` lo manda directo a la DLT (sin reintentos).
   - producto `FALLO-DLQ` -> 3 reintentos (`intento 1..4`) y luego a la DLT.
   - El pedido igual se crea y RabbitMQ sigue funcionando (notificaciones y tickets tambien se ven en su DLQ, como en la EP3).
   - Ver el mensaje en Kafka UI: Topics > `pedidos.eventos.analytics.DLT` > Messages.
6. **Consumer group:** en Kafka UI > Consumers > `analytics-metricas`: 3 miembros, una particion cada uno y lag 0.
7. **Administrador de Kafka (JWT):** con el token de Firebase (mismo procedimiento que el administrador de RabbitMQ):

```powershell
$body  = @{ email = "TU_CORREO"; password = "TU_CLAVE"; returnSecureToken = $true } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -ContentType "application/json" -Body $body -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=TU_API_KEY"
$h = @{ Authorization = "Bearer $($login.idToken)" }

Invoke-RestMethod -Uri http://localhost:8085/api/cluster -Headers $h | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri http://localhost:8085/api/topics -Headers $h
Invoke-RestMethod -Uri http://localhost:8085/api/topics/pedidos.eventos -Headers $h | ConvertTo-Json -Depth 6

# crear, modificar, aumentar particiones y eliminar un topico
Invoke-RestMethod -Method Post -Uri http://localhost:8085/api/topics -Headers $h -ContentType "application/json" -Body '{"name":"demo.topic","partitions":2,"replicationFactor":2,"retentionMs":3600000,"cleanupPolicy":"delete","minInsyncReplicas":1}' | ConvertTo-Json -Depth 6
Invoke-RestMethod -Method Patch -Uri http://localhost:8085/api/topics/demo.topic/config -Headers $h -ContentType "application/json" -Body '{"set":{"retention.ms":"7200000"}}' | ConvertTo-Json -Depth 6
Invoke-RestMethod -Method Put -Uri http://localhost:8085/api/topics/demo.topic/partitions -Headers $h -ContentType "application/json" -Body '{"partitions":4}' | ConvertTo-Json -Depth 6
Invoke-RestMethod -Method Delete -Uri http://localhost:8085/api/topics/demo.topic -Headers $h

# consumer groups y lag
Invoke-RestMethod -Uri http://localhost:8085/api/consumer-groups -Headers $h
Invoke-RestMethod -Uri http://localhost:8085/api/consumer-groups/analytics-metricas -Headers $h | ConvertTo-Json -Depth 6

# validacion: factor de replicacion mayor al numero de brokers -> error claro; nombre vacio -> 400; sin token -> 401
Invoke-RestMethod -Method Post -Uri http://localhost:8085/api/topics -Headers $h -ContentType "application/json" -Body '{"name":"","partitions":0,"replicationFactor":2}'
```
(Los dos ultimos comandos deben fallar: es lo esperado. Para ver el mensaje: `try { ... } catch { $_.ErrorDetails.Message }`.)

## 4) Donde esta cada cosa de la rubrica EP5

| Indicador (peso) | Donde verlo |
|---|---|
| Configuracion del cliente Kafka (12%) | `spring.kafka.*` en el `application.yml` de pedidos, analytics y notificaciones: bootstrap servers (2 brokers), serializers/deserializers, `group-id`, `acks=all`, idempotencia, timeouts, `auto-offset-reset`, commit manual por registro, `CooperativeStickyAssignor`, concurrencia |
| Productores y consumidores con los dos topicos y DTO (12%) | `pedidos.eventos`: produce pedidos, consume analytics. `pedidos.metricas`: produce analytics, consume notificaciones. DTO tipados: `PedidoEvent`, `PedidoEventoDto`, `MetricasPedidosDto` |
| Admin: crear, listar, mantener (6%) | `kafka-admin-service/controller/KafkaAdminController` + `service/KafkaAdminService` |
| Productores (14%) | `PedidoKafkaPublisher` (pedidos) y `MetricasPublisher` (analytics): `KafkaTemplate`, `JsonSerializer`, clave por pedido, callback con particion y offset |
| Consumidores (14%) | `PedidoEventoConsumer` y `MetricasAlertaConsumer` con `@KafkaListener`; la logica esta aparte en `MetricasService` y `AlertaDemandaService` (no importan nada de Kafka) |
| Manejo de errores (12%) | `ErrorHandlingDeserializer` (mensaje malformado), validacion del payload en los servicios, `DefaultErrorHandler` con `FixedBackOff` (3 reintentos), `DeadLetterPublishingRecoverer` a topico DLT, logs con topico/particion/offset (`KafkaErrorHandlingConfig`, `KafkaConsumerConfig`) |
| Parametros de topicos (12%) | `app.kafka.topics.*` en los yml + `KafkaTopicConfig` (NewTopic): particiones, replicas, `retention.ms`, `cleanup.policy` (delete / compact), `min.insync.replicas`; `modify-topic-configs=true` los reajusta si el topico ya existe |
| Consumer groups (12%) | `analytics-metricas` (3 hilos reparten las 3 particiones; mas instancias = mas paralelismo hasta 3) y `notificaciones-alertas` (otro microservicio, recibe todas las metricas). Se inspeccionan con `GET /api/consumer-groups/{id}` y Kafka UI |
| Admin de topicos y grupos (6%) | Endpoints del administrador (ver guion, punto 7) y Kafka UI |
| docker-compose (instrucciones) | `docker-compose.yml` (RabbitMQ), `docker/docker-compose.kafka.yml` (Zookeeper + 2 brokers), `docker/docker-compose.kafka-ui.yml` (Kafka UI) |

Se mantiene todo lo anterior: filtro JWT de Firebase en `pedidos-service`, `rabbit-admin-service`, `analytics-service` y `kafka-admin-service`; login con Firebase en Angular; RabbitMQ con DLQ.

## 5) Preguntas probables y respuesta corta

- **Diferencia Kafka vs RabbitMQ?** RabbitMQ reparte tareas y borra el mensaje al confirmarlo; Kafka guarda un log de eventos que varios grupos pueden leer y releer. Aqui RabbitMQ hace notificaciones y tickets, y Kafka alimenta metricas en tiempo real.
- **Para que sirven las particiones?** Paralelismo y orden: los eventos con la misma clave (mismo pedido) van a la misma particion y se mantienen en orden; hay hasta un consumidor por particion dentro de un grupo.
- **Que es un consumer group?** Consumidores que se reparten las particiones de un topico. Dos grupos distintos leen el mismo topico sin quitarse mensajes.
- **Y si Kafka se cae?** `pedidos-service` no se bloquea (`max.block.ms=3000`): registra el error y el pedido se crea igual. RabbitMQ sigue funcionando.
- **Por que 2 brokers y replicas 2?** Si un broker cae, el otro tiene copia de cada particion.
- **Por que el topico de metricas esta compactado?** Solo interesa la ultima foto por clave, no todo el historial.
- **Como evitan que un mensaje malo frene el procesamiento?** Deserializador tolerante, validacion del evento, reintentos con espera y envio a una DLT; despues el consumidor sigue con el siguiente mensaje.

## 6) Limitaciones (decirlas con honestidad)

- Las metricas viven **en memoria** de `analytics-service`: si se reinicia, el contador vuelve a 0 y solo cuenta eventos nuevos (el offset del grupo ya quedo confirmado). Para persistirlas habria que guardarlas en una base de datos.
- Si se levantan varias instancias de `analytics-service`, cada una veria solo parte de las particiones (el balanceo funciona, pero las metricas en memoria quedarian parciales).
- Este codigo se escribio sin poder compilarlo ni probarlo contra el cluster Kafka. Haz `.\start.ps1 -Docker` con tiempo y, si algo falla, copia el error completo y se corrige.
- `mvn spring-boot:run` no ejecuta los tests; `mvn test` si.
