# Comandos y protocolo de verificación

## Comandos disponibles hoy

Desde raíz Smartvest:
```bash
git status --short
npm ci
npm run build
```
Desde mobile/ (Expo legado): `npm ci` y `npx tsc --noEmit`.
No existe npm test ni npm run lint en la raíz. No ejecutar deploy-xampp.sh para validar esta entrega documental. El workflow actual solo construye web y publica Pages al push main; esta rama no activa ese deploy.

## Comandos objetivo, todavía NO disponibles

T03/T04/T07 deben crear y documentar estos runners antes de usarlos como gates. Todos terminan no cero ante fallo; versiones fijadas y portables Linux/macOS. No son evidencia de implementación.
```bash
# Desde raíz; Compose con configuración PRUEBA, nunca credenciales reales
./scripts/ci/check-contracts.sh
./scripts/ci/check-secrets.sh
./scripts/ci/test-api.sh
./scripts/ci/test-migrations.sh
./scripts/ci/audit-dependencies.sh
docker compose -f infra/smartvest/compose.lab.yml config --quiet
docker compose -f infra/smartvest/compose.lab.yml up -d --build
./scripts/ci/smoke-lab.sh
git diff --check
# Desde android-native/ tras scaffolding y SDK fijados
./gradlew --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
./gradlew --no-daemon :app:connectedDebugAndroidTest
./gradlew --no-daemon :app:assembleRelease
# Desde firmware/esp32/platformio-smartvest con PlatformIO instalado y fijado
pio run
```
connectedDebugAndroidTest necesita emulador/dispositivo real. assembleRelease sin firma protegida no acredita APK instalable de actualización. SDK/JDK/AGP/Kotlin y PHP/MariaDB se registran junto a resultados. Nunca imprimir un compose expandido con secretos.

## Ensayo físico M1

1. Datos PRUEBA, dos cuentas/chalecos provisionados, cuenta A vinculada al hardware real.
2. API HTTPS del laboratorio accesible y lectura nueva registrada. Mac apagada; móvil usando red distinta del ESP32.
3. 30 min: 20 mediciones, historial, distancia, GPS solo con fix y SOS físico. Registrar timestamp servidor y UI, sin filmar credenciales/PII.
4. Cortar red del ESP32: alertas físicas operan; app muestra VENCIDO tras 30 s. Cortar red del móvil: SIN_CONEXIÓN; recuperar sin duplicar evento SOS.
5. Cuenta B intenta todos los recursos A: negar; QR público solo allowlist consentida. Revocar QR y sesión, repetir pruebas.
6. TalkBack y escala 200%; actualizar APK con misma firma sin borrar instalación.
7. Reiniciar servicios y restaurar copia cifrada en BD aislada, comprobar conteos; rollback digest con schema compatible.
8. Adjuntar commit/digest, equipo Android, runtime, pruebas, métricas y limitaciones a checklist. No declarar LISTO con pruebas pendientes.

M2 añade segundo plano, Doze, permisos negados, force-stop, provider failure, worker crash y duplicados; distinguir aceptación del proveedor, recepción y apertura. No acreditar push con log local.
