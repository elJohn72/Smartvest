# Comparativa de mercado — dispositivos similares a SmartVest

Análisis de referencia (2025–2026) para orientar mejoras del proyecto. SmartVest combina **chaleco IoT** + **perfil médico/QR** + **panel web para cuidadores**.

---

## 1. Tipos de solución en el mercado

| Familia | Ejemplos | Enfoque |
|---------|----------|---------|
| Chaleco / mochila con IA | [NOA (biped.ai)](https://biped.ai/) | Cámaras, IA, audio espacial, GPS; complementa bastón |
| Wearable + app | [Vizion 1](https://www.besensable.com/meet-vizion-1) | Ultrasonido en bandas + háptica + navegación en el móvil |
| Ecosistema | [VISO](http://viso-nori.com/) | Gafas + bastón + asistente de voz offline |
| Prototipo académico / open | Drishti, Path Pulse, papers ESP32 | Ultrasonido + vibración + SOS + dashboard |

---

## 2. Tabla comparativa (SmartVest vs referentes)

| Capacidad | SmartVest (actual) | Proyectos ESP32 / papers | Productos premium |
|-----------|-------------------|--------------------------|-------------------|
| Obstáculo (ultrasonido) | 1× HC-SR04 frontal | A menudo 2–3 sensores | Cámara + IA (170°) |
| Alerta local (buzzer/vibrador) | Sí, por umbrales | Sí | Háptica + audio 3D |
| GPS + mapa cuidador | Web + NEO-6M | Variable | GPS + contexto visual |
| SOS | GPIO27 + web; SMS si SIM800 activo | GSM frecuente | Alertas automáticas |
| Perfil médico + QR | **Diferenciador fuerte** | Raro | No prioritario |
| Historial telemetría | `iot_history` | Poco común en baratos | Nube propietaria |
| Batería en PCB | No (ADC desactivado) | Divisor + ADC típico | Gestión avanzada |
| Sin internet (usuario caminando) | Hardware alerta sí; mapa no | Modo offline buscado | On-device AI |
| Voz al usuario | Pitidos (buzzer) | Voz en app/servidor | Core del producto |

---

## 3. Dónde SmartVest ya es competitivo

1. **Emergencia médica integrada** — sangre, alergias, contacto, QR para terceros.
2. **Stack abierto y barato** — XAMPP/MariaDB/ESP32 HTTP, ideal para institución educativa.
3. **Loop real validado** — PCB → WiFi → `api/iot.php` → perfil web.
4. **Hardware preparado** — SIM800L y UART ESP32-CAM en esquema (por activar).

---

## 4. Brechas prioritarias (sin copiar productos de miles de dólares)

### Usuario que camina

- Más cobertura espacial (pecho/cabeza): 2.º/3.er ultrasonido o ESP32-CAM.
- Feedback accesible documentado (ver [ALERTAS-ACCESIBLES.md](./ALERTAS-ACCESIBLES.md)).
- Operación si cae WiFi: buzzer/vibrador/SMS locales.

### Cuidador

- Notificaciones cuando SOS (PWA + Notification API — implementado).
- Servidor fijo en LAN (ver [SERVIDOR-FIJO-LAN.md](./SERVIDOR-FIJO-LAN.md)).
- Historial de distancia (implementado).

### Producto

- Pruebas con usuarios reales (métricas O&M).
- Servidor no atado al Mac de desarrollo.

---

## 5. Referencias útiles

| Recurso | URL / nota |
|---------|------------|
| NOA / biped | https://biped.ai/ |
| NavWear (Brasil, 2025) | https://techxplore.com/news/2025-06-wearable-device-people-obstacles.html |
| Path Pulse (ESP32, 3 ultrasonidos) | https://github.com/AmirHosseinOsooli/blind-assistance-system-Fall-2025 |
| Paper IoT smart glasses (ESP32+GPS+GSM) | https://isarpublisher.com/backend/public/assets/articles/1747563544-ISARJST-1592025-GP.pdf |
| Drishti (ESP32-S3 conceptual) | https://www.ijert.org/drishti-a-smart-wearable-design-for-obstacle-detection-and-navigation-aid-for-visually-impaired-individuals |

---

## 6. Posicionamiento recomendado

**No competir de entrada con NOA/VISO en IA y precio.** SmartVest debe destacar como:

> Sistema de **emergencia + movilidad económica** para instituciones y familias, con hardware reparable y datos médicos bajo control local.

Ver plan de implementación en [ROADMAP-MEJORAS.md](./ROADMAP-MEJORAS.md).
