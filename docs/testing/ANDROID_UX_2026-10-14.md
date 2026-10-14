# SmartVest — pruebas de interfaz y teléfono

Candidato de interfaz: 1.2.4-m1-ui / código 31. Ámbito SMARTVEST, datos PRUEBA.
Sistema visual: `.interface-design/system.md`.

## Preparación

El laboratorio configurado responde en HTTPS. No sustituirlo por XAMPP ni abrir una API en claro. El firmware/ESP32 debe enviar datos reales para validar telemetría; un registro semilla no acredita lectura física.

Conectar el teléfono por USB, activar depuración USB y aceptar la autorización en el teléfono. No compartir contraseñas ni habilitar acceso ADB por internet.

Desde `android-native/`, con SDK interno configurado:

```bash
./gradlew --no-daemon :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
adb devices -l
adb install -r app/build/outputs/apk/release/app-release.apk
```

Si la firma no coincide, detener la actualización y conservar la app/datos existentes; no desinstalar para forzarla. No asumir éxito por nombre de paquete.

## Recorrido de prueba

1. Acceso: registrar/iniciar sesión con cuenta PRUEBA. Contraseña oculta, error comprensible, teclado sin tapar formulario.
2. Monitoreo sin perfiles: acción clara Crear perfil, sin lecturas inventadas.
3. Perfiles: crear PRUEBA, editar, elegir persona, vincular ID real y código de un solo uso. No usar un identificador ficticio como dispositivo real.
4. Monitoreo: lectura reciente y antigüedad visibles; desvinculado, sin datos, vencido y sin conexión distinguibles.
5. SOS físico: observar alerta roja con texto. Pulsación breve y red caída son pruebas de firmware separadas; no simular aceptación física.
6. QR: elegir campos, generar, escanear desde otro teléfono, revocar y comprobar que el enlace dejó de abrir.
7. Historial: corresponde al perfil seleccionado. Volver a Monitoreo sin perder selección.
8. Ampliar texto al 200%: todas las pestañas y diálogos siguen accesibles por desplazamiento, sin textos cortados ni botones inaccesibles.
9. TalkBack: orden de foco, etiquetas de navegación y controles, error anunciado, SOS anunciado sin leer cada muestra automáticamente.
10. Background/foreground y cierre de sesión: polling visible y sesión consistente. No prometer push con app cerrada.

## Cierre

Registrar dispositivo, Android, commit/APK, capturas saneadas y cada resultado. No filmar datos personales ni contraseñas. La evidencia de UI no sustituye SC-001–005, restauración/rollback ni validación del chaleco.

## Resultado de la revisión visual — 2026-10-14

Ámbito SMARTVEST, modo aislado. Emulador `SmartVest_UI` (`emulator-5554`), API 35, 1080×1920 con densidad 420. El AVD guarda 320×640 a 160 dpi; la sesión de prueba usa ese tamaño solo como base y la presentación en 1080×1920. No se borró el AVD ni se volvió a apuntar el SDK al SSD.

### Qué cambió en la interfaz

- El error de acceso distingue credenciales, límite de intentos, error del laboratorio y falta de conexión. Un HTTP 401 ya no se presenta como “No se pudo conectar”.
- Perfiles deja de ser una pila de chips: cada persona es una tarjeta, con el chaleco vinculado o no, y las acciones quedan bajo la tarjeta elegida.
- Historial vacío explica el estado y vuelve a Monitoreo. El aviso de laboratorio vive en Monitoreo, no en las tres pestañas.
- El nombre del perfil puede ocupar varias líneas. Los diálogos de perfil, vinculación y QR limitan su altura y se desplazan, con espacio al final para que el último campo quede sobre los botones.
- El listado deja 64 dp libres al final para que el último texto pueda subir por encima de la barra de navegación con texto grande o pantalla compacta.
- El teclado del acceso avanza de usuario a contraseña y envía con Listo.

### Pruebas hechas

| Prueba | Resultado |
| --- | --- |
| Acceso en el emulador con cuenta PRUEBA ya creada | Llegó a Monitoreo con el perfil “PRUEBA UX — Sin chaleco”. Un envío largo de una sola vez por ADB recortó el usuario; no es un límite de la app. |
| Error de credenciales | Texto visible: “Usuario o contraseña incorrectos.” Contraseña oculta. Captura `login-error-100.png`. |
| Monitoreo, Perfiles, Historial, editar, vincular y consentimiento QR | Abiertos en el emulador. No se guardó un cambio, no se envió un código de chaleco y no se generó un QR. |
| Texto 200% | Pestañas, Vincular, QR y el aviso de laboratorio quedan completos al desplazar. El diálogo de edición muestra Observaciones por encima de Guardar. |
| Pantalla compacta 720×1280 | Vincular, QR y el aviso de laboratorio quedan completos al desplazar. |
| Cierre de sesión | El control “Cerrar sesión” vuelve a la pantalla de acceso, con los campos vacíos. |
| Firma del APK en el emulador | Actualización con `adb install -r`. Paquete `com.ajtecnology.smartvest`. La firma no cambió. |
| Laboratorio HTTPS | Readiness, registro, login y perfil PRUEBA ya respondieron 200/201 con TLS verificado en la pasada anterior. No se vinculó hardware ni se inventó telemetría. |
| Teléfono físico | ADB solo veía el emulador. No se instaló en el Redmi. |
| TalkBack | El servicio quedó enlazado y el motor de voz habló. En acceso, el orden del árbol es marca, bienvenida, usuario, contraseña, mostrar contraseña, iniciar sesión y crear cuenta. “Cerrar sesión” tiene descripción. La pestaña elegida marca como seleccionado el nodo que contiene su nombre. El error de acceso es una región en vivo educada. La región asertiva sigue solo en la tarjeta de SOS. No se recorrió cada control con gestos ni se anunció una lectura, porque no hay telemetría. |

Capturas saneadas en `docs/testing/screenshots/`. No incluyen contraseña, token ni QR generado.

### Lo que esto no acredita

M1 no está aceptado. Siguen abiertos SC-001 a SC-005, el chaleco real, la prueba con la Mac apagada, la restauración y la firma en el teléfono que ya tiene la app. El Redmi no apareció en ADB en esta pasada. TalkBack habló en el emulador; falta el recorrido de gestos en el teléfono.
