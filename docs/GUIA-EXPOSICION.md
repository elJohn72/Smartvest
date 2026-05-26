# Guía de exposición — SmartVest

Material para **presentar el proyecto** en ferias, defensas de tesis o demostraciones en la Unidad Educativa Andrés Bello (Quinindé) u otros eventos.

---

## Mensaje principal (30 segundos)

> **SmartVest** es un chaleco inteligente para personas con discapacidad visual que detecta obstáculos, activa alertas en el propio chaleco y avisa al familiar por **web y SMS** en una emergencia. Además guarda un **perfil médico con código QR** para paramédicos o terceros.

---

## Qué mostrar en pantalla

1. **Landing** — `http://localhost/Smartvest/` (o IP del servidor en la red del evento).
2. **Login** — usuario registrado vinculado a `VEST-001`.
3. **Perfil de monitoreo:**
   - Distancia en tiempo real.
   - Gráfico de historial.
   - Guía de alertas hápticas.
   - Banner «Activar notificaciones» (PWA).
4. **QR de emergencia** — modo texto o enlace (datos médicos sin depender del chaleco).

---

## Demostración en vivo (orden sugerido)

| Paso | Acción | Qué explica al público |
|------|--------|------------------------|
| 1 | Chaleco encendido en la mesa | ESP32 + sensores en PCB real |
| 2 | Acercar la mano al ultrasonido | Cambia distancia en la web (ej. ~214 cm → menos) |
| 3 | Alejar / acercar obstáculo | Colores y vibración según umbrales (ver ALERTAS-ACCESIBLES) |
| 4 | Pulsar **botón SOS** | Pantalla roja en web + sonido en PC del cuidador |
| 5 | Mostrar celular del cuidador **0963930791** | Llega SMS con `VEST-001` y mapa si hay GPS |
| 6 | Escanear **QR** impreso | Acceso a datos médicos sin login |

**Duración total recomendada:** 5–8 minutos + preguntas.

---

## Requisitos técnicos el día del evento

### Servidor

- [ ] XAMPP (Apache + MariaDB) o PC con la app desplegada.
- [ ] IP fija en la red WiFi del salón — ver [SERVIDOR-FIJO-LAN.md](./SERVIDOR-FIJO-LAN.md).
- [ ] `./scripts/print-lan-ip.sh` y misma IP en `SMARTVEST_API_URL` del firmware.

### Chaleco

- [ ] WiFi del salón configurada en `smartvest_config.h`.
- [ ] `deviceId` = `VEST-001` igual que el usuario en la web.
- [ ] SIM800 con **antena** y **saldo** (para demo SMS).
- [ ] Batería o fuente de la PCB (no depender del USB del portátil).

### Web

- [ ] Perfil abierto en portátil o tablet del «cuidador».
- [ ] Notificaciones del navegador **activadas**.
- [ ] Probar SOS una vez **antes** de abrir al público.

---

## Datos del piloto documentados

| Dato | Valor |
|------|--------|
| Dispositivo | `VEST-001` |
| Usuario de prueba | Anthony Perez |
| SMS emergencia (cuidador) | 0963930791 / +593963930791 |
| Institución (landing) | Unidad Educativa Andrés Bello, Quinindé |
| Contacto proyecto | contacto@smartvest.app |

---

## Preguntas frecuentes del jurado

**¿Reemplaza al bastón blanco?**  
No. Complementa detección a nivel del pecho; el bastón sigue siendo esencial para el suelo.

**¿Funciona sin internet?**  
Las alertas **locales** (buzzer/vibrador) y el **SMS** sí. El mapa en la web necesita que el chaleco y el servidor estén en la misma red WiFi.

**¿Por qué no miden batería?**  
La PCB actual no tiene divisor al ADC; está documentado y previsto en el roadmap (GPIO34).

**¿Cómo se compara con productos comerciales?**  
Ver [COMPARATIVA-MERCADO.md](./COMPARATIVA-MERCADO.md). SmartVest destaca en **emergencia médica + QR + costo abierto**.

**¿Es seguro el QR con datos médicos?**  
Se ofrecen tres modos (texto offline, enlace con datos, solo ID). Para exposición, recomienda el modo acorde al público.

---

## Archivos que el jurado puede revisar en el repo

| Documento | Tema |
|-----------|------|
| [PROYECTO.md](./PROYECTO.md) | Visión general |
| [ARQUITECTURA.md](./ARQUITECTURA.md) | Diagramas web + API + ESP32 |
| [SMS-SOS-EMERGENCIA.md](./SMS-SOS-EMERGENCIA.md) | SMS al cuidador 0963930791 |
| [COMPARATIVA-MERCADO.md](./COMPARATIVA-MERCADO.md) | Estado del arte |
| [ROADMAP-MEJORAS.md](./ROADMAP-MEJORAS.md) | Trabajo futuro |
| [FUNCIONALIDADES.md](./FUNCIONALIDADES.md) | Lista de features |

---

## Checklist impreso (día D)

```text
[ ] Servidor encendido — curl http://IP/Smartvest/api/iot.php?deviceId=VEST-001
[ ] Chaleco en línea — "En línea" en perfil
[ ] SMS de prueba — SOS -> OK en serial
[ ] QR impreso en mesa
[ ] Cableado ordenado / seguridad eléctrica
[ ] Plan B: video corto si falla WiFi o GSM
```

---

## Créditos sugeridos en lámina final

SmartVest — Sistema de emergencia inteligente · ESP32 · MariaDB · React · PHP · Quinindé, Ecuador · 2026.
