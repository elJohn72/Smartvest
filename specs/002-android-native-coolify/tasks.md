# Tareas propuestas — no ejecutadas

Todas las casillas permanecen abiertas. Cada unidad incluye aceptación y verificación. Rutas debajo de contracts/ son relativas a esta feature; demás rutas, a la raíz del repo. Nombres Kotlin abreviados comparten el paquete de su primer archivo. Los directorios de scaffolding se dividen en sublotes de hasta cinco archivos sin ampliar el alcance.

- [ ] **T01 — Confirmar alcance y decisiones**
  - Depende de: G0–G2.
  - Archivos previstos: spec.md, plan.md, README.md.
  - Aceptación: Revisiones fechadas, D1–D8 aplicables resueltas y M1/M2 separados.
  - Verificación: Revisar checklist de aprobación con John.

- [ ] **T02 — Resolver baseline y enmienda**
  - Depende de: T01.
  - Archivos previstos: research.md, .specify/memory/constitution.md.
  - Aceptación: Cinco commits revisados mediante PR; base elegida documentada; enmienda aprobada.
  - Verificación: git log de rama/base y revisión constitucional.

- [ ] **T03 — Fijar runtime y checks PR**
  - Depende de: T02.
  - Archivos previstos: docs/RUNTIME_NATIVE.md, .github/workflows/checks.yml, scripts/ci/check-secrets.sh.
  - Aceptación: Matriz compatible SDK/AGP/Kotlin/JDK/PHP/MariaDB; versiones/digests fijos; CI PR sin deploy.
  - Verificación: Pipeline PR y scanner sin secretos ni warnings bloqueantes.

- [ ] **T04 — Formalizar contrato OpenAPI**
  - Depende de: T03.
  - Archivos previstos: contracts/api.md, contracts/openapi.yaml, scripts/ci/check-contracts.sh.
  - Aceptación: Todos los endpoints/errores/esquemas/permisos documentados; replay de bootId/sequence definido.
  - Verificación: Linter OpenAPI y trazabilidad FR-001–013.

- [ ] **T05 — Crear esquema temporal versionado**
  - Depende de: T04.
  - Archivos previstos: api/migrations/001_native.sql, tests/fixtures/PRUEBA/seed.sql, scripts/ci/test-migrations.sh, tests/api/MigrationTest.php.
  - Aceptación: FK, uniqueness, timestamps UTC, fixtures aislados; migración/reversión no destruye legado.
  - Verificación: Migración y rollback en BD efímera con duplicados/FK.

- [ ] **T06 — Sesión revocable y autorización común**
  - Depende de: T05.
  - Archivos previstos: api/v1/index.php, api/lib/session.php, api/lib/authorization.php, tests/api/AuthorizationTest.php.
  - Aceptación: 401/404, caducidad, revocación y cache no eluden owner; sin secretos default.
  - Verificación: Test integración con cuentas A/B y token expirado/revocado.

- [ ] **T07 — Registro/login/logout de laboratorio**
  - Depende de: T06.
  - Archivos previstos: api/v1/auth.php, api/lib/rate_limit.php, tests/api/AuthTest.php, scripts/ci/test-api.sh.
  - Aceptación: Hash, usuario normalizado, mensajes genéricos, límites y logout real; registro lab explícito.
  - Verificación: PHPUnit: doble registro, brute-force, payload sobredimensionado.

- [ ] **T08 — CRUD perfiles propietario**
  - Depende de: T07.
  - Archivos previstos: api/v1/profiles.php, api/lib/profile_validation.php, tests/api/ProfileTest.php.
  - Aceptación: Lectura/edición/borrado solo propietario; ninguna ruta legacy pública.
  - Verificación: PHPUnit: A/B, campos protegidos, revoke/desvinculación al borrar.

- [ ] **T09 — Provisionar y vincular chaleco**
  - Depende de: T08.
  - Archivos previstos: scripts/provision-lab-device.php, api/v1/device_assignment.php, tests/api/PairingTest.php.
  - Aceptación: Credencial propia hash; pairingCode temporal de un uso; transacción impide doble vínculo.
  - Verificación: Intentar asignación concurrente, código usado/expirado y deviceId adivinado.

- [ ] **T10 — QR consentido y revocable**
  - Depende de: T09.
  - Archivos previstos: api/v1/shares.php, api/v1/public_emergency.php, api/templates/emergency.php, tests/api/ShareTest.php.
  - Aceptación: Allowlist exacta, URL opaca, página HTML accesible; no referrer/cache/índice.
  - Verificación: Test revocación, token aleatorio, ausencia de PII no consentida.

- [ ] **T11 — Ingestión y lectura de telemetría**
  - Depende de: T09.
  - Archivos previstos: api/v1/telemetry.php, api/lib/telemetry_validation.php, tests/api/TelemetryTest.php.
  - Aceptación: Clave solo su chaleco; null/gpsFix/UTC; límite/paginación; dedupe/orden por boot/sequence.
  - Verificación: Payload inválido, replay, boot viejo, key cruzada, concurrencia.

- [ ] **T12 — Persistir eventos y job maintenance**
  - Depende de: T11.
  - Archivos previstos: api/lib/outbox.php, api/worker.php, tests/api/OutboxTest.php.
  - Aceptación: Evento único/transaccional, leases y retries finitos; job M1 no afirma entrega push.
  - Verificación: Crash worker, dos consumidores, retry y cleanup por retención.

