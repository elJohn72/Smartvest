# API REST y base de datos

Referencia técnica de los endpoints PHP y las tablas MariaDB. Todas las respuestas son **JSON** con `Content-Type: application/json`.

**Base URL local:** `http://localhost/Smartvest/api/`

---

## Configuración

### `api/config.php`

| Constante | Valor por defecto |
|-----------|-------------------|
| `DB_HOST` | `127.0.0.1` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `smartvest` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | vacío |
| `SMARTVEST_IOT_API_KEY_DEFAULT` | `smartvest-local-dev-key` |

### `api/config.local.php` (opcional, no en Git)

Copia desde `config.local.php.example`:

```php
const SMARTVEST_IOT_API_KEY = 'tu-clave-secreta-iot';
const SMARTVEST_GEMINI_API_KEY = 'AIza...';
const SMARTVEST_GROQ_API_KEY = 'gsk_...';
```

---

## Tabla `users`

| Columna SQL | Campo JSON | Descripción |
|-------------|------------|-------------|
| `id` | `id` | UUID primario |
| `full_name` | `fullName` | Nombre completo |
| `national_id` | `nationalId` | Cédula / ID |
| `age` | `age` | Edad |
| `blood_type` | `bloodType` | Ej. `O+` |
| `address` | `address` | Domicilio |
| `emergency_phone` | `emergencyPhone` | Teléfono principal |
| `emergency_contact` | `emergencyContact` | JSON: name, relationship, phone |
| `medical_observations` | `medicalObservations` | Alergias, notas |
| `created_at` | `createdAt` | ISO 8601 |
| `photo` | `photo` | Base64 opcional |
| `username` | `username` | Login |
| `password` | — | **Nunca se devuelve en GET**; hash bcrypt en BD |
| `device_id` | `deviceId` | Ej. `VEST-001` |

---

## Tabla `iot_states`

Una fila por chaleco (último estado conocido).

| Columna SQL | Campo JSON | Descripción |
|-------------|------------|-------------|
| `device_id` | `deviceId` | PK, ej. `VEST-001` |
| `distance_cm` | `distanceCm` | Ultrasonido (cm) |
| `latitude` | `latitude` | GPS |
| `longitude` | `longitude` | GPS |
| `sos_active` | `sosActive` | `true` / `false` |
| `battery_level` | `batteryLevel` | 0–100; opcional si el firmware tiene ADC configurado |
| `sos_active` | `sosActive` | `true` cuando el botón SOS está pulsado; ver [SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md) |

### Historial de telemetría

`GET /api/iot.php?deviceId=VEST-001&history=1&limit=60`

Devuelve puntos ordenados del más antiguo al más reciente (tabla `iot_history`). Cada `POST` del chaleco añade una fila.
| `last_update` | `lastUpdate` | Milisegundos epoch en JSON |

**Dato demo:** `VEST-DEMO` se inserta con `database.sql` para pruebas sin hardware.

---

## Optimización Semana 8 (cache / N+1 / cola / auth)

| Pieza | Archivo | Qué hace |
|-------|---------|----------|
| Cache-aside | `api/lib/cache_aside.php` | TTL + invalidación en lecturas users/IoT/dashboard |
| Cola async | `api/lib/job_queue.php` + `api/worker.php` | `prune_iot_history` y `notify_sos` fuera del HTTP |
| Auth token | `api/lib/auth_token.php` | HMAC tras login; sin reconsultar password |
| Anti N+1 | `api/dashboard.php` | `LEFT JOIN` users↔iot_states (`?mode=n1` demo) |

```bash
/Applications/XAMPP/xamppfiles/bin/php api/worker.php --once
```

Login ahora puede devolver `token`. Usarlo así:

```http
Authorization: Bearer <token>
GET /Smartvest/api/dashboard.php
```

---

## `users.php`

### GET — listar o buscar uno

```http
GET /Smartvest/api/users.php
GET /Smartvest/api/users.php?id=<uuid>
```

Respuesta (sin passwords):

```json
{
  "success": true,
  "users": [ { "id": "...", "fullName": "...", "deviceId": "VEST-001" } ]
}
```

