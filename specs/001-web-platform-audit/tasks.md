# Tasks: Remediación auditoría web SmartVest

**Input**: [spec.md](./spec.md), [plan.md](./plan.md)  
**Prerequisites**: XAMPP + MariaDB activos; branch `001-web-platform-audit` (opcional)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallelizable
- **[Story]**: US1 seguridad, US2 IoT, US3 A11y, US4 SEO

---

## Phase 1: Setup

- [ ] T001 Verificar squirrelscan CLI (`squirrel --version`) o instalar desde squirrelscan.com
- [ ] T002 [P] Confirmar build actual desplegado: `npm run build && ./scripts/deploy-xampp.sh`
- [ ] T003 [P] Baseline re-audit: `squirrel audit http://localhost/Smartvest/ -C surface --format llm` (guardar reporte)

---

## Phase 2: Hotfixes datos (US2)

- [ ] T004 [US2] SQL: DELETE fila `device_id='VEST-DEMO esta '` en `iot_states`
- [ ] T005 [US2] SQL: INSERT/UPDATE seed `VEST-DEMO` desde `database.sql`
- [ ] T006 [US2] SQL: Reset `sos_active=0` en `VEST-001` si emergencia no activa
- [ ] T007 [US2] Validar con curl GET `iot.php` para `VEST-001` y `VEST-DEMO`

---

## Phase 3: Seguridad API (US1) — BLOCKING

- [ ] T008 [US1] `api/config.php`: función `public_user_row()` sin password
- [ ] T009 [US1] `api/users.php`: GET lista/detalle usa fila pública; login usa verify
- [ ] T010 [US1] `api/users.php`: hash en upsert/import; migración contraseñas existentes
- [ ] T011 [US1] `api/iot.php`: validar API key o token en POST
- [ ] T012 [US1] `services/storageService.ts`: no guardar password en localStorage
- [ ] T013 [US1] Crear `api/address.php` proxy Gemini; remover API_KEY de `vite.config.ts`
- [ ] T014 [US1] Cabeceras seguridad en `api/config.php` helper `send_security_headers()`

---

## Phase 4: UX / Accesibilidad (US3)

- [ ] T015 [P] [US3] `components/Input.tsx`: htmlFor, focus-visible, sin outline-none
- [ ] T016 [P] [US3] `components/Button.tsx`: focus-visible pattern
- [ ] T017 [P] [US3] aria-label en botones icono: Home, UserProfile, QRCodeView, Landing
- [ ] T018 [US3] `components/Login.tsx`: aria-live errores; estados loading
- [ ] T019 [P] [US3] Reemplazar alert() en Home y RegistrationForm
- [ ] T020 [US3] `index.css`: prefers-reduced-motion para animaciones

---

## Phase 5: SEO (US4)

- [ ] T021 [P] [US4] Meta description + OG en `index.html`
- [ ] T022 [P] [US4] Añadir `public/robots.txt` y `public/sitemap.xml`
- [ ] T023 [US4] Incluir archivos SEO en `deploy-xampp.sh`

---

## Phase 6: Validación

- [ ] T024 Ejecutar checklist [checklist.md](./checklist.md)
- [ ] T025 Re-audit squirrel + comparar score vs baseline
- [ ] T026 Prueba manual navegador: landing → login → perfil (sin SOS falso)
- [ ] T027 `curl` regression: users GET sin password; login OK; iot POST rechazado sin key

---

## Dependencies & Execution Order

```text
Phase 1 → Phase 2 (hotfix) → Phase 3 (security) → Phase 4 ∥ Phase 5 → Phase 6
```

**Critical path**: T008–T014 antes de exponer app en red pública.

---

## Parallel Example

```bash
# Tras T008 definido:
Agent A: T015, T016, T017 (componentes)
Agent B: T021, T022 (SEO estáticos)
```
