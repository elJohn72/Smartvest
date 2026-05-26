# Alertas accesibles — buzzer y vibrador

SmartVest alerta al **usuario que lleva el chaleco** sin depender de la pantalla del cuidador. Los umbrales coinciden con los colores del perfil web.

---

## Umbrales de distancia (HC-SR04 frontal)

| Nivel | Distancia | Color en web | Comportamiento hardware |
|-------|-----------|--------------|-------------------------|
| **Clear** | > 200 cm | Verde | Sin buzzer ni vibrador |
| **Precaución** | ≤ 200 cm | Amarillo | Pulso lento (~1 s) |
| **Alerta** | ≤ 100 cm | Naranja | Pulso medio (~0,6 s) |
| **Peligro** | ≤ 40 cm | Rojo | Pulso rápido (~0,4 s) |

Lectura estabilizada con **mediana de 3 muestras** en firmware.

---

## Botón SOS (GPIO27)

- **Pulsado (LOW):** patrón de alarma continuo (buzzer + vibrador) hasta soltar.
- **Web:** pantalla roja SOS en el perfil del cuidador.
- **HTTP:** envío cada ~2 s mientras SOS está activo (antes ~5 s).
- **SMS:** con SIM800 activo, envía al **0993212257** (`+593993212257`) — ver [SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md).

---

## Recomendaciones de uso (orientación y movilidad)

1. El chaleco **complementa** el bastón blanco; no sustituye la técnica de barrido del suelo.
2. Un solo sensor frontal no detecta ramas laterales ni obstáculos solo por encima del hombro — planear sensores extra (Fase B del roadmap).
3. Practicar en espacio conocido: identificar por sonido/vibración cada nivel antes de calles con tráfico.
4. El cuidador debe tener **notificaciones activadas** en el navegador al abrir el perfil.

---

## Código de referencia

- Firmware: `firmware/esp32/platformio-smartvest/src/main.cpp` — `getPatternForDistance`, `applyAlertPattern`, SOS en `loop`.
- Web: `utils/obstacleLevel.ts` — mismos umbrales para la UI.
