# Guía de instalación local (XAMPP)

Pasos para dejar SmartVest funcionando en tu computadora con **Apache + MariaDB + PHP**, típico en macOS con XAMPP.

---

## Requisitos

| Software | Para qué |
|----------|----------|
| **XAMPP** | Apache, PHP 8.2, MariaDB |
| **Node.js** 18+ | Compilar el frontend (`npm run build`) |
| **Git** (opcional) | Clonar el repositorio |

---

## 1. Clonar o copiar el proyecto

```bash
cd "/Users/TU_USUARIO/Repositorios Githup/03-proyectos/Smartvest"
```

(Ajusta la ruta a donde tengas el repo.)

---

## 2. Base de datos

1. Abre **XAMPP** → inicia **Apache** y **MySQL/MariaDB**.
2. Entra a [http://localhost/phpmyadmin](http://localhost/phpmyadmin).
3. Pestaña **Importar** → selecciona `database.sql` del proyecto.
4. Debe crearse la base `smartvest` con tablas `users` e `iot_states`.

**Verificar por consola:**

```bash
"/Applications/XAMPP/xamppfiles/bin/mysql" -h 127.0.0.1 -u root smartvest -e "SHOW TABLES;"
```

---

## 3. Configurar PHP (opcional pero recomendado)

```bash
cp api/config.local.php.example api/config.local.php
```

Edita `api/config.local.php`:

- `SMARTVEST_GEMINI_API_KEY` — para validar direcciones en el registro.
- `SMARTVEST_IOT_API_KEY` — debe coincidir con el firmware (`smartvest_config.h`).

Si no creas este archivo, IoT usa la clave por defecto `smartvest-local-dev-key`.

---

## 4. Frontend: dependencias y build

```bash
npm install
npm run build
```

Si `npm run build` falla, revisa que Node esté instalado: `node -v`.

---

## 5. Publicar en htdocs

```bash
./scripts/deploy-xampp.sh
```

Esto copia el proyecto a:

`/Applications/XAMPP/xamppfiles/htdocs/Smartvest`

y actualiza `index.html` + `assets/` desde `dist/`.

**Regla de oro:** cada vez que cambies código React:

```bash
npm run build && ./scripts/deploy-xampp.sh
```

Nunca en paralelo (el deploy podría copiar un build incompleto).

---

## 6. Abrir la aplicación

[http://localhost/Smartvest/](http://localhost/Smartvest/)

### Primera prueba

1. **Crear cuenta** → completa el formulario → genera QR.
2. O **Iniciar sesión** si ya hay usuario en BD.
3. En el perfil, revisa estado del chaleco (`VEST-001` o el `deviceId` que hayas puesto).

---

## 7. Firmware (opcional)

Ver [firmware/esp32/platformio-smartvest/README.md](../firmware/esp32/platformio-smartvest/README.md).

Resumen:

1. Copia `include/smartvest_config.h.example` → `smartvest_config.h`.
2. Configura WiFi y `SMARTVEST_API_URL` (IP de tu PC en la red, no `localhost`).
3. Misma `SMARTVEST_IOT_API_KEY` que en PHP.
4. `pio run -t upload`.

Ejemplo URL para el ESP32:

`http://192.168.1.50/Smartvest/api/iot.php`

---

## Solución de problemas

| Problema | Qué revisar |
|----------|-------------|
| Página en blanco | Consola del navegador (F12); ¿existe `assets/index-*.js`? |
| API no responde | Apache encendido; ruta `/Smartvest/api/users.php` |
| Login falla | Usuario en BD; ejecutar migración de contraseñas si migraste de versión antigua |
| IoT siempre offline | Fila en `iot_states` para tu `deviceId`; POST con Api-Key |
| Mapa en 0,0 | GPS sin fix; en demo usa «Mover GPS» |
| SOS siempre activo | `UPDATE iot_states SET sos_active=0 WHERE device_id='VEST-001'` |

---

## Desarrollo con hot-reload (sin XAMPP para UI)

```bash
npm run dev
```

Vite suele usar `http://localhost:5173/Smartvest/`. Las APIs deben estar en XAMPP o configurar proxy (no incluido por defecto); para prueba completa usa XAMPP tras el build.

---

## Siguiente lectura

- [Funcionalidades](./FUNCIONALIDADES.md)
- [Despliegue](./DESPLIEGUE.md)
