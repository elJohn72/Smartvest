# Identidad SmartVest — propuesta v2

Creada 2026-10-13 por petición de John. Ámbito SMARTVEST / AISLADO. Propuesta visual para la aplicación; no cambia el manifiesto corporativo activo ni afirma registro de marca.

Símbolo: chaleco compacto con cuello en V, ondas de detección y punto de señal naranja. Sin hojas ni identidad EcoGrow. Master PNG generado mediante imagegen, fondo transparente; exports únicamente redimensionados, sin alterar el diseño.

## Paleta y uso

| Color | HEX | Uso |
|---|---|---|
| Azul señal | #1E40AF | Marca, acciones primarias; texto blanco |
| Noche | #0F172A | Títulos y superficies oscuras |
| Naranja | #EA580C | Acento del logo/decoración; no texto pequeño sobre blanco |
| Papel | #F8FAFC | Fondo general |
| Blanco | #FFFFFF | Tarjetas |
| Texto | #334155 | Cuerpo de texto |
| Secundario | #64748B | Metadatos sobre blanco; verificar fondos antes de usar |
| Aviso | #C2410C | Avisos legibles sobre blanco |
| SOS | #B91C1C | Emergencia, siempre con texto/semántica |
| Correcto | #166534 | Estado confirmado, nunca inferido por ausencia de SOS |

Colores base conservan la identidad declarada de SmartVest; tonos funcionales ya existen en Theme.kt. palette.json es referencia; CSS expone variables; Android añade BrandAccent sin alterar los estados existentes. No utilizar color como única señal.

## Activos

- smartvest-logo-master.png: original transparente de 1280 px.
- public/images/smartvest-v2/: favicon ICO 16/32/48; PNG 16/32/48/180/192/512 y tokens CSS.
- Android mipmap mdpi–xxxhdpi: iconos 48/72/96/144/192.
- Web: favicon y apple-touch-icon; manifests PWA actualizados.
- Portal de emergencia: favicon /brand/favicon.ico, servido desde imagen Docker.

Los iconos anteriores se conservan. No se cambió nombre/paquete/firma ni se desplegó. Revisión de esta rama contra codex/smartvest-native-m1; los bloqueos funcionales de M1 son independientes de la identidad.

## Contraste medido sobre blanco

Azul 8,72:1; noche 17,85:1; naranja 3,56:1; aviso oscuro 5,18:1; SOS 6,47:1. El naranja brillante no satisface 4,5:1 para texto normal.
