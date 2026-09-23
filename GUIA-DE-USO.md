# RumboU — Guía de uso de la API con Postman

Esta guía explica cómo probar RumboU de punta a punta con la colección [`postman_collection.json`](postman_collection.json): cuentas y sesión, el orden de los requests, qué devuelve cada uno, las reglas de negocio (planes, tipos de simulacro) y las fórmulas con las que se calculan los puntajes. El informe del proyecto está en el [README](README.md).

## Índice

1. [Antes de empezar](#1-antes-de-empezar)
2. [Cuentas, roles e inicio de sesión](#2-cuentas-roles-e-inicio-de-sesión)
3. [Variables de la colección](#3-variables-de-la-colección)
4. [Recorrido completo, request por request](#4-recorrido-completo-request-por-request)
5. [Reglas de negocio](#5-reglas-de-negocio)
6. [Fórmulas](#6-fórmulas)
7. [Datos de referencia (ids de áreas y temas)](#7-datos-de-referencia)
8. [Códigos de error y qué significan](#8-códigos-de-error-y-qué-significan)

---

## 1. Antes de empezar

| Qué | Dónde |
|---|---|
| API en producción (AWS) | `http://184.194.122.22` |
| Health check (sin token) | `GET /api/v1/health` → `{"status":"ok", ...}` |
| API local | `http://localhost:8080` (ver instalación en el README, apéndice A) |

**Importar la colección:** en Postman, *File → Import* y arrastrar `postman_collection.json`. Si ya existía una versión anterior, elegir **Replace**. No hace falta crear un *environment*: todo vive en las variables de la colección.

**Ejecutar todo de una vez:** clic derecho sobre la colección → **Run collection** → Run. Los requests corren en orden, cada uno guarda en variables lo que necesitan los siguientes (tokens, ids) y cada uno trae un *test* que valida la respuesta. Se puede repetir las veces que se quiera: el registro crea un usuario nuevo en cada corrida.

---

## 2. Cuentas, roles e inicio de sesión

### Cómo funciona la sesión

RumboU no usa sesiones ni cookies: usa **JSON Web Tokens (JWT)**. Al registrarse o iniciar sesión, la API devuelve dos tokens:

| Token | Dura | Para qué |
|---|---|---|
| `accessToken` | 15 minutos | Va en cada request, en el header `Authorization: Bearer <token>` |
| `refreshToken` | 7 días | Se envía a `POST /auth/refresh` para obtener un `accessToken` nuevo sin volver a escribir la contraseña |

La colección hace esto sola: los requests de registro, login y refresh guardan los tokens en las variables `accessToken` y `refreshToken`, y la autorización Bearer está configurada a nivel de colección, así que **no hay que copiar ningún token a mano**. Si un request responde `401` después de un rato, basta con ejecutar **Refrescar token** (o Login) de nuevo.

### Roles

| Rol | Cómo se obtiene | Qué puede hacer |
|---|---|---|
| `USER` | Al registrarse (siempre) | Simulacros, progreso, ver preguntas, suscribirse |
| `ADMIN` | Solo lo asigna otro administrador (`PATCH /usuarios/{id}/rol`) o las variables `ADMIN_EMAIL`/`ADMIN_PASSWORD` al arrancar el servidor | Todo lo anterior, más crear/editar/aprobar/borrar preguntas y administrar usuarios. **Además pasa todos los límites del plan** (puede usar el tutor de IA y los paneles PRO sin suscripción) |

El rol vive en la base de datos **y dentro del token**. Por eso, si a un usuario se le cambia el rol, tiene que volver a iniciar sesión para que su token nuevo lo refleje.

**Importante:** iniciar sesión no verifica el rol; solo verifica correo y contraseña. Un `200` en el login no significa que la cuenta sea administradora: eso se ve en **Mi perfil** (`"role"`) o cuando un request de administración responde `403`.

### Las tres formas de entrar

1. **Registrar usuario** — crea una cuenta `USER` nueva. La colección genera el correo automáticamente (`postulante<fecha>@rumbou.com`, contraseña `password123`) y lo guarda en la variable `email`.
2. **Login** — entra con ese mismo correo. Útil para renovar tokens.
3. **Login como administrador** — el mismo endpoint de login, pero con la **cuenta de evaluación** del curso, que ya tiene rol `ADMIN` en producción:

   | Campo | Valor |
   |---|---|
   | `adminEmail` | `evaluador@rumbou.app` |
   | `adminPassword` | `RumboU-Evaluador-2026` |

   A partir de ese request, toda la colección corre como administrador. Cualquier otra cuenta con rol `ADMIN` también sirve: basta con cambiar esas dos variables.

### Recuperar la contraseña

`POST /auth/forgot-password` responde `200` **siempre**, exista o no el correo (para no revelar qué cuentas están registradas), y dispara un correo con un token de un solo uso que vence a los 30 minutos. `POST /auth/reset-password` recibe ese token y la contraseña nueva. En el despliegue de AWS el correo **sí se envía** (Gmail por SMTP): al registrarse llega el de bienvenida y al pedir la recuperación llega el del token. En la colección, "Resetear contraseña" responde `400` porque usa un token de ejemplo; para probarlo de verdad, copia el token del correo en la variable `resetToken` y vuelve a ejecutar ese request.

---

## 3. Variables de la colección

Se ven en la colección → pestaña **Variables**. Las que empiezan con id las llenan los propios requests.

| Variable | Valor por defecto | Quién la llena |
|---|---|---|
| `baseUrl` | URL de producción | Cambiar por el valor de `baseUrlLocal` para probar en local |
| `baseUrlLocal` | `http://localhost:8080` | — |
| `email` | `postulante@rumbou.com` | Registrar usuario (genera uno nuevo por corrida) |
| `accessToken`, `refreshToken` | vacío | Registrar, Login, Refrescar, Login como administrador |
| `adminEmail`, `adminPassword` | cuenta de evaluación | Editar solo para usar otro administrador |
| `usuarioId` | vacío | Mi perfil / Buscar usuario por email |
| `areaId` | `1` (UNI, General) | Listar ofertas académicas |
| `temaId` | `1` (Razonamiento Matemático) | Editar a mano para probar otro tema (ver sección 7) |
| `ofertaAcademicaId`, `ofertaAcademicaIdAlterna` | vacío | Listar ofertas académicas |
| `objetivoId` | vacío | Crear objetivo |
| `simulacroId`, `preguntaId`, `respuestaUsuarioId` | vacío | Iniciar simulacro (y Crear pregunta, para las operaciones de administración) |
| `suscripcionId` | vacío | Crear suscripción PRO |
| `resetToken` | vacío | Pegar a mano el token recibido por correo |

---

## 4. Recorrido completo, request por request

Los códigos entre paréntesis son los que aceptan los tests de la colección. Todo lo que no es público requiere token.

### Sistema
| Request | Envía | Devuelve |
|---|---|---|
| Health check | nada, sin token | `200` `{status, servicio, timestamp}` |

### Autenticación
| Request | Envía | Devuelve |
|---|---|---|
| Registrar usuario | `{email, password, nombre}` | `201` con `accessToken`, `refreshToken`, `tokenType`. `409` si el correo ya existe, `400` si falta un campo o el correo es inválido |
| Login | `{email, password}` | `200` con tokens. `401` si las credenciales no coinciden |
| Refrescar token | `{refreshToken}` | `200` con tokens nuevos. `401` si el refresh token es inválido, venció o es un access token |
| Olvidé mi contraseña | `{email}` | `200` siempre |
| Resetear contraseña | `{token, newPassword}` | `200`; `400`/`401` si el token está vacío, ya se usó o venció |
| Login como administrador | `{email: adminEmail, password: adminPassword}` | `200`; desde aquí todo corre como ADMIN |

### Usuarios
| Request | Envía | Devuelve |
|---|---|---|
| Mi perfil | — | `200` `{id, email, nombre, role}`. Nunca incluye la contraseña |
| Buscar usuario por email (ADMIN) | `?email=` | `200` con el usuario (`403` como USER, `404` si no existe) |
| Cambiar rol (ADMIN) | `{"role": "ADMIN"}` o `"USER"` sobre `/usuarios/{usuarioId}/rol` | `200` con el usuario actualizado. `400` si un admin intenta quitarse su propio rol, `403` como USER |

### Catálogo académico
| Request | Envía | Devuelve |
|---|---|---|
| Listar ofertas académicas | filtros opcionales `universidad`, `area`, `carrera` (texto parcial) | `200` lista de `{id, universidad, areaId, area, areaNombre, carrera, facultad, procesoAdmision, puntajeUltimoIngresante, vacantes}`. Guarda `ofertaAcademicaId` y `areaId` |

### Simulacros
| Request | Envía | Devuelve |
|---|---|---|
| Iniciar simulacro por tema | `{areaId, tipo: "POR_TEMA", temaId}` | `201` `{id, tipo, estado: EN_CURSO, fechaInicio, preguntas[]}`. Trae solo las preguntas de ese tema, tantas como tiene en el examen real (Razonamiento Matemático: 32; Trigonometría: 10). `400` sin `temaId` o si el tema no pertenece al área; `403` si se superó el límite del plan |
| Iniciar simulacro completo | `{areaId, tipo: "COMPLETO"}` (o `"DIAGNOSTICO"`) | `201` con el examen entero: 180 preguntas en la UNI, 100 en UNMSM. `403` si se superó el límite mensual |
| Responder pregunta | `{preguntaId, alternativaMarcada}` sobre `/simulacros/{simulacroId}/respuestas`. `alternativaMarcada` va de `0` a `4`; `null` deja la pregunta en blanco | `200`. `400` si la pregunta no es de ese simulacro, la alternativa no existe o el simulacro ya terminó |
| Finalizar simulacro | — | `200` `{simulacroId, puntajeObtenido, psp, estado: FINALIZADO}`. Dispara gamificación (racha, XP, logros) y progreso |

Cada pregunta del simulacro llega como `{respuestaUsuarioId, preguntaId, enunciado, alternativas[5]}`, **sin** la clave correcta ni la explicación.

### Progreso
| Request | Envía | Devuelve |
|---|---|---|
| Crear objetivo | `{ofertaAcademicaId}` | `201` con el panel de esa carrera (ver abajo). `409` si ya es objetivo activo, `404` si la oferta no existe, `403` si se superó el límite de objetivos del plan |
| Crear un segundo objetivo | otra oferta | `403` con plan gratuito (1 objetivo); `201` con PRO (3) o ADMIN (sin tope) |
| Listar mis objetivos | — | `200` lista de `{id, universidad, area, areaId, carrera, procesoAdmision, puntajeUltimoIngresante, ultimoPsp, indicePreparacion, estado, estadoDescripcion, fechaActualizacion}`. `ultimoPsp`, `indicePreparacion` y `estado` son `null` hasta finalizar un simulacro completo o diagnóstico de esa área |
| Dominio por tema (PRO) | — | `200` lista de `{temaId, tema, correctas, incorrectas, enBlanco, total, porcentajeAciertos}`, ordenada del tema más débil al más fuerte. Usa **todas** las respuestas, incluidos los simulacros por tema. `403` con plan gratuito |
| Historial de PSP (PRO) | `?areaId=` | `200` lista cronológica de `{simulacroId, tipo, fechaFin, puntajeObtenido, psp}`. `400` sin `areaId`, `403` con plan gratuito |
| Desactivar objetivo | `/objetivos/{objetivoId}` | `204`. El objetivo se desactiva (no se borra): si se vuelve a elegir la misma carrera, se reactiva con su historial |

### Preguntas
| Request | Envía | Devuelve |
|---|---|---|
| Listar preguntas | `?temaId=&dificultad=&origen=&aprobada=&page=&size=` (todos opcionales; `size` máximo 50) | `200` página de preguntas aprobadas. Solo un ADMIN puede pedir `aprobada=false` |
| Obtener pregunta por id | — | `200` `{id, temaId, tema, enunciado, alternativas, dificultad}` sin clave ni explicación |
| Tutor de IA (PRO) | `/preguntas/{preguntaId}/tutor-ia` | `200` `{preguntaId, explicacion}` generada por Gemini; descuenta 1 de las 30 consultas diarias. `403` con plan gratuito o al superar el tope; `502` si el servidor no tiene `GEMINI_API_KEY` (no descuenta cupo) |
| Crear pregunta (ADMIN) | `{temaId, enunciado, alternativas[5], claveCorrecta (0-4), explicacion, dificultad, origen, aprobada}` | `201` con la pregunta completa (con clave y explicación). Guarda `preguntaId`: los requests siguientes actúan sobre **esta** pregunta de prueba, no sobre el banco |
| Actualizar pregunta (ADMIN) | mismo cuerpo, reemplaza todos los campos | `200` |
| Aprobar pregunta (ADMIN) | `PATCH /preguntas/{id}/aprobar` | `200` con `aprobada: true` |
| Aprobar preguntas en lote (ADMIN) | `{"ids": [...]}` | `200` lista de aprobadas. Si un id no existe, no se aprueba ninguna (`404`) |
| Eliminar pregunta (ADMIN) | — | `204` |

Valores posibles: `dificultad` = `FACIL`, `MEDIA`, `DIFICIL`; `origen` = `SEMILLA` (creada a mano o cargada por el seed), `IA_APROBADA` (generada con IA y aprobada por una persona).

### Suscripciones
| Request | Envía | Devuelve |
|---|---|---|
| Crear suscripción PRO | — | `201` `{id, plan: PRO, estado: PENDIENTE, fechaInicio, fechaFin, linkPago}` con el enlace de pago de Mercado Pago (S/ 39 al mes). `502` si el servidor no tiene `MP_ACCESS_TOKEN` |
| Webhook de Mercado Pago | `{action: "payment.approved", paymentId, mercadoPagoPreapprovalId, externalReference}` (público, lo llama Mercado Pago) | `200`. Es idempotente: el mismo `paymentId` dos veces no activa dos veces. Al aprobarse, activa la suscripción y envía el correo de pago |

---

## 5. Reglas de negocio

### Planes y límites

| Función | Plan gratuito | Plan PRO (S/ 39 al mes) | ADMIN |
|---|---|---|---|
| Simulacros por tema | 3 por semana | Ilimitados | Ilimitados |
| Simulacros completos | 1 al mes (el diagnóstico cuenta aquí) | Ilimitados | Ilimitados |
| Carreras objetivo activas | 1 | 3 | Sin tope |
| Tutor de IA | No | 30 consultas al día | 30 al día |
| Dominio por tema e historial de PSP | No | Sí | Sí |
| Explicación estática de preguntas falladas, racha, XP y logros | Sí | Sí | Sí |

Al superar un límite la API responde `403` con el mensaje del límite (por ejemplo *"Limite alcanzado: 1 simulacro completo al mes"*). El control está centralizado en un solo servicio (`PlanService`); los contadores de uso se guardan por usuario y por día.

### Tipos de simulacro

| Tipo | Qué arma | Contador que consume |
|---|---|---|
| `COMPLETO` | Toda la estructura del área (todos los temas, con su cantidad real de preguntas) | Simulacros completos |
| `DIAGNOSTICO` | Igual que el completo; es el examen inicial para saber de dónde se parte | Simulacros completos |
| `POR_TEMA` | Solo el tema indicado en `temaId`, con la cantidad que ese tema tiene en el examen real del área | Simulacros por tema |

Las preguntas se eligen al azar entre las aprobadas de cada tema (hay 36 por tema en el banco), así que dos simulacros nunca son iguales.

### Estructura de los exámenes

Sale de la tabla `EstructuraExamen`, cargada desde CSV. No hay ningún valor escrito en el código.

**UNI — área General (180 preguntas, 1 800 puntos):**

| Bloque | Temas (preguntas) | Acierto | Penalidad por error |
|---|---|---|---|
| Aptitud Académica y Humanidades | Razonamiento Matemático (32), Razonamiento Verbal (32), Humanidades (36) | +6 | −1.2 |
| Matemática | Aritmética (10), Álgebra (10), Geometría (10), Trigonometría (10) | +15 | −3 |
| Física y Química | Física (20), Química (20) | +15 | −3 |

**UNMSM — áreas B (Ciencias Básicas) y C (Ingenierías) (100 preguntas, 2 000 puntos):**

| Bloque | Temas | Acierto | Penalidad por error |
|---|---|---|---|
| Actitudinal | Actitud (10) | +20 | 0 |
| Habilidades | Habilidad Verbal (10), Habilidad Lógico-Matemática (10) | +20 | −1.125 |
| Conocimientos | 17 temas (Aritmética, Álgebra, Geometría, Trigonometría, Lenguaje, Literatura, Psicología, Educación Cívica, Historia del Perú, Historia Universal, Geografía, Economía, Filosofía, Física, Química, Biología), 70 preguntas en total; la cantidad por tema varía entre B y C | +20 | −1.125 |

Las preguntas **en blanco no suman ni restan**.

### Gamificación

- Cada simulacro finalizado da **100 XP** (total y semanal).
- **Racha:** si la última actividad fue ayer, sube 1; si fue hoy, no cambia; si fue antes de ayer, vuelve a 1. Se guarda también la racha más larga.
- **Logros:** Primer simulacro; Racha de 7 días; Racha de 30 días; 1 000 XP; 5 000 XP. Se evalúan al finalizar cada simulacro y no se repiten.

### Correos

Se envían de forma asíncrona (la respuesta HTTP no espera): bienvenida al registrarse, enlace de recuperación de contraseña y confirmación de pago aprobado.

---

## 6. Fórmulas

### Puntaje de un simulacro

Cada respuesta aporta un puntaje según el bloque al que pertenece su tema:

```
correcta   → + valorAcierto del bloque
incorrecta → − valorPenalidad del bloque
en blanco  →   0
puntajeObtenido = suma de todos los aportes (puede ser negativo)
```

*Ejemplo (UNI):* 20 preguntas de Física, 15 correctas, 3 incorrectas, 2 en blanco → 15 × 15 − 3 × 3 = **216**.

### PSP — Puntaje Simulado Proyectado

Lleva el resultado del simulacro (de cualquier tamaño) a la escala oficial de la universidad, para poder compararlo con los puntajes reales de ingreso:

```
puntajeMaximoDelSimulacro = Σ (preguntas de cada tema × valorAcierto de su bloque)
PSP = (puntajeObtenido ÷ puntajeMaximoDelSimulacro) × puntajeMaximoUniversidad
```

`puntajeMaximoUniversidad` es 1 800 en la UNI y 2 000 en la UNMSM. Si `puntajeObtenido` es negativo, el PSP también.

*Ejemplo:* en el simulacro anterior el máximo era 20 × 15 = 300; PSP = (216 ÷ 300) × 1 800 = **1 296**.

En un examen completo el PSP es representativo. En un simulacro de **un solo tema** no lo es (10 de 10 en Trigonometría daría PSP 1 800): por eso el PSP de un `POR_TEMA` se guarda en su historial pero **no actualiza el panel de objetivos**.

### IP — Índice de Progreso

Compara el PSP con el puntaje del último ingresante de la carrera objetivo, en el proceso de admisión cargado (2026-II):

```
IP = PSP ÷ puntajeUltimoIngresante
```

| IP | Estado | Descripción en la API |
|---|---|---|
| ≥ 1.10 | `HOLGADO` | Zona de ingreso holgada |
| 1.00 – 1.09 | `AJUSTADO` | Zona de ingreso ajustada |
| 0.85 – 0.99 | `CERCA` | Cerca, falta poco |
| < 0.85 | `REFORZAR` | Necesitas reforzar |

*Ejemplo:* Ingeniería de Sistemas en la UNI cerró en 1 209. Con PSP 1 296, IP = 1.07 → `AJUSTADO`. Con PSP 900, IP = 0.74 → `REFORZAR`.

El IP se recalcula automáticamente para todos los objetivos de esa área cada vez que el usuario finaliza un simulacro `COMPLETO` o `DIAGNOSTICO`.

### Dominio por tema

```
porcentajeAciertos = correctas ÷ (correctas + incorrectas + enBlanco) × 100
```

Cuenta todas las respuestas del usuario en simulacros finalizados, de cualquier tipo.

---

## 7. Datos de referencia

### Áreas (`areaId`)

| id | Universidad | Área | Puntaje máximo |
|---|---|---|---|
| 1 | UNI | GENERAL — General | 1 800 |
| 2 | UNMSM | B — Ciencias Básicas | 2 000 |
| 3 | UNMSM | C — Ingenierías | 2 000 |

### Temas (`temaId`) y en qué áreas entran

| id | Tema | Áreas | id | Tema | Áreas |
|---|---|---|---|---|---|
| 1 | Razonamiento Matemático | 1 | 12 | Habilidad Lógico-Matemática | 2, 3 |
| 2 | Razonamiento Verbal | 1 | 13 | Lenguaje | 2, 3 |
| 3 | Humanidades | 1 | 14 | Literatura | 2, 3 |
| 4 | Aritmética | 1, 2, 3 | 15 | Psicología | 2, 3 |
| 5 | Álgebra | 1, 2, 3 | 16 | Educación Cívica | 2, 3 |
| 6 | Geometría | 1, 2, 3 | 17 | Historia del Perú | 2, 3 |
| 7 | Trigonometría | 1, 2, 3 | 18 | Historia Universal | 2, 3 |
| 8 | Física | 1, 2, 3 | 19 | Geografía | 2, 3 |
| 9 | Química | 1, 2, 3 | 20 | Economía | 2, 3 |
| 10 | Actitud | 2, 3 | 21 | Filosofía | 2, 3 |
| 11 | Habilidad Verbal | 2, 3 | 22 | Biología | 2, 3 |

Un `POR_TEMA` con un tema que no está en el área responde `400` (por ejemplo, Humanidades en UNMSM).

### Catálogo

2 universidades, 3 áreas, 6 esquemas de calificación, 22 temas, 51 carreras, 70 ofertas académicas del proceso 2026-II con su puntaje de ingreso, 5 logros y 792 preguntas aprobadas (36 por tema: 12 fáciles, 12 medias y 12 difíciles).

---

## 8. Códigos de error y qué significan

Todas las respuestas de error tienen la misma forma:

```json
{
  "timestamp": "2026-09-20T10:15:30",
  "status": 403,
  "error": "Forbidden",
  "message": "Limite alcanzado: 1 simulacro completo al mes",
  "path": "/api/v1/simulacros"
}
```

| Código | Cuándo | Qué hacer |
|---|---|---|
| `400` | Cuerpo inválido (`@Valid`), JSON mal formado, parámetro de URL ausente o inválido, o una regla de negocio (simulacro ya finalizado, tema fuera del área, alternativa inexistente, admin quitándose su rol) | Leer `message`: dice exactamente qué falta |
| `401` | Sin token, token vencido o inválido, credenciales incorrectas | Ejecutar Login o Refrescar token |
| `403` | Sin el rol necesario (`ADMIN`) o límite del plan alcanzado | Entrar como administrador o esperar el reinicio del contador |
| `404` | El recurso no existe (simulacro, pregunta, oferta, usuario, ruta) | Revisar el id |
| `409` | Duplicado: correo ya registrado, objetivo ya activo | — |
| `502` | Un servicio externo falló o no está configurado (Gemini, Mercado Pago) | No es un error del cliente ni del código; falta la credencial en el servidor |
| `500` | Error no controlado | No debería ocurrir; reportarlo con el `path` y la hora |
