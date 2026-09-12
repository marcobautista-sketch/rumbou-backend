# Estado del proyecto RumboU

> **Para qué sirve este archivo:** darle contexto completo a Claude Code (o a cualquiera que se incorpore) sobre qué existe ya en el repositorio y qué falta, sin tener que explorar todo el código.
> **Actualizado:** 12 de septiembre de 2026 · **Entrega:** viernes 25 de septiembre de 2026, 11:59 p.m.
> **Léelo junto con `CLAUDE.md`** (raíz del repo), que tiene las reglas de arquitectura e interacción no negociables.

---

## 1. Resumen en una línea

API REST en Spring Boot para preparación de exámenes de admisión (UNI y UNMSM). El núcleo, la autenticación y el motor de examen están terminados y probados; **falta cargar datos reales del catálogo** (lo que bloquea todo lo demás) y construir el módulo de suscripción, gamificación y correo.

---

## 2. Stack y entorno

| Cosa | Detalle |
|---|---|
| Lenguaje / framework | Java 21, Spring Boot 3.3.5, Maven |
| Base de datos | PostgreSQL en Docker (`docker-compose.yml`), **puerto 5433** en el host (no 5432, por conflicto con instalaciones nativas en Windows) |
| Seguridad | Spring Security + JWT (jjwt 0.12.6) |
| Tests | JUnit 5, Mockito, MockMvc, TestContainers |
| CI | GitHub Actions (`.github/workflows/ci.yml`), corre `mvnw verify` en push y PR contra `main` |
| Esquema de BD | `ddl-auto=update` — Hibernate crea y actualiza las tablas solo |

### Problemas conocidos del entorno (no son bugs del proyecto)

- **TestContainers falla en Windows** con Docker Desktop reciente (error de "named pipe"). Los tests que necesitan base de datos real (`UsuarioRepositoryTest`, `PreguntaRepositoryTest`, `ApplicationContextSmokeTest`) no corren en local en Windows, **pero sí pasan en el CI de Linux**, que es la fuente de verdad. Para correr el resto en local:
  ```
  ./mvnw test "-Dtest=!UsuarioRepositoryTest,!PreguntaRepositoryTest,!ApplicationContextSmokeTest"
  ```
- **Mockito sobre JDK muy nuevo (26)**: si aparece "Mockito cannot mock this class", agregar `-DargLine=-Dnet.bytebuddy.experimental=true` al comando de Maven.
- Si `mvnw` dice que `JAVA_HOME` no está definido, hay que apuntarlo al JDK instalado antes de correr.

---

## 3. Reglas de arquitectura (resumen — el detalle está en `CLAUDE.md`)

1. **Ninguna constante del examen vive en el código.** Acierto, penalidad y puntaje máximo son datos en `EsquemaCalificacion`.
2. **Ningún `if` por universidad, área o nombre de prueba dentro de los services.** Toda la variación es dato. *(Verificado: hoy se cumple en todo el código.)*
3. **Los módulos se comunican por eventos**, no por llamadas directas entre services de distintos módulos.
4. Toda operación que toque varias tablas va con `@Transactional`.
5. Los listeners que dependen de datos recién escritos van con `@TransactionalEventListener(phase = AFTER_COMMIT)`; si además llaman a un servicio externo, agregan `@Async`.
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
| POST | `/register` | Crea usuario, devuelve access + refresh token |
| POST | `/login` | Autentica, devuelve access + refresh token |
| POST | `/refresh` | Renueva tokens a partir del refresh token |
| POST | `/forgot-password` | Genera token de reseteo y publica evento. Responde 200 exista o no el email (para no revelar qué correos están registrados) |
| POST | `/reset-password` | Cambia la contraseña validando el token |

**Evento que publica:** `PasswordResetRequestedEvent(usuarioId, email, nombre, token)` — **esperando que el módulo de correo lo escuche.**

### 4.2 `examen/` — motor de examen · dueño: Marco · **COMPLETO**

- `Simulacro`, `RespuestaUsuario` (relación M:N con atributos; `puntajeAportado` puede ser negativo).
- `SimulacroGeneratorService.generar(usuario, area, tipo)`: lee `EstructuraExamen` del área y elige preguntas aprobadas al azar por tema. **Falla con error claro si no hay preguntas aprobadas.**
- `CalificadorService`: `calcularPuntajeBloque`, `calcularPsp`, `calificarRespuesta`, `calificarSimulacro`. Dirigido por datos, sin ningún `if` por universidad.
- `SimulacroService`: `iniciar()`, `responder()`, `finalizar()`.

**Endpoints** (requieren JWT, bajo `/api/v1/simulacros`):

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `` | Inicia un simulacro para un área |
| POST | `/{id}/respuestas` | Marca la alternativa de una pregunta (`null` = en blanco) |
| POST | `/{id}/finalizar` | Califica, cierra el simulacro y publica eventos |

