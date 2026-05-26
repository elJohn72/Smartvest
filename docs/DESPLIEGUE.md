# Despliegue y hosting

Opciones para publicar SmartVest en Internet o en una red local.

---

## Opción 1: XAMPP (local o servidor en LAN) — recomendada para desarrollo

Ya documentada en [INSTALACION.md](./INSTALACION.md).

- URL típica: `http://localhost/Smartvest/` o `http://IP-DEL-PC/Smartvest/`
- El ESP32 debe usar la **IP de la PC**, no `localhost`, en `SMARTVEST_API_URL`.

```bash
npm run build && ./scripts/deploy-xampp.sh
```

---

## Opción 2: Vercel (frontend estático)

Adecuado para la **parte React**. La API PHP **no** corre en Vercel sin adaptar a serverless o usar otro backend.

Pasos resumidos:

1. Sube el repo a GitHub.
2. Importa en [vercel.com](https://vercel.com).
3. Framework: **Vite**.
4. Variable de entorno: ya **no** hace falta `API_KEY` en el cliente para Gemini (se usa `api/address.php` en tu servidor PHP).

**Limitación:** necesitas hospedar `api/*.php` y MariaDB en otro sitio (Railway, VPS, XAMPP con túnel, etc.) o la app solo funcionará en modo `localStorage` sin backend.

Si usas Vercel solo para UI, configura `base: '/Smartvest/'` en `vite.config.ts` (ya está).

---

## Opción 3: GitHub Pages

El repo puede incluir workflow en `.github/` para publicar `dist/`.

- Misma limitación que Vercel: **sin PHP** en GitHub Pages.
- Útil para demo estática de la landing; perfil/IoT requieren API externa.

---

## Opción 4: Servidor VPS (producción completa)

Stack recomendado:

| Componente | Servicio |
|------------|----------|
| Web + PHP | Apache o Nginx + PHP-FPM |
| BD | MariaDB |
| HTTPS | Let's Encrypt |
| Dominio | ej. `smartvest.tudominio.com` |

Pasos:

1. Clonar repo en el servidor.
2. `npm ci && npm run build`.
3. Apuntar document root a `dist/` + copiar carpeta `api/`.
4. Importar `database.sql`.
5. Crear `api/config.local.php` con claves fuertes (IoT y Gemini).
6. Cambiar `SMARTVEST_IOT_API_KEY_DEFAULT` o usar solo config local.

---

## Archivos SEO al desplegar

El script `deploy-xampp.sh` copia:

- `public/robots.txt`
- `public/sitemap.xml`

Ajusta las URLs en `sitemap.xml` si tu dominio no es `/Smartvest/` en la raíz.

---

## Checklist antes de producción pública

- [ ] Contraseñas solo como hash en BD.
- [ ] `config.local.php` con claves fuertes (no la clave dev de IoT).
- [ ] HTTPS activo.
- [ ] No exponer phpMyAdmin a Internet sin protección.
- [ ] Revisar qué datos van en el QR «enlace completo» (datos médicos en URL).
- [ ] Backup periódico de MariaDB.

---

## Referencia legada

El archivo [LEEME_HOSTING.txt](../LEEME_HOSTING.txt) menciona Vercel y variables `API_KEY` en el cliente; la versión actual prefiere **Gemini en el servidor**. Usa este documento como fuente actualizada.

---

## Siguiente lectura

- [Instalación](./INSTALACION.md)
- [README principal](../README.md)
