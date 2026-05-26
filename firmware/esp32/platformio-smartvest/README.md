# SmartVest PlatformIO

Firmware del chaleco inteligente (ESP32). Documentación general del sistema: [docs/PROYECTO.md](../../../docs/PROYECTO.md) · API IoT: [docs/API.md](../../../docs/API.md).

Firmware base alineado al esquematico `Schematic_Proyecto-No-Vidente_2025-11-20 (1).png`.

## Hardware identificado

- ESP32 DevKit V1
- HC-SR04
- NEO-6M GPS
- SIM800L
- Buzzer
- Motor vibrador con 2N2222A
- Boton SOS
- ESP32-CAM por UART secundaria (pendiente de confirmar cruce RX/TX exacto)

## Pines usados

- `GPIO5`: TRIG HC-SR04
- `GPIO18`: ECHO HC-SR04
- `GPIO13`: buzzer
- `GPIO32`: motor vibrador
- `GPIO27`: boton SOS
- `GPIO4`: RX GPS desde TX del NEO-6M
- `GPIO14`: TX GPS hacia RX del NEO-6M
- `GPIO16`: RX SIM800L desde TX del modulo
- `GPIO17`: TX SIM800L hacia RX del modulo
- `GPIO23`: UART hacia ESP32-CAM (segun net `UOR`)
- `GPIO22`: UART hacia ESP32-CAM (segun net `UOT`)

## Lo que hace este firmware

- Lee distancia del HC-SR04
- Genera alerta con buzzer y motor vibrador segun cercania
- Lee GPS NMEA del NEO-6M
- Detecta boton SOS
- Emite telemetria JSON por Serial
- Puede publicar estado IoT al backend `api/iot.php`
- Envía SMS por SIM800L al pulsar SOS (configurado: **+593993212257** / 0993212257) — ver [docs/SMS-SOS-EMERGENCIA.md](../../../docs/SMS-SOS-EMERGENCIA.md)

## Configuracion

1. Copia `include/smartvest_config.h.example` a `include/smartvest_config.h`
2. Ajusta `SMARTVEST_DEVICE_ID`
3. Si vas a publicar a la app web, habilita WiFi y coloca la URL completa de `api/iot.php` (usa la **IP de tu PC** en la red, no `localhost`)
4. Define `SMARTVEST_IOT_API_KEY` igual que en `api/config.local.php` o el valor por defecto del servidor (`smartvest-local-dev-key` en desarrollo)
5. Si vas a usar SOS por SMS, habilita SIM800L y coloca el numero destino

El firmware envia la cabecera HTTP `X-SmartVest-Api-Key` en cada POST a `iot.php`.

## Compilar

```bash
pio run
```

## Subir

```bash
pio run -t upload
pio device monitor
```

## Telemetria Serial

Cada segundo imprime una linea JSON como:

```json
{"deviceId":"VEST-001","distanceCm":85.4,"obstacle":"alert","gpsFix":true,"latitude":-0.18,"longitude":-78.46,"sosActive":false,"batteryLevel":null}
```

## Observaciones del esquema

- Aqui se priorizo el `.ino` viejo que ya estaba funcionando: `TRIG=5`, `ECHO=18`.
- El reporte tecnico menciona proteccion ECHO con diodos, pero el esquematico muestra divisor resistivo `2k/1k`; para firmware esto no cambia el pin, pero conviene validar la PCB real.
- **Batería:** el esquema no trae divisor de tensión hacia un ADC del ESP32. Por defecto `SMARTVEST_BATTERY_ADC_PIN` es `-1` y la web muestra «Sin sensor en PCB». Si añades un divisor (p. ej. 100k/100k) desde el pack de batería a **GPIO34** (solo entrada), define `#define SMARTVEST_BATTERY_ADC_PIN 34` en `smartvest_config.h` y calibra los valores en `updateBatteryLevel()`.
- La UART con ESP32-CAM aparece etiquetada como `UOR/UOT`, pero el cruce RX/TX debe validarse fisicamente antes de usarla.