**Eventos que publica:**
- `SimulacroFinalizadoEvent(simulacroId, usuarioId, areaId, puntajeObtenido, psp)` — **sin listener todavía**; lo esperan progreso y gamificación.
- `RespuestaIncorrectaEvent(respuestaUsuarioId, preguntaId, usuarioId)` — ya lo escucha el tutor de IA de `contenido/`.

**Validaciones de negocio que ya hace:** un simulacro solo lo puede responder/finalizar su dueño; no se puede responder ni finalizar uno ya cerrado; la alternativa marcada debe existir en esa pregunta; el área no puede tener el mismo tema en dos bloques de calificación.

### 4.3 `contenido/` — banco de preguntas · dueña: Fabiana · **MERGEADO**

- `Pregunta`: tema, enunciado, alternativas (`@ElementCollection`), `claveCorrecta` (índice 0-4), explicación, dificultad, origen, `aprobada`.
- `PreguntaService`, `PreguntaRepository` (filtros por tema/dificultad/origen/aprobada + paginado).
- DTOs: `CreatePreguntaRequest`, `UpdatePreguntaRequest`, `PreguntaResponse` (sin la clave, para el postulante), `PreguntaAdminResponse` (con clave y explicación).
- `gemini/`: `GeminiClient`, `GeminiPreguntaValidator`, `GeneradorPreguntasRunner` (`@Profile("generar-preguntas")`), `PreguntaGeneradaDto`.
- `TutorIaExplicacionListener`: escucha `RespuestaIncorrectaEvent` con `@Async` + `AFTER_COMMIT` + `@Transactional(REQUIRES_NEW)`.

**Endpoints** (bajo `/api/v1/preguntas`): `GET` (listar con filtros y paginado), `GET /{id}`, `POST` (ADMIN), `PUT /{id}` (ADMIN), `PATCH /{id}/aprobar` (ADMIN), `DELETE /{id}` (ADMIN).

**Cómo generar preguntas** (requiere `GEMINI_API_KEY` como variable de entorno y que el tema exista en la base):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=generar-preguntas \
  -Dspring-boot.run.arguments="--temaId=1 --dificultad=MEDIA --cantidad=10"
```
Guarda todo con `aprobada=false`, pendiente de revisión humana.

### 4.4 `academico/` — catálogo · dueño: Juan Carlos · **ESTRUCTURA SÍ, DATOS NO**

Entidades completas: `Universidad`, `Area`, `Carrera`, `OfertaAcademica`, `EsquemaCalificacion`, `Tema`, `EstructuraExamen`, enum `AreaConocimiento` (`MATEMATICA`, `FISICA_QUIMICA`, `APTITUD`, `HUMANIDADES`, `ACTITUDINAL`).

**En `main` solo existen 2 repositorios:** `AreaRepository` y `EstructuraExamenRepository`.

> ⚠️ Los otros 5 repositorios (`Universidad`, `Carrera`, `OfertaAcademica`, `EsquemaCalificacion`, `Tema`) **ya están escritos en la rama `feat/juancarlos-catalogo-progreso` pero todavía no se mergearon a `main` ni tienen PR abierto.**

**No hay ningún dato cargado en la base.** No hay seed. Esto bloquea todo lo demás.

### 4.5 `shared/` y `config/` — territorio común

- `BaseEntity` (id autogenerado, la heredan todas las entidades).
- `GlobalExceptionHandler` maneja: `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `InvalidCredentialsException`/`BadCredentialsException` (401), `InvalidTokenException` (401), `InvalidOperationException` (400), `UnauthorizedException` (403), `AccessDeniedException` (403), `MethodArgumentNotValidException` (400), `HttpMessageNotReadableException` (400), `NoResourceFoundException` (404), y un handler genérico (500).
- `ErrorResponse` (record): timestamp, status, error, message, path.
- `SecurityConfig`: CSRF desactivado (API stateless con JWT en header), CORS abierto en dev, `/api/v1/auth/**` público y todo lo demás autenticado, `@EnableMethodSecurity` para `@PreAuthorize`.
- `JwtAuthenticationEntryPoint`: responde 401 (no 403) cuando falta el token.
- `AsyncConfig`: `@EnableAsync` + `ThreadPoolTaskExecutor`, listo para usar con `@Async`.

### 4.6 Tests

| Archivo | Tipo | Casos |
|---|---|---|
| `ApplicationContextSmokeTest` | `@SpringBootTest` | 1 — levanta la aplicación completa |
| `AuthServiceTest` | JUnit + Mockito | 7 |
| `AuthControllerTest` | `@WebMvcTest` | 7 |
| `UsuarioRepositoryTest` | `@DataJpaTest` + TestContainers | 1 |
| `SimulacroServiceTest` | JUnit + Mockito | 7 |
| `CalificadorServiceTest` | JUnit parametrizado | 6 |
| `PreguntaServiceTest` | JUnit + Mockito | 6 |
| `PreguntaControllerTest` | `@WebMvcTest` | 9 |
| `PreguntaRepositoryTest` | `@DataJpaTest` + TestContainers | 5 |

