# Checklist de revisión y entrega

## Documentación entregada

- [x] Objetivo, historias, supuestos y límites escritos.
- [x] Base main y rama móvil no fusionada identificadas.
- [x] Plan, tareas, modelo, contrato textual y comandos objetivo distinguidos del runtime actual.
- [x] Datos PRUEBA, aislamiento y políticas de privacidad explícitos.

## Aprobación SDD (pendiente)

- [ ] G0: John valida spec y decisiones D1–D8 aplicables a M1.
- [ ] G1: John valida arquitectura/contratos/enmienda constitucional.
- [ ] G2: John valida tareas antes de implementar.

## Release M1 (no ejecutado)

- [ ] Pruebas SC-001–005 con evidencia física y runtime real.
- [ ] OpenAPI machine-validated y negativos de permisos/TLS/QR verdes.
- [ ] Sin defaults secretos, rutas legacy inseguras, PII/logs ni cache pública.
- [ ] Firma y paquete comprobados para actualizar APK.
- [ ] Build/lint/unitarias/integ/migraciones y auditorías requeridas del HEAD final verdes.
- [ ] Backup cifrado/restauración separada y rollback verificados.
- [ ] PR correcto, checks obligatorios presentes y verdes; autorización de merge/deploy registrada.
- [ ] Panel Coolify privado; scope SMARTVEST, DB/red/volumen exclusivos.
- [ ] Reseña UI, seguridad y lógica de negocio; sin pendientes críticos.

## Release M2 (no ejecutado)

- [ ] D6 aprobado; proveedor/cuenta/credenciales propios.
- [ ] SC-006 y pruebas de fallos/background físicas.
- [ ] No equivalencia falsa entre log, providerAccepted, deviceReceived y userOpened.

Esta entrega solo especifica; ninguna casilla de release acredita software listo o VPS modificado.
