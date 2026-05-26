# Roadmap de mejoras SmartVest

Estado de la hoja de ruta alineada con el hardware y software actuales.

**Leyenda:** ✅ Hecho · 🟡 En curso / parcial · ⬜ Pendiente (requiere hardware o más tiempo)

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
| A12 | SMS SOS (SIM800L) | ✅ | `+593993212257` (0993212257); flashear firmware |
| A13 | Servidor fijo en LAN | 🟡 | Doc + script IP; despliegue manual |
| A14 | Documentación mercado y roadmap | ✅ | Este archivo |

---

## Fase B — PCB v2 o módulos extra

| ID | Mejora | Estado | Requisito |
|----|--------|--------|-----------|
| B1 | 2.º / 3.er HC-SR04 | ⬜ | Pines GPIO libres |
| B2 | ADC batería (GPIO34 + divisor) | ⬜ | Soldadura divisor |
| B3 | ESP32-CAM (UART 22/23) | ⬜ | Validar cruce RX/TX |
| B4 | IMU caída / inactividad | ⬜ | MPU6050 |
| B5 | SIM800L en producción (campo) | 🟡 | Config y docs listos; validar señal y saldo en exposición |

---

## Fase C — Producto / institución

| ID | Mejora | Estado |
|----|--------|--------|
| C1 | Panel multi-dispositivo (admin) | ⬜ |
| C2 | Pruebas con usuarios (O&M) | ⬜ |
| C3 | IA ligera en cámara | ⬜ |
| C4 | App móvil nativa o TTS en wearable | ⬜ |

---

## Orden sugerido para el siguiente sprint

1. Flashear firmware con SOS mejorado y mediana ultrasonido.
2. Fijar IP del servidor ([SERVIDOR-FIJO-LAN.md](./SERVIDOR-FIJO-LAN.md)) y actualizar `SMARTVEST_API_URL`.
3. Probar notificaciones SOS en el móvil del cuidador (perfil abierto o PWA instalada).
4. Si hay SIM: activar `SMARTVEST_ENABLE_SIM800` y probar SMS.
