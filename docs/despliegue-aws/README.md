# Evidencia del despliegue en AWS

La API se desplegó en AWS (laboratorio de AWS Academy, región `us-east-1`): una instancia **EC2** con la aplicación como servicio de systemd detrás de nginx y una base **RDS PostgreSQL 16**. La dirección pública fija es `http://184.194.122.22`.

## Consola de AWS

| Captura | Qué muestra |
| --- | --- |
| [Laboratorio](01-laboratorio-aws-academy.png) | Sesión del laboratorio de AWS Academy activa, en la cuenta y región del despliegue |
| [Instancia EC2](02-instancia-ec2.png) | `rumbou-backend` en ejecución (`t3.small`, `us-east-1a`), con las comprobaciones de estado aprobadas y la IP `184.194.122.22` |
| [Grupos de seguridad](03-grupos-de-seguridad.png) | `rumbou-ec2-sg` para la instancia y `rumbou-rds-sg` para la base |
| [IP elástica](04-ip-elastica.png) | `184.194.122.22` asociada a la instancia, así la dirección no cambia al reiniciarla |
| [Regla de entrada de RDS](05-regla-de-entrada-rds.png) | El puerto 5432 de la base solo acepta conexiones del grupo de la instancia: la base no está expuesta a internet |
| [Base de datos RDS](06-base-de-datos-rds.png) | `rumbou-db` disponible: PostgreSQL, `db.t3.micro`, con las conexiones activas de la aplicación |

## Aplicación en funcionamiento

| Archivo | Qué muestra |
| --- | --- |
| [`estado-del-servidor.txt`](estado-del-servidor.txt) | Salida tomada dentro de la instancia: id y tipo de instancia, IP pública, sistema operativo y Java, servicio `rumbou` activo, nginx, nombres de las variables de entorno (sin valores), conexión a RDS con el conteo de datos cargados, health por la IP pública y correos enviados por SMTP |
| [`reporte-postman-aws.html`](reporte-postman-aws.html) | Reporte de la colección completa ejecutada contra `http://184.194.122.22`: 35 de 35 validaciones correctas, incluidos el tutor de IA con Gemini (`200`) y la suscripción PRO con Mercado Pago (`201`). Los tokens están ocultos. Se abre en el navegador |
