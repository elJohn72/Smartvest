# Configuración ESP32 DevKit en Mac

Guía rápida para conectar el chaleco a tu WiFi y probar con SmartVest en XAMPP.

---

## Requisitos

- ESP32 conectado por USB → en Mac suele ser `/dev/cu.usbserial-0001`
- Mac y ESP32 en la **misma red WiFi**
- XAMPP con **Apache + MariaDB** encendidos
- Usuario web con `deviceId: VEST-001` (o el mismo ID que pongas en el firmware)

---

## Paso 1 — Editar WiFi y API

Archivo: `firmware/esp32/platformio-smartvest/include/smartvest_config.h`

| Macro | Valor recomendado |
|-------|-------------------|
| `SMARTVEST_DEVICE_ID` | `VEST-001` (igual que en tu perfil web) |
| `SMARTVEST_WIFI_SSID` | Nombre exacto de tu red WiFi |
| `SMARTVEST_WIFI_PASSWORD` | Contraseña WiFi |
| `SMARTVEST_API_URL` | `http://192.168.0.105/Smartvest/api/iot.php` (IP de tu Mac) |
| `SMARTVEST_IOT_API_KEY` | `smartvest-local-dev-key` (igual que PHP por defecto) |

**Detectar IP de tu Mac:**

```bash
ipconfig getifaddr en0
```

Si cambia de red, actualiza `SMARTVEST_API_URL` y vuelve a subir el firmware.

---

## Paso 2 — Compilar y subir

Desde la carpeta del firmware:

```bash
cd firmware/esp32/platformio-smartvest

"/Users/eljhon72/Library/Python/3.12/bin/pio" run

"/Users/eljhon72/Library/Python/3.12/bin/pio" run -t upload --upload-port /dev/cu.usbserial-0001

"/Users/eljhon72/Library/Python/3.12/bin/pio" device monitor --port /dev/cu.usbserial-0001
```

En el monitor serial (115200 baud) deberías ver:

- `SmartVest PlatformIO iniciado`
- `deviceId=VEST-001`
- JSON de telemetría cada ~1 s
- `HTTP iot.php -> 200` si WiFi y XAMPP están bien

---

## Paso 3 — XAMPP y base de datos

