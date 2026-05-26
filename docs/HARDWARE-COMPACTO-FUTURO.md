# Hardware compacto — investigación de mercado (SmartVest v2)

Documento de referencia para **reducir tamaño** del chaleco y **integrar** módulos que hoy van separados (GSM, GPS, cámara, MCU). Complementa el firmware actual (ESP32 DevKit + SIM800L + NEO-6M + ESP32-CAM por UART).

**Estado del proyecto actual:** fase de mejoras web/firmware **cerrada** (mayo 2026). Este documento orienta la **siguiente generación física**.

**Plan de implementación v2 (alcance completo, sin código aún):** [SMARTVEST-V2-PLAN.md](./SMARTVEST-V2-PLAN.md).

---

## 1. Lo que tienes hoy (baseline)

| Módulo | Función | Interfaz al DevKit | Tamaño aprox. |
|--------|---------|-------------------|---------------|
| ESP32 DevKit V1 | MCU, WiFi, lógica | — | ~52 × 28 mm (solo placa) |
| SIM800L | GSM/GPRS, SMS SOS | UART (16/17) | ~24 × 24 mm + antena |
| NEO-6M | GPS | UART (4/14) | ~25 × 35 mm + antena |
| ESP32-CAM (AI-Thinker) | Imagen / futura IA | UART (22/23) | ~27 × 40 mm + cámara |
| HC-SR04 | Distancia cm | TRIG 5, ECHO 18 | ~45 × 20 mm |
| Periféricos | SOS, buzzer, vibrador | 27, 13, 32 | Cableado en PCB |

**Problema de volumen:** varios módulos, varias antenas, varias fuentes y mucho cableado → chaleco grande y difícil de integrar en prenda.

**Objetivo v2:** una placa principal que concentre **MCU + celular + GPS (+ cámara o bus de cámara)** y dejar el ultrasonido (y actuadores) como periféricos pequeños en el pecho.

---

## 2. Criterios de selección

| Criterio | Peso |
|----------|------|
| Integrar GSM/LTE + GNSS en un solo módulo | Alto |
| MCU con WiFi (mantener `api/iot.php` por HTTP) | Alto |
| Soporte cámara OV2640/OV3660 o módulo IA compacto | Alto |
| GPIO libres para HC-SR04, SOS, buzzer, vibrador | Alto |
| Consumo y batería (chaleco portátil) | Alto |
| Documentación / ejemplos Arduino o ESP-IDF | Medio |
| Bandas LTE/GSM válidas en Ecuador | Crítico al comprar |
| Tamaño final de placa | Alto |

**Ecuador (celular):** verificar variantes del módem — **A7670G** (global) suele ser más flexible que **A7670E** (regiones específicas). Confirmar con tu operador (Claro, Movistar, CNT) antes de comprar.

---

## 3. Opciones de mercado (resumen)

### Nivel A — Solo fusionar GSM + GPS (mínimo cambio de arquitectura)

Sustituyen **SIM800L + NEO-6M** por un breakout; el ESP32 DevKit y la ESP32-CAM pueden seguir igual.

