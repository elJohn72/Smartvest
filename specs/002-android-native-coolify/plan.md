# Plan de implementación propuesto

## Arquitectura

Android Kotlin/Compose → API PHP v1 HTTPS → MariaDB privada.
ESP32 → HTTPS con clave por dispositivo → ingestión v1.
API → outbox MariaDB → worker M2 → proveedor push → Android.
QR → página HTML mínima de emergencia HTTPS, sin necesitar la app.
Coolify: proyecto SMARTVEST / environment lab; contenedores api, worker y db, redes y volúmenes propios. No usar coolify-db ni coolify-redis como datos de SmartVest. Panel continúa solo por túnel SSH.

## Estructura objetivo

android-native/app/src/main/java/com/ajtecnology/smartvest/{ui,data,core}/
android-native/app/src/test/ y src/androidTest/
api/v1/{auth,profiles,devices,public}/ (rutas mediante front controller)
api/lib/ (helpers compartidos revisados, sin fallback de secretos)
api/migrations/ · tests/api/ · tests/fixtures/PRUEBA/
infra/smartvest/{Dockerfile,compose.lab.yml}/ · scripts/ci/ · .github/workflows/
Rutas propuestas, aún no creadas. No sobreescribir mobile/, hardware ni configuraciones privadas.

## Programación y dependencias

Kotlin: Compose + ViewModel + coroutines/StateFlow; UI sin red directa, repositories inyectados por constructor, DTO separados del modelo de UI y errores tipados. Cancelar al salir de pantalla; no catch genérico que oculte errores ni GlobalScope. Retrofit/OkHttp + serialización como propuesta a fijar en catálogo Gradle; JUnit y coroutines-test/MockWebServer para prueba de red; Compose UI tests para flujos. Un módulo app inicialmente; añadir abstracciones solo ante lógica compartida real.
PHP: strict_types, PDO prepared statements, controladores pequeños, validación de entrada central, política owner en capa común, transacciones para vínculo/estado/eventos, password_hash/password_verify. PHPUnit para autorización/contratos/integración, Composer lock; PHPStan y formatter con configuración documentada y sin baseline que oculte hallazgos nuevos.
SQL: índices por consultas, FK, migraciones idempotentes con tabla de versiones/checksum, paginación; no concatenar entrada. Retención mediante job duradero y batched deletes.
Firmware: flujo local de sensores prioritario, networking aislado/timeout, certificado CA válido, NTP antes de TLS, payload nullable/gpsFix y eventId/sequence; reintentos con cola acotada, preservar un SOS pendiente antes de telemetría rutinaria.

Ejemplo de estilo objetivo (ilustrativo, no implementación):
```kotlin
sealed interface MonitorUiState {
    data object Loading : MonitorUiState
    data object NoData : MonitorUiState
    data class Ready(val reading: Reading, val stale: Boolean) : MonitorUiState
    data class Failed(val message: String) : MonitorUiState
}
interface TelemetryRepository {
    suspend fun latest(deviceId: String): Reading?
}
```
Clases PascalCase, funciones camelCase, constantes UPPER_SNAKE_CASE; mensajes en strings.xml español, no literal de negocio en composables. Sin passwords en DTO persistente ni logs. Config por entorno, sin IP LAN/paths macOS en release.

## Fases y checkpoints

0. Validar G0–G2, reconciliar cinco commits con main, fijar matriz runtime/locks, aprobar enmienda constitucional y decisiones D1–D8 aplicables. No implementar bloqueado por una pregunta no relacionada; D6 solo bloquea M2.
1. Contratos OpenAPI y modelo/migraciones, fixtures PRUEBA, CI de PR sin deploy. Checkpoint: contrato válido, migración temporal/rollback probado.
2. Auth/owner/provisionamiento/QR/ingestión protegida. Checkpoint: negativos de SC-003 verdes, rutas legadas bloqueadas, ningún secreto default.
3. Contenedores lab, volúmenes, worker, backup cifrado y restore. Checkpoint: smoke local Linux y macOS, readiness real, reinicio sin pérdida; revisión antes de deploy remoto.
4. Android nativo incremental: acceso + monitoreo primero, luego perfil, vínculo, QR, historial y TalkBack. Firmware TLS coordinado. Checkpoint: APK/debug y release, pruebas y accesibilidad; compatibilidad de firma verificada.
5. Deploy lab autorizado desde digest/HEAD probado; pruebas físicas SC-001–005, rollback. M1 solo se acepta con evidencia; no etiquetar push disponible.
6. M2 tras D6: proveedor push aislado, outbox/leases, permisos, lifecycle y pruebas de entrega; no WorkManager como reloj de emergencia. Checkpoint SC-006 y ensayo de fallos, estados providerAccepted/deviceReceived/userOpened separados.

