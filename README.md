# LogiTrack Agro — Sistema de Gestión Logística
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

## Descripción del proyecto
**LogiTrack Agro** es un ERP logístico integral diseñado específicamente para el sector agrícola en Argentina. La plataforma permite gestionar el ciclo de vida completo del transporte de granos , desde el alta del envío en origen (campos/acopios) hasta la entrega en destino (puertos/fábricas).

El sistema se enfoca en la **trazabilidad total**, integrando validaciones de seguridad, roles de acceso y un motor de ruteo y seguimiento.

---

## Entornos cloud
El proyecto se encuentra desplegado y funciona de manera continua
* **Aplicación web (frontend):** **[Vercel](https://logitrackagro.vercel.app)**
* **API REST (backend):** **[Render](https://logitrack-omv3.onrender.com)**
* **Documentación de API:** **[Swagger UI](https://logitrack-omv3.onrender.com/swagger-ui/index.html)**

## Stack tecnológico

### Backend
* **Lenguaje:** Java 21
* **Framework:** Spring Boot 
* **Persistencia:** Spring Data JPA / Hibernate
* **Seguridad:** Spring Security + JWT (JSON Web Tokens) + BCrypt
* **Gestión de Dependencias:** Maven
* **Calidad de Código:** JaCoCo 
* **Base de Datos:** PostgreSQL Serverless alojada en **Neon.tech**

### Frontend
* **Tecnologías:** React
* **Herramienta de servidor:** Node.js

---

## Instalación y configuración para usar localmente

### 1. Requisitos Previos
* Java JDK 21 o superior instalado.
* Apache Maven instalado.
* IDE recomendado: IntelliJ IDEA o Visual Studio Code (con *Extension Pack for Java*).

### 2. Configuración de Variables de Entorno
El proyecto requiere credenciales para conectarse a la base de datos en la nube y para firmar los tokens de seguridad.

1. Creá un archivo llamado `application.properties` en `src/main/resources/`.
2. Configurá las siguientes variables(solicitar las contraseñas a algun miembro del equipo):
```properties
# Conexión a Neon.tech
spring.datasource.url=jdbc:postgresql://ep-mi-base-de-datos.neon.tech/logitrack_db?sslmode=require
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD

# Seguridad JWT
jwt.secret=TU_CLAVE_SECRETA_LARGA
jwt.expiration=86400000
```
### 3. Ejecutar el proyecto
Una vez configuradas las credenciales, abrí tu terminal en la raíz del proyecto y ejecutá:

```
mvn spring-boot:run
```
### 4. Visualizar la documentación API
Una vez que el servidor esté corriendo, podés ver y probar todos los endpoints documentados ingresando a:
**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### 5. Ejecución de pruebas unitarias
para correr la suite de pruebas y generar el reporte de cobertura
```
mvn clean test jacoco:report 
```
Una vez que el servidor esté corriendo, podés ver y probar todos los endpoints documentados ingresando a:
**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### 3. Estructura de repositorios

Por decisiones de arquitectura y despliegue, el frontend y el backend operan en repositorios distintos:

* **Backend:** `Logitrack-LHdM/Logitrack`
* **Frontend:** `Logitrack-LHdM/Logitrack-frontend`

---


## Arquitectura 
El proyecto sigue una arquitectura de **capas (N-Tier)** para asegurar la escalabilidad y el mantenimiento:

* **Model:** Entidades JPA que mapean las tablas de PostgreSQL (Camiones, Choferes, Envíos, etc.).
* **Repository:** Interfaces que extienden de `JpaRepository` para el acceso a datos.
* **DTO (Data Transfer Objects):** Objetos para recibir y enviar datos de forma segura (ej. `EnvioRequestDTO`, `MetadatosDTO`).
* **Service:** Capa de lógica de negocio y transacciones `@Transactional`.
* **Controller:** Endpoints REST que exponen los servicios al Frontend.
* **Security:** Filtros JWT para autenticación stateless.

---

## Funcionalidades Principales
* **Autenticación:** Login seguro con roles (Supervisor, Operador, Chofer).
* **Ciclo de vida de envíos:** Creación de viajes, generación de tracking ID y registro automático de historial.
* **Seguimiento en tiempo real:** Seguimiento de un envio en el mapa en tiempo real.
* **Trazabilidad total:** Todos los cambios que sufre un envio quedan registrados, con fecha, hora, y usuario.
* **Panel gerencial:** Dashboards de métricas logísticas y exportación de reportes (CSV/Excel).
* **Alertas:** Sistema de notificaciones ante incidencias.
