# Pedidos360

Aplicación web para gestionar pedidos, con frontend en Angular, backend en Spring Boot y autenticación basada en Firebase Authentication. La idea principal es mostrar una arquitectura de software moderna con separación de responsabilidades: frontend para la interfaz, backend para la lógica y servicios, y Firebase como identidad y validación de tokens.

## Objetivo del proyecto
Desarrollar una aplicación pequeña, funcional y demostrable, que permita:
- iniciar sesión con Firebase
- proteger la API REST con JWT
- registrar y consultar pedidos
- mostrar una base sólida para una presentación de evaluación técnica

## Stack utilizado
- Frontend: Angular 17
- Backend: Java 17 + Spring Boot 3
- Base de datos: H2 en memoria
- Autenticación: Firebase Authentication
- Validación de sesión: Firebase Admin SDK + JWT
- CORS: configurado para localhost:4200

## Arquitectura
```text
Angular (frontend)
   -> obtiene token de Firebase
   -> envia Authorization: Bearer <jwt>
   -> consume /api/pedidos
                 v
Spring Boot (backend)
   -> FirebaseAuthFilter intercepta /api/**
   -> valida JWT con Firebase Admin SDK
   -> si es válido, permite el acceso
   -> conecta con H2 para gestión de pedidos
```

### Qué hace cada capa
- Frontend: login, registro, navegación y consumo de la API
- Backend: validación de sesión, cálculo de lógica y persistencia
- Firebase: identidad del usuario, tokens, Google Auth y gestión de sesión
- H2: almacenamiento local para la demo

## Funcionalidades actuales
- Login con email y contraseña
- Registro de usuarios con Firebase
- Login con Google
- Flujo de verificación por SMS preparado en frontend y Firebase, sujeto a habilitación del proveedor Phone en la nube
- CRUD de pedidos
- Protección de rutas y API con JWT
- Interfaz visual mejorada para presentación

## Importante sobre SMS / MFA
La versión actual del proyecto ya tiene implementado el flujo para autenticación por SMS en la interfaz y en la lógica del frontend, pero el envío real de mensajes depende de la activación del proveedor de teléfono en Firebase Console y de la configuración regional de la cuenta. Es decir:
- la app está lista desde el código para ese flujo
- la entrega real de SMS depende de la configuración en Firebase

Esto es importante para la defensa: no se debe decir que “el SMS funciona” si la nube aún no lo tiene habilitado. Se debe decir que la funcionalidad queda diseñada e integrada, y que la activación real es un paso de configuración en Firebase.

## Requisitos previos
- Java 17
- Maven
- Node.js 18+
- npm
- una cuenta de Firebase

## Cómo levantar el proyecto en otra computadora
### 1) Clonar el repositorio
```bash
git clone <URL_DEL_REPO>
cd pedidos360
```

### 2) Configurar Firebase
1. Crear proyecto en Firebase Console.
2. Activar Authentication.
3. Habilitar Email/Password y Google.
4. Dentro de la configuración del proyecto, generar un archivo JSON de cuenta de servicio.
5. Guardarlo como:
```text
pedidos360/backend/src/main/resources/firebase-service-account.json
```
6. En el frontend, abrir:
```text
pedidos360/frontend/src/environments/environment.ts
```
y completar la configuración de Firebase Web con `apiKey`, `authDomain`, `projectId`, etc.

### 3) Instalar dependencias del frontend
```bash
cd pedidos360/frontend
npm install
```

### 4) Levantar backend
```bash
cd ../backend
mvn spring-boot:run
```
El backend queda en:
- http://localhost:8080

Si la consola muestra:
- `Firebase Admin SDK inicializado correctamente.`
- `Tomcat started on port 8080`
entonces quedó bien.

### 5) Levantar frontend
```bash
cd ../frontend
npm start -- --host 0.0.0.0 --port 4200
```
El frontend queda en:
- http://localhost:4200

### 6) Ejecutar con PowerShell (opción rápida)
Si el proyecto se abre desde Windows y te da problemas con puertos ocupados, usa esto desde la raíz del proyecto:
```powershell
cd "C:\ruta\a\pedidos360\pedidos360"
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\start.ps1
```
Ese script:
- libera los puertos 8080 y 4200
- levanta backend
- levanta frontend

## Cómo presentar el proyecto en orden
### Orden recomendado para la exposición
1. Presentar el problema y la solución
   - se necesitaba un sistema de gestión de pedidos con autenticación segura
   - la app resuelve la gestión con autenticación basada en Firebase y validación de JWT

2. Mostrar la arquitectura
   - frontend Angular
   - backend Spring Boot
   - Firebase como Identity Provider
   - H2 como base de datos local para la demo

3. Mostrar login
   - email/password
   - Google
   - SMS como capa de autenticación adicional

4. Mostrar gestión de pedidos
   - listar pedidos
   - crear pedido
   - eliminar pedido

5. Explicar seguridad
   - el backend valida el token JWT
   - sin token, la API responde 401
   - esto protege los endpoints de pedidos

6. Mencionar los puntos clave técnicos
   - REST
   - JWT
   - Firebase Authentication
   - Firebase Admin SDK
   - H2 para demo local

## Guion breve para la defensa
> “Nuestra aplicación Pedidos360 permite gestionar pedidos de forma segura. La interfaz está desarrollada en Angular y el backend en Spring Boot. La autenticación se centraliza en Firebase Authentication, que entrega un JWT al frontend, y el backend valida ese token antes de autorizar el acceso a la API. Además, implementamos un flujo de seguridad con Google y una capa adicional de verificación por SMS, que queda preparada en la aplicación y depende de la configuración del servicio en Firebase para su activación real. Esto permite un diseño moderno, escalable y seguro para una demo funcional en entorno local.”

## Checklist final antes de entregar
- [ ] Backend levantado en http://localhost:8080
- [ ] Frontend levantado en http://localhost:4200
- [ ] Login con email/password funcionando
- [ ] Google Sign-In habilitado en Firebase
- [ ] API protegida ante requests sin token
- [ ] Firebase service account JSON presente en backend/src/main/resources
- [ ] README actualizado y claro para otra persona ejecutar el proyecto
- [ ] Presentación preparada con arquitectura, flujo de login y ejemplo de funcionamiento

## Recomendación final
Si la evaluación es práctica y funcional, el proyecto ya está en una buena posición: tiene arquitectura clara, autenticación real con Firebase, JWT y CRUD funcional. Si la evaluación exige validación en vivo del SMS real, entonces hay que activar la configuración del servicio Phone en Firebase; pero para defensa y demostración, la base funcional del sistema ya está bien construida y explicada.
