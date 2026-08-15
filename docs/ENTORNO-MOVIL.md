# Entorno de desarrollo móvil — SmartVest (Semana 9)

Documento de configuración reproducible (taller EVA: configuración, verificación y conexión).

## Proyecto

**SmartVest** — asistencia a personas con discapacidad visual. Cliente móvil React Native + TypeScript que consume el backend PHP/MariaDB de este repositorio (`api/iot.php`).

Repositorio: https://github.com/elJohn72/Smartvest  
Carpeta del cliente: `mobile/`

## Versiones de este equipo (2026-08-15)

| Herramienta | Versión / estado |
|-------------|------------------|
| macOS | Darwin 25.5.0 |
| Node.js | v25.8.1 |
| npm | 11.11.0 |
| Java | OpenJDK 24.0.2 (no usado en esta entrega; no hay build nativo) |
| Expo SDK | ~57.0.13 (`mobile/package.json`) |
| React | 19.2.3 |
| React Native | 0.86.2 |
| Android Studio / SDK / adb | **No instalados** |
| Xcode | **No instalado** |
| Watchman | No instalado |
| XAMPP | Presente en `/Applications/XAMPP` (Apache requiere root para arrancar) |
| Destino de ejecución | Dispositivo físico + Expo Go, Wi-Fi LAN `192.168.0.103` |

## Pasos de configuración

1. Clonar el repo y `cd mobile && npm install`.
2. Instalar Expo Go en el celular.
3. Publicar SmartVest en XAMPP (`npm run build && ./scripts/deploy-xampp.sh` desde la raíz, si el htdocs no está al día).
4. Arrancar Apache + MySQL de XAMPP.
5. Copiar `mobile/.env.example` → `mobile/.env` con la IP LAN (`ipconfig getifaddr en0`).
6. `npm start` y escanear el QR.
7. `npm run doctor` y resolver hallazgos de la plataforma prevista (JS/Expo Go).

## Diagnóstico

```bash
cd mobile
npm run doctor
npx expo --version
node -v
```

Hallazgos de Android SDK, emulador o Xcode **no aplican** a esta entrega: el destino previsto es el dispositivo físico + Expo Go. En la verificación del 2026-08-15, `npx expo-doctor` quedó **21/21**.

## Conectividad con la API propia

| Destino | URL base |
|---------|----------|
| Mac (curl) | `http://127.0.0.1/Smartvest/api` |
| AVD (si existiera) | `http://10.0.2.2/Smartvest/api` |
| Celular físico | `http://192.168.0.103/Smartvest/api` |

Prueba en el Mac:

```bash
curl "http://127.0.0.1/Smartvest/api/iot.php?deviceId=VEST-DEMO"
```

Prueba en la app: botón **Consultar VEST-DEMO**.

Tráfico HTTP (cleartext) solo en desarrollo. En `app.json`: plugin `expo-build-properties` (`android.usesCleartextTraffic`) y ATS `NSAllowsLocalNetworking`. Eliminar antes de cualquier distribución.

## Recarga en caliente

Expo Fast Refresh. Evidencia: editar `HOT_RELOAD_LABEL` en `mobile/src/config.ts`.
