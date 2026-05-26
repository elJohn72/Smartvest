# Release Checklist: SmartVest Web (post-auditoría)

**Purpose**: Validar que hallazgos críticos de `spec.md` están cerrados antes de demo pública o despliegue GitHub Pages.  
**Created**: 2026-05-26

## Seguridad

- [ ] CHK001 GET `api/users.php` no incluye campo `password` en JSON
- [ ] CHK002 Contraseñas en BD son hashes (no texto legible)
- [ ] CHK003 POST `api/iot.php` rechaza peticiones sin credencial de dispositivo
- [ ] CHK004 Bundle JS en `dist/assets/*.js` no contiene cadena de API key Gemini
- [ ] CHK005 No hay credenciales de prueba commiteadas en repo

## IoT / Datos

- [ ] CHK006 `iot_states` sin `device_id` corruptos
- [ ] CHK007 Perfil usuario con `VEST-001` refleja `sos_active` real
- [ ] CHK008 Fallback `VEST-DEMO` solo cuando explícito en UX demo

## Accesibilidad

- [ ] CHK009 Foco visible en tab por todos los botones/inputs principales
- [ ] CHK010 Errores de formulario anunciados (aria-live o texto visible)
- [ ] CHK011 Contraste texto principal ≥ 4.5:1 en landing y formularios

## SEO / PWA

- [ ] CHK012 `<meta name="description">` presente en HTML servido
- [ ] CHK013 `manifest.json` accesible y iconos cargan
- [ ] CHK014 `robots.txt` presente si sitio es público

## Build / Deploy

- [ ] CHK015 `npm run build` exitoso
- [ ] CHK016 `./scripts/deploy-xampp.sh` apunta al repo correcto
- [ ] CHK017 `curl http://localhost/Smartvest/` → 200 y assets coinciden con dist

## Funcional

- [ ] CHK018 Login con usuario real funciona vía API
- [ ] CHK019 Registro + QR genera enlace válido
- [ ] CHK020 Import/export JSON no pierde `deviceId`