- [ ] **T13 — Empaquetar servicios lab**
  - Depende de: T07.
  - Archivos previstos: infra/smartvest/Dockerfile, infra/smartvest/compose.lab.yml, infra/smartvest/.env.example, scripts/ci/smoke-lab.sh.
  - Aceptación: API/db/worker privados por scope; healthchecks y volúmenes; env ejemplo sin valores secretos; no mounts Mac.
  - Verificación: compose config --quiet, build/smoke, arranque sin secreto falla cerrado.

- [ ] **T14 — Backup cifrado y rollback**
  - Depende de: T12,T13.
  - Archivos previstos: scripts/lab-backup.sh, scripts/lab-restore.sh, docs/RUNBOOK_SMARTVEST_LAB.md.
  - Aceptación: Backups verificados en destino aprobado; restore a BD aparte; digest anterior/schema compatible.
  - Verificación: Ensayo restore y reinicio sin pérdida; evidencia de conteos y logs saneados.

- [ ] **T15 — Scaffolding Kotlin y build reproducible**
  - Depende de: T04.
  - Archivos previstos: android-native/settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, app/build.gradle.kts, app/src/main/AndroidManifest.xml.
  - Aceptación: Wrapper y catálogo fijos; paquete/firma/minSdk definidos; TLS release sin cleartext.
  - Verificación: Gradle assembleDebug/lint; subdividir scaffolding en lotes <=5 archivos.

- [ ] **T16 — Cliente API y sesión Android**
  - Depende de: T15,T07.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/data/ApiClient.kt, SessionStore.kt, AuthRepository.kt, android-native/app/src/test/AuthRepositoryTest.kt.
  - Aceptación: Tokens cifrados con Keystore, fuera de backup; timeout/error tipado y logout borra token.
  - Verificación: MockWebServer: 401, 429, timeout; prueba dispositivo de storage/backup.

- [ ] **T17 — Acceso y navegación nativa**
  - Depende de: T16.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/LoginScreen.kt, LoginViewModel.kt, AppNavigation.kt, android-native/app/src/androidTest/LoginFlowTest.kt.
  - Aceptación: Acceso/reautenticación/logout; strings y semántica TalkBack, UI sin networking.
  - Verificación: Test navegación, error validación, recomposición y process death.

- [ ] **T18 — Monitoreo nativo e historial**
  - Depende de: T17,T11.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/data/TelemetryRepository.kt, ui/MonitorViewModel.kt, MonitorScreen.kt, android-native/app/src/test/MonitorViewModelTest.kt.
  - Aceptación: StateFlow/lifecycle; NoData, Vencido, SinConexión; polling único; SOS sin depender de color.
  - Verificación: Reloj fake: >30 s, timeout, reconexión; cancelación al background y GPS null.

- [ ] **T19 — Perfil y vinculación nativos**
  - Depende de: T17,T08,T09.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/data/ProfileRepository.kt, ui/ProfileViewModel.kt, ProfileScreen.kt, android-native/app/src/androidTest/ProfileFlowTest.kt.
  - Aceptación: CRUD y pairingCode nativos; errores sin inventar valores obligatorios; strings.xml actualizado.
  - Verificación: E2E registro/vínculo/edición/borrado; subdividir strings si excede 5 archivos.

- [ ] **T20 — QR y enlaces de emergencia Android**
  - Depende de: T19,T10.
  - Archivos previstos: android-native/app/src/main/java/com/ajtecnology/smartvest/ui/ShareScreen.kt, ShareViewModel.kt, android-native/app/src/androidTest/ShareFlowTest.kt.
  - Aceptación: Consentimiento granular; genera QR del token, revoca; enlaces externos restringidos.
  - Verificación: Escaneo sin app instalada, revocación y TalkBack.

- [ ] **T21 — Accesibilidad transversal y update firmado**
  - Depende de: T18,T20.
  - Archivos previstos: android-native/app/src/androidTest/AccessibilityFlowTest.kt, docs/ANDROID_RELEASE.md.
  - Aceptación: TalkBack y 200%, táctil >=48dp; firma compatible sin desinstalar; permisos mínimos.
  - Verificación: Prueba física y APK update; no certificar firma por nombre de paquete.

- [ ] **T22 — Firmware HTTPS y contrato v1**
  - Depende de: T11.
  - Archivos previstos: firmware/esp32/platformio-smartvest/src/main.cpp, include/smartvest_config.h.example, README.md, docs/FIRMWARE_TLS_TEST.md.
  - Aceptación: CA/NTP, gpsFix/null, eventId/sequence/boot; red no bloquea sensores; config privada intacta.
  - Verificación: pio run; sensor físico con TLS inválido/red caída y reintentos.

- [ ] **T23 — CI de release con gates completos**
  - Depende de: T14,T21,T22.
  - Archivos previstos: scripts/ci/audit-dependencies.sh, .github/workflows/checks.yml, .github/workflows/release-lab.yml, docs/RUNBOOK_SMARTVEST_LAB.md.
  - Aceptación: Imagen/APK solo desde commit verde; permisos mínimos y actions SHA; signing/environment protegidos; autodeploy libre apagado.
  - Verificación: PR HEAD exacto, imagen digest, artefactos, auditoría y flujo sin Mac.

- [ ] **T24 — Deploy y ensayo M1**
  - Depende de: T23 + autorización deploy.
  - Archivos previstos: checklists/readiness.md, docs/evidence/M1.md.
  - Aceptación: SC-001–005 y negativos verdes; Mac apagada/redes distintas; scope/rollback/restore.
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