`AbstractContainerBaseTest` (en `src/test/.../shared/`) es la clase base para tests `@DataJpaTest` con PostgreSQL real.

---

## 5. Errores que ya cometimos (no repetirlos)

1. **`@Lob` sobre un `String` con PostgreSQL** mapea al tipo `oid` (para binarios), no a texto, y falla al insertar. Usar `@Column(columnDefinition = "TEXT")`.
2. **`@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional` normal** hace que **la aplicación no arranque**. Hay que usar `@Transactional(propagation = Propagation.REQUIRES_NEW)`, porque en esa fase la transacción original ya se cerró.
3. **El CI puede estar en verde con una aplicación que no enciende**, porque los tests de rebanada (`@WebMvcTest`, `@DataJpaTest`) y los de Mockito no cargan el contexto completo. Por eso existe `ApplicationContextSmokeTest`. **Antes de abrir un PR, arrancar la app al menos una vez** con `./mvnw spring-boot:run` y verificar que diga `Started BackendApplication`.
4. **Un mismo `Tema` no puede estar en dos bloques de calificación de la misma área.** El motor resuelve el esquema de puntaje a partir del tema. Si "Matemática" entra en Habilidades y en Conocimientos, deben ser dos temas distintos.
5. **`@WebMvcTest` sin cargar `SecurityConfig`** aplica una seguridad por defecto con CSRF que devuelve 403 en los POST. Se resuelve con `@AutoConfigureMockMvc(addFilters = false)` cuando lo que se prueba es la validación y no la seguridad.

---

## 6. Qué falta, por persona

### Juan Carlos — catálogo real y progreso · **BLOQUEA A TODO EL EQUIPO**
- [ ] Abrir PR y mergear los 5 repositorios que ya tiene en su rama.
- [ ] Agregar un campo `temario` (texto largo) a `Tema`, para guardar la lista oficial de subtemas del prospecto. Lo usará el generador de preguntas.
- [ ] Seed de datos reales: universidades, áreas, esquemas de calificación con los valores exactos, temas con su temario, estructura de examen, carreras y ofertas académicas con el puntaje del último ingresante (procesos 2023-II a 2025-I).
- [ ] Paquete nuevo `progreso/`: IP (`PSP / puntajeUltimoIngresante`) con semáforo (≥1.10 holgado, 1.00-1.09 ajustado, 0.85-0.99 cerca, <0.85 reforzar), dominio por tema, listener de `SimulacroFinalizadoEvent`, endpoint del panel.

### Fabiana — banco de preguntas
- [ ] Pasar el `temario` del tema dentro del prompt de Gemini, para que las preguntas salgan del nivel y contenido correctos (depende del campo que agregue Juan Carlos).
- [ ] Conseguir `GEMINI_API_KEY` y generar preguntas por tema.
- [ ] Revisar y aprobar preguntas (`PATCH /api/v1/preguntas/{id}/aprobar`).

### Zoe — suscripción, gamificación y correo · **SIN EMPEZAR**
- [ ] `suscripcion/`: `Suscripcion`, `UsoDiario` (con `@Version`), `PlanService.puedeAcceder(...)`, Mercado Pago (preapproval + webhook **idempotente**).
- [ ] `gamificacion/`: listener de `SimulacroFinalizadoEvent` para racha y XP (los campos ya existen en `Usuario`), `Logro` y `UsuarioLogro`.
- [ ] Servicio de correo: registro, **recuperación de contraseña** (el evento `PasswordResetRequestedEvent` ya se publica, solo hay que escucharlo) y confirmación de pago. Con plantillas HTML y envío `@Async`.
- [ ] Al construir el control de límites, crear una excepción propia (ej. `LimiteDePlanExcedidoException`): **la rúbrica pide más de 7 excepciones personalizadas y hoy hay 6.**

### Transversal (de nadie en particular)
- [ ] `postman_collection.json` en la raíz del repo, con todos los endpoints documentados.
- [ ] Informe de 1000-2000 palabras dentro del `README.md`, con la estructura fija que pide la rúbrica.
- [ ] GitHub Projects/Issues con milestones y labels.
- [ ] Deployment en AWS (ECS/EC2 + RDS). Antes: pasar la contraseña de base de datos a variable de entorno y hacer configurable el CORS.

---

## 7. Convenciones de trabajo

- Una rama por módulo: `feat/<modulo>`. **Nunca push directo a `main`.**
- PRs pequeños, con el CI en verde antes de mergear.
- Commits en español, en imperativo: *"agrega repositorio de OfertaAcademica"*.
- Nunca commitear: `.env`, API keys, credenciales, la carpeta `target/`.
- No copiar preguntas textuales de exámenes oficiales: se usan solo como referencia de estilo y dificultad.

> Nota: la protección de rama de GitHub no se puede activar en este repositorio (es privado en una cuenta personal, y GitHub exige plan Team). La regla se cumple **por acuerdo del equipo**, no porque la plataforma lo impida.
