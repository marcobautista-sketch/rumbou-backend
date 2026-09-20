# CLAUDE.md — RumboU Backend

Este archivo va en la raíz del repositorio y se commitea. Claude Code lo lee automáticamente al inicio de cada sesión, para los 4 integrantes.

---

## Qué estamos construyendo

RumboU es una API REST en Spring Boot para preparación de exámenes de admisión universitaria en Perú. El usuario elige una carrera objetivo, rinde simulacros que replican fielmente el examen real (incluida la penalidad por respuesta incorrecta), y ve cuánto le falta para alcanzar el puntaje del último ingresante de esa carrera. Tiene un plan gratuito con límites y un plan PRO de S/ 39 al mes que desbloquea simulacros ilimitados y un tutor de IA.

Es el proyecto del curso CS2031 Desarrollo Basado en Plataformas (UTEC). Somos 4 personas trabajando por GitHub. **Solo backend en esta etapa. No hay frontend y no se debe generar ninguno.**

**ESCENARIO ACTIVO: [UNI Y UNMSM]**

---

## Entrega y rúbrica (Semana 7 — fecha límite 2026-09-25, 11:59 p.m.)

Esta es la entrega del backend completo: funcionalidades, seguridad, pruebas, deployment, informe y colección de Postman. Se califica sobre 20 puntos. Lo que sigue es lo que la rúbrica exige **además** de lo ya descrito en este documento — hay que tenerlo presente en cada módulo:

