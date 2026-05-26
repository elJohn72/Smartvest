# Roadmap de mejoras SmartVest (fase incremental)

Estado de la hoja de ruta del **prototipo actual** (ESP32 DevKit + XAMPP + web).

> **Nota (mayo 2026):** La fase incremental de mejoras web/firmware se considera **cerrada** para este hardware.  
> Siguiente trabajo documentado en:
> - **[SMARTVEST-V2-PLAN.md](./SMARTVEST-V2-PLAN.md)** — plan maestro v2 (híbrido WiFi/LTE/SMS, sin red, checklist; **no implementar aún**)  
> - [ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md) — visión artificial como complemento del ultrasonido  
> - [HARDWARE-COMPACTO-FUTURO.md](./HARDWARE-COMPACTO-FUTURO.md) — placa más pequeña con GSM+GPS+cámara integrados  
> - [SMARTVEST-V2-ECUADOR.md](./SMARTVEST-V2-ECUADOR.md) — precios y operadores Ecuador  

**Leyenda:** ✅ Hecho · 🟡 En curso / parcial · ⬜ Pendiente · ⏸ Pausado / siguiente generación

---

## Fase A — Con lo que ya tienes (sin cambiar PCB)

| ID | Mejora | Estado | Notas |
|----|--------|--------|-------|
| A1 | Sesión web tras F5 | ✅ | `sessionStorage` |
| A2 | Seguridad API (hash, API key IoT) | ✅ | Ver `api/users.php`, `iot.php` |
| A3 | Historial distancia (`iot_history`) | ✅ | Gráfico en perfil |
| A4 | Batería: UI honesta «Sin sensor en PCB» | ✅ | Ver [CONFIGURACION-ESP32.md](./CONFIGURACION-ESP32.md) |
| A5 | Filtro mediana ultrasonido (firmware) | ✅ | 3 muestras en `main.cpp` |
| A6 | Simulación solo en `localhost` | ✅ | Perfil |
| A7 | Notificaciones SOS al cuidador (PWA) | ✅ | `notificationService.ts` |
| A8 | Sonido de alerta SOS en navegador | ✅ | `alertSound.ts` |
| A9 | Guía de alertas hápticas en perfil | ✅ | Tarjeta en perfil |
| A10 | SOS: patrón buzzer/vibrador dedicado | ✅ | Firmware |
| A11 | Telemetría HTTP más rápida con SOS | ✅ | ~2 s si SOS activo |
| A12 | SMS SOS (SIM800L) | ✅ | `+593963930791` (0963930791 cuidador); flashear firmware |
| A13 | Servidor fijo en LAN | 🟡 | Doc + script IP; despliegue manual |
| A14 | Documentación mercado y roadmap | ✅ | Este archivo |

---

## Fase B — PCB v2 o módulos extra

| ID | Mejora | Estado | Requisito |
|----|--------|--------|-----------|
| B1 | 2.º / 3.er HC-SR04 | ⏸ | Pines GPIO libres |
| B2 | ADC batería (GPIO34 + divisor) | ⏸ | Soldadura divisor |
| B3 | ESP32-CAM UART (corroboración) | ⏸ | Usuario tiene stream imagen; ver ROADMAP-VISION-IA |
| B4 | IMU caída / inactividad | ⏸ | MPU6050 |
| B5 | SIM800L en producción (campo) | 🟡 | Config listo; validar en exposición |
| B6 | SmartVest v2 (placa + firmware híbrido) | ⏸ | Plan: [SMARTVEST-V2-PLAN.md](./SMARTVEST-V2-PLAN.md) |

---

## Fase C — Producto / institución

| ID | Mejora | Estado |
|----|--------|--------|
| C1 | Panel multi-dispositivo (admin) | ⏸ |
| C2 | Pruebas con usuarios (O&M) | ⏸ |
| C3 | IA ligera en cámara | ⏸ → ROADMAP-VISION-IA |
| C4 | App móvil nativa o TTS en wearable | ⏸ |

---

## Orden sugerido para el siguiente sprint

1. Flashear firmware con SOS mejorado y mediana ultrasonido.
2. Fijar IP del servidor ([SERVIDOR-FIJO-LAN.md](./SERVIDOR-FIJO-LAN.md)) y actualizar `SMARTVEST_API_URL`.
3. Probar notificaciones SOS en el móvil del cuidador (perfil abierto o PWA instalada).
4. Si hay SIM: activar `SMARTVEST_ENABLE_SIM800` y probar SMS.
