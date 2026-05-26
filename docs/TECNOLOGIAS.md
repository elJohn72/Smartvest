# Tecnologías implementadas

Explicación del stack de SmartVest: qué es cada pieza y por qué se eligió algo **simple y mantenible** (ideal para XAMPP y prototipos).

---

## Vista general

| Capa | Tecnología | Versión aprox. | Función |
|------|------------|----------------|---------|
| Interfaz | React | 19.x | Pantallas interactivas |
| Lenguaje UI | TypeScript | 5.7 | Tipos y menos errores |
| Build | Vite | 6.x | Empaquetado rápido para producción |
| Estilos | Tailwind CSS | 3.4 | Diseño responsive |
| Iconos | lucide-react | — | Iconografía consistente |
| QR | qrcode.react | — | Generación de códigos QR |
| Servidor web | Apache (XAMPP) | — | Sirve HTML/JS/PHP |
| API | PHP | 8.2+ | JSON REST sin framework |
| Base de datos | MariaDB | 10.x+ | Usuarios y estado IoT |
| Microcontrolador | ESP32 | — | Chaleco inteligente |
| Firmware tool | PlatformIO | — | Compilar/subir C++ al ESP32 |
| IA (opcional) | Google Gemini API | — | Verificar direcciones en servidor |

---

## Frontend (lo que ves en el navegador)

### React + TypeScript

- **React:** biblioteca para construir la interfaz por componentes (`Login`, `UserProfile`, etc.).
- **TypeScript:** añade tipos (`UserData`, `IotData` en `types.ts`) para documentar qué datos espera cada pantalla.

### Vite

- En desarrollo: `npm run dev` (servidor local con recarga).
- En producción: `npm run build` genera la carpeta `dist/` con JS/CSS optimizados.
- **Base path:** `/Smartvest/` — importante para XAMPP y GitHub Pages (`vite.config.ts`).

### Tailwind CSS

- Clases utilitarias (`bg-blue-600`, `rounded-xl`) en lugar de muchos archivos CSS sueltos.
- Entrada principal: `index.css` (incluye animaciones y `prefers-reduced-motion`).

### Servicios TypeScript (`services/`)

| Archivo | Rol |
|---------|-----|
| `storageService.ts` | Usuarios: API + respaldo `localStorage` |
| `iotService.ts` | Polling IoT, estado de conexión, simulación demo |
| `geminiService.ts` | Llama a `api/address.php` (ya no expone API key en el navegador) |
| `toastService.ts` | Avisos flotantes (éxito, error, info) |

### Utilidades (`utils/`)

- `formatRelativeTime.ts` — “hace 5 s”, “hace 2 min”.
- `obstacleLevel.ts` — colores según distancia (igual que firmware: 40 / 100 / 200 cm).
- `validateRegistration.ts` — reglas del formulario de registro.
- `buildAppUrl.ts`, `clipboard.ts` — enlaces QR y copiar al portapapeles.

---

## Backend (servidor PHP)

### Por qué PHP y no Node/Java para la API

- XAMPP ya trae **Apache + PHP + MariaDB**.
- Cero procesos extra: un archivo `.php` por endpoint, fácil de probar con `curl`.

### Archivos importantes

| Archivo | Descripción |
|---------|-------------|
| `api/config.php` | Conexión PDO, helpers JSON, hash de contraseñas, cabeceras de seguridad |
| `api/config.local.php` | Claves locales (gitignored): IoT y Gemini |
| `api/users.php` | CRUD usuarios, login, importación JSON |
| `api/iot.php` | GET/POST telemetría por `deviceId` |
| `api/address.php` | Proxy a Gemini para direcciones |

### Seguridad implementada

- Contraseñas con **bcrypt** (`password_hash` / `password_verify`).
- Las respuestas **no incluyen** el campo `password`.
- `POST api/iot.php` exige cabecera `X-SmartVest-Api-Key`.
- Cabeceras: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.

---

## Base de datos (MariaDB)

- Script inicial: `database.sql`.
- Base: `smartvest`.
- Tablas: `users`, `iot_states` (ver [API.md](./API.md)).

---

## Firmware (ESP32)

- C++ con **PlatformIO**.
- Librerías típicas: WiFi, HTTPClient, GPS serial, ultrasonido.
- Configuración: `include/smartvest_config.h` (copiar desde `.example`, no se sube a Git).
- Envía JSON por POST a `api/iot.php` cada `SMARTVEST_HTTP_INTERVAL_MS` (por defecto 5 s).

---

## Herramientas de desarrollo y calidad

| Herramienta | Uso en el repo |
|-------------|----------------|
| **GitHub Spec Kit** | Carpeta `.specify/`, skills `/speckit-*`, specs en `specs/` |
| **ESLint/Tests** | No configurados aún (`package.json` solo tiene dev/build/preview) |
| **scripts/deploy-xampp.sh` | Copia el proyecto a `htdocs/Smartvest` |

---

## Comunicación IoT: HTTP (no MQTT)

| Aspecto | Detalle |
|---------|---------|
| Chaleco → servidor | `POST` JSON a `iot.php` |
| Web → servidor | `GET ?deviceId=VEST-001` cada ~2 s |
| Latencia típica | 2–7 s (no tiempo real estricto) |
| Alternativa futura | MQTT + broker si se necesitan muchos dispositivos o push instantáneo |

---

## Dependencias npm principales

```json
"react", "react-dom", "vite", "typescript", "tailwindcss",
"lucide-react", "qrcode.react", "@google/genai" (solo si se usara cliente; hoy la IA va por PHP)
```

---

## Siguiente lectura

- [Arquitectura](./ARQUITECTURA.md)
- [Instalación](./INSTALACION.md)
