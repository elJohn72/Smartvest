# Investigación y decisiones técnicas

## Evidencia local del 2026-10-13

Repo real: ~/Repositorios Githup/04-robotica/Smartvest; la ficha AJENZA aún apunta a 03-proyectos (ruta desactualizada).
Base documental origin/main: d888d1f4a37481893c8de8c92224076f73b7f639.
Rama observada codex/fix-smartvest-android-register-validation HEAD 97ba2a6: cinco commits no fusionados (eabf174, ba2a3c9, 7a6dfd6, 9ef8967, 97ba2a6). Su mobile/App.tsx usa WebView y mobile/README.md describe APK versión 24. main conserva el cliente Expo previo. Antes de implementar se revisa y resuelve esa divergencia mediante PR; no se elimina trabajo ni se da por desplegado.

- api/users.php: lecturas de perfiles/listados sin autorización; token de login no protege todas las rutas.
- api/lib/auth_token.php: fallback del secreto derivado de clave IoT local; sin revocación de sesión persistente.
- api/lib/job_queue.php: cola /tmp/smartvest_queue con permisos abiertos; no adecuada para contenedores independientes ni eventos duraderos.
- api/worker.php: notify_sos escribe log; no entrega push.
- Firmware publica mediante HTTPClient y clave compartida configurada localmente; el payload HTTP usa 0,0 sin fix y no incluye gpsFix. Requiere contrato y TLS explícitos.
- .github/workflows/deploy.yml: Node 18, npm ci/build y GitHub Pages al push main; no checks PR, Kotlin, PHP ni despliegue API. GitHub Pages no ejecuta PHP.
- Constitución 1.0.0 presupone XAMPP y pruebas manuales; precisa enmienda para la feature.
- Ficha del VPS lab: 2 vCPU, 8 GB RAM, Coolify. Cierre de seguridad 2026-10-12: administración solo túnel SSH, no puerto 8000 público. Es evidencia documental, no comprobación remota actual. No aplicar comandos de la ficha antigua que crean código manualmente en el servidor.

## Decisiones propuestas

ADR-01: Kotlin/Compose, una Activity y un módulo app con paquetes ui/data/core. ViewModel + StateFlow, flujo unidireccional, inyección por constructor; sin capas de dominio vacías ni frameworks innecesarios.
ADR-02: PHP y MariaDB conservados, nueva API v1; adaptar endpoint de telemetría heredado únicamente si puede aplicar exactamente las mismas garantías. Backend Kotlin no aporta a esta migración y se difiere.
ADR-03: sesiones opacas revocables (hash en BD), caducidad propuesta 12 h absoluta/30 min inactividad, sin refresh M1. Reautenticar al expirar. Credencial IoT nunca es sesión móvil.
ADR-04: UI y API aisladas; QR público tiene página mínima propia y no depende de instalación Android. Legacy React/XAMPP se conserva localmente como referencia hasta paridad; no se sirve con API insegura en VPS.
ADR-05: eventos/outbox en MariaDB con transacciones y leases; sin Redis obligatorio. Worker M1 solo registra/procesa mantenimiento; push queda M2.
ADR-06: GitHub Actions construye imagen probada y APK; Coolify despliega digest aprobado desde main después de checks del HEAD final. Desactivar autodeploy indiscriminado por webhook; credential Coolify en environment protegido, nunca repositorio.
ADR-07: TLS con CA válida y sincronización horaria ESP32; prohibido setInsecure. Timeouts y trabajo de red aislado de alertas locales; limitar RAM y cola de reintentos.
ADR-08: pruebas reales de autorización y contrato primero; complejidad adicional requiere caso de uso y actualización de spec. APK conserva com.ajtecnology.smartvest si firma compatible; ruta propuesta android-native/ no sobrescribe mobile/.

## Fuentes primarias consultadas

- [Arquitectura Android](https://developer.android.com/topic/architecture/recommendations): capas UI/data, repositorios, ViewModels y lifecycle.
- [Accesibilidad Compose](https://developer.android.com/develop/ui/compose/accessibility).
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore): protege claves criptográficas; no almacena directamente tokens.
- [Coolify Compose](https://coolify.io/docs/applications/choose-deployment-method).
- [Almacenamiento persistente](https://coolify.io/docs/applications/configuration/persistent-storage): volúmenes definidos en Compose y locales al servidor, no constituyen backup.
- [Coolify/GitHub Actions](https://coolify.io/docs/applications/sources/github/actions).

Versiones concretas y capacidades se validan nuevamente al implementar; no copiar las versiones del prototipo Expo a Kotlin.
