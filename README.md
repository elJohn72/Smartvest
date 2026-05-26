# SmartVest — Sistema de emergencia inteligente

**SmartVest** es una plataforma para registrar perfiles médicos de emergencia, generar códigos QR, y monitorear en tiempo (casi) real un **chaleco inteligente** con sensores IoT (distancia, GPS, botón SOS, batería). Está pensado para apoyar a personas con **discapacidad visual** y a sus familiares o cuidadores.

| Enlace | Contenido |
|--------|-----------|
| [¿Qué es SmartVest?](docs/PROYECTO.md) | Misión, usuarios, flujo general |
| [Tecnologías](docs/TECNOLOGIAS.md) | Stack completo explicado en lenguaje claro |
| [Arquitectura](docs/ARQUITECTURA.md) | Cómo se conectan web, API, BD y ESP32 |
| [API y base de datos](docs/API.md) | Endpoints, tablas, seguridad |
| [Instalación local (XAMPP)](docs/INSTALACION.md) | Paso a paso en tu Mac/PC |
| [Funcionalidades y mejoras](docs/FUNCIONALIDADES.md) | Todo lo que hace la app hoy |
| [Firmware ESP32](firmware/esp32/platformio-smartvest/README.md) | Chaleco, pines, telemetría |
| [Configurar ESP32 en Mac](docs/CONFIGURACION-ESP32.md) | WiFi, upload, pruebas |
| [Despliegue en Internet](docs/DESPLIEGUE.md) | Vercel, GitHub Pages, XAMPP |
| [Comparativa de mercado](docs/COMPARATIVA-MERCADO.md) | Productos similares y posicionamiento |
| [Roadmap de mejoras](docs/ROADMAP-MEJORAS.md) | Hecho / pendiente por fases |
| [Alertas accesibles](docs/ALERTAS-ACCESIBLES.md) | Buzzer, vibrador, SOS |
| [Servidor fijo en LAN](docs/SERVIDOR-FIJO-LAN.md) | IP para el ESP32 (no localhost) |
| [SMS de emergencia](docs/SMS-SOS-EMERGENCIA.md) | SIM800L → 0993212257 |
| [Guía de exposición](docs/GUIA-EXPOSICION.md) | Presentar el proyecto en ferias o defensa |
| [Guía para agentes IA](AGENTS.md) | Comandos y convenciones del repo |

---

## Inicio rápido (desarrollo local)

**Requisitos:** Node.js 18+, XAMPP (Apache + MariaDB) o equivalente.

```bash
# 1. Dependencias del frontend
npm install

# 2. Base de datos (en phpMyAdmin o consola)
#    Ejecutar database.sql → crea BD smartvest

# 3. Build y publicar en XAMPP
npm run build
./scripts/deploy-xampp.sh

# 4. Abrir en el navegador
open http://localhost/Smartvest/
```

**Configuración opcional:** copia `api/config.local.php.example` → `api/config.local.php` para claves de Gemini (direcciones) e IoT.

**IP para el chaleco:** `./scripts/print-lan-ip.sh` — usa esa URL en `smartvest_config.h`, no `localhost`.

---

## Estructura del repositorio

```text
Smartvest/
├── App.tsx, index.tsx, types.ts   # App React (sin carpeta src/)
├── components/                    # Pantallas y UI
├── services/                      # API cliente, IoT, toasts, almacenamiento
├── utils/                         # Validación, GPS, obstáculos, etc.
├── api/                           # Backend PHP (users, iot, address)
├── database.sql                   # Esquema MariaDB
├── firmware/esp32/                # Código del chaleco (PlatformIO)
├── scripts/                       # deploy-xampp.sh, migración contraseñas
├── specs/                         # Auditoría y plan Spec Kit
└── docs/                          # Documentación detallada (este índice)
```

---

## APIs principales

| Endpoint | Uso |
|----------|-----|
| `GET/POST api/users.php` | Registro, login, listado de usuarios |
| `GET/POST api/iot.php` | Telemetría del chaleco por `deviceId` |
| `POST api/address.php` | Verificación de dirección (Gemini en servidor) |

---

## Comunicación IoT (resumen)

El chaleco envía datos por **HTTP POST** cada ~5 s. La web consulta el último estado por **HTTP GET** cada ~2 s (1 s si hay SOS). No se usa MQTT en la versión actual; ver [Arquitectura](docs/ARQUITECTURA.md).

---

## Licencia y contacto

Proyecto académico / producto en evolución. Contacto de referencia en la landing: **contacto@smartvest.app**.

**Documentación generada para la versión actual del código** (mayo 2026). Si cambias API o esquema, actualiza `database.sql`, `docs/API.md` y este README.
