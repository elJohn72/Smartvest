# SmartVest PlatformIO

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
- Puede enviar SMS por SIM800L si habilitas la opcion

## Configuracion

1. Copia `include/smartvest_config.h.example` a `include/smartvest_config.h`
2. Ajusta `SMARTVEST_DEVICE_ID`
3. Si vas a publicar a la app web, habilita WiFi y coloca la URL completa de `api/iot.php`
4. Si vas a usar SOS por SMS, habilita SIM800L y coloca el numero destino

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
- No se observa divisor dedicado para medicion real de bateria hacia ADC; por eso `batteryLevel` sale `null` por defecto.
- La UART con ESP32-CAM aparece etiquetada como `UOR/UOT`, pero el cruce RX/TX debe validarse fisicamente antes de usarla.
