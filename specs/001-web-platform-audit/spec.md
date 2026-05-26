# Feature Specification: Auditoría integral plataforma web SmartVest

**Feature Branch**: `001-web-platform-audit`  
**Created**: 2026-05-26  
**Status**: Completed (audit phase)  
**Input**: Auditoría total de la página web local en `http://localhost/Smartvest/` con skills de [skills.sh](https://skills.sh/) y flujo [GitHub Spec Kit](https://github.com/github/spec-kit).

## Executive Summary

| Área | Puntuación estimada | Grado |
|------|---------------------|-------|
| Seguridad / API | 25/100 | F |
| Privacidad de datos médicos | 35/100 | F |
| Accesibilidad (WCAG) | 55/100 | D |
| UX / UI (Vercel Guidelines) | 62/100 | D |
| SEO técnico | 40/100 | F |
| Rendimiento | 70/100 | C |
| Funcionalidad IoT integrada | 50/100 | D |
| DevOps / despliegue | 55/100 | D |
| **Global ponderado** | **48/100** | **F** |

**Alcance auditado**: frontend React (build en XAMPP), APIs `api/users.php` y `api/iot.php`, MariaDB `smartvest`, manifest PWA, flujos landing → login → registro → perfil → QR.

**Herramientas usadas**:
- Skills instalados: `audit-website`, `webapp-testing`, `web-design-guidelines`, `seo-audit` ([skills.sh](https://skills.sh/))
- Navegador (snapshot visual + árbol de accesibilidad)
- `curl` contra APIs y cabeceras HTTP
- Revisión estática de código TS/TSX/PHP
- Spec Kit inicializado (`specify init --integration cursor-agent`)

**No ejecutado**: `squirrel audit` (CLI squirrelscan no instaló correctamente en este entorno; reintentar con instalación manual desde [squirrelscan.com/download](https://squirrelscan.com/download)).

---

## User Scenarios & Testing *(audit findings as requirements gaps)*

### User Story 1 - Acceso seguro (Priority: P1)

Usuario inicia sesión sin exponer credenciales ni datos médicos a terceros en la red local.

**Why this priority**: Riesgo crítico actual; datos de salud y contraseñas expuestos.

**Independent Test**: `curl http://localhost/Smartvest/api/users.php` no debe devolver campo `password`.

**Findings**:
- Contraseñas almacenadas en texto plano en MariaDB.
- `GET /api/users.php` lista todos los usuarios **con contraseñas** sin autenticación.
- `POST` upsert acepta escritura de usuarios sin token/sesión.
- Login compara contraseña en claro (`users.php:52`).

**Acceptance Criteria** (target state):

1. **Given** API pública, **When** GET users sin sesión admin, **Then** respuesta excluye `password` y datos sensibles innecesarios.
2. **Given** registro/login, **When** se guarda contraseña, **Then** solo hash bcrypt/argon2 en BD.
3. **Given** escritura en API, **When** no hay sesión válida, **Then** HTTP 401 en POST/PUT excepto login/registro acotados.

---

### User Story 2 - Monitoreo IoT confiable (Priority: P1)

Cuidador abre perfil vinculado al chaleco y ve telemetría coherente (sin SOS falso permanente).

**Why this priority**: Bloquea uso real del producto (overlay SOS).

**Independent Test**: Perfil con `deviceId=VEST-001` muestra SOS solo si `sos_active=1` en BD refleja emergencia real.

**Findings**:
- BD: `VEST-001` tiene `sos_active=1`, GPS `0,0`, batería `0`.
- Fila demo corrupta: `device_id = 'VEST-DEMO esta '` (15 chars) → API `VEST-DEMO` retorna `null`.
- `POST api/iot.php` sin autenticación (cualquiera puede falsificar telemetría).
- Perfil usa fallback `VEST-DEMO` si usuario sin `deviceId`.

**Acceptance Criteria**:

1. **Given** dispositivo en reposo, **When** se consulta IoT, **Then** `sosActive` es false.
2. **Given** `deviceId` en perfil, **When** API no tiene fila, **Then** UI muestra estado “sin señal”, no datos demo engañosos.
3. **Given** firmware POST, **When** payload válido, **Then** requiere clave/dispositivo registrado.

---

### User Story 3 - Accesibilidad y UX de emergencia (Priority: P2)

Persona con discapacidad visual o cuidador usa la app con lector de pantalla y contraste legible.

**Findings** (Vercel Web Interface Guidelines + snapshot):
- `Input.tsx:11` — `outline-none` sin `:focus-visible` dedicado (anti-pattern).
- `Button.tsx:17` — `focus:outline-none` (mitigado parcialmente con ring).
- Labels en `Input` sin `htmlFor` / `id` enlazados.
- Botones solo icono (`Home`, `UserProfile`, `QRCodeView`) sin `aria-label`.
- Sin `aria-live` en errores de login/registro.
- Sin skip link a contenido principal.
- Animaciones (`animate-pulse`, `fadeIn`) sin `prefers-reduced-motion`.
- Uso de `alert()` para feedback (`Home`, `RegistrationForm`) — no accesible.
- Landing: texto hero `text-gray-300` sobre fondo oscuro — riesgo de contraste en viewports reducidos (ver captura auditoría).

**Acceptance Criteria**:

1. **Given** navegación por teclado, **When** tab en controles, **Then** foco visible en todos los interactivos.
2. **Given** lector de pantalla, **When** error de login, **Then** anuncio en región `aria-live`.

---

### User Story 4 - SEO y descubrimiento (Priority: P3)

Página indexable con metadatos básicos si se publica en Internet.

**Findings** (seo-audit framework):
- Sin `<meta name="description">`.
- Sin Open Graph / Twitter Cards.
- Sin `canonical`.
- Sin `robots.txt` ni `sitemap.xml` en el proyecto.
- Título genérico: “SmartVest Registro”.
- Sin JSON-LD (MedicalWebPage / WebApplication).
- Manifest PWA con iconos en CDN externo (flaticon) — dependencia externa.

**Acceptance Criteria**:

1. **Given** HTML servido, **When** se inspecciona `<head>`, **Then** description + og:title + og:description presentes.
2. **Given** despliegue público, **When** crawlers visitan `/Smartvest/`, **Then** `robots.txt` y sitemap referencian rutas válidas.

---

## Functional Requirements

### Security

- **FR-001**: API MUST NOT return password hashes or plaintext passwords on GET endpoints.
- **FR-002**: Passwords MUST be hashed before persistence (PHP `password_hash` / `password_verify`).
- **FR-003**: Mutating API endpoints MUST require authentication or device-scoped API keys.
- **FR-004**: Gemini `API_KEY` MUST NOT be embedded in client bundle; address validation MUST run server-side or via proxy.
- **FR-005**: HTTP responses SHOULD include security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`; CSP cuando sea viable).

### Privacy

- **FR-006**: Medical profile QR/links MUST minimize sensitive fields (evaluar qué va en `?data=` vs solo `?uid=`).
- **FR-007**: `localStorage` fallback MUST NOT persist passwords post-login.

### IoT

- **FR-008**: `iot_states.device_id` MUST match firmware and user `deviceId` exactly.
- **FR-009**: Demo seed data MUST use canonical id `VEST-DEMO` without typos.

### UX / A11y

- **FR-010**: Form controls MUST associate label via `htmlFor`/`id`.
- **FR-011**: Icon-only buttons MUST have `aria-label`.
- **FR-012**: User feedback MUST use inline UI, not `alert()`.

### SEO

- **FR-013**: `index.html` MUST include meta description and social preview tags.
- **FR-014**: Static `robots.txt` and `sitemap.xml` SHOULD ship with deploy script.

### DevOps

- **FR-015**: `scripts/deploy-xampp.sh` MUST point to this repository root.
- **FR-016**: Post-frontend changes: `npm run build` THEN deploy (sequential).

---

## Key Entities

- **User**: perfil médico + credenciales + `deviceId`
- **IotState**: telemetría por `device_id`
- **AuditFinding**: id, severity, category, file, recommendation, status

---

## Success Criteria

- **SC-001**: Zero critical security findings open (password exposure, open write API).
- **SC-002**: Global audit score ≥ 85 after remediation (re-audit with squirrel + checklist).
- **SC-003**: Login → profile flow usable without false SOS for linked device.
- **SC-004**: WCAG 2.1 AA contrast on primary text/buttons on landing and forms.

---

## Assumptions

- Entorno principal: XAMPP local `/Smartvest/`.
- App no está expuesta a Internet en esta auditoría; riesgos de red local siguen siendo válidos en LAN.
- Usuario de prueba existente con `deviceId=VEST-001`.

## Out of Scope (this audit)

- Auditoría de firmware ESP32 (ver `firmware/esp32/platformio-smartvest/`).
- Pentest externo / OWASP ZAP automatizado.
- Cumplimiento legal HIPAA/LOPD exhaustivo (solo señalado como gap).
