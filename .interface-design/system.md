# SmartVest Android — sistema de interfaz

Fecha: 2026-10-14. Ámbito SMARTVEST. Pedido de John: interfaz profesional y preparada para pruebas.

Usuario: cuidador que necesita saber si recibe lecturas recientes y gestionar el perfil de la persona asistida. El SOS físico es prioritario.
Dirección: Sophistication & Trust. Base fría. Profundidad por bordes y superficies sutiles. Espaciado 4 dp, escala 4/8/12/16/20/24/32.

## Auditoría y cambio

Antes: una lista mezcla avisos largos, selección de perfiles, seis botones de administración, sensor e historial. El dato central queda abajo. Login sin identidad y con texto técnico como encabezado.
Después: tres destinos Monitoreo / Perfiles / Historial; última lectura primero; acciones administrativas en Perfiles y eliminación dentro de confirmación. Aviso compacto de laboratorio y limitación de primer plano siempre accesibles. Login con logo, título humano, formulario y acción única.

## Tokens

Azul #1E40AF, noche #0F172A, papel #F8FAFC, cuerpo #334155. Superficie blanca, borde #E2E8F0. Naranja decorativo #EA580C; texto de aviso #C2410C; SOS #B91C1C. No expresar estado solo con color.
Tipografía Material 3 definida explícitamente: título 26–28 sp, sección 20 sp, cuerpo 16 sp, secundario 14 sp, lectura principal 56 sp. Controles mínimo 48 dp, botón principal 56 dp como mínimo, sin altura fija que corte texto ampliado.
Cards 20 dp, controles 14 dp, padding 20 dp. Sin sombras grandes, gradientes, animación decorativa ni tarjetas para cada etiqueta.

## Aceptación de UI

- Login/registro, navegación de tres destinos, creación/edición, vínculo, QR y confirmación de borrado accesibles.
- Monitoreo visible sin recorrer acciones administrativas.
- Estados sin perfil/sin chaleco/sin datos/vencido/sin conexión diferenciados; no datos inventados.
- Screenshot real Android de login y pantallas con datos PRUEBA; revisión a 100% y 200% de texto.
- Compilación y unitarias; smoke API real. Evidencia física del chaleco sigue independiente.
- Conservar modelo, endpoints, firmware, firma y paquete; no ampliar alcance M2.
