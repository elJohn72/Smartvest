# Especificación: Android nativo y backend independiente de la Mac

## Objetivo y usuarios

Reemplazar el cliente móvil por una app Android Kotlin con pantallas nativas, y alojar API/BD en el laboratorio Coolify. El cuidador administra un perfil y consulta su chaleco; la persona asistida recibe las alertas físicas del ESP32 aunque no haya red. El sistema es un prototipo de asistencia; no garantiza respuesta ante emergencias ni sustituye las ayudas existentes.

## Decisiones confirmadas y supuestos

Confirmado: Android exclusivamente con Kotlin; independencia operativa de la Mac; VPS de pruebas y Coolify; diseño previo mediante SDD.
Propuesto: Jetpack Compose; conservar PHP/MariaDB; API v1 separada del legado; primer hito con datos PRUEBA y un propietario por perfil/chaleco. Android minSdk 26 propuesto, pendiente verificar teléfono real. Versiones exactas SDK/AGP/Kotlin/JDK/PHP/MariaDB se fijarán tras comprobar compatibilidad, con locks y digest de imágenes; no hay versiones certificadas en esta spec.

## Alcance por hito

M1: login/logout, alta de cuenta de laboratorio, alta/edición de perfil, vinculación controlada de chaleco provisionado, QR revocable, estado/distancia/batería/GPS/historial, SOS visible en primer plano, API segura y despliegue reproducible. APK firmado e instalable.
M2: notificaciones push Android con proveedor propuesto FCM, worker duradero, reintentos y deduplicación. Proveedor/cuenta/costo/acceso pendientes de John. Sin M2 la app debe indicar que las alertas se consultan solo al abrirla.
Fuera de alcance: iOS, Kotlin Multiplatform, BLE, cambio a MQTT, cámara/visión IA, nuevo hardware, órdenes remotas hacia el ESP32, SMS desde servidor, tienda pública y datos personales reales. El SMS SIM800L existente sigue independiente y no acredita entrega en la API.

## Historias y aceptación

US1 P1 — Como cuidador accedo solo a mis datos. Dadas dos cuentas PRUEBA y dos chalecos, A no puede leer, escribir ni consultar historial de B; sin sesión recibe 401, recurso ajeno 404. Logout revoca la sesión en servidor.
US2 P1 — Como cuidador monitoreo datos reales. Con ESP32 emitiendo y Mac apagada, teléfono por red distinta muestra un nuevo punto e identifica su antigüedad. Una semilla PRUEBA nunca aparece como sensor físico.
US3 P1 — Como persona asistida conservo protección local. Cortar internet mantiene sensores, buzzer y vibrador; no bloquea su bucle por intentos de HTTP/TLS. App indica datos vencidos; no inventa GPS ni batería.
US4 P1 — Como cuidador comparto un QR limitado. Escanearlo sin instalar app abre una página HTTPS de emergencia con campos consentidos; nunca lista cuentas, dirección, cédula, foto, historial o ubicación. Revocar el enlace impide nuevas lecturas; el consentimiento puede retirarse.
US5 P1 — Como usuario con TalkBack completo acceso, perfil y monitoreo. Etiquetas, orden de foco, errores anunciados, tamaños táctiles >=48 dp y escala de texto 200% sin ocultar acciones esenciales; SOS tiene texto y no depende solo del color.
US6 P2/M2 — Como cuidador recibo una notificación con app en segundo plano. Evento único, usuario autorizado, permisos concedidos y red disponible: verificar recepción en teléfono físico. Bloqueo de permisos, force-stop, Doze, red caída o rechazo del proveedor tienen estado explícito; aceptación del proveedor no equivale a recepción ni lectura.

## Requisitos funcionales

