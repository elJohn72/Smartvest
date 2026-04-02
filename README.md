<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# SmartVest

Aplicación React/Vite para registro de usuarios y monitoreo IoT con QR.

## Desarrollo con Node

**Requisitos:** Node.js

1. Instala dependencias con `npm install`.
2. Si vas a usar Gemini, configura `API_KEY` en tu entorno de Vite.
3. Ejecuta `npm run dev`.

## Ejecutar en XAMPP + MariaDB

**Requisitos:** XAMPP con Apache y MariaDB activos.

1. Copia esta carpeta a `htdocs/Smartvest`.
2. Entra a `http://localhost/phpmyadmin` y ejecuta el archivo `database.sql`.
3. Verifica las credenciales de `api/config.php`.
   Por defecto usa:
   - host: `127.0.0.1`
   - puerto: `3306`
   - base: `smartvest`
   - usuario: `root`
   - contraseña: vacía
4. En la carpeta del proyecto ejecuta `npm install` y luego `npm run build`.
5. Abre `http://localhost/Smartvest/`.

## APIs locales incluidas

- `api/users.php`: guarda y consulta usuarios en MariaDB.
- `api/iot.php`: guarda y consulta la última ubicación/estado por `deviceId`.

## Flujo de monitoreo local

1. Registras a la persona.
2. Se genera un QR con su perfil público de monitoreo y `deviceId`.
3. Al escanear el QR desde otro equipo, se abre el perfil.
4. El frontend consulta `api/iot.php` para ver la última ubicación del chaleco.

## Simulación IoT

Desde el perfil puedes usar los botones de simulación para mover el GPS o activar SOS.
También puedes enviar actualizaciones reales al endpoint `api/iot.php` desde tu ESP32 o backend.
