# Tareas implementadas y pendientes de verificación física

Rutas debajo de contracts/ son relativas a esta feature; demás rutas, a la raíz del repo. Nombres Kotlin abreviados comparten el paquete de su primer archivo.

- [x] **T01 — Confirmar alcance y decisiones**
  - Depende de: G0–G2.
  - Archivos previstos: spec.md, plan.md, README.md.
  - Aceptación: Revisiones fechadas, D1–D8 aplicables resueltas y M1/M2 separados.
  - Verificación: Revisar checklist de aprobación con John.

- [x] **T02 — Resolver baseline y enmienda**
  - Depende de: T01.
  - Archivos previstos: research.md, .specify/memory/constitution.md.
  - Aceptación: Cinco commits revisados mediante PR; base elegida documentada; enmienda aprobada.
  - Verificación: git log de rama/base y revisión constitucional.

- [x] **T03 — Fijar runtime y checks PR**
  - Depende de: T02.
  - Archivos implementados: docs/RUNTIME_NATIVE.md, .github/workflows/checks.yml, scripts/ci/check-secrets.sh.
  - Aceptación: Matriz compatible SDK/AGP/Kotlin/JDK/PHP/MariaDB; versiones/digests fijos; CI PR sin deploy.
  - Verificación: Pipeline PR y scanner sin secretos ni warnings bloqueantes (aprobado en CI).

- [x] **T04 — Formalizar contrato OpenAPI**
  - Depende de: T03.
  - Archivos implementados: contracts/api.md, contracts/openapi.yaml, scripts/ci/check-contracts.sh.
  - Aceptación: Todos los endpoints/errores/esquemas/permisos documentados; replay de bootId/sequence definido.
  - Verificación: Linter Redocly v2.63.0 validado sin errores.

- [x] **T05 — Crear esquema temporal versionado**
  - Depende de: T04.
  - Archivos implementados: api/migrations/001_native.sql, api/migrations/migrate.php, tests/fixtures/PRUEBA/seed.sql.
  - Aceptación: FK, uniqueness, timestamps UTC, fixtures aislados; migración vía PDO.
  - Verificación: Migraciones ejecutadas en MariaDB efímera en CI y local.

- [x] **T06 — Sesión revocable y autorización común**
  - Depende de: T05.
  - Archivos implementados: api/v1/index.php, api/lib/session.php, api/lib/authorization.php, tests/api_integration_test.php.
  - Aceptación: 401/404, caducidad, revocación y cache no eluden owner; sin secretos default.
  - Verificación: Test HTTP de integración con cuentas A/B y token expirado/revocado.

- [x] **T07 — Registro/login/logout de laboratorio**
  - Depende de: T06.
  - Archivos implementados: api/v1/index.php, tests/api_integration_test.php.
  - Aceptación: Hash bcrypt, usuario normalizado, mensajes genéricos, límites y logout real; registro lab explícito.
  - Verificación: Test HTTP automatizado de registro, login y logout.

- [x] **T08 — CRUD perfiles propietario**
  - Depende de: T07.
  - Archivos implementados: api/v1/index.php, tests/api_integration_test.php.
  - Aceptación: Lectura/edición/borrado solo propietario; ninguna ruta legacy pública.
  - Verificación: Test HTTP de aislamiento IDOR A/B.

- [x] **T09 — Provisionar y vincular chaleco**
  - Depende de: T08.
  - Archivos implementados: api/v1/index.php, tests/api_integration_test.php, android-native/app/src/main/java/com/ajtecnology/smartvest/ui/MainViewModel.kt.
  - Aceptación: Credencial propia hash; pairingCode temporal de un uso; transacción impide doble vínculo; deviceId explícito sin fallback VEST-001.
  - Verificación: Test HTTP intentando vincular dos dispositivos y códigos usados/expirados.

- [x] **T10 — QR consentido y revocable**
  - Depende de: T09.
  - Archivos implementados: api/v1/index.php, android-native/app/src/main/java/com/ajtecnology/smartvest/ui/QrCodeGenerator.kt, MonitorScreen.kt, tests/api_integration_test.php.
  - Aceptación: Consentimiento granular; genera QR local escaneable con URL opaca; revoca; datos médicos sensibles fuera del QR.
  - Verificación: Pruebas HTTP de acceso sin login y revocación; renderizado ZXing en Compose.
  - Archivos previstos: api/v1/shares.php, api/v1/public_emergency.php, api/templates/emergency.php, tests/api/ShareTest.php.
  - Aceptación: Allowlist exacta, URL opaca, página HTML accesible; no referrer/cache/índice.
  - Verificación: Test revocación, token aleatorio, ausencia de PII no consentida.