- FR-001: UI Kotlin/Compose sin WebView para acceso, perfil, QR y monitoreo; el navegador externo es válido para QR público/mapa.
- FR-002: sesiones revocables, contraseña con hash y consultas siempre acotadas al propietario; nunca credencial maestra en APK.
- FR-003: vincular solo chalecos provisionados usando código de vinculación de un solo uso y corta duración; conocer deviceId no concede propiedad.
- FR-004: credencial de ingestión independiente por chaleco, revocable y separada de la sesión del cuidador.
- FR-005: datos desconocidos son null; GPS requiere gpsFix=true y coordenadas válidas. Timestamps del servidor en UTC y lastUpdate en milisegundos Unix.
- FR-006: consulta solo con pantalla visible; intervalo propuesto 2 s, 1 s durante SOS, una solicitud activa, timeout y backoff. Nunca polling perpetuo en background.
- FR-007: más de 30 s desde receivedAt implica VENCIDO; fallo de transporte implica SIN_CONEXIÓN; ausencia de estado implica SIN_DATOS. Mostrar hora de última lectura, sin insinuar seguridad por ausencia de SOS.
- FR-008: datos persistidos en servidor; móvil mantiene telemetría en memoria para M1. No cache médica persistente; tokens cifrados usando clave del Android Keystore, excluidos de backup, eliminados al salir.
- FR-009: QR usa token opaco >=128 bits, hash en BD, revocación y allowlist de campos consentidos. No JSON médico ni base64 en URL.
- FR-010: SOS remoto es una observación del botón físico; reconocer una notificación no limpia el latch del firmware. Sin comando de apagado del chaleco.
- FR-011: API limita tamaño y frecuencia, valida campos/rangos, devuelve errores estructurados y no registra PII, secretos ni payloads médicos.
- FR-012/M2: persistir evento y outbox en transacción, deduplicar por eventId; reintentos finitos con backoff y estado fallido observable.
- FR-013: el despliegue lab bloquea legacy API insegura y todos los flujos register_lab/delete_lab antiguos. Registro v1 lab es explícito, acotado y rate-limited; no importa usuarios de XAMPP.

## Requisitos operativos y éxito

SC-001: ejecutar US1–US5 en Android físico con Mac apagada durante 30 min y redes independientes; guardar evidencia saneada.
SC-002: 20 muestras físicas, red disponible: 95% visibles <=10 s desde envío, sin UI bloqueada; registrar condiciones y tiempos, objetivo propuesto, no SLA garantizado.
SC-003: negar 100% de casos de acceso ajeno, anónimo a privados, credencial dispositivo cruzada, sesión expirada/revocada, QR revocado, certificado inválido y datos fuera de rango del conjunto de pruebas.
SC-004: reiniciar contenedores preserva datos; restaurar backup cifrado en BD aparte y comparar conteos/relaciones; rollback de imagen no destruye schema compatible.
SC-005: APK actualizado conserva identidad de paquete y firma verificada; minSdk validado en teléfono. Sin firma original, no afirmar actualización compatible.
SC-006/M2: probar SOS, evento duplicado, worker reiniciado y notificaciones denegadas; un único evento genera una entrega lógica por destinatario y estados auditables. No prometer recepción garantizada.

## Decisiones y validación de compuertas (M1 validado 2026-10-13)

- **G0–G2**: Validados y autorizados por John para ejecución autónoma de M1.
- **D1 (Dirección HTTPS)**: John, 2026-10-14, no compra dominio. El laboratorio usa `https://smartvest.179.236.74.107.sslip.io` (el nombre incluye la IP del VPS y Let's Encrypt lo emite Coolify/Traefik). `smartvest-lab.ajenzasas.com` no está registrado y no se usa. Si cambia la IP del VPS, hay que cambiar este nombre.
- **D2 (Acceso Coolify)**: Proyecto `SMARTVEST`, entorno `lab`, redes y volúmenes aislados; deploy vía GitHub Actions / webhook autenticado; panel por túnel SSH.
- **D3 (Firma APK)**: Mantener `com.ajtecnology.smartvest`, firmado con el keystore debug existente para actualización compatible sin desinstalar. La instalación de laboratorio del 2026-10-14 quedó en versionCode 26, versionName `1.1.1-m1`; la anterior compatible fue la 25. El candidato de interfaz en el emulador es versionCode 31 / `1.2.4-m1-ui`; el teléfono físico no recibió esa actualización en esta pasada.
- **D4 (Campos públicos QR)**: Allowlist consentida: únicamente nombre de presentación, teléfono de emergencia y tipo de sangre (opcional). Bloqueo total de PII/GPS en público.
- **D5 (Modelo M1)**: Propietario único por perfil y por chaleco en M1.
- **D6 (Proveedor M2)**: Diferido para M2 (fuera de M1).
- **D7 (Retención)**: 7 días para historial de telemetría, 30 días para eventos/logs saneados, backups cifrados (7 diarios + 4 semanales).
- **D8 (Enmienda constitucional)**: Aprobada versión 1.1.0 incorporando entorno cloud-lab Coolify, Android nativo Compose y checks automatizados de PR.
