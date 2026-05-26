# SmartVest Constitution

## Core Principles

### I. Seguridad de datos médicos y credenciales

Los datos de salud y credenciales son activos de máxima sensibilidad. Las contraseñas se almacenan solo como hash. Las APIs no exponen secretos en GET. Los enlaces QR y perfiles públicos minimizan campos sensibles. Cualquier exposición de contraseñas o datos médicos sin autenticación es un defecto bloqueante de release.

### II. Contratos API estables

Los payloads JSON usan camelCase hacia el frontend y snake_case en MariaDB. Cambios en `types.ts`, `api/*.php` y `database.sql` se realizan en el mismo cambio. El firmware y `iotService.ts` deben acordar las mismas claves (`distanceCm`, `deviceId`, `sosActive`).

### III. Build y despliegue secuencial

Tras cambios en frontend: `npm run build` y luego `./scripts/deploy-xampp.sh`. Nunca en paralelo. El target XAMPP es `/Applications/XAMPP/xamppfiles/htdocs/Smartvest`.

### IV. Fallback local consciente

`localStorage` es respaldo cuando la API falla, no la fuente de verdad en producción local. No persistir contraseñas ni duplicar datos médicos innecesariamente en cliente.

### V. Accesibilidad como requisito de producto

SmartVest sirve a personas con discapacidad visual y a cuidadores bajo estrés. Controles con etiquetas asociadas, foco visible, feedback sin `alert()`, respeto a `prefers-reduced-motion`, y contraste legible en flujos de emergencia (SOS, llamada, mapa).

### VI. Simplicidad y alcance mínimo

Preferir el diff más pequeño correcto. No introducir frameworks o capas nuevas sin necesidad. Sin tests automatizados hoy: documentar validación manual en checklist de feature antes de cerrar.

## Restricciones técnicas

- Frontend: Vite + React + TypeScript en raíz del repo (sin `src/`).
- Base path: `/Smartvest/`.
- Backend local: PHP + MariaDB vía XAMPP.
- Firmware: PlatformIO bajo `firmware/esp32/platformio-smartvest/`; WiFi/API en `smartvest_config.h` (gitignored).

## Flujo de desarrollo (Spec Kit)

1. `/speckit-specify` — requisitos (qué/por qué).
2. `/speckit-clarify` — opcional antes de plan.
3. `/speckit-plan` — stack y fases.
4. `/speckit-tasks` — tareas ordenadas.
5. `/speckit-analyze` — opcional, consistencia spec/plan/tasks.
6. `/speckit-implement` — ejecución.
7. Checklist de feature + `npm run build` + deploy + `curl` API.

## Quality gates

- Ningún release con GET de usuarios exponiendo passwords.
- Ningún release con SOS activo en BD sin emergencia confirmada en demo.
- Build verde antes de merge a `main`.

## Governance

Esta constitución prevalece sobre convenciones ad hoc. Enmiendas documentadas en commit con bump de versión menor. PRs y agentes deben verificar compliance con checklist de la feature activa en `specs/`.

**Version**: 1.0.0 | **Ratified**: 2026-05-26 | **Last Amended**: 2026-05-26
