# Contrato API v1 propuesto (no implementado)

Prefijo /api/v1. Origen HTTPS real PENDIENTE; no inventar dominio. JSON camelCase UTF-8. Todos los endpoints privados exigen Authorization: Bearer <sesión opaca>; autorización se consulta en servidor antes de cache/BD. Claves IoT solo en X-SmartVest-Api-Key, nunca querystring.

## Operaciones

| Método y ruta | Autorización | Request / respuesta principal |
|---|---|---|
| GET /health/live | Pública sin detalles internos | 200 {status:"ok"}; no versión/credenciales |
| GET /health/ready | Interna | 200 si BD/migración compatibles; 503 en fallo |
| POST /auth/register | Lab acotado y rate-limit | username,password; 201 cuenta PRUEBA; 409 duplicado; deshabilitable |
| POST /auth/login | Pública rate-limit | username,password; 200 token,expiresAt; 401 genérico |
| POST /auth/logout | Sesión | revoca token, 204; reintento de token ya revocado recibe 401 |
| GET /me | Sesión | id,username; nunca hash/token |
| POST /profiles | Sesión | presentationName y opcionales; 201 perfil propio |
| GET /profiles/{id} | Owner | perfil privado; ajeno/inexistente 404 |
| PATCH /profiles/{id} | Owner | campos permitidos; 200 perfil; owner/device no editables aquí |
| DELETE /profiles/{id} | Owner | revoca shares, desvincula dispositivo; 204 |
| POST /profiles/{id}/device | Owner + pairingCode | vínculo transaccional; 200 deviceId; 409 dispositivo asignado; inválido 400 genérico |
| POST /profiles/{id}/shares | Owner + consentimiento | allowedFields; 201 shareId,token (solo una vez); vacío o campos no permitidos 422 |
| DELETE /profiles/{id}/shares/{shareId} | Owner | revoca; 204 |
| GET /public/emergency/{token} | Token QR | solo campos consentidos; revocado/inválido 404; no cache ni referrer ni indexación |
| GET /emergency/{token} | Token QR | HTML accesible mínimo desde misma allowlist; sin scripts terceros ni métricas |
| GET /devices/{deviceId}/state | Owner de asignación | 200 data o null si nunca recibido |
| GET /devices/{deviceId}/history?limit=20&cursor=... | Owner | limit 1..120; cursor opaco estable, data y nextCursor |
| POST /devices/{deviceId}/telemetry | Clave del mismo device | ver payload; 202 persistido; duplicado 200 con duplicate=true |
| PUT /push/installations/{installationId} M2 | Sesión | providerToken; registro idempotente y asociado a cuenta |
| DELETE /push/installations/{installationId} M2 | Owner | deshabilita; 204 |

Los id de recurso son identificadores técnicos, nunca secretos. Aprovisionamiento de Device/PairingCode es comando administrativo interno, no endpoint de alta libre. Un deviceId en el cuerpo que difiera del path se rechaza.

## Telemetría y SOS

Payload requerido: deviceId string, bootId UUID técnico, sequence entero >=0, distanceCm number|null, gpsFix boolean, latitude number|null, longitude number|null, batteryLevel integer|null, sosActive boolean, sosEventId UUID|null.
Si gpsFix=false, ambas coordenadas deben ser null. Si sosActive=true, sosEventId es obligatorio; persiste durante el latch y se renueva únicamente al nuevo evento físico. No reducir SOS a una transición perdida entre polls. Evento queda persistido para M2 aunque la telemetría posterior tenga sosActive=false. API no permite ACK apagar chaleco.
Respuesta state: {data:{deviceId,distanceCm,gpsFix,latitude,longitude,batteryLevel,sosActive,lastUpdate,bootId,sequence}}. lastUpdate es receivedAt UTC epoch ms de servidor, no reloj del sensor. Datos recibidos fuera de orden no reemplazan estado reciente. Validar reinicio bootId sin permitir replays antiguos: mantener sesión de boot registrada y secuencias aceptadas; fixture de replay obligatorio.

## Semántica y seguridad

Errors: {error:{code,message,requestId}}; códigos 400 JSON inválido, 401 sin credencial válida, 404 ajeno/inexistente, 409 conflicto, 413 tamaño, 422 campos, 429 límite (Retry-After), 500 error genérico/503 indisponible. No stack traces ni SQL. Private/public emergency: Cache-Control no-store; Referrer-Policy no-referrer; X-Robots-Tag noindex,nofollow. No CORS wildcard para rutas privadas.
Login propuesto 5 fallos/15 min por IP+username y por cuenta, sin lockout indefinido; ingestión admite 1 Hz por chaleco con burst pequeño para SOS (tuning en test). TLS obligatorio, sin redirect que pueda reenviar Authorization a otro host.
Contrato se formalizará en OpenAPI 3.1 durante T04, antes de código API/Android; esa tarea debe cubrir todos los endpoints y validarse con linter. Esta tabla no se presenta como contrato machine-validated.

## Compatibilidad

Endpoints legacy /api/users.php, /api/dashboard.php, /api/iot.php, /api/address.php no se publican sin equivalente autorización. Firmware se migra primero en laboratorio con credencial aislada. No incluir handler general de upsert legado ni register_lab/delete_lab antiguos. QR existentes ?uid= / ?data= no migran automáticamente: hay riesgo de PII y deben regenerarse con consentimiento.
