# rumbou-backend

## Cómo levantar la base de datos local

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