El listado aplica **cache-aside** y omite `photo` (lazy): la foto se pide con `?id=`.

### POST — acciones

**Login:**

```http
POST /Smartvest/api/users.php
Content-Type: application/json

{
  "action": "login",
  "username": "usuario@ejemplo.com",
  "password": "********"
}
```

**Registro / actualización (upsert):**

```http
POST /Smartvest/api/users.php
Content-Type: application/json

{ "id": "...", "fullName": "...", "password": "...", "deviceId": "VEST-001", ... }
```

**Importar backup JSON:**

```json
{ "action": "import", "users": [ ... ] }
```

---

## `iot.php`

### GET — leer estado

```http
GET /Smartvest/api/iot.php?deviceId=VEST-001
```

```json
{
  "success": true,
  "data": {
    "deviceId": "VEST-001",
    "distanceCm": 85,
    "latitude": -0.180653,
    "longitude": -78.467834,
    "sosActive": false,
    "batteryLevel": 85,
    "lastUpdate": 1779743136000
  }
}
```

Si no existe el dispositivo: `"data": null`.

### POST — actualizar (chaleco o simulación)

```http
POST /Smartvest/api/iot.php
Content-Type: application/json
X-SmartVest-Api-Key: smartvest-local-dev-key

{
  "deviceId": "VEST-001",
  "distanceCm": 50,
  "latitude": -0.18,
  "longitude": -78.47,
  "sosActive": false,
  "batteryLevel": 85
}
```

Sin cabecera válida → **401** `No autorizado`.

---

## `assistant.php`

Análisis de riesgo, lugares frecuentes, eventos SOS y chat con contexto del paciente/chaleco. Usa estadística sobre `iot_history` + narrativa opcional con **Groq** (`llama-3.3-70b-versatile` por defecto).

```http
POST /Smartvest/api/assistant.php
Content-Type: application/json

{ "action": "analyze", "userId": "<uuid>", "deviceId": "VEST-001" }
```

```json
{
  "success": true,
  "analytics": { "riskScore": 42, "dailyRiskLevel": "medio", "frequentPlaces": [], "sosEvents": [] },
  "insights": "## Resumen del día\n...",
  "aiPowered": true
}
```

**Chat:**

```http
POST /Smartvest/api/assistant.php
Content-Type: application/json

{
  "action": "chat",
  "userId": "<uuid>",
  "deviceId": "VEST-001",
  "messages": [{ "role": "user", "content": "¿Está en línea el chaleco?" }]
}
```

En `api/config.local.php`:

```php
const SMARTVEST_GROQ_API_KEY = 'gsk_...';
const SMARTVEST_GROQ_MODEL = 'llama-3.3-70b-versatile'; // opcional
```

Sin `SMARTVEST_GROQ_API_KEY` el endpoint sigue respondiendo con métricas estadísticas y respuestas locales en el chat.

---

## `address.php`

Verificación de dirección vía Gemini **solo en servidor**.

```http
POST /Smartvest/api/address.php
Content-Type: application/json

{ "address": "Av. Principal 123, Quito" }
```

```json
{ "success": true, "address": "Dirección formateada..." }
```

Si no hay clave Gemini configurada → **503**.

---

## Scripts de mantenimiento

| Script | Uso |
|--------|-----|
| `scripts/migrate-plain-passwords.php` | Convierte contraseñas en texto plano a bcrypt |
| `database.sql` | Crear o resetear esquema |

Ejemplo migración:

```bash
/Applications/XAMPP/xamppfiles/bin/php scripts/migrate-plain-passwords.php
```

---

## Pruebas rápidas con curl

```bash
# Listar usuarios (sin passwords)
curl -s "http://localhost/Smartvest/api/users.php"

# IoT
curl -s "http://localhost/Smartvest/api/iot.php?deviceId=VEST-001"

# Login
curl -s -X POST "http://localhost/Smartvest/api/users.php" \
  -H "Content-Type: application/json" \
  -d '{"action":"login","username":"TU_USUARIO","password":"TU_CLAVE"}'
```

---

## Siguiente lectura

- [Instalación](./INSTALACION.md)
- [Firmware](../firmware/esp32/platformio-smartvest/README.md)