UI/firmware y contenedores pueden avanzar después de contrato estable; cambios API/DB son secuenciales y coordinados. Este plan no solicita crear agentes ni multiplica ramas automáticamente.

## Enmienda constitucional propuesta

Añadir ámbito cloud-lab: XAMPP queda solo para legado, no requisito del release Kotlin; build→checks del HEAD→merge autorizado→deploy por workflow, sin editar servidor. Añadir tests de autorización/contrato y Android como gates de nueva feature; conservar seguridad, accesibilidad, simplicidad y contratos estables. Bump propuesto 1.1.0, aprobar antes de implementar; no declarar aprobada en esta entrega.

## Pruebas, límites y entrega

Cobertura dirigida: 100% de escenarios críticos enumerados (aislamiento, revocación, validación, stale, SOS, TLS, QR); no imponer porcentaje cosmético global. Unitarias para repositorio/ViewModel y transformaciones; integración PHP+MariaDB temporal; E2E acceso→perfil→vínculo→estado→QR; no borrar pruebas fallidas para obtener verde.
CI PR: secretos, formato/lint, estática/tipos, unitarias, migraciones/integración, OpenAPI, build web legado si se toca, build Android, firmware si se toca, auditoría de dependencias e imagen, git diff --check. Builds firmados solo en ambiente protegido; forks no reciben secretos. Vulnerabilidad explotable sin mitigación bloquea release; falsos positivos se documentan con evidencia y revisión.
Revisión de UI, seguridad y coherencia negocio/código antes de cerrar. Checks requeridos siempre del HEAD final; resolver fallos dentro de la misma entrega. Planificar ejecutar gates equivalentes localmente según quickstart; comandos futuros no se reportan como checks realizados.

Siempre: diff pequeño, verificar entradas/owner, seguir contrato, lockfiles, evidencia saneada y actualizar docs.
Consultar antes: ampliar alcance, usar datos reales, cambiar política QR/retención, infraestructura/DNS/credenciales, firma o publicar en tienda; migraciones destructivas.
Nunca: editar servidor a mano, secretos en git/chat, reset/clean destructivo, credenciales globales en APK, PII en logs, TLS sin validar, telemetría demo como real.

## Riesgos y mitigaciones

Firma no recuperable: mantener app anterior y acordar instalación separada, sin prometer actualización.
Latencia/conectividad: alertas físicas autónomas, edad visible y ensayo de cortes.
QR filtrado: revocación y minimización, no cache/indexación y consentimiento explícito.
Evento perdido/duplicado: transacción+outbox+leases/dedupe, ensayo crash durante envío; exactly-once en proveedor no se promete.
Dispositivo viejo: validar minSdk en equipo; no bajar seguridad para hacerlo compatible.
Deploy rollback: schema expand/contract y backup probado; no confundir volumen con copia.

## Ajuste UX/UI pedido por John — 2026-10-14

Implementación autorizada: reemplazar la lista que mezcla gestión y lecturas por tres destinos (Monitoreo, Perfiles, Historial), priorizar lectura/estado, integrar logo SmartVest, definir tipografía Material 3 y mejorar acceso/estados vacíos. Sistema en `.interface-design/system.md`; recorrido físico en `docs/testing/ANDROID_UX_2026-10-14.md`. Mantener contrato API, sesión, firmware, paquete/firma y límites M1. No declarar aceptación física por screenshots de emulador.
