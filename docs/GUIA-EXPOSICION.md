# Guion de exposición — SmartVest

**Enfoque:** Aplicaciones Móviles y Sistemas Embebidos

**Duración sugerida:** 8–9 minutos

**Uso:** guía privada del expositor. No compartir esta ventana en OBS.

## Antes de grabar

- Abre la aplicación **SmartVest** que ya está instalada en el Redmi. No uses Expo Go ni la app `SmartVest Demo`.
- Conecta el teléfono y la Mac a la misma Wi‑Fi del servidor. El APK conserva la URL local que ya tenía configurada.
- Inicia Apache y MariaDB en XAMPP; deja abierta la página de SmartVest y phpMyAdmin en la Mac.
- Usa únicamente la cuenta de prueba que registraste y que está marcada `PRUEBA/LOCAL`. Entra con el usuario y la contraseña que tú creaste; esta guía no los almacena.
- En OBS muestra el Redmi con `scrcpy` y tu cámara. Cambia a la ventana de phpMyAdmin solo para la parte de base de datos. Mantén esta guía y las pantallas de credenciales fuera de la captura.
- Enciende el chaleco y comprueba que la ESP32 esté conectada a la Wi‑Fi y que llegue telemetría reciente. No presentes una lectura de ejemplo como si fuera del sensor.
- Avisa al contacto de prueba antes de pulsar SOS: si el SIM800L está conectado y habilitado, el firmware puede enviarle un SMS real.

## Secuencia: qué hacer y qué decir

### 1. Presentación — 0:00 a 0:40

**Haz:** muestra tu rostro y el chaleco.

**Di:** «SmartVest es un chaleco de asistencia para personas con discapacidad visual. El sensor ultrasónico detecta obstáculos; la ESP32 procesa la distancia y transmite el estado al servidor. La aplicación móvil presenta el perfil, la ubicación y las alertas para el cuidador».

### 2. Abrir la aplicación e ingresar — 0:40 a 1:30

**Haz:** en el Redmi abre **SmartVest** → **Ingresar**. Escribe el usuario y la contraseña de la cuenta de prueba que registraste.

**Di:** «Esta es la misma aplicación SmartVest completa: inicia sesión y consulta el perfil asociado al chaleco. No estoy usando Expo Go ni una aplicación distinta».

### 3. Mostrar el perfil — 1:30 a 2:20

**Haz:** enseña el nombre de prueba, la información médica de demostración y el contacto ficticio. No abras datos reales.

**Di:** «El perfil reúne los datos necesarios para orientar a un familiar o a quien brinde ayuda. La información de esta demostración es de prueba y queda asociada a la cuenta registrada».

### 4. Distancia, alertas e historial — 2:20 a 3:40

**Haz:** acerca y aleja un obstáculo del sensor. Enseña la distancia y el historial cuando se actualicen. Si el chaleco activa buzzer o vibrador, señala la respuesta física.

**Di:** «La lectura cambia cuando varía la distancia. El chaleco genera sus alertas locales según los umbrales configurados; la aplicación también permite revisar las lecturas guardadas. Aquí estoy mostrando datos recibidos del dispositivo».

**Si no llegan lecturas reales:** di «En este momento no hay señal reciente del chaleco» y continúa con las pantallas; no uses datos semilla como si fueran una lectura real.

### 5. GPS — 3:40 a 4:30

**Haz:** muestra **Ubicación en tiempo real**. Solo si el GPS tiene una fijación válida, enseña el mapa; evita dejar coordenadas exactas visibles en la grabación.

**Di:** «Cuando el módulo GPS obtiene fijación, la ESP32 reporta la ubicación y SmartVest la representa en el mapa. La disponibilidad depende de la señal satelital y de la conexión con el servidor».

**Si dice “GPS sin fijar”:** no simules una posición. Di que el módulo necesita cielo abierto o una ventana para adquirir satélites.

### 6. Botón SOS y GSM — 4:30 a 5:45

**Haz:** con el contacto de prueba avisado, pulsa el **botón físico SOS del chaleco**. Muestra la alerta roja en la aplicación. No pulses «Llamar a emergencia» durante la grabación.

**Di:** «El SOS se activa desde el botón físico del chaleco. La ESP32 reporta el evento y la aplicación presenta la alerta, el contacto y la ubicación disponible. Si el SIM800L está conectado, habilitado y tiene señal y saldo, el firmware intenta enviar un SMS. La aplicación muestra el evento SOS, pero no recibe confirmación de entrega del SMS».

