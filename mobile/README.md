# Cliente móvil SmartVest (Semana 9 — UEA)

App **React Native + TypeScript** (Expo) del proyecto integrador **SmartVest**: consulta el backend PHP propio (`api/iot.php`) para leer la telemetría del chaleco de asistencia a personas con discapacidad visual.

No es un ejemplo genérico. El backend sigue siendo `api/` de este repositorio.

## Por qué este stack

| Decisión | Motivo |
|----------|--------|
| React Native + TypeScript | Un código para Android e iOS; tipos alineados con `types.ts` del frontend web. |
| Expo (SDK 57) | Misma base RN; `expo-doctor` verifica el entorno; Expo Go permite un destino físico sin AVD. |
| Destino: celular físico | En este Mac no hay Android Studio ni Xcode; el AVD no es reproducible. EVA permite dispositivo físico justificado. |
| Backend PHP + MariaDB | El de las semanas 4–8 (`api/iot.php`, `VEST-DEMO`). |

## Requisitos

- Node.js 18+ (este equipo: v25.8.1)
- npm 10+
- App **Expo Go** en el celular (misma Wi-Fi que el Mac)
- XAMPP (Apache + MariaDB) con SmartVest publicado en `http://localhost/Smartvest/`

## Comandos de verificación

Desde `mobile/`:

```bash
node -v
npm -v
npx expo --version
npm run doctor
```

`npm run doctor` debe quedar **sin hallazgos pendientes**. En este equipo (2026-08-15) sale **21/21**. El destino es Expo Go en dispositivo físico; no se exige AVD ni Xcode.

## Variables de entorno

Copia `.env.example` → `.env`:

```bash
# Celular físico (IP LAN del Mac; hoy 192.168.0.103)
EXPO_PUBLIC_API_BASE_URL=http://192.168.0.103/Smartvest/api

# Si más adelante hay AVD:
# EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2/Smartvest/api
```

`localhost` en el celular **no** es el Mac. En AVD el alias del host es `10.0.2.2`. HTTP sin TLS solo en desarrollo; el plugin `expo-build-properties` activa `usesCleartextTraffic` en Android y ATS local en iOS (`NSAllowsLocalNetworking`). Eliminar ambos antes de distribuir.

Obtener la IP:

```bash
ipconfig getifaddr en0
```

## Cómo ejecutar

1. Arrancar XAMPP (Apache + MySQL) — hace falta privilegio de administrador.
2. Comprobar la API en el Mac:

```bash
curl "http://127.0.0.1/Smartvest/api/iot.php?deviceId=VEST-DEMO"
```

3. En `mobile/`:

```bash
cp .env.example .env   # ajustar la IP
npm start
```

4. Abrir Expo Go, escanear el QR. Misma red Wi-Fi.
5. Pulsar **Consultar VEST-DEMO**. Debe verse `success` y el JSON de telemetría.
6. Fast Refresh: cambiar `HOT_RELOAD_LABEL` en `src/config.ts` y guardar.

## Estructura

```text
mobile/
├── App.tsx              # Pantalla de telemetría
├── src/config.ts        # URL base + etiqueta de hot reload
├── src/api.ts           # GET api/iot.php
├── .env.example
└── README.md            # este archivo
```

## Limitaciones de este entorno

- Sin Android Studio / AVD y sin Xcode en el Mac de desarrollo.
- Node 25 y OpenJDK 24 están instalados; no se usa el JDK para un build nativo en esta semana.
- Apache de XAMPP no arranca sin `sudo`; hay que levantarlo a mano antes de grabar la demo.
- La IP LAN cambia; actualizar `.env` si el router reasigna.

Documentación canónica del entorno: `docs/ENTORNO-MOVIL.md`.
