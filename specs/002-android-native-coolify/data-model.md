# Modelo de datos propuesto

El esquema actual mezcla cuenta y perfil en users; no migrar datos reales automáticamente. Migraciones SQL versionadas, aditivas y ensayadas sobre BD temporal. No copiar database.sql completo al VPS como inicializador: incluye semillas legadas.

| Entidad | Campos principales | Integridad y exposición |
|---|---|---|
| Account | id UUID, username único normalizado, password_hash, status, created_at | Solo servidor; password_hash nunca respuesta |
| Profile | id, owner_account_id FK, presentation_name, campos médicos/identidad opcionales, device_id nullable, updated_at | Owner obligatorio; ninguna lista pública; UUID no es autorización |
| Device | id, status, credential_hash, credential_version | Provisionado por operador; hash de credencial aleatoria, no secreto plaintext |
| DeviceAssignment | device_id único, profile_id único, owner_account_id, assigned_at | M1 un propietario/perfil por chaleco; FK y transacción evitan doble asignación |
| PairingCode | code_hash único, device_id, expires_at, used_at | Uso único, duración propuesta 10 min, intentos limitados; operador entrega fuera de logs |
| Session | token_hash único, account_id, issued_at, expires_at, last_seen_at, revoked_at | Token opaco de alta entropía; consultar estado revocado en cada operación |
| PublicShare | token_hash único, profile_id, allowed_fields, created_at, revoked_at | Consentimiento separado; URL opaca, no secretos ni PII en path |
| TelemetryState | device_id PK, distance_cm nullable, gps_fix, lat/lng nullable, battery nullable, sos_active, received_at, boot_id, sequence | Estado monotónico por boot/sequence; recibido por servidor UTC |
| TelemetryHistory | id, device_id, boot_id, sequence, mediciones, received_at | UNIQUE(device_id,boot_id,sequence) evita duplicados; índice (device_id,received_at) |
| SosEvent | id UUID, device_id, event_id único por device, received_at, status | eventId permanece igual mientras persiste la misma emergencia; ACK no limpia latch |
| Outbox M2 | id, event_id FK, recipient_account_id, status, attempts, next_attempt_at, lease_until | UNIQUE(event_id,recipient_account_id); claim atómico, retries y recuperación tras crash |
| PushInstallation M2 | id, account_id, token protegido, status, updated_at | Endpoint idempotente, invalidar al logout/cuenta retirada; token no en logs |

## Tiempo, validación y privacidad

BD/config en UTC; receivedAt asignado por servidor; cliente expone lastUpdate epoch ms. Invalid fix implica lat/lng null, incluso si firmware antiguo manda 0,0; nunca inferir ubicación real sin gpsFix explícito. Rangos: lat [-90,90], lng [-180,180], batería [0,100], distancia >=0 y <= rango físico validado del sensor. Tamaños máximos propuestos: texto 500 caracteres, solicitud 32 KiB; imágenes fuera de M1 para evitar base64 pesado.
El historial de telemetría no contiene perfil médico. Borrado de cuenta revoca sesiones, QR y push, desvincula dispositivo y aplica retención confirmada con limpieza verificable; no dejar referencias huérfanas. Cada consulta y cache key incluye propietario/alcance; una cache compartida nunca salta autorización.

## Migración y rollback

1. Inventariar esquema real sin extraer PII al chat; preparar esquema nuevo vacío.
2. Crear migración inicial y fixture claramente PRUEBA con dos cuentas y dos dispositivos.
3. Ensayar duplicados, FK, rollback sobre BD temporal y backup/restore independiente.
4. M1 no importa usuarios locales. Importación real requeriría autorización y plan propio.
5. Despliegue expand/contract: mantener compatibilidad con imagen anterior; cambios destructivos se difieren. Un rollback de imagen nunca restaura automáticamente una BD antigua.
