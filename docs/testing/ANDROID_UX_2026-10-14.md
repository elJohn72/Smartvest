# SmartVest — pruebas de interfaz y teléfono

Versión propuesta: 1.2.0-m1-ui / código 27. Ámbito SMARTVEST, datos PRUEBA.
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

## Estado del handoff — 2026-10-14

- UX implementada: tres destinos Monitoreo/Perfiles/Historial, logo, tipografía explícita, acceso y estados vacíos mejorados. Android versión 27 / 1.2.0-m1-ui.
- Validación local: assembleRelease, testDebugUnitTest y lintDebug terminaron; XML lint sin Error/Fatal. Persisten advertencias de dependencias/target ya existentes.
- Smoke real de laboratorio: readiness HTTP 200, registro PRUEBA HTTP 201, login HTTP 200 y creación de perfil PRUEBA HTTP 201 por cliente TLS verificado. No dispositivo vinculado ni telemetría inventada.
- Emulador SmartVest_UI nuevo en disco interno: instalación APK exitosa y screenshot login-100.png revisado. La ruta SDK system-images dejó de depender de un enlace roto al SSD externo, conservado como respaldo local. No borrar el AVD antiguo.
- Acceso mediante UI del emulador todavía no validado: la prueba automatizada mostró mensaje de conexión y el texto del usuario observado no coincidía completamente con el generado. Revisar entrada de teclado, credenciales y conectividad antes de atribuir una causa. No presentar ese intento como login exitoso.
- 200% de texto, pantallas autenticadas, QR, TalkBack e instalación en teléfono físico: PENDIENTES. John indicó que conectaría el teléfono; ADB aún no lo detectaba al revisar.
- No se hizo merge, deploy ni flasheo. El siguiente agente debe verificar CI del HEAD de este handoff y completar los pendientes antes de afirmar listo.