- **Roles y autorización (1 punto).** `Usuario` necesita un rol (mínimo `USER` y `ADMIN`), verificado con `@PreAuthorize`/`@Secured` en endpoints sensibles, y el rol debe viajar también dentro del JWT, no solo en la base de datos.
- **Refresh tokens (parte de los 1.5 puntos de JWT).** El sistema de login no solo genera el access token: también debe emitir y validar un refresh token.
- **Servicio de correo electrónico (0.5 puntos).** Con plantillas HTML (Thymeleaf u otro), enviado de forma asíncrona, para: confirmación de registro, **recuperación de contraseña** (endpoint de "olvidé mi contraseña" que hoy no existe en el diseño) y confirmación de pago aprobado.
- **Deployment (2 puntos).** Mínimo en una plataforma de "instant deployment" (Railway, Render, Heroku) con base de datos en la nube, para el punto completo de esa categoría. Sin desplegar, son 0 puntos en este rubro.
- **Más de 7 excepciones personalizadas** organizadas por categoría (no solo `ResourceNotFoundException`), y el `GlobalExceptionHandler` debe manejar también las excepciones propias de Spring (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`).
- **Más de 10 DTOs especializados** (Request/Response/Create/Update/Detail) en total — no un DTO genérico por entidad.
- **Convenciones REST con versionado:** rutas tipo `/api/v1/recurso`, recursos en plural.
- **`postman_collection.json`** debe vivir literalmente con ese nombre en la raíz del repo, con endpoints documentados, ejemplos, variables y autorización configurada.
- **GitHub Projects/Issues** para gestionar tareas, con milestones y labels — no solo código, también proceso.
- **El `README.md` debe incluir además un informe narrativo de 1000-2000 palabras** con esta estructura fija: Portada, Índice, Introducción (contexto + objetivos), Identificación del Problema (descripción + justificación), Descripción de la Solución (funcionalidades + tecnologías), Modelo de Entidades (diagrama + descripción), Manejo de Errores, Medidas de Seguridad (seguridad de datos + prevención de SQLi/XSS/CSRF), Eventos y Asincronía, GitHub & Management, Conclusión (logros + aprendizajes + trabajo futuro), Apéndices (licencia + referencias). Esto convive con las instrucciones técnicas de instalación que ya tiene el README (la rúbrica también las pide, en la sección 9.1).

Estado actual frente a la rúbrica: 18 entidades, 5 eventos con 7 listeners, 8 excepciones propias, 28 DTOs, 28 endpoints, roles + refresh tokens + correo + admin por variables de entorno, progreso (objetivos, IP, dominio por tema), desplegado en Railway. Falta la migración a AWS si llega la cuenta.

---

## Reglas de interacción (las más importantes)

1. **Un paso a la vez.** Propón el plan, espera mi confirmación, ejecuta ese paso, y detente. No encadenes cinco archivos en una sola respuesta. Si un paso genera un error, lo resolvemos antes de seguir.
2. **Explica en español, código en inglés.** Nombres de clases, métodos y variables en inglés. Comentarios y explicaciones en español.
3. **Explícame qué hace el código antes o después de escribirlo**, en lenguaje simple. Estoy aprendiendo Spring Boot; no asumas que conozco una anotación solo porque la usaste.
4. **Si algo se puede explicar con una analogía, úsala.** Las de cocina o correo postal me funcionan bien.
5. **Pregunta antes de crear entidades, cambiar el esquema de base de datos, o instalar dependencias nuevas.** No agregues librerías por tu cuenta.
6. **Si detectas que lo que pido contradice el diseño de este archivo, dilo antes de escribir código.** Prefiero discutirlo a descubrirlo después.

---

## Stack

- Java 21, Spring Boot 3.x, Maven
- Spring Data JPA + Hibernate, PostgreSQL
- Spring Security con JWT
- JUnit 5, TestContainers, MockMvc
- GitHub Actions para CI
- Postman como entregable de demostración (no hay frontend)

---

## Reglas de arquitectura (no negociables)

Estas reglas existen porque el proyecto ya fue diseñado. Romperlas obliga a refactorizar después.

1. **Ninguna constante del examen vive en el código.** Los valores de acierto, penalidad y puntaje máximo están en la tabla `EsquemaCalificacion`. El `CalificadorService` los recibe como parámetro.
2. **Ningún `if` por universidad, área o nombre de prueba dentro de los services.** Toda la variación es dato en `EsquemaCalificacion` y `EstructuraExamen`. Si sientes la necesidad de escribir ese condicional, el modelo está mal y hay que avisarme.
3. **Los módulos se comunican por eventos, no por llamadas directas entre services de distintos módulos.** Ver la sección de eventos.
4. **Toda operación que toque varias tablas va con `@Transactional`.**
5. **Los listeners que dependen de datos recién escritos van con `@TransactionalEventListener(phase = AFTER_COMMIT)`.** Si además llaman a un servicio externo, agregan `@Async`.
6. **Los contadores de uso y el XP usan locking optimista (`@Version`).** Es una condición de carrera real, no un adorno.
7. **Cada endpoint recibe y devuelve DTOs, nunca entidades.** Validación con Bean Validation en los DTOs de entrada.
8. **Los errores se manejan en un `@ControllerAdvice` global.** Nada de `try/catch` devolviendo strings desde los controllers.
9. **Las API keys van en variables de entorno.** Nunca en el repositorio, ni siquiera en un comentario.

---

## Estructura de paquetes

Organización **por capas**, como pide el curso (cambio hecho el 17 de septiembre a pedido del profesor; antes era por módulo funcional). Cada carpeta contiene un solo tipo de componente:

```
com.rumbou.backend
  config/          AsyncConfig, SchedulingConfig, SecurityConfig
  controller/      un controller por recurso REST (/api/v1/...)
  dto/request/     lo que entra a la API (con Bean Validation)
  dto/response/    lo que sale de la API (nunca entidades)
  entity/          entidades JPA y sus enums (Role, Dificultad, TipoSimulacro, ...)
  exception/       excepciones propias + GlobalExceptionHandler
  repository/      interfaces Spring Data JPA
  security/        JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl, entry point
  service/         lógica de negocio (AuthService, SimulacroService, PlanService, EmailService, ...)
  event/           eventos de dominio (records)
  listener/        oyentes de esos eventos (@EventListener / @TransactionalEventListener)
  seed/            runners de carga del catálogo desde CSV (profile "seed") + ArchivoSeed
  client/gemini/   cliente de Gemini, validador y generador/exportador de preguntas
  client/mercadopago/  cliente de Mercado Pago
  scheduler/       tareas @Scheduled
```

Cómo se reparte el trabajo ahora que las carpetas son compartidas: **cada persona sigue siendo dueña de sus clases** (las de su módulo: auth/examen → Marco, contenido → Fabiana, academico/progreso → Juan Carlos, suscripcion/gamificacion/correo → Zoe), estén en la carpeta que estén. Antes de editar una clase ajena, se avisa. Una funcionalidad nueva se crea en las capas que le tocan: entidad en `entity/`, repositorio en `repository/`, servicio en `service/`, DTOs en `dto/`, controller en `controller/`.

---

## Eventos del sistema

| Evento | Publica | Escuchan | Modo |
|---|---|---|---|
| `SimulacroFinalizadoEvent` (lleva el tipo) | `SimulacroService` | `GamificacionListener`, `ProgresoListener` (solo COMPLETO/DIAGNOSTICO mueven el IP) | síncrono, transaccional |
| `RespuestaIncorrectaEvent` | `SimulacroService` | `TutorIaExplicacionListener` | `@Async` + AFTER_COMMIT |
| `PagoAprobadoEvent` | `WebhookService` | `SuscripcionActivacionListener`, `PagoAprobadoCorreoListener` | AFTER_COMMIT |
| `PasswordResetRequestedEvent` | `AuthService` | `RecuperacionContrasenaCorreoListener` | `@Async` + AFTER_COMMIT |
| `UsuarioRegistradoEvent` | `AuthService` | `RegistroConfirmacionListener` | `@Async` + AFTER_COMMIT |

Sin eventos, el módulo de examen tendría que llamar a los services de progreso, gamificación y contenido, y nadie podría avanzar hasta que los otros terminen. Los eventos son lo que permite que 4 personas trabajen en paralelo.

---

## Modelo de datos, resumen

Entidades principales y la razón de existir de las menos obvias:

- `Usuario` — con `currentStreak`, `longestStreak`, `lastActivityDate`, `xpTotal`, `xpSemanal`, `version`
- `Universidad`, `Carrera` — catálogo
- `OfertaAcademica` — **M:N con atributos**: universidad + carrera + proceso de admisión + `puntajeUltimoIngresante`. Se cargan varios procesos por carrera para que la tabla tenga contenido real.
- `EsquemaCalificacion` — las reglas de puntaje como dato: `valorAcierto`, `valorPenalidad`, `puntajeMaximoBloque`
- `Tema`, `EstructuraExamen` — qué preguntas y cuántas arma cada simulacro
- `Pregunta` — pertenece a un `Tema`, nunca a una universidad. Campos `origen` y `aprobada` para trazabilidad
- `Simulacro`, `RespuestaUsuario` — **M:N con atributos**: incluye `puntajeAportado`, que puede ser negativo
- `Suscripcion`, `UsoDiario` — plan y contadores con `@Version`; `PagoWebhook` — notificaciones de pago ya procesadas (idempotencia)
- `Logro`, `UsuarioLogro`
- `ObjetivoUsuario` — carrera objetivo del usuario con su último PSP e IP (se desactiva, no se borra)
- `PasswordResetToken` — token de un solo uso para recuperar la contraseña

### Cálculo de puntaje

```
puntajeBloque = (correctas * esquema.valorAcierto)
              - (incorrectas * esquema.valorPenalidad)
```

Las preguntas en blanco no suman ni restan.

### Métricas

```
PSP = (puntajeObtenido / puntajeMaximoDelSimulacro) * universidad.puntajeMaximo
IP  = PSP / ofertaAcademica.puntajeUltimoIngresante
```

`IP >= 1.10` holgado, `1.00–1.09` ajustado, `0.85–0.99` cerca, `< 0.85` reforzar.

---

## Planes

**Gratuito:** 1 objetivo activo, diagnóstico completo una vez, 3 simulacros por tema a la semana, 1 simulacro completo al mes, explicación estática de las preguntas, panel básico, gamificación completa.

**PRO (S/ 39/mes):** simulacros ilimitados, tutor de IA con tope de 30 consultas diarias, hasta 3 objetivos, panel completo con histórico y dominio por tema, reportes.

El control lo centraliza `PlanService.puedeAcceder(usuario, funcionalidad)`. Los controllers no consultan la suscripción directamente. Al superar un límite se responde **403** con un DTO que indica qué límite se alcanzó. El rol `ADMIN` pasa todos los límites (decisión del 17 de septiembre) para poder probar y demostrar el tutor de IA sin Mercado Pago.

---

## Testing

Cada persona escribe los tests de su propio módulo. Un PR sin tests no se mergea.

| Tipo | Anotación | Ejemplo en el proyecto |
|---|---|---|
| Unitario | JUnit puro | `CalificadorService`, PSP, IP, lógica de racha |
| Repositorio | `@DataJpaTest` + TestContainers | filtros de `Pregunta` por tema y dificultad |
| Web | `@WebMvcTest` | validación de DTOs, 403 al superar el límite |
| Integración | `@SpringBootTest` | flujo completo de simulacro y de webhook |

El `CalificadorService` se prueba con `@ParameterizedTest` y `@MethodSource`, recorriendo todos los esquemas de calificación en un solo test.

TestContainers requiere Docker corriendo localmente.

---

## Git

- `main` protegida. Nadie hace push directo.
- Una rama por módulo o funcionalidad: `feat/examen-calificador`, `fix/webhook-idempotencia`.
- PRs pequeños. El CI de GitHub Actions debe estar en verde para mergear.
- Commits en español, en imperativo: `agrega CalificadorService con esquema configurable`.
- **No commitear:** `.env`, `application-local.properties`, API keys, ni el `target/`.

---

## Qué NO hacer

- No generar frontend, ni HTML, ni React. Solo backend.
- No copiar preguntas textuales de exámenes oficiales al banco de datos. Se generan preguntas propias.
- No hardcodear valores de puntaje, penalidad ni cortes de admisión.
- No usar `spring.jpa.hibernate.ddl-auto=create-drop` en nada que no sea test.
- No instalar dependencias sin preguntar.
- No refactorizar código de un módulo que no es el mío sin avisar al dueño.
