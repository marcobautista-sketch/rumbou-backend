# Estado del proyecto RumboU

> **Para qué sirve este archivo:** darle contexto completo a Claude Code (o a cualquiera que se incorpore) sobre qué existe ya en el repositorio y qué falta, sin tener que explorar todo el código.
> **Actualizado:** 15 de septiembre de 2026, junto con el PR del seed del catálogo (rama `feat/juancarlos-seed-progreso`) · **Entrega:** viernes 25 de septiembre de 2026, 11:59 p.m.
> **Léelo junto con `CLAUDE.md`** (raíz del repo), que tiene las reglas de arquitectura e interacción no negociables.

---

## 1. Resumen en una línea

API REST en Spring Boot para preparación de exámenes de admisión (UNI y UNMSM). Están en `main` el núcleo, la autenticación, el motor de examen, el banco de preguntas con tutor de IA, la gamificación y la base de suscripciones. El catálogo ya tiene **seed** de universidades, áreas, esquemas de calificación, temas y estructura del examen. **Falta:** carreras y ofertas académicas, el paquete `progreso/`, el webhook de Mercado Pago y el correo (rama de Zoe pendiente de mergear), conectar los límites de plan a los simulacros, y el deployment.

---

## 2. Stack y entorno

| Cosa | Detalle |
|---|---|
| Lenguaje / framework | Java 21, Spring Boot 3.3.5, Maven |
| Base de datos | PostgreSQL en Docker (`docker-compose.yml`), **puerto 5433** en el host (no 5432, por conflicto con instalaciones nativas en Windows) |
| ORM | Hibernate 6.5.3 con `ddl-auto=update`: crea y actualiza las tablas solo |
| Seguridad | Spring Security + JWT (jjwt 0.12.6) |
| Tests | JUnit 5, Mockito, MockMvc, TestContainers |
| CI | GitHub Actions (`.github/workflows/ci.yml`), corre `mvnw verify` en push y PR contra `main` |

### Problemas conocidos del entorno (no son bugs del proyecto)

- **TestContainers falla en Windows**, incluso con Docker Desktop encendido: responde `BadRequestException (Status 400)` (verificado con Docker Engine 29.5.2; es una incompatibilidad de la librería `docker-java` que trae TestContainers 1.21.4). Los tests con base de datos real (`UsuarioRepositoryTest`, `PreguntaRepositoryTest`, `ApplicationContextSmokeTest`, `AcademicoSeedRunnerTest`) no corren en local en Windows, **pero sí pasan en el CI de Linux**, que es la fuente de verdad. Para correr el resto en local:
  ```
  ./mvnw test "-Dtest=!UsuarioRepositoryTest,!PreguntaRepositoryTest,!ApplicationContextSmokeTest,!AcademicoSeedRunnerTest"
  ```
- **Mockito sobre JDK muy nuevo (26)**: si aparece "Mockito cannot mock this class", agregar `-DargLine=-Dnet.bytebuddy.experimental=true` al comando de Maven.
- Si `mvnw` dice que `JAVA_HOME` no está definido, hay que apuntarlo al JDK instalado antes de correr.

---

## 3. Reglas de arquitectura (resumen — el detalle está en `CLAUDE.md`)

