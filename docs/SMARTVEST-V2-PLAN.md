# SmartVest v2 — plan documentado (próxima modificación)

**Estado:** documentación para implementación **futura**. El prototipo **v1** (ESP32 DevKit + SIM800L + XAMPP + web actual) sigue siendo la línea activa hasta que el equipo decida comprar hardware y abrir un sprint v2.

**No implementar ahora** salvo acuerdo explícito: este archivo es la **fuente única** del alcance v2.

---

## 1. Objetivo de v2

| Área | Meta |
|------|------|
| **Hardware** | Una placa compacta: MCU + LTE + GNSS + interfaz cámara |
| **Conectividad** | WiFi + datos móviles (LTE) + SMS; celular como **ayuda** (hotspot), no única dependencia |
| **Sin internet** | Alertas locales (ultrasonido, buzzer, vibrador) + SMS SOS si hay señal celular |
| **Software** | Misma API JSON (`api/iot.php`); firmware nuevo en carpeta o rama v2 |
| **IA** | Cámara como **complemento** del HC-SR04 (ver [ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md)) |
| **País** | Ecuador — Claro / Movistar / CNT ([SMARTVEST-V2-ECUADOR.md](./SMARTVEST-V2-ECUADOR.md)) |

---

## 2. Decisión de producto (cerrada en documentación)

### Conectividad híbrida (recomendada)

```text
Telemetría HTTP (mismo JSON que v1):
  1) WiFi → router / servidor LAN (casa, institución)
  2) Si falla → WiFi → hotspot del celular (datos del teléfono, opcional)
  3) Si falla → LTE → datos de la SIM del chaleco (independiente)

SOS:
  → Siempre SMS por módem del chaleco (no depender de app del celular)
  → Patrón buzzer/vibrador local (igual que v1)
  → Reintentos SMS mientras SOS activo y hay registro en red
```

### Sin internet / sin datos

| Capa | Comportamiento v2 |
|------|-------------------|
| **Local** | HC-SR04, mediana de muestras, umbrales, buzzer, vibrador: **siempre activos** |
| **SMS** | No requiere “internet”; requiere **señal celular** y SIM con SMS |
| **Panel web** | Sin actualización hasta haya WiFi o LTE con datos |
| **Opcional futuro** | Cola en SPIFFS/SD: sincronizar historial al reconectar |

---

## 3. Hardware objetivo

| Ítem | Elección documentada | Referencia |
|------|----------------------|------------|
| Placa principal | **LilyGO T-SIM7670G-S3 Standard** [H802] | [HARDWARE-COMPACTO-FUTURO.md](./HARDWARE-COMPACTO-FUTURO.md) |
| Módem | **SIM7670G** (variante **G**, global) | Bandas B2, B4, B28 → Ecuador |
| Evitar SKU | **A7670E** (Europa/SEA) como única opción | |
| MCU | ESP32-S3 (WiFi 2,4 GHz + más RAM para IA) | |
| GNSS | Integrado en módem (retirar NEO-6M suelto en v2) | |
| Cámara | OV2640/OV3660 en bus de la placa Standard | |
| Periféricos que se mantienen | HC-SR04, buzzer, vibrador, botón SOS | Cableado nuevo en `smartvest_config.h` v2 |
| SIM | Nano-SIM prepago **datos + SMS** (Claro o Movistar) | APN en config |

**Presupuesto orientativo placa + extras:** ~45–90 USD hardware + ~8–20 USD/mes SIM (ver [SMARTVEST-V2-ECUADOR.md](./SMARTVEST-V2-ECUADOR.md)).

---

## 4. Qué no cambia en v2 (contrato estable)

Para no romper la web ni la BD ya desplegada:

| Componente | v2 |
|------------|-----|
| `POST api/iot.php` | Mismo cuerpo JSON (camelCase): `deviceId`, `distanceCm`, `lat`, `lng`, `sos`, `batteryLevel`, etc. |
| `GET api/iot.php?deviceId=` | Igual |
| API key IoT | Igual (`IOT_API_KEY` en config) |
| Tablas `users`, `iot_states`, `iot_history` | Sin cambio obligatorio en primera entrega v2 |
| App React | Sin cambio obligatorio; opcional mostrar “conectado por WiFi / LTE” más adelante |

Campos **nuevos opcionales** (solo si se implementa IA): p. ej. `camScore`, `camObstacle` — acordar antes de escribir firmware.

---

## 5. Migración firmware v1 → v2

### v1 actual (referencia)

- Ruta: `firmware/esp32/platformio-smartvest/`
- ESP32 clásico, SIM800L, WiFi HTTP, GPS NEO-6M UART
- Config: `include/smartvest_config.h` (gitignored)

### v2 propuesto (cuando se ejecute)

