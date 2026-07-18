# Teleprompter — lee esto en voz alta (4–6 min)

**Antes de darle a grabar:** XAMPP (Apache+MySQL) en verde · Postman con la colección · Cursor con `api/dashboard.php`.

**Login Postman (variables):**
- username: `demo.s8@smartvest.local`
- password: `DemoS8!2026`
- iotKey: `smartvest-local-dev-key`

---

### 0 · Intro

Soy John Miraba. En este taller optimizo el backend real de SmartVest, con PHP y MariaDB: cache-aside, corrección del N+1, cola asíncrona, lazy versus eager loading, y autenticación sin reconsultar la base de datos.

### 1 · Login

En Postman hago login. La API me devuelve un token. Ese token lo mando en el header X-Smartvest-Token, sin volver a pedir el password a la base.

*(Ejecuta: 1. Login → copia el valor `token` a la variable de colección `token`)*

### 2 · Antes — N+1

Ahora demuestro el anti-patrón. Llamo a dashboard con mode=n1: una consulta de usuarios y luego una consulta IoT por cada deviceId. Miren queryCount: es 1 + N, y el tiempo en elapsedMs.

*(Ejecuta: 2. ANTES — dashboard N+1 · señala queryCount)*

### 3 · Después — eager

La corrección es eager loading: un solo LEFT JOIN entre users e iot_states. queryCount queda en 1.

*(Ejecuta: 3. DESPUÉS — dashboard eager · abre `api/dashboard.php` y muestra el JOIN)*

### 4 · Cache-aside

Repito la misma petición. Ahora cache es HIT y queryCount es 0: respondemos desde cache-aside con TTL e invalidación al escribir.

*(Ejecuta: 4. DESPUÉS — dashboard cache HIT · opcional: muestra `api/lib/cache_aside.php`)*

### 5 · Cola async

Al postear telemetría con SOS, el HTTP no hace el prune ni la notificación en el request: encola jobs. Corro el worker y los jobs pasan a done. Así el chaleco ESP32 no espera trabajo pesado.

*(Ejecuta: 5. POST IoT · luego en Terminal:)*

```bash
/Applications/XAMPP/xamppfiles/bin/php /Applications/XAMPP/xamppfiles/htdocs/Smartvest/api/worker.php --once
```

### 6 · Lazy vs eager + cierre

En el listado de usuarios aplico lazy: no traigo la foto pesada. En el dashboard aplico eager del estado IoT porque sí lo necesito junto al usuario.

Repositorio: https://github.com/elJohn72/Smartvest  
Con esto bajamos consultas, latencia y trabajo síncrono en el request crítico.

---

**Fin.** Detén QuickTime → Guardar → subir a Drive → pegar URL en EVA.