1. **Ninguna constante del examen vive en el código.** Acierto, penalidad y puntaje máximo son datos en `EsquemaCalificacion`, y en el seed viven en archivos (`src/main/resources/seed/`), no en Java.
2. **Ningún `if` por universidad, área o nombre de prueba dentro de los services.** Toda la variación es dato.
3. **Los módulos se comunican por eventos**, no por llamadas directas entre services de distintos módulos. La excepción prevista es `PlanService`, que centraliza el control de planes para todo el proyecto.
4. Toda operación que toque varias tablas va con `@Transactional`.
5. Los listeners que dependen de datos recién escritos van con `@TransactionalEventListener(phase = AFTER_COMMIT)`; si además llaman a un servicio externo, agregan `@Async`. Si escriben en la base, llevan `@Transactional(propagation = REQUIRES_NEW)` (ver error #2).
6. **Cada endpoint recibe y devuelve DTOs, nunca entidades.** Validación con Bean Validation.
7. Los errores se manejan en el `@RestControllerAdvice` global, nunca con `try/catch` devolviendo strings.
8. Las API keys van en variables de entorno, nunca en el repositorio.
9. **Nadie edita el paquete de otro sin avisar.** `config/` y `shared/` son territorio común.

---

## 4. Qué existe hoy en `main`

### 4.1 `auth/` — autenticación · dueño: Marco · **COMPLETO**

- `Usuario` (implementa `UserDetails`): email, passwordHash, nombre, `role`, currentStreak, longestStreak, lastActivityDate, xpTotal, xpSemanal, `@Version`.
- `Role`: enum `USER` / `ADMIN`. El rol viaja **dentro del JWT** además de estar en la base.
- `JwtService`: access token (15 min) y refresh token (7 días).
- `PasswordResetToken`: token de un solo uso, 30 min de vigencia. Al pedir un reseteo nuevo se invalidan los anteriores.
- `JwtAuthenticationFilter`, `UserDetailsServiceImpl`, `UsuarioRepository`, `PasswordResetTokenRepository`.

**Endpoints** (todos públicos, bajo `/api/v1/auth`):

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/register` | Crea usuario, devuelve access + refresh token y publica `UsuarioRegistradoEvent` |
| POST | `/login` | Autentica, devuelve access + refresh token |
| POST | `/refresh` | Renueva tokens a partir del refresh token |
| POST | `/forgot-password` | Genera token de reseteo y publica evento. Responde 200 exista o no el email (para no revelar qué correos están registrados) |
| POST | `/reset-password` | Cambia la contraseña validando el token |

**Eventos que publica:** `UsuarioRegistradoEvent` y `PasswordResetRequestedEvent(usuarioId, email, nombre, token)`. **Ninguno tiene listener en `main` todavía**: los escucha el módulo de correo, que está en la rama pendiente de Zoe (ver 4.6).

### 4.2 `examen/` — motor de examen · dueño: Marco · **COMPLETO**

- `Simulacro`, `RespuestaUsuario` (relación M:N con atributos; `puntajeAportado` puede ser negativo).
- `SimulacroGeneratorService.generar(usuario, area, tipo)`: lee `EstructuraExamen` del área y elige preguntas aprobadas al azar por tema. Si un tema tiene menos preguntas que las pedidas, toma las que haya (`Math.min`). **Falla con error claro si no consigue ninguna pregunta.**
- `CalificadorService`: `calcularPuntajeBloque`, `calcularPsp`, `calificarRespuesta`, `calificarSimulacro`. Dirigido por datos, sin ningún `if` por universidad.
- `SimulacroService`: `iniciar()`, `responder()`, `finalizar()`.

**Endpoints** (requieren JWT, bajo `/api/v1/simulacros`):

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `` | Inicia un simulacro para un área |
| POST | `/{id}/respuestas` | Marca la alternativa de una pregunta (`null` = en blanco) |
| POST | `/{id}/finalizar` | Califica, cierra el simulacro y publica eventos |

**Eventos que publica:**
- `SimulacroFinalizadoEvent(simulacroId, usuarioId, areaId, puntajeObtenido, psp)` — ya lo escucha **gamificación**; falta el listener de **progreso**.
- `RespuestaIncorrectaEvent(respuestaUsuarioId, preguntaId, usuarioId)` — lo escucha el tutor de IA de `contenido/`.

**Validaciones de negocio que ya hace:** un simulacro solo lo puede responder/finalizar su dueño; no se puede responder ni finalizar uno ya cerrado; la alternativa marcada debe existir en esa pregunta; el área no puede tener el mismo tema en dos bloques de calificación.

> ⚠️ **Todavía no aplica límites de plan:** iniciar un simulacro no llama a `PlanService`. Ver pendientes.

### 4.3 `contenido/` — banco de preguntas y tutor de IA · dueña: Fabiana · **EN MAIN**

- `Pregunta`: tema, enunciado, alternativas (`@ElementCollection`), `claveCorrecta` (índice 0-4), explicación, dificultad, origen, `aprobada`.
- `PreguntaService`, `PreguntaRepository` (filtros por tema/dificultad/origen/aprobada + paginado).
- DTOs: `CreatePreguntaRequest`, `UpdatePreguntaRequest`, `PreguntaResponse` (sin la clave, para el postulante), `PreguntaAdminResponse` (con clave y explicación), `TutorIaResponse`.
- `gemini/`: `GeminiClient`, `GeminiPreguntaValidator`, `GeneradorPreguntasRunner` (`@Profile("generar-preguntas")`), `PreguntaGeneradaDto`, `GeminiException` (hereda de `ExternalServiceException`).
- **Tutor de IA PRO** (`PreguntaService.pedirExplicacionTutorIa`): explicación bajo demanda con Gemini. Pasa por `PlanService.puedeAcceder(usuario, TUTOR_IA)` y registra el uso (exclusivo PRO, tope de 30 consultas al día).
- `TutorIaExplicacionListener`: escucha `RespuestaIncorrectaEvent` con `@Async` + `AFTER_COMMIT` + `@Transactional(REQUIRES_NEW)` y cachea la explicación estática en la pregunta. No es el tutor PRO.

**Endpoints** (bajo `/api/v1/preguntas`): `GET` (listar con filtros y paginado), `GET /{id}`, `POST` (ADMIN), `PUT /{id}` (ADMIN), `PATCH /{id}/aprobar` (ADMIN), `DELETE /{id}` (ADMIN), `POST /{id}/tutor-ia` (según plan, no según rol).

**Cómo generar preguntas** (requiere `GEMINI_API_KEY` como variable de entorno y que el tema exista en la base; tras correr el seed, los ids salen de `SELECT id, nombre FROM temas;`):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas \
  -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
```
Guarda todo con `aprobada=false`, pendiente de revisión humana. **El generador todavía no usa `Tema.temario`.**

### 4.4 `academico/` — catálogo · dueño: Juan Carlos · **SEED DE ESTRUCTURA LISTO, FALTAN CARRERAS Y OFERTAS**

**Entidades:** `Universidad`, `Area`, `Carrera`, `OfertaAcademica`, `EsquemaCalificacion`, `Tema`, `EstructuraExamen`, enum `AreaConocimiento` (`MATEMATICA`, `FISICA_QUIMICA`, `BIOLOGIA`, `APTITUD`, `HUMANIDADES`, `ACTITUDINAL`).

- Los 7 repositorios existen, con las búsquedas que usa el seed para no duplicar filas (`findBySiglas`, `findByNombre`, `findByUniversidadIdAndCodigo`, `findByUniversidadIdAndNombreBloque`, `findByAreaIdAndTemaId`, etc.).
- `EstructuraExamen` tiene restricción única `(area_id, tema_id)`: la base impide repetir un tema en dos bloques de la misma área.
- `Tema.temario` (TEXT, opcional): lista oficial de subtemas del prospecto, pensada para el generador de preguntas.

**Seed del catálogo** — `AcademicoSeedRunner` (`CommandLineRunner` + `@Profile("seed")`):

```
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

- **Los datos viven en archivos**, no en Java: `src/main/resources/seed/` con `universidades.csv`, `areas.csv`, `esquemas.csv`, `temas.csv` y `estructura-examen.csv`. Formato: UTF-8, separador `|`, las líneas con `#` se ignoran. La lectura está en `ArchivoSeed`.
- **Es idempotente:** cada fila se busca por su clave natural y se crea o se actualiza. Correrlo dos veces no duplica nada, y para corregir un dato basta con editar el archivo y volver a correrlo.
- **Va en una sola transacción:** si un archivo trae un error, no queda nada a medias, y el mensaje indica el archivo y la línea.
- ⚠️ **Si tu tabla `temas` se creó antes de agregar `BIOLOGIA`**, el seed falla hasta que actualices su regla CHECK (ver error #6).

**Qué carga** (fuentes: Prospecto de Admisión UNMSM 2026-I y prospecto/temario oficial de UNI):

| Universidad | Puntaje máx. | Preguntas | Áreas | Bloques (acierto / penalidad / máximo) |
|---|---|---|---|---|
| UNI | 1800 | 180 | `GENERAL` | Aptitud Académica y Humanidades 6 / 1.20 / 600 · Matemática 15 / 3 / 600 · Física y Química 15 / 3 / 600 |
| UNMSM | 2000 | 100 | `B` Ciencias Básicas, `C` Ingenierías | Actitudinal 20 / 0 / 200 · Habilidades 20 / 1.125 / 400 · Conocimientos 20 / 1.125 / 1400 |

- **22 temas**, 21 con temario (`Actitud` no lo trae la fuente). Aritmética, Álgebra, Geometría, Trigonometría, Física y Química son **temas compartidos** entre UNI y UNMSM.
- **47 filas de estructura:** 9 en UNI `GENERAL` (180 preguntas) y 19 en cada área de UNMSM (100 preguntas cada una, con la distribución oficial de Conocimientos por área).
- **Decisiones de datos:**
  - La sección Actitudinal se siembra según el documento de decisiones. Mientras el banco no tenga preguntas actitudinales, aporta 0 preguntas al simulacro.
  - Los temas de Habilidades tienen nombre propio (`Habilidad Verbal`, `Habilidad Lógico-Matemática`) para no chocar con los de Conocimientos.
  - Humanidades de UNI es un solo tema, porque la fuente no separa sus 36 preguntas por subárea.

**Todavía no hay carreras ni ofertas académicas en la base** (van en el siguiente PR). Sin ofertas no se puede calcular el IP de `progreso/`.

### 4.5 `gamificacion/` — dueña: Zoe · **BASE EN MAIN**

- `GamificacionListener`: escucha `SimulacroFinalizadoEvent` con `@EventListener` síncrono y delega en `GamificacionService`.
- **Racha:** si la última actividad fue hoy no cambia; si fue ayer, suma 1; si fue antes, se reinicia a 1. Actualiza también la racha más larga.
- **XP:** +100 en `xpTotal` y `xpSemanal` por cada simulacro finalizado.
- **Logros:** `Logro` (catálogo, con `condicion` y `valorRequerido`) y `UsuarioLogro`. Condiciones: `PRIMER_SIMULACRO`, `RACHA_DIAS`, `XP_TOTAL`. No desbloquea el mismo logro dos veces.
- Sin endpoints todavía. **El catálogo de logros no tiene seed en el repo** (hoy se siembra a mano en la base local).

### 4.6 `suscripcion/` — dueña: Zoe · **BASE EN MAIN, WEBHOOK Y CORREO EN RAMA PENDIENTE**

En `main`:
- `Suscripcion` (plan, estado, fechas, id de preaprobación de Mercado Pago), `UsoDiario` (contadores con `@Version`), enums `Plan`, `EstadoSuscripcion` y `Funcionalidad` (`SIMULACRO_TEMA`, `SIMULACRO_COMPLETO`, `TUTOR_IA`).
- `PlanService`, la única puerta del control de planes:
  - `puedeAcceder(usuario, funcionalidad)`: en plan gratuito permite 3 simulacros de tema en los últimos 7 días y 1 simulacro completo en el último mes; el tutor de IA es solo PRO, con 30 consultas al día. Si no alcanza, lanza `UnauthorizedException` (403).
  - `esPro(usuarioId)` y `registrarUso(usuario, funcionalidad)`.
- `SuscripcionActivacionListener`: escucha `PagoAprobadoEvent` con `AFTER_COMMIT` + `REQUIRES_NEW`. `SuscripcionService.activar` deja la suscripción `ACTIVA` por un mes y no la toca si ya estaba activa (pago duplicado).
- Sin endpoints en `main`.

**Rama `feat/suscripcion-webhook-email` (2 commits, sin mergear):** `SuscripcionController`, `WebhookController` + `WebhookService` con idempotencia (`PagoWebhook`), `MercadoPagoService`, y el módulo `correo/` con `EmailService` (plantillas Thymeleaf) y listeners de registro, recuperación de contraseña y pago aprobado. Modifica `pom.xml`, `SecurityConfig` y `application.properties` (propiedades de correo con valores por defecto, así que la app sigue arrancando sin configurar nada).

### 4.7 `shared/` y `config/` — territorio común

- `BaseEntity` (id autogenerado, la heredan todas las entidades).
- **8 excepciones propias** (la rúbrica pide más de 7): `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidCredentialsException`, `InvalidTokenException`, `InvalidOperationException`, `UnauthorizedException`, `ExternalServiceException` (en `shared/`) y `GeminiException` (en `contenido/gemini/`, hereda de `ExternalServiceException`).
- `GlobalExceptionHandler` maneja: `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `InvalidCredentialsException`/`BadCredentialsException` (401), `InvalidTokenException` (401), `InvalidOperationException` (400), `UnauthorizedException` (403), `AccessDeniedException` (403), `MethodArgumentNotValidException` (400), `HttpMessageNotReadableException` (400), `ExternalServiceException` (502, sin exponer el mensaje del proveedor), `NoResourceFoundException` (404), y un handler genérico (500).
- `ErrorResponse` (record): timestamp, status, error, message, path.
- `SecurityConfig`: CSRF desactivado (API stateless con JWT en header), CORS abierto en dev, `/api/v1/auth/**` público y todo lo demás autenticado, `@EnableMethodSecurity` para `@PreAuthorize`.
- `JwtAuthenticationEntryPoint`: responde 401 (no 403) cuando falta el token.
- `AsyncConfig`: `@EnableAsync` + `ThreadPoolTaskExecutor`, listo para usar con `@Async`.
- `application.properties`: datasource local (la contraseña **todavía está fija**), `JWT_SECRET` y `GEMINI_API_KEY` por variable de entorno.

### 4.8 Tests

| Archivo | Tipo | Casos |
|---|---|---|
| `ApplicationContextSmokeTest` | `@SpringBootTest` | 1 — levanta la aplicación completa |
| `AuthServiceTest` | JUnit + Mockito | 9 |
| `AuthControllerTest` | `@WebMvcTest` | 7 |
| `UsuarioRepositoryTest` | `@DataJpaTest` + TestContainers | 1 |
| `SimulacroServiceTest` | JUnit + Mockito | 7 |
| `CalificadorServiceTest` | JUnit parametrizado | 6 |
| `PreguntaServiceTest` | JUnit + Mockito | 11 |
| `PreguntaControllerTest` | `@WebMvcTest` | 10 |
| `PreguntaRepositoryTest` | `@DataJpaTest` + TestContainers | 5 |
| `GamificacionServiceTest` | Unitario | 4 |
| `PlanServiceTest` | Unitario | 14 |
| `SuscripcionServiceTest` | Unitario | 3 |
| `ArchivosSeedConsistenciaTest` | JUnit puro | 7 — los archivos del seed cuadran con los totales oficiales |
| `AcademicoSeedRunnerTest` | `@DataJpaTest` + TestContainers | 3 — carga, idempotencia y sincronización con los archivos |

**88 tests en total** (78 corren en local sin TestContainers). `AbstractContainerBaseTest` (en `src/test/.../shared/`) es la clase base para tests `@DataJpaTest` con PostgreSQL real.

### 4.9 Entregables transversales ya existentes

- `postman_collection.json` en la raíz, con autenticación (5), simulacros (3) y preguntas (6). **Todavía no incluye** `POST /api/v1/preguntas/{id}/tutor-ia`.
- Informe narrativo completo dentro del `README.md`. Sus estados de avance hay que actualizarlos a medida que se terminan módulos (hoy todavía lista el seed como pendiente).

---

## 5. Errores que ya cometimos (no repetirlos)

1. **`@Lob` sobre un `String` con PostgreSQL** mapea al tipo `oid` (para binarios), no a texto, y falla al insertar. Usar `@Column(columnDefinition = "TEXT")`.
2. **`@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional` normal** hace que **la aplicación no arranque**. Hay que usar `@Transactional(propagation = Propagation.REQUIRES_NEW)`, porque en esa fase la transacción original ya se cerró.
3. **El CI puede estar en verde con una aplicación que no enciende**, porque los tests de rebanada (`@WebMvcTest`, `@DataJpaTest`) y los de Mockito no cargan el contexto completo. Por eso existe `ApplicationContextSmokeTest`. **Antes de abrir un PR, arrancar la app al menos una vez** con `./mvnw spring-boot:run` y verificar que diga `Started BackendApplication`.
4. **Un mismo `Tema` no puede estar en dos bloques de calificación de la misma área.** El motor resuelve el esquema de puntaje a partir del tema. Si "Matemática" entra en Habilidades y en Conocimientos, deben ser dos temas distintos. Hoy lo impide también la restricción única de `EstructuraExamen`.
5. **`@WebMvcTest` sin cargar `SecurityConfig`** aplica una seguridad por defecto con CSRF que devuelve 403 en los POST. Se resuelve con `@AutoConfigureMockMvc(addFilters = false)` cuando lo que se prueba es la validación y no la seguridad.
6. **Agregar un valor a un enum persistido rompe las bases que ya existían.** Hibernate 6.5, al crear una tabla con un campo `@Enumerated(EnumType.STRING)`, le agrega una regla `CHECK` con los valores del enum en ese momento, y `ddl-auto=update` **no la actualiza** después. Pasó con `BIOLOGIA` en `AreaConocimiento`: la base rechazó la fila con `violates check constraint "temas_area_conocimiento_check"`. Afecta a todas las columnas de enums (`temas.area_conocimiento`, `preguntas.dificultad`, `preguntas.origen`, `simulacros.estado`, `simulacros.tipo`, `usuarios.role`, `logros.condicion`, `suscripciones.estado`, `suscripciones.plan`). El CI y un deploy con base nueva no se ven afectados. Arreglo en una base existente, sin perder datos (ejemplo con `temas`, desde `docker compose exec postgres psql -U rumbou -d rumbou`):
   ```sql
   ALTER TABLE temas DROP CONSTRAINT temas_area_conocimiento_check;
   ALTER TABLE temas ADD CONSTRAINT temas_area_conocimiento_check
     CHECK (area_conocimiento IN ('MATEMATICA', 'FISICA_QUIMICA', 'BIOLOGIA', 'APTITUD', 'HUMANIDADES', 'ACTITUDINAL'));
   ```
   **Si agregas un valor a cualquier enum, avisa al equipo con el SQL correspondiente.**

---

## 6. Qué falta, por persona

### Juan Carlos — catálogo real y progreso
- [x] Los 5 repositorios de `academico/`.
- [x] Campo `temario` en `Tema`.
- [x] Seed de universidades, áreas, esquemas de calificación, temas con temario y estructura del examen (PR `feat/juancarlos-seed-progreso`).
- [ ] Seed de carreras (51) y ofertas académicas del proceso 2026-II (70), con el puntaje del último ingresante de todas las carreras de UNI y de las áreas B y C de UNMSM. **Por decisión del equipo se usa solo el proceso 2026-II.** Va en un PR aparte.
- [ ] Paquete nuevo `progreso/`: `ObjetivoUsuario` e `HistorialProgreso`, IP (`PSP / puntajeUltimoIngresante`) con semáforo (≥1.10 holgado, 1.00-1.09 ajustado, 0.85-0.99 cerca, <0.85 reforzar), dominio por tema, listener de `SimulacroFinalizadoEvent` y endpoints del panel. Las funciones PRO se limitan con `PlanService.esPro(usuarioId)`.

### Fabiana — banco de preguntas
- [ ] Pasar el `temario` del tema dentro del prompt de Gemini. El campo ya existe y el seed lo llena en 21 de los 22 temas.
- [ ] Conseguir `GEMINI_API_KEY` y generar preguntas por tema (los temas existen después de correr el seed).
- [ ] Revisar y aprobar preguntas (`PATCH /api/v1/preguntas/{id}/aprobar`).

### Zoe — suscripción, gamificación y correo
- [x] Gamificación base: racha, XP y logros sobre `SimulacroFinalizadoEvent`.
- [x] `Suscripcion`, `UsoDiario` con `@Version`, `PlanService` y activación de la suscripción con `PagoAprobadoEvent`.
- [ ] Mergear `feat/suscripcion-webhook-email`: webhook de Mercado Pago idempotente, endpoint de suscripción y servicio de correo (registro, recuperación de contraseña y pago aprobado).
- [ ] Seed del catálogo de logros con un `GamificacionSeedRunner` (`@Profile("seed")`), siguiendo la convención del equipo (ver sección 7).
- [ ] Conectar `PlanService.puedeAcceder` / `registrarUso` al inicio de los simulacros. Toca `examen/`: coordinar con Marco.
- [ ] Job diario con `@Scheduled` que marque como `VENCIDA` las suscripciones vencidas (sección 9.5 del documento de decisiones).

### Transversal (de nadie en particular)
- [ ] Mantener `postman_collection.json` al día: tutor de IA, suscripción, webhook y progreso a medida que se terminen.
- [ ] Actualizar los estados de avance del informe del `README.md` al terminar cada módulo.
- [ ] GitHub Projects/Issues con milestones y labels (ya se usan issues para decisiones del equipo; falta confirmar milestones y labels).
- [ ] Deployment en AWS (ECS/EC2 + RDS). Antes: pasar la contraseña de base de datos a variable de entorno y hacer configurable el CORS.

---

## 7. Convenciones de trabajo

- Una rama por módulo: `feat/<modulo>`. **Nunca push directo a `main`.**
- PRs pequeños, con el CI en verde antes de mergear. Un PR abierto se actualiza solo con cada push a su rama: no hace falta cerrarlo y abrir otro.
- Commits en español, en imperativo: *"agrega repositorio de OfertaAcademica"*.
- Nunca commitear: `.env`, API keys, credenciales, la carpeta `target/`.
- No copiar preguntas textuales de exámenes oficiales: se usan solo como referencia de estilo y dificultad.
- **Datos iniciales (seed):** un `CommandLineRunner` con `@Profile("seed")` por paquete, dueño solo de sus propias tablas (`AcademicoSeedRunner` en `academico/`, `GamificacionSeedRunner` en `gamificacion/`), idempotente y **nunca con `data.sql`**. Se corren todos juntos con `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed`.

> Nota: la protección de rama de GitHub no se puede activar en este repositorio (es privado en una cuenta personal, y GitHub exige plan Team). La regla se cumple **por acuerdo del equipo**, no porque la plataforma lo impida.
