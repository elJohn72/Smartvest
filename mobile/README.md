# Aplicación Android SmartVest

Esta es la app SmartVest instalada en el teléfono (`com.ajtecnology.smartvest`). Está construida con React Native y un WebView que abre la aplicación completa de este repositorio desde el XAMPP local. Así Android presenta la misma interfaz web —inicio, acceso, registro, perfil, QR y monitoreo— en vez de mantener una segunda interfaz reducida.

**No se usa Expo Go ni la aplicación `SmartVest Demo`.** El APK conserva la URL LAN ya configurada en `src/config.ts` o en `EXPO_PUBLIC_API_BASE_URL`: la vista web se calcula quitando `/api` de esa URL. El teléfono y la Mac deben estar en la misma Wi‑Fi, con Apache y MariaDB activos.

## Funciones que se muestran

- Inicio, acceso con usuario y contraseña, registro y perfil de emergencia.
- QR del perfil, información médica y contacto registrado.
- Distancia recibida del ESP32, alertas hápticas e historial almacenado en MariaDB.
- Mapa GPS cuando el dispositivo reporta una fijación válida.
- Alerta visual roja cuando llega `sosActive=true` desde el botón físico SOS del chaleco.
- Tarjetas SOS y GSM/SMS con la explicación del flujo real.

El botón SOS se pulsa en el chaleco: esta app recibe la alerta; no hay un canal de comandos de la app hacia la ESP32. El firmware puede intentar enviar un SMS mediante SIM800L si el módulo está conectado y habilitado, con antena, señal y saldo. La API no informa nivel de señal GSM ni confirma entrega de SMS. No mostrar GPS simulado ni telemetría semilla como datos físicos.

## Cuenta para la demo

Usa la cuenta que registraste en la base local, con su contraseña definida por ti. El flujo de laboratorio proponía `eva.lab.presentacion` como usuario inicial; si lo cambiaste al registrarte, usa el que escribiste. No guardar la contraseña en este README ni mostrarla en OBS.

## Compilar e instalar

Desde `mobile/`, instalar las dependencias fijadas:

```bash
npm ci
npx tsc --noEmit
```

Para generar el APK Android de esta demo, desde `mobile/android/`:

```bash
./gradlew assembleRelease
```

El APK actual es `android/app/build/outputs/apk/release/app-release.apk`, versión 24, paquete `com.ajtecnology.smartvest`, compilado para ARM64 del Redmi conectado. Se instala como actualización de esa misma app:

```bash
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

No desinstales la app existente para actualizarla: así Android conserva sus datos locales.

## Comprobar el backend local

En la Mac, con XAMPP activo:

```bash
curl "http://127.0.0.1/Smartvest/api/iot.php?deviceId=VEST-001"
```

Un `success: true` sin `data` significa que la API contestó pero aún no tiene telemetría para el dispositivo. Para presentarla como lectura física, verifica un `lastUpdate` reciente y un nuevo punto en `iot_history`.

Para el orden, las acciones y el texto de exposición, sigue [la guía de exposición](../docs/GUIA-EXPOSICION.md). La guía contiene datos de ensayo solamente y debe quedar fuera de la captura OBS.
