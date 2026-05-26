# Servidor fijo en la red local

El chaleco no puede usar `localhost`: debe apuntar a la **IP de un equipo siempre encendido** en la misma WiFi.

---

## Opción 1 — Mac con XAMPP (desarrollo)

1. Obtén la IP:
   ```bash
   ./scripts/print-lan-ip.sh
   ```
2. En `smartvest_config.h`:
   ```c
   #define SMARTVEST_API_URL "http://TU_IP/Smartvest/api/iot.php"
   ```
3. Mantén Apache y MySQL activos en XAMPP.

**Limitación:** si el Mac duerme o cambia de red, el chaleco deja de enviar datos.

---

## Opción 2 — PC / Raspberry Pi dedicado (recomendado)

1. Instala Apache + PHP + MariaDB (XAMPP, LAMP o similar).
2. Despliega el proyecto:
   ```bash
   npm run build
   ./scripts/deploy-xampp.sh   # o copia manual a htdocs/Smartvest
   ```
3. Reserva IP fija en el router (DHCP reservation) para ese equipo.
4. Actualiza `SMARTVEST_API_URL` con esa IP y vuelve a flashear el ESP32.

---

## Comprobar desde otro dispositivo

```bash
curl "http://TU_IP/Smartvest/api/iot.php?deviceId=VEST-001"
```

Debe devolver JSON con `success: true`.

---

## Firewall

En macOS, permite conexiones entrantes a Apache si el chaleco no conecta desde la LAN.
