# Evidencia del despliegue en AWS

La API se desplegó en AWS (laboratorio de AWS Academy, región `us-east-1`): una instancia **EC2** con la aplicación como servicio de systemd detrás de nginx y una base **RDS PostgreSQL 16**. La dirección pública fija es `http://184.194.122.22`.

| Archivo | Qué muestra |
| --- | --- |
| [`estado-del-servidor.txt`](estado-del-servidor.txt) | Salida tomada dentro de la instancia: id y tipo de instancia, IP pública, sistema operativo y Java, servicio `rumbou` activo, nginx, nombres de las variables de entorno (sin valores), conexión a RDS con el conteo de datos cargados, health por la IP pública y correos enviados por SMTP |
| [`reporte-postman-aws.html`](reporte-postman-aws.html) | Reporte de la colección completa ejecutada contra `http://184.194.122.22`: 35 de 35 validaciones correctas. Los tokens están ocultos. Se abre en el navegador |