1. Inicia Apache y MySQL en XAMPP.
2. Abre [http://localhost/Smartvest/](http://localhost/Smartvest/)
3. Inicia sesión y abre el perfil vinculado a `VEST-001`.

---

## Paso 4 — Probar sin hardware (simulación)

Desde el perfil web, sección **Simulación (solo demo)**:

- Mover GPS
- Activar / Apagar SOS

O desde terminal (simula el ESP32):

```bash
curl -s -X POST "http://localhost/Smartvest/api/iot.php" \
  -H "Content-Type: application/json" \
  -H "X-SmartVest-Api-Key: smartvest-local-dev-key" \
  -d '{"deviceId":"VEST-001","distanceCm":55,"latitude":-0.180653,"longitude":-78.467834,"sosActive":false,"batteryLevel":88}'
```

---

## Problemas frecuentes

| Síntoma | Solución |
|---------|----------|
| `HTTP error` en serial | XAMPP apagado, IP incorrecta, o Mac con firewall bloqueando |
| `HTTP iot.php -> 401` | Clave distinta entre `smartvest_config.h` y `api/config.php` |
| WiFi no conecta | SSID/contraseña mal; red 5 GHz solo (algunos ESP32 prefieren 2.4 GHz) |
| Perfil sin datos | `deviceId` del usuario ≠ `SMARTVEST_DEVICE_ID` |
| Puerto USB distinto | `ls /dev/cu.*` y cambia `--upload-port` |

---

## Batería (investigación hardware)

| Pregunta | Respuesta en tu proyecto |
|----------|---------------------------|
| ¿La PCB mide batería hoy? | **No** en el esquema revisado: no hay divisor de tensión hacia ADC del ESP32. |
| ¿Qué envía el firmware? | Sin pin ADC configurado, **no** manda `batteryLevel` y la web muestra «Sin sensor en PCB». |
| ¿Cómo activarlo después? | Añade divisor (ej. 100k/100k) del + de la batería a **GPIO34** (ADC solo entrada, libre en tu pinout) y en `smartvest_config.h`: `#define SMARTVEST_BATTERY_ADC_PIN 34`. Luego calibra `updateBatteryLevel()` en `main.cpp`. |
| Pines ocupados (no usar para ADC) | 5, 18, 13, 32, 27, 4, 14, 16, 17, 23, 22 |

## Notificaciones al cuidador (web)

1. Abre el perfil del usuario en el navegador (ideal: móvil del familiar).
2. Pulsa **Activar** en el aviso azul de notificaciones (o acepta el permiso del navegador).
3. Opcional: menú del navegador → **Añadir a pantalla de inicio** (PWA).
4. Cuando el usuario pulse SOS en el chaleco, sonará una alerta y aparecerá notificación del sistema.

## SMS por SIM800L

Documentación completa: **[SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md)** (número, pruebas, exposición).

Configuración actual en `smartvest_config.h`:

```c
#define SMARTVEST_ENABLE_SIM800 true
#define SMARTVEST_SOS_PHONE "+593993212257"   /* 0993212257 */
```

Requiere módulo SIM800L con antena, SIM activa y registro en red GSM.

## Botón SOS y ultrasonido

- **GPIO27** — botón SOS (LOW = activo)
- **GPIO5/18** — HC-SR04 distancia (lectura con **mediana de 3 muestras** para estabilizar)
- Buzzer y motor vibran según cercanía (≤40 cm peligro, ≤100 alerta, ≤200 precaución)

---

## Montar el ESP32 en la PCB (después de flashear)

Flashear **solo el ESP32 suelto** por USB es correcto. El firmware **ya queda guardado en la flash**; no se pierde al montar la PCB.

### ¿Puedo desconectar el Mac?

**Sí.** Para uso normal el chaleco no necesita estar en la computadora:

1. Desconecta el cable USB del Mac.
2. Inserta el ESP32 en la PCB (o conecta los cables según tu diseño).
3. Alimenta la PCB con su fuente/batería habitual.
4. El ESP32 debe unirse a WiFi **AJTECNOLOGY** y enviar datos a `http://192.168.0.105/Smartvest/api/iot.php` solo con alimentación y antena WiFi.

Vuelve a conectar USB al Mac solo si quieres:

- Ver el monitor serial (115200 baud), o
- Subir un firmware nuevo.

### Checklist antes de encender en la PCB

| Paso | Acción |
|------|--------|
| 1 | Mac con XAMPP encendido (Apache + MySQL) en la misma WiFi |
| 2 | IP del Mac sigue siendo `192.168.0.105` (si cambió, edita `SMARTVEST_API_URL` y vuelve a flashear) |
| 3 | `SMARTVEST_DEVICE_ID` = `VEST-001` (igual que el usuario en la web) |
| 4 | **Una sola fuente de alimentación** a la vez al probar (evita USB Mac + fuente PCB a la vez si no está diseñado para eso) |
| 5 | GND común entre ESP32, sensores y PCB |
| 6 | HC-SR04: TRIG→GPIO5, ECHO→GPIO18 |
| 7 | GPS: TX del módulo → GPIO4 (RX ESP), RX del módulo → GPIO14 (TX ESP) |
| 8 | Botón SOS → GPIO27 (a GND al presionar, con pull-up interno) |
| 9 | Buzzer GPIO13, motor GPIO32 |

### Cómo saber que todo está bien (sin USB)

1. Abre el perfil en [http://localhost/Smartvest/](http://localhost/Smartvest/) con usuario `VEST-001`.
2. En 10–30 s debería mostrar **En línea** y **Actualizado hace X s**.
3. Acerca la mano al ultrasonido: la distancia en la web debe bajar de 9999 cm a un valor real (ej. 20–150 cm).
4. GPS: puede tardar varios minutos al aire libre hasta `gpsFix: true` y mapa visible.
5. Presiona SOS: perfil en rojo + `sosActive: true` en la API.

### Si la IP de tu Mac cambia

```bash
ipconfig getifaddr en0
```

Actualiza `SMARTVEST_API_URL` en `include/smartvest_config.h`, vuelve a flashear con USB y monta de nuevo en la PCB.