| Producto | Qué integra | Tamaño ref. | Pros | Contras |
|----------|-------------|-------------|------|---------|
| [Seeed SIM808](https://wiki.seeedstudio.com/Mini_GSM_GPRS_GPS_Breakout_SIM808/) | GPRS + GPS | ~27 × 46 mm | Barato, muchos tutoriales, AT commands | 2G (GPRS), no LTE; sigues con 2 UART (GSM+GPS internos al módulo) |
| [Seeed LoNet 808](https://wiki.seeedstudio.com/LoNet_808-Mini_GSM_GPRS_Plus_GPS_Breakout/) | Igual familia SIM808 | Compacto | Misma lógica que SIM808 | Igual que arriba |
| [SIMCom SIM868E](https://www.simcom.com/product/SIM868E.html) | GPRS + GNSS multi-constelación | LCC ~24×24 | Más moderno que SIM800 | Sigue siendo celular 2G en muchos despliegues |
| [Waveshare A7670E GNSS HAT](https://www.waveshare.com/wiki/A7670E_Cat-1/GNSS_HAT) | LTE Cat-1 + GNSS | HAT + ESP32 aparte | LTE, SMS, TCP | Necesita ESP32 host; no es wearable por sí solo |

**Ahorro de espacio:** ~1 módulo y 1 antena GPS menos; **no elimina** el DevKit ni la CAM.

---

### Nivel B — Placa “todo en uno” MCU + LTE + GPS (recomendado para SmartVest v2)

Un solo PCB con ESP32 o ESP32-S3 + módem celular + GNSS integrado en el módem.

| Producto | MCU | Celular + GPS | Cámara | Dimensiones ref. | Notas |
|----------|-----|---------------|--------|------------------|-------|
| [LilyGO T-A7670G/E](https://wiki.lilygo.cc/get_started/en/High_speed/T-PCIE/T-A7670/T-A7670E/T-A7670E.html) | ESP32 | A7670 + GNSS | No nativa | ~111 × 34 × 19 mm | 4G, SMS, WiFi, SD, batería 18650; ejemplos en [LilyGo-Modem-Series](https://github.com/Xinyuan-LilyGO/LilyGo-Modem-Series) |
| [LilyGO T-SIM7670G-S3](https://lilygo.cc/products/t-sim-7670g-s3) | ESP32-S3 | SIM7670G + GNSS | Solo versión **Standard** | Similar, más moderno | **Standard [H802]**: interfaz cámara OV, QWIIC, deep sleep ~147 µA |
| [LilyGO T-SIM7670G-S3 Standard](https://lilygo.cc/products/t-sim-7670g-s3) | ESP32-S3 16MB + 8MB PSRAM | SIM7670G LTE + GNSS | **Sí (OV)** | Placa única | **Mejor candidato “una placa”** para reemplazar DevKit+SIM800+NEO-6M+cableado CAM |

**Por qué destaca T-SIM7670G-S3 Standard:**

- Un solo firmware base (ESP32-S3) para WiFi, HTTP a SmartVest, SMS SOS y GPS.
- PSRAM 8 MB útil para **visión por IA en el borde** (ver [ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md)).
- Ranura cámara en la misma placa → eliminas el segundo ESP32-CAM y el cable UART 22/23.
- Comunidad LilyGO + repositorio unificado de módems.

**GPIO para conservar de SmartVest:** TRIG/ECHO ultrasonido, SOS, buzzer, vibrador — validar pinout en wiki LilyGO antes del diseño PCB final.

---

### Nivel C — Módulo mínimo solo para visión IA (complemento ultrasonido)

Para **corroborar** obstáculos sin reemplazar el HC-SR04 (alineado con tu decisión actual).

| Producto | Tamaño | Cámara / IA | Celular / GPS |
|----------|--------|-------------|---------------|
| [Seeed XIAO ESP32-S3 Sense](https://www.seeedstudio.com/XIAO-ESP32S3-Sense-p-5639.html) | ~21 × 17.5 mm | OV2640, mic; Edge Impulse | No — añadir [L76K GNSS](https://www.seeedstudio.com/L76K-GNSS-Module-for-Seeed-Studio-XIAO-p-5374.html) o módem aparte |
| [DFRobot ESP32-S3 AI Camera](https://www.dfrobot.com/product-2899.html) | ~42 × 42 mm | OV3660 160°, IR, altavoz/mic | No |
| ESP32-S3-EYE (Espressif) | Devkit oficial | Cámara + ML ejemplos | No |

**Uso típico en SmartVest:** XIAO Sense en pecho (visión) + placa Nivel B en costado (LTE/GPS/SOS) comunicadas por UART/I2C — **más pequeño que la torre actual** pero dos PCBs coordinados.

---

## 4. Comparativa frente a tu lista de necesidades

| Necesidad | DevKit actual | SIM808/868 | T-SIM7670G-S3 Std | XIAO S3 Sense |
|-----------|---------------|------------|-------------------|---------------|
| WiFi → SmartVest | Sí | Requiere ESP32 host | Sí | Sí |
| SMS SOS | SIM800L | Sí | Sí (SIM7670) | No solo |
| GPS en mapa | NEO-6M | Sí integrado | GNSS en módem | Con L76K opcional |
| Cámara / stream | ESP32-CAM aparte | No | Interfaz OV en Standard | Integrada |
| IA obstáculos | No en repo | No | ESP32-S3 + PSRAM | Ideal para Edge Impulse |
| Ultrasonido cm | HC-SR04 | GPIO host | GPIO (revisar mapa) | No — mantener en placa B |
| Tamaño global | Grande | Mediano | **Mediano-bajo** | Mínimo (solo visión) |

---

## 5. Arquitecturas recomendadas (documentar para la tesis / exposición)

### Opción recomendada 1 — “Una placa central” (más simple de mantener)

```text
[LilyGO T-SIM7670G-S3 Standard]
  ├── Cámara OV2640/OV3660 (IA + corroboración visual)
  ├── HC-SR04 + buzzer + vibrador + SOS (GPIO)
  ├── LTE + GNSS (SMS SOS, backup si no hay WiFi)
  └── WiFi → SmartVest API (igual que hoy)
```

### Opción recomendada 2 — “Dos nodos” (más pequeño en pecho)

```text
[XIAO ESP32-S3 Sense]  — visión IA, baja latencia, UART/I2C
        ↕
[T-SIM7670G-S3]  — WiFi, LTE, GPS, SMS, ultrasonido, alertas
```

---

## 6. Qué comprar / prototipar primero (orden sugerido)

1. **Confirmar bandas LTE** con operador local → elegir A7670**G** vs E vs SA.  
2. **Evaluar LilyGO T-SIM7670G-S3 Standard** con ejemplos TinyGSM + HTTP + GNSS del repo [LilyGo-Modem-Series](https://github.com/Xinyuan-LilyGO/LilyGo-Modem-Series).  
3. **Migrar firmware** `platformio-smartvest` a entorno `esp32-s3-devkitc-1` (o board LilyGO concreto).  
4. **Portar lógica** ultrasonido + SOS + umbrales (ya probada en `main.cpp`).  
5. **Añadir visión IA** según [ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md).  
6. **Diseñar PCB final** de chaleco (FPC, antenas integradas, batería LiPo plana).

---

## 7. Lo que este documento no sustituye

- Diseño mecánico de la prenda (costura, ubicación de sensores).
- Certificación radio (LTE/GSM según país).
- Coste de datos SIM en producción.
- El firmware actual **sigue siendo válido** en el hardware actual hasta que migres.

---

## 8. Referencias útiles

| Recurso | URL |
|---------|-----|
| LilyGO modem series (GitHub) | https://github.com/Xinyuan-LilyGO/LilyGo-Modem-Series |
| T-A7670G getting started | https://randomnerdtutorials.com/lilygo-ttgo-t-a7670g-a7670e-a7670sa-esp32/ |
| SIM808 Seeed Wiki | https://wiki.seeedstudio.com/Mini_GSM_GPRS_GPS_Breakout_SIM808/ |
| XIAO ESP32-S3 Sense | https://www.seeedstudio.com/XIAO-ESP32S3-Sense-p-5639.html |
| Espressif ESP-DL | https://github.com/espressif/esp-dl |
| Edge Impulse | https://edgeimpulse.com/ |

---

## 9. Ecuador: operadores, bandas y WiFi + LTE + SMS

Documento detallado (precios, APN, arquitectura dual): **[SMARTVEST-V2-ECUADOR.md](./SMARTVEST-V2-ECUADOR.md)**.

Resumen:

| Operador | LTE necesario | Compatible SIM7670G / A7670G |
|----------|---------------|-----------------------------|
| Claro | B4 (+ 3G B5) | Sí |
| Movistar | B2, B4 | Sí |
| CNT | B4, B28 | Sí |

**Variante módem a comprar:** **G** (global) o **SA** (Sudamérica), **no E** (Europa/SEA).

**Conectividad v2:** WiFi para telemetría en casa; **datos móviles LTE** cuando no hay WiFi; **SMS** por el mismo módem en SOS.

---

## 10. Decisión registrada (para el equipo)

| Tema | Decisión |
|------|----------|
| Mejoras software actuales | **Pausa** — baseline estable |
| Ultrasonido | Se mantiene; no lo reemplaza la cámara |
| Cámara actual (ESP32-CAM UART) | Prototipo visual; **no integrada** en repo |
| Próximo hardware objetivo | **LilyGO T-SIM7670G-S3 Standard** (~40–55 USD) |
| País / red | **Ecuador** — Claro / Movistar; ver SMARTVEST-V2-ECUADOR |
| Próximo software | Visión IA + firmware WiFi/LTE fallback — ver roadmap IA |

*Actualizar esta tabla cuando el equipo compre y valide una placa concreta.*
