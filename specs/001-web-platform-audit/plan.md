# Implementation Plan: Remediación post-auditoría SmartVest Web

**Branch**: `001-web-platform-audit` | **Date**: 2026-05-26 | **Spec**: [spec.md](./spec.md)

## Summary

Remediar hallazgos de la auditoría integral priorizando seguridad de API/datos médicos, fiabilidad IoT y accesibilidad. El frontend React + PHP API en XAMPP permanece como stack; los cambios son incrementales y alineados con la constitución del proyecto.

## Technical Context

**Language/Version**: TypeScript 5.7, React 19, PHP 8.2, MariaDB  
**Primary Dependencies**: Vite 6, Tailwind 3, PDO, lucide-react, qrcode.react  
**Storage**: MariaDB (`users`, `iot_states`) + `localStorage` fallback  
**Testing**: No runner configurado (gap — añadir smoke tests manuales documentados)  
**Target Platform**: XAMPP macOS, base path `/Smartvest/`  
**Project Type**: SPA + PHP JSON API  
**Performance Goals**: LCP < 2.5s en LAN; bundle actual ~320KB JS gzip ~98KB  
**Constraints**: Sin romper contrato camelCase API ↔ frontend; firmware HTTP a `api/iot.php`  
**Scale/Scope**: < 1000 usuarios locales; 1 dispositivo por usuario

## Constitution Check

*Gates derived from `.specify/memory/constitution.md` (SmartVest v1.0.0):*

| Gate | Status | Notes |
|------|--------|-------|
| Seguridad datos médicos | FAIL | Passwords expuestas — bloquea release público |
| Contratos API alineados | PASS | camelCase consistente |
| Build antes de deploy | WARN | Script deploy tenía ruta incorrecta (corregido) |
| Tests antes de features | FAIL | Sin `npm test` — documentar checklist manual |

## Project Structure

### Documentation (this feature)

```text
specs/001-web-platform-audit/
├── spec.md          # Hallazgos y requisitos objetivo
├── plan.md          # Este archivo
├── tasks.md         # Tareas ejecutables
└── checklist.md     # Quality gate pre-release
```

### Source Code (repository root)

```text
api/
├── config.php       # PDO + normalize_user_row (quitar password en GET)
├── users.php        # Auth + hash
└── iot.php          # Auth dispositivo + validación

components/          # A11y fixes
services/            # storageService, iotService
scripts/
└── deploy-xampp.sh  # Ruta repo corregida
```

**Structure Decision**: Monorepo existente sin `src/`; cambios localizados en `api/`, `components/`, `index.html`, `scripts/`.

## Phase 0: Hotfixes datos (sin cambio de código)

1. SQL: corregir `iot_states` — eliminar `VEST-DEMO esta `, insertar `VEST-DEMO` canónico.
2. SQL: `UPDATE iot_states SET sos_active=0, battery_level=85 WHERE device_id='VEST-001'` (si no hay emergencia real).
3. Verificar: `curl api/iot.php?deviceId=VEST-001` y `VEST-DEMO`.

## Phase 1: Seguridad API (P1)

1. `normalize_user_row`: omitir `password` en respuestas por defecto; flag solo para uso interno login.
2. `password_hash` / `password_verify` en login y upsert.
3. Middleware simple: API key header para `iot.php` POST y `users.php` POST (excepto `action=login`).
4. Mover Gemini a endpoint PHP `api/address.php` (proxy); quitar `process.env.API_KEY` del bundle Vite.

## Phase 2: UX / A11y (P2)

1. `Input.tsx`: `id` + `htmlFor`, `focus-visible:ring`, quitar `outline-none`.
2. `aria-label` en botones icono; `aria-live` en errores Login/Registration.
3. Reemplazar `alert()` por toast/inline.
4. `@media (prefers-reduced-motion: reduce)` en animaciones globales.

## Phase 3: SEO + headers (P3)

1. Meta description, OG tags en `index.html`.
2. `public/robots.txt`, `public/sitemap.xml` copiados en deploy.
3. Cabeceras Apache en `.htaccess` (solo XAMPP) o PHP `header()` en `config.php`.

## Phase 4: Calidad continua

1. Instalar squirrelscan CLI y re-ejecutar `squirrel audit http://localhost/Smartvest/ -C surface --format llm`.
2. Añadir checklist en `specs/001-web-platform-audit/checklist.md` al pipeline de release.
3. Opcional: Playwright smoke (`webapp-testing` skill) para login + perfil.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| API key para IoT | Falsificación de SOS | Solo IP allowlist insuficiente si LAN compartida |

## Risks

- Migrar passwords existentes requiere script de re-hash o reset forzado.
- Firmware debe actualizarse con misma API key que backend.
- Cambiar QR payload puede romper enlaces impresos existentes.
