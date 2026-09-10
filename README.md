# RumboU [PENDIENTE: ajustar título descriptivo del proyecto]

**Curso:** CS 2031 Desarrollo Basado en Plataformas — UTEC
**Integrantes:** [PENDIENTE: nombres completos de los 4 integrantes]

> Este README cumple doble función, como exige la rúbrica de la Semana 7: es la documentación técnica para levantar el proyecto en local, y también el informe narrativo de la entrega (portada, introducción, modelo de entidades, seguridad, etc.). Las secciones marcadas **[PENDIENTE]** se completan a medida que avanza el desarrollo — no bloquean el código, pero deben estar listas antes del 25 de septiembre.

---

## Índice

1. [Instalación y ejecución local](#instalación-y-ejecución-local)
2. [Introducción](#introducción)
3. [Identificación del problema o necesidad](#identificación-del-problema-o-necesidad)
4. [Descripción de la solución](#descripción-de-la-solución)
5. [Modelo de entidades](#modelo-de-entidades)
6. [Manejo de errores](#manejo-de-errores)
7. [Medidas de seguridad implementadas](#medidas-de-seguridad-implementadas)
8. [Eventos y asincronía](#eventos-y-asincronía)
9. [GitHub y gestión del proyecto](#github-y-gestión-del-proyecto)
10. [Conclusión](#conclusión)
11. [Apéndices](#apéndices)

---

## Instalación y ejecución local

### Requisitos previos

- Java 21
- Docker Desktop corriendo
- Maven (se puede usar el wrapper `mvnw` incluido, no hace falta instalarlo aparte)

### Base de datos

Este proyecto usa PostgreSQL corriendo en Docker. Para levantarlo:

```
docker compose up -d
```

Esto crea un contenedor de PostgreSQL escuchando en el puerto **5433** de tu máquina (no el 5432 por defecto, ver nota abajo). Las credenciales están en `docker-compose.yml` y deben coincidir con `src/main/resources/application.properties`.

Para verificar que quedó corriendo:

```
docker ps
```

### ⚠️ Nota sobre el puerto 5432

Si en tu computadora ya tienes PostgreSQL instalado directamente en Windows (no en Docker) — por ejemplo de otro curso o proyecto — es muy probable que ya esté usando el puerto 5432. Cuando eso pasa, Docker deja el contenedor "corriendo" sin avisar del conflicto, pero tu aplicación termina conectándose al PostgreSQL nativo en vez del de Docker, y falla con un error de autenticación (`password authentication failed`) aunque las credenciales en `application.properties` sean correctas.

Por eso este proyecto usa el puerto **5433** en el host (`"5433:5432"` en `docker-compose.yml`) en vez del 5432 estándar. Si igual te aparece ese error, revisa si tienes un servicio de PostgreSQL nativo corriendo (`services.msc` en Windows, busca algo como `postgresql-x64-...`) y confirma que `application.properties` apunte al puerto 5433, no al 5432.

### ⚠️ Nota sobre TestContainers en Windows

Los tests que heredan de `AbstractContainerBaseTest` (los que usan `@Testcontainers`) pueden fallar en Windows con versiones muy nuevas de Docker Desktop, con un error como `BadRequestException (Status 400: ...)` al intentar conectarse por el "named pipe". Es una incompatibilidad conocida entre la librería `docker-java` (que usa TestContainers por dentro) y el pipe interno de Docker Desktop — no es un error en el código del proyecto.

Si te pasa esto localmente: no es bloqueante. El pipeline de **GitHub Actions corre en Linux**, donde Docker funciona de forma nativa sin este problema, así que el CI es la fuente de verdad para estos tests (ver sección "Condiciones de escape" del documento de decisiones del equipo). Mientras tanto, puedes seguir desarrollando y dejar que el CI confirme que los tests con base de datos real pasan.

### Variables de entorno

[PENDIENTE: documentar aquí las variables de entorno reales una vez que existan — JWT secret, credenciales de Mercado Pago, API key de Gemini, credenciales del servicio de correo — ninguna debe ir commiteada.]

### Ejecutar la aplicación

Desde el IDE: correr la clase `BackendApplication`. Desde terminal: `./mvnw spring-boot:run`.

### Link a producción

[PENDIENTE: URL del deployment una vez publicado]

---

## Introducción

### Contexto

[PENDIENTE]

### Objetivos del proyecto

[PENDIENTE]

---

## Identificación del problema o necesidad

### Descripción del problema

[PENDIENTE]

### Justificación

[PENDIENTE]

---

## Descripción de la solución

### Funcionalidades implementadas

[PENDIENTE: se completa a medida que cada módulo quede terminado]

### Tecnologías utilizadas

- Java 21, Spring Boot 3.3.5, Maven
- Spring Data JPA + Hibernate, PostgreSQL (Docker en local)
- Spring Security con JWT (incluye refresh tokens)
- JUnit 5, TestContainers, MockMvc
- GitHub Actions (CI)
- Mercado Pago (pagos), Google AI Studio / Gemini (generación de preguntas), [PENDIENTE: proveedor de correo]
- Postman (documentación de API)

---

## Modelo de entidades

[PENDIENTE: diagrama entidad-relación]

[PENDIENTE: descripción de las entidades principales — ver la lista completa en `CLAUDE.md`]

---

## Manejo de errores

[PENDIENTE: explicar el `GlobalExceptionHandler` y las excepciones personalizadas una vez ampliadas]

---

## Medidas de seguridad implementadas

### Seguridad de datos

[PENDIENTE: JWT + refresh tokens, roles, encriptación de contraseñas con BCrypt]

### Prevención de vulnerabilidades

[PENDIENTE: SQL injection (JPA/prepared statements), XSS, CSRF]

---

## Eventos y asincronía

[PENDIENTE: `SimulacroFinalizadoEvent`, `RespuestaIncorrectaEvent`, `PagoAprobadoEvent` — ver `CLAUDE.md`]

---

## GitHub y gestión del proyecto

[PENDIENTE: cómo se usó GitHub Projects/Issues, y el flujo de GitHub Actions]

---

## Conclusión

### Logros del proyecto

[PENDIENTE]

### Aprendizajes clave

[PENDIENTE]

### Trabajo futuro

[PENDIENTE]

---

## Apéndices

### Licencia

[PENDIENTE]

### Referencias

[PENDIENTE]