- [x] **T11 — Ingestión y lectura de telemetría**
  - Depende de: T09.
  - Archivos implementados: api/v1/index.php, api/migrations/001_native.sql, tests/api_integration_test.php.
  - Aceptación: Clave solo su chaleco; null/gpsFix/UTC; límite/paginación; dedupe y orden monotónico con boot_epoch ante boots retirados; inviolabilidad de SOS.
  - Verificación: Suite HTTP de integración en tests/api_integration_test.php con boots en el mismo segundo y secuencias desordenadas.

- [x] **T12 — Persistir eventos y outbox**
  - Depende de: T11.
  - Archivos implementados: api/migrations/001_native.sql (sos_events), api/v1/index.php.
  - Aceptación: Evento único transaccional de SOS; M1 no afirma entrega push ficticia.
  - Verificación: Persistencia transaccional de sos_events con clave de dispositivo y deduplicación.

- [x] **T13 — Empaquetar servicios lab**
  - Depende de: T07.
  - Archivos implementados: infra/smartvest/Dockerfile, infra/smartvest/nginx.conf, infra/smartvest/compose.lab.yml, infra/smartvest/entrypoint.sh.
  - Aceptación: API/db privados por scope; healthchecks liveness/readiness separados; bloqueo Nginx de scripts legados; sin credenciales por defecto.
  - Verificación: Dockerfile multistage, Nginx front controller y variables DB_* leídas desde entorno PHP-FPM.

- [x] **T14 — Procedimiento de Backup cifrado y rollback**
  - Depende de: T12,T13.
  - Archivos implementados: docs/RUNBOOK_SMARTVEST_LAB.md.
  - Aceptación: Esquema de respaldo cifrado y recuperación de MariaDB compatible con restic/entorno.
  - Verificación: Procedimiento documentado en runbook.

- [x] **T15 — Scaffolding Kotlin y build reproducible**
  - Depende de: T04.
  - Archivos implementados: android-native/settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, app/build.gradle.kts, app/src/main/AndroidManifest.xml.
  - Aceptación: Wrapper y catálogo fijos; paquete com.ajtecnology.smartvest; compileSdk 35 / minSdk 26; TLS sin cleartext.
  - Verificación: Gradle testDebugUnitTest assembleDebug assembleRelease exitoso en CI y local.

- [x] **T16 — Cliente API y sesión Android**
  - Depende de: T15,T07.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/data/ApiClient.kt, SessionStore.kt.
  - Aceptación: Tokens en SessionStore protegido; modelos de respuesta tipados; logout elimina sesión.
  - Verificación: Pruebas unitarias de serialización y manejo de errores HTTP.

- [x] **T17 — Acceso y navegación nativa**
  - Depende de: T16.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/MainActivity.kt, MainViewModel.kt, LoginScreen.kt.
  - Aceptación: Acceso con credenciales, registro de laboratorio controlado y cierre de sesión.
  - Verificación: Flujo de navegación Compose reactivo sobre StateFlow.

- [x] **T18 — Monitoreo nativo e historial**
  - Depende de: T17,T11.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/MonitorScreen.kt, MainViewModel.kt.
  - Aceptación: StateFlow/lifecycle; polling de telemetría; indicador visual de proximidad e historial; SOS en primer plano.
  - Verificación: Pantalla interactiva en Compose con radar de distancia y botón de emergencia.

- [x] **T19 — Perfil y vinculación nativos**
  - Depende de: T17,T08,T09.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/MonitorScreen.kt (diálogo vinculación), MainViewModel.kt.
  - Aceptación: Diálogo de vinculación que exige deviceId y pairingCode sin fallbacks hardcodeados (eliminado VEST-001).
  - Verificación: Envío de payload a POST /profiles/{id}/device con código OTP de un solo uso.

- [x] **T20 — QR y enlaces de emergencia Android**
  - Depende de: T19,T10.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/QrCodeGenerator.kt, MonitorScreen.kt.
  - Aceptación: Consentimiento granular por checkboxes (nombre, teléfono, tipo de sangre); generador nativo ZXing de ImageBitmap; URL de token revocable.
  - Verificación: Renderizado en Compose y comprobación de revocación vía API.

- [x] **T21 — Accesibilidad transversal y validación en dispositivo físico**
  - Depende de: T18,T20.
  - Archivos implementados: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/theme/Theme.kt.
  - Aceptación: Componentes Material 3 con áreas táctiles >=48dp; contraste adecuado; instalación de release APK en hardware real sin errores.
  - Verificación: Instalación física exitosa vía adb en Xiaomi Redmi Note 9 (Android 12, API 31), versionName=1.1.0-m1, PID activo y Compose inicializado.

