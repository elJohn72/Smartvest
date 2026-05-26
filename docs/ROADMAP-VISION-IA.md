# Roadmap — visión artificial (complemento al ultrasonido)

Plan para la **siguiente fase de software** de SmartVest: la cámara **corrobora** la presencia de obstáculos; el **HC-SR04 sigue siendo la fuente de distancia en centímetros**.

**Prerrequisito hardware:** MCU con PSRAM (ideal **ESP32-S3**) y cámara montada al frente. Ver [HARDWARE-COMPACTO-FUTURO.md](./HARDWARE-COMPACTO-FUTURO.md).

---

## 1. Principio de diseño (no negociable)

| Sensor | Rol |
|--------|-----|
| **HC-SR04** | Medición principal: `distanceCm`, umbrales buzzer/vibrador, historial web |
| **Cámara + IA** | Corroboración: “hay obstáculo / zona ocupada” en cono visual |
| **Fusión** | Alerta reforzada si **ambos** coinciden; precaución si solo uno dispara |

Ejemplo de regla v1:

```text
danger_us  = distanceCm <= 40
danger_cam = camObstacleScore >= 0.7  (zona central ocupada)

peligro_final = danger_us AND danger_cam   → buzzer rápido + SOS opcional
precaución    = danger_us OR danger_cam    → patrón medio
```

Ajustar umbrales con pruebas con usuarios (O&M).

---

## 2. Estado actual en el repositorio

| Ítem | Estado |
|------|--------|
| Ultrasonido + API | Implementado |
| ESP32-CAM por UART (22/23) | Pines definidos; **sin firmware** en DevKit |
| Código de stream de imagen del usuario | **Fuera del repo** (flasheado aparte) |
| Campo `camObstacle` en API | No |
| Modelo IA entrenado | No |

---

## 3. Rutas técnicas de IA (comparación)

| Enfoque | Herramienta | Pros | Contras | Adecuado SmartVest |
|---------|-------------|------|---------|-------------------|
| Diferencia de frames | OpenCV / esp32-camera | Simple, bajo CPU | Falsos positivos (luz, sombras) | Prototipo rápido |
| Detección objetos ligera | **ESP-DL** (Espressif) | En ESP32-S3, sin nube | Modelos fijos; curva de aprendizaje | **Sí** |
| Clasificación personalizada | **Edge Impulse** | Exporta a TFLite; buenos tutoriales | Necesita dataset y entrenamiento | **Sí** |
| YOLOv5/v8 completo | Python / servidor | Muy preciso | Pesado para wearable | Solo nube o PC |
| Gemini / API nube | Ya usas en `address.php` | Potente | Latencia, sin internet | No para alerta en marcha |

**Recomendación:** empezar con **Edge Impulse** o **ESP-DL human/person detection** en ROI frontal; mantener ultrasonido como hoy.

---

## 4. Fases de implementación (software)

### Fase IA-0 — Preparación (sin cambiar SmartVest web)

- [ ] Elegir placa ESP32-S3 con cámara (ver hardware doc).
- [ ] Dataset: 200–500 imágenes “camino libre” vs “obstáculo frontal” (pecho, altura real).
- [ ] Probar inferencia < 200 ms por frame en S3.

### Fase IA-1 — Prototipo local

- [ ] Firmware: captura + score `0.0–1.0` de obstáculo en zona central.
- [ ] Serial/WiFi debug: imprimir `camScore` junto a `distanceCm`.
- [ ] Sin fusión aún — solo validar que la cámara no miente en escenas reales.

### Fase IA-2 — Fusión con ultrasonido

- [ ] Portar lógica de `getPatternForDistance()` para combinar con `camScore`.
- [ ] LED/buzzer/vibrador según tabla de fusión (sección 1).
- [ ] Opcional: si solo cámara dispara → vibración suave (usuario ciego confía en multimodal).

### Fase IA-3 — Plataforma SmartVest

- [ ] Extender JSON telemetría: `camObstacle`, `camScore`, `fusionLevel`.
- [ ] `api/iot.php` + `types.ts` + tarjeta en perfil (“Corroborado por cámara”).
- [ ] Documentar en [API.md](./API.md).

### Fase IA-4 — Calidad y exposición

- [ ] Pruebas con 3+ escenarios (interior, exterior, baja luz).
- [ ] Métricas: falsas alarmas / hora, latencia media.
- [ ] Actualizar [GUIA-EXPOSICION.md](./GUIA-EXPOSICION.md).

---

## 5. Stack software sugerido

```text
Captura: esp32-camera (ESP-IDF) o Arduino esp_camera
Inferencia: Edge Impulse exported library  OR  esp-dl
Fusión: misma tarea FreeRTOS que loop ultrasonido (prioridad alta a US)
Red: WiFi HTTP existente → api/iot.php (sin MQTT por ahora)
```

**No mezclar** el stream MJPEG del navegador (puerto 81) con el firmware de producción — un solo binario para campo.

---

## 6. Relación con hardware compacto

| Hardware | Impacto en IA |
|----------|--------------|
| ESP32 DevKit + CAM UART | Dos MCUs; IA en CAM y UART al DevKit — **complejo** (lo que tienes hoy) |
| ESP32-S3 + cámara en misma placa | **Un firmware** — recomendado |
| XIAO Sense + placa LTE | IA en XIAO; fusionar por UART — flexible y pequeño |

---

## 7. Lo que no está en alcance de esta fase IA

- Reemplazar bastón blanco o perro guía.
- Reconocimiento facial / personas conocidas.
- Navegación turn-by-turn por voz (producto tipo NOA).
- Entrenamiento en la nube obligatorio para caminar.

---

## 8. Enlaces de partida

- [Espressif ESP-DL](https://github.com/espressif/esp-dl)
- [Edge Impulse — ESP32-S3](https://docs.edgeimpulse.com/)
- [esp32-camera](https://github.com/espressif/esp32-camera)
- Comparativa mercado: [COMPARATIVA-MERCADO.md](./COMPARATIVA-MERCADO.md)

---

*Documento creado al cerrar la fase de mejoras incrementales del prototipo actual.*
