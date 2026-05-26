# Funcionalidades y mejoras implementadas

Lista detallada de lo que hace la aplicación web hoy, incluyendo mejoras de seguridad, UX, IoT y accesibilidad (2026).

---

## Pantallas principales

### Landing (inicio)

- Presentación del producto: misión, visión, contacto.
- Botones **Iniciar sesión** y **Crear cuenta**.
- Diseño responsive con mockup de teléfono.

### Login

- Modo **Usuario + contraseña** (API `action: login`).
- Modo **ID / QR** — abre perfil por UUID sin contraseña (útil en el mismo equipo o si el ID es público en el QR).
- Mensajes de error con `aria-live`.
- Estado **Ingresando…** / **Buscando…** durante la petición.

### Registro

- Datos personales, médicos, contacto de emergencia, foto (máx. 2 MB).
- Usuario, contraseña y **ID del dispositivo** (`VEST-xxx`).
- Validación antes de enviar (edad, teléfono, longitud de usuario/clave).
- Verificación opcional de dirección vía **servidor** (`api/address.php` + Gemini).
- ID de dispositivo auto-generado si se deja vacío (`VEST-<número>`).

### Vista QR (tras registro)

Tres modos de código QR:

| Modo | Contenido del QR | Cuándo usarlo |
|------|------------------|---------------|
| **Texto offline** | Texto plano con datos médicos | Sin internet; lectura directa |
| **Enlace completo** | URL con `?data=<base64>` | Otro dispositivo abre perfil con datos embebidos |
| **Solo ID** | URL con `?uid=<uuid>` | QR pequeño; requiere datos ya en el servidor |

Acciones:

- Descargar imagen PNG del QR.
- Copiar enlace (modos web).
- Ir al inicio / **Ver perfil y mapa**.

### Perfil de monitoreo

- Cabecera con foto, tipo de sangre, llamada de emergencia.
- **Estado del chaleco:** Conectando / En línea / Sin señal reciente / Sin datos.
- **Última actualización:** “hace X s” (actualiza cada segundo).
- Mapa Google embebido si GPS válido; mensaje claro si GPS = 0,0.
- Batería: muestra porcentaje si el firmware envía `batteryLevel`; si no hay sensor en PCB, indica **«Sin sensor en PCB»**.
- Distancia al obstáculo con **colores** (verde → rojo según umbrales del firmware).
- **Historial de distancia** (gráfico de barras desde `iot_history`).
- **Notificaciones SOS** del navegador + sonido de alerta para el cuidador.
- **Guía de alertas hápticas** (umbrales buzzer/vibrador).
- **PWA** (`manifest.json`, `sw.js`, iconos locales).
- **SMS SOS (SIM800L)** al **0993212257** al pulsar el botón físico — [SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md).

### Emergencia SOS (tres canales)

| Canal | Comportamiento |
|-------|----------------|
| Hardware | Buzzer + vibrador (patrón SOS dedicado) |
| Web | Overlay rojo, notificación navegador, `tel:` al contacto |
| GSM | SMS con `deviceId`, distancia y mapa (si GPS fix) |
- Tarjetas: contacto, observaciones médicas, dirección.
- **Pantalla SOS** a pantalla completa si `sosActive` es true.
- **Imprimir ficha** de emergencia (vista impresión limpia).
- Panel **Simulación (demo)** plegable: mover GPS, activar/apagar SOS.
- Modal de ayuda para conexión del hardware.

### Enlaces de URL

- `?data=...` — importa perfil portable (monitoreo sin login).
- `?uid=...` — abre perfil por ID si existe en BD/local.

---

## IoT y comunicación HTTP

| Característica | Detalle |
|----------------|---------|
| Polling web | ~2 s normal, ~1 s con SOS activo |
| POST chaleco | ~5 s (configurable en firmware) |
| Autenticación POST | Cabecera `X-SmartVest-Api-Key` |
| Simulación local | Envía POST desde navegador en `localhost` con clave dev |
| Sin historial | Solo último estado en `iot_states` |

### Umbrales de obstáculos (igual que ESP32)

| Distancia | Nivel | Color en UI |
|----------|-------|-------------|
| > 200 cm | Despejado | Verde |
| ≤ 200 cm | Precaución | Amarillo |
| ≤ 100 cm | Alerta | Naranja |
| ≤ 40 cm | Peligro | Rojo |

---

## Seguridad y privacidad

| Mejora | Estado |
|--------|--------|
| Contraseñas con bcrypt | Sí |
| GET usuarios sin campo `password` | Sí |
| Migración de contraseñas en claro | Script PHP |
| API key en POST IoT | Sí |
| Gemini solo en servidor | Sí (no en bundle JS) |
| Cabeceras HTTP de seguridad | Sí |
| localStorage sin guardar password | Sí |
| QR sin credenciales de login | Sí (enlace portable sin user/pass) |

---

## Experiencia de usuario (UX)

| Mejora | Descripción |
|--------|-------------|
| **Toasts** | Avisos abajo a la derecha (éxito, error, info) |
| Sin `alert()` | Export CSV/JSON, importación, validaciones |
| Skip link | “Saltar al contenido principal” |
| Login/registro | Feedback visual y mensajes claros |

---

## Accesibilidad

| Mejora | Descripción |
|--------|-------------|
| Labels asociados | `htmlFor` + `id` en inputs |
| Foco visible | `focus-visible` en botones |
| `aria-label` | Botones con solo icono, estado del chaleco |
| `aria-live` | Errores de login y avisos |
| `prefers-reduced-motion` | Reduce animaciones si el SO lo pide |
| Título en iframe mapa | Descripción para lectores de pantalla |

---

## SEO y PWA

| Elemento | Ubicación |
|----------|-----------|
| Meta description | `index.html` |
| Open Graph básico | `index.html` |
| `manifest.json` | PWA metadata |
| `robots.txt` | `public/` |
| `sitemap.xml` | `public/` |

---

## Importación / exportación

- **Exportar CSV** — listado de usuarios (sin contraseña en API; CSV sin columna password).
- **Exportar JSON** — backup completo para restaurar.
- **Importar JSON** — fusiona usuarios vía API `action: import`.

*(Pantalla `Home.tsx` con estas acciones existe en el código; la navegación principal actual usa `LandingPage` — las funciones siguen disponibles vía `storageService` si se enlaza la pantalla.)*

---

## Organización del proyecto (Spec Kit)

- Inicializado **GitHub Spec Kit** (`specify init`).
- Constitución del proyecto: `.specify/memory/constitution.md`.
- Auditoría documentada: `specs/001-web-platform-audit/`.
- Skills Cursor: `/speckit-specify`, `/speckit-plan`, etc.

---

## Lo que está planificado pero no implementado

- Panel de administración de usuarios (CRUD web).
- Historial de telemetría (tabla de series temporales).
- MQTT o WebSocket para push instantáneo.
- Tests automatizados (`npm test`).
- Despliegue CI/CD documentado en el repo.

---

## Siguiente lectura

- [API](./API.md)
- [Tecnologías](./TECNOLOGIAS.md)