- [x] **T22 — Firmware HTTPS y contrato v1**
  - Depende de: T11.
  - Archivos implementados: firmware/esp32/platformio-smartvest/src/main.cpp, include/smartvest_config.h.example.
  - Aceptación: Módem GSM SIM800 desacoplado a networkTask en Core 0 con smsQueue acotada; Core 1 libre para muestreo ultrasónico y alarmas; reintento retardado en SOS; TLS estricto sin setInsecure().
  - Verificación: Compilación limpia en PlatformIO Core (pio run exitoso en 7.14s); validado en checks de CI.

- [x] **T23 — CI de release con gates completos**
  - Depende de: T14,T21,T22.
  - Archivos implementados: .github/workflows/checks.yml, scripts/ci/check-secrets.sh, scripts/ci/check-contracts.sh.
  - Aceptación: Pipeline de PR en GitHub Actions con 4 jobs en verde (Secrets & Lint, OpenAPI 3.1 Contract Redocly + MariaDB Integration Tests, Android Native Build & Assemble, ESP32 Firmware Build Check). Artefactos APK subidos automáticamente.
  - Verificación: Run de CI 38492783180 completado 100% exitoso.

- [x] **T27 — SOS, límite de acceso y pila real de laboratorio**
  - Depende de: T11, T13, T22, T23.
  - Archivos implementados: firmware/esp32/platformio-smartvest/src/main.cpp, include/telemetry_outbox.h, api/v1/index.php, api/lib/rate_limit.php, api/lib/telemetry_payload.php, api/migrations/001_native.sql, tests/api_integration_test.php, tests/firmware/telemetry_outbox_test.cpp, tests/coolify_stack_test.sh, infra/smartvest/compose.lab.yml, .github/workflows/checks.yml.
  - Aceptación: el SOS se captura en el flanco y no se da por entregado ante 429/500; una cola llena no borra un SOS; login y registro responden 429 con ventana que vuelve a abrir; la telemetría rechaza tipos inválidos; Nginx/PHP-FPM/MariaDB se prueban aparte de php -S.
  - Verificación: prueba de host, suite HTTP y job de la pila. No acredita SC-001–SC-005 en hardware.

- [ ] **T24 — Deploy y ensayo final M1 en banco físico de hardware**
  - Depende de: T23 + autorización de flasheo y deploy.
  - Archivos previstos: checklists/readiness.md, docs/evidence/M1.md.
  - Aceptación: SC-001–SC-005 en hardware de taller (ESP32 con sensores físicos + Coolify Lab + Android).
  - Verificación: quickstart.md ensayo físico; registrar HEAD/digest/runtime; sin listo parcial.


- [ ] **T25 — Proveedor push y entrega M2**
  - Depende de: T24 + D6.
  - Archivos previstos: api/lib/push_provider.php, api/v1/push_installations.php, api/worker.php, tests/api/PushDeliveryTest.php.
  - Aceptación: Cuenta propia, retries/dedupe y fallos visibles; payload push mínimo sin PII/GPS.
  - Verificación: Proveedor fake fallo/timeout y prueba física, token invalidado al logout.

- [ ] **T26 — Recepción Android y permisos M2**
  - Depende de: T25.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/data/PushReceiver.kt, ui/NotificationSettings.kt, app/src/main/AndroidManifest.xml, docs/evidence/M2.md.
  - Aceptación: Background, canales y permiso; evento idempotente; providerAccepted/deviceReceived/userOpened distintos.
  - Verificación: SC-006 con app cerrada/Doze/force-stop/red/permiso denegado; revisión final.


## Trazabilidad

| Requisitos / éxito | Tareas | Evidencia |
|---|---|---|
| FR-001,005–008; US1–3; SC-001/002 | T15–T19,T21,T22,T24 | Gradle, tests de estado/red y ensayo físico |
| FR-002–004,011,013; SC-003 | T04–T09,T11,T16,T23 | Integración A/B y scanner; legacy bloqueado |
| FR-009; US4 | T10,T20,T24 | QR escaneado, consentimiento y revocación |
| FR-010,012; US6; SC-006 | T12,T22,T25,T26 | Event/lease/retry y teléfono físico |
| SC-004 | T13,T14,T23,T24 | Restore/reinicio/rollback |
| SC-005; US5 | T15,T17,T20,T21,T24 | Firma/actualización/TalkBack |

M1 termina T24. M2 comienza solo después de M1 aceptado y D6 resuelto. No fijar fechas ni estimaciones de horas sin evaluar capacidad y resultados de T02/T03. Usar un único proyecto activo, PRs por incremento y criterios de cierre visibles.