| Tarea | Detalle |
|-------|---------|
| Nuevo entorno PlatformIO | p. ej. `firmware/esp32/platformio-smartvest-v2/` o rama `hardware/v2` |
| Base de código | Port desde `main.cpp` v1: lógica ultrasonido, SOS, mediana |
| Librería módem | [LilyGo-Modem-Series](https://github.com/Xinyuan-LilyGO/LilyGo-Modem-Series) (TinyGSM fork SIM7670/A7670) |
| WiFi | `WiFiMulti` o lista: SSID casa + SSID hotspot cuidador (opcional) |
| LTE | `modem.gprsConnect(apn, user, pass)` + HTTP cliente por módem |
| SMS | AT / `sendSMS()` del ejemplo LilyGO; número `+593...` en config |
| GNSS | UART del módem o PPS según wiki Standard |
| Cámara | Fase posterior dentro del mismo sprint v2 o sub-fase v2.1 |
| Ejemplo config | `smartvest_config.h.example` v2 con `SMARTVEST_APN`, `SMARTVEST_WIFI_*`, `SMARTVEST_SOS_PHONE` |

### Orden de implementación sugerido (checklist)

- [ ] **v2.0** — Comprar placa Standard + SIM; blink + registro LTE + SMS de prueba
- [ ] **v2.1** — WiFi + POST `iot.php` (paridad con v1)
- [ ] **v2.2** — Fallback LTE si WiFi cae 30 s
- [ ] **v2.3** — SOS: SMS + HTTP rápido + patrón háptico
- [ ] **v2.4** — GNSS en JSON (reemplazar NEO-6M)
- [ ] **v2.5** — Hotspot opcional (SSID del cuidador)
- [ ] **v2.6** — Cámara + IA ligera ([ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md))
- [ ] **v2.7** — Cola offline / sync (opcional)

---

## 6. Modo sin red (especificación)

Documentado para que el firmware v2 no asuma “siempre hay cloud”:

1. **Lectura ultrasonido** cada ciclo → alerta local si `< umbral`.
2. **SOS pulsado** → vibrador/buzzer inmediato; iniciar envío SMS; reintentar cada N segundos hasta confirmación o timeout.
3. **HTTP** → si no hay WiFi ni GPRS/LTE attached, no bloquear el loop; seguir sensores.
4. **LED / serial** (opcional): estado `OFFLINE_LOCAL`, `SMS_PENDING`, `LTE_OK`.
5. **Cuidador** → sin PWA hasta que haya red en el **dispositivo del cuidador**; el SMS sigue siendo el canal crítico offline.

---

## 7. Relación con otros documentos

| Documento | Rol en v2 |
|-----------|-----------|
| [SMARTVEST-V2-ECUADOR.md](./SMARTVEST-V2-ECUADOR.md) | Precios, bandas, APN, arquitectura WiFi/LTE/SMS |
| [HARDWARE-COMPACTO-FUTURO.md](./HARDWARE-COMPACTO-FUTURO.md) | Comparativa de placas y decisión LilyGO |
| [ROADMAP-VISION-IA.md](./ROADMAP-VISION-IA.md) | IA + fusión con ultrasonido |
| [ROADMAP-MEJORAS.md](./ROADMAP-MEJORAS.md) | Fase A cerrada; ítems B6+ apuntan aquí |
| [SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md) | Comportamiento SOS v1 (referencia para v2) |
| [SERVIDOR-FIJO-LAN.md](./SERVIDOR-FIJO-LAN.md) | URL LAN; en v2 añadir URL pública o túnel si LTE sale a Internet |
| [API.md](./API.md) | Contrato REST |

---

## 8. Riesgos y validación antes de producción

| Riesgo | Mitigación |
|--------|------------|
| Zona sin cobertura | Prueba de campo en Quinindé / ruta real; no prometer SMS 100 % |
| Consumo batería LTE+WiFi | Deep sleep LilyGO Standard; intervalo telemetría 5–10 s normal |
| Costo datos | Hotspot cuando hay celular; LTE solo si falla WiFi |
| SIM bloqueada / sin saldo | UI en perfil “última conexión”; alerta si `iot_states` viejo |
| Cambio de pines | Tabla pinout v2 en `CONFIGURACION-ESP32.md` (sección v2 al implementar) |

**Pruebas mínimas de aceptación v2:**

1. POST por WiFi con API key → fila en `iot_states`.
2. Apagar WiFi, solo LTE → mismo POST.
3. SOS → SMS recibido en número de emergencia.
4. Modo avión / sin red → buzzer por obstáculo + SOS local sin colgar el loop.

---

## 9. Estado del repositorio

| Versión | Estado | Carpeta / rama |
|---------|--------|----------------|
| **v1** | Activo, mantenimiento | `firmware/esp32/platformio-smartvest/` |
| **v2** | **Solo documentado** | Plan en este archivo; código cuando el equipo abra sprint |

Al iniciar v2: actualizar [ROADMAP-MEJORAS.md](./ROADMAP-MEJORAS.md) (B6 → en curso) y crear entrada en `CHANGELOG` o nota en `README.md`.

---

## 10. Resumen en una frase

**SmartVest v2 = placa LilyGO T-SIM7670G-S3 Standard + SIM Ecuador + firmware híbrido WiFi/LTE/SMS, alertas locales siempre, misma API PHP; implementación aplazada, alcance fijado en este plan.**

*Última actualización del plan: mayo 2026 — coherente con decisiones de conectividad híbrida y modo sin internet.*
