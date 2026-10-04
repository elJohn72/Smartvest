# Entorno móvil — SmartVest

Actualizado el 4 de octubre de 2026 para la exposición de Aplicaciones Móviles y Sistemas Embebidos.

## Aplicación que se presenta

El APK instalado `com.ajtecnology.smartvest` es una envoltura Android que abre la aplicación SmartVest completa servida por XAMPP. La interfaz canónica sigue en la raíz web del repositorio; `mobile/App.tsx` carga esa misma interfaz dentro del APK. No se usa Expo Go ni se reemplaza la app por un cliente de telemetría reducido.

El teléfono y la Mac necesitan estar en la misma Wi‑Fi y Apache/MariaDB deben estar activos. La URL web se deriva de la URL API existente (`EXPO_PUBLIC_API_BASE_URL` o `mobile/src/config.ts`) quitando el sufijo `/api`; no cambies la red del ESP32 para esta demo.

## Funciones y límites

- La app completa conserva inicio, acceso, registro, perfil, QR, configuración y ficha de emergencia.
- El perfil muestra distancia, historial y guía háptica con información que devuelve la API.
- El mapa aparece cuando la ESP32 reporta una fijación GPS válida; no rellenar coordenadas ni mostrar valores simulados como reales.
- El SOS se activa con el botón físico del chaleco. La app reacciona al recibir `sosActive=true`.
- El firmware puede intentar un SMS por SIM800L al activar SOS si está habilitado, conectado y tiene antena, señal y saldo. La API no reporta señal GSM ni confirma entrega del SMS.
- Para login, usar la cuenta de prueba registrada en MariaDB y su contraseña. El usuario inicial sugerido por el flujo de laboratorio era `eva.lab.presentacion`; si se cambió al registrarlo, usar el valor elegido. La contraseña no se escribe en documentación.

## Compilación Android

Las dependencias del cliente se instalan con el lockfile desde `mobile/`:

~~~bash
npm ci
npx tsc --noEmit
~~~

Para el APK release, ejecutar desde `mobile/android/`:

~~~bash
./gradlew assembleRelease
~~~

El APK de esta sesión es versión 24, paquete `com.ajtecnology.smartvest`, y está limitado a ARM64 porque ese es el procesador del Redmi conectado. Se actualiza encima de la instalación existente con `adb install -r`; no hace falta instalar Expo Go ni borrar datos.

## Servidor, base de datos y telemetría

La Mac debe servir la aplicación en `/Smartvest/` y mantener disponibles `api/users.php` y `api/iot.php`. Consultar desde la Mac:

~~~bash
curl "http://127.0.0.1/Smartvest/api/iot.php?deviceId=VEST-001"
curl "http://127.0.0.1/Smartvest/api/iot.php?deviceId=VEST-001&history=1&limit=10"
~~~

`iot_states` guarda el último estado y `iot_history` conserva las lecturas históricas. `database.sql` incluye una fila de ejemplo `VEST-DEMO`; no es evidencia de un sensor conectado. Para mostrar telemetría física, verificar que `lastUpdate` sea reciente y aparezca un nuevo punto de `VEST-001` en `iot_history`.

## Grabación y privacidad

Usa solamente perfiles identificados `PRUEBA/LOCAL`. En phpMyAdmin enseña tablas y lecturas recientes de prueba; no muestres contraseñas, claves API, datos clínicos reales ni coordenadas exactas. Avisa al contacto de prueba antes de pulsar SOS porque el firmware puede enviar un SMS real.

La secuencia y el texto hablado están en [GUIA-EXPOSICION.md](./GUIA-EXPOSICION.md). No muestres esa guía en OBS. El backend usa HTTP en la red local y es un prototipo académico, no un servicio listo para producción.