**No afirmes que el SMS fue entregado** hasta comprobarlo en el teléfono receptor o en el monitor serial. No muestres números reales. Al terminar la prueba, desactiva el SOS con el control físico correspondiente.

### 7. Base de datos en phpMyAdmin — 5:45 a 6:45

**Haz:** en la Mac cambia a phpMyAdmin, selecciona `smartvest` y enseña las tablas `users`, `iot_states` e `iot_history`. Para evidencia de telemetría, abre `iot_history` y muestra solo filas recientes del dispositivo de prueba. Regresa luego a la app.

**Di:** «La API PHP recibe las lecturas y las guarda en MariaDB. `iot_states` conserva el último estado y `iot_history` permite revisar lecturas anteriores. La aplicación y phpMyAdmin consultan el mismo entorno local».

No expongas contraseñas, claves API, perfiles clínicos reales ni números de contacto. Si solo existe la fila inicial de ejemplo, identifícala como dato semilla.

### 8. Cierre — 6:45 a 7:15

**Haz:** vuelve a la aplicación y deja visible el perfil o el panel del chaleco.

**Di:** «SmartVest integra una aplicación móvil, una API, una base de datos y un sistema embebido. El chaleco detecta y alerta localmente; la aplicación facilita el seguimiento del cuidador. Gracias».

## Estado real de cada función

| Función | Qué muestra SmartVest | Qué se necesita para probarla en vivo |
|---|---|---|
| Inicio, acceso y perfil | Pantallas de la aplicación completa y perfil autenticado | Cuenta `PRUEBA/LOCAL` existente en la base local |
| Distancia e historial | Telemetría y puntos guardados | ESP32 encendida, Wi‑Fi y API accesibles; comprobar fecha reciente |
| SOS | Alerta roja cuando llega `sosActive=true` | Pulsar el botón físico del chaleco |
| GPS | Mapa cuando existe fijación válida | Módulo GPS con señal satelital |
| GSM/SMS | La tarjeta explica el envío asociado al SOS | SIM800L habilitado y conectado, antena, señal y saldo; confirmar recepción aparte |
| MariaDB | Tablas y lecturas guardadas | Apache y MariaDB activos en XAMPP |

**Límite conocido:** la API actual no reporta a la aplicación el nivel de señal GSM ni una confirmación de entrega del SMS. No presentes esas métricas como si ya existieran.

## Respuestas breves

**¿Reemplaza al bastón blanco?** No; es una ayuda complementaria para detectar obstáculos y avisar al cuidador.

**¿Qué pasa si no hay internet?** Las alertas locales del chaleco pueden seguir funcionando. La sincronización con la aplicación depende de la red local y del servidor; el SMS depende de la cobertura móvil del SIM800L.

**¿Por qué puede no aparecer el mapa?** El módulo GPS necesita obtener fijación. Sin coordenadas válidas la app informa que todavía no hay posición.

**¿La app sabe si el SMS llegó?** No. El firmware intenta enviarlo; se confirma por el teléfono receptor o el monitor serial.

## Archivos técnicos para abrir antes de grabar

- `components/UserProfile.tsx` — perfil, tarjeta SOS, GSM/SMS, ubicación e historial.
- `mobile/App.tsx` — APK Android que abre la aplicación SmartVest completa.
- `services/iotService.ts` — consulta periódica de telemetría.
- `api/iot.php` — recepción y lectura de estado del chaleco.
- `firmware/esp32/platformio-smartvest/src/main.cpp` — sensores, GPS, SOS y envío del SMS.
- `database.sql` — tablas de la base SmartVest.

## Lista rápida de salida

```text
[ ] SmartVest abre en el Redmi y permite ingresar con la cuenta PRUEBA/LOCAL.
[ ] Apache y MariaDB responden en la Mac.
[ ] La lectura mostrada es reciente o se declara que no hay señal.
[ ] GPS solo se presenta como disponible si tiene fijación válida.
[ ] Contacto de prueba avisado antes de activar SOS.
[ ] SMS verificado por separado; no se afirma entrega sin evidencia.
[ ] OBS captura teléfono y rostro; esta guía queda fuera del video.
[ ] phpMyAdmin muestra datos de prueba, sin claves ni perfiles reales.
```
