# Arquitectura del sistema

Cómo se relacionan la web, la API, la base de datos y el chaleco ESP32.

---

## Diagrama de componentes

```mermaid
flowchart TB
  subgraph cliente [Navegador]
    UI[React App /Smartvest/]
    LS[(localStorage respaldo)]
  end

  subgraph xampp [Servidor XAMPP]
    Apache[Apache]
    PHP[api/*.php]
    DB[(MariaDB smartvest)]
  end

  subgraph hardware [Chaleco]
    ESP[ESP32]
    US[HC-SR04]
    GPS[NEO-6M]
    SOS[Botón SOS]
  end

  UI -->|fetch JSON| Apache
  Apache --> PHP
  PHP --> DB
  UI -.->|si API falla| LS
  ESP -->|POST + Api-Key| PHP
  US --> ESP
  GPS --> ESP
  SOS --> ESP
```

---

## Carpetas del frontend (sin `src/`)

El proyecto coloca el código React en la **raíz** del repositorio (convención histórica del repo):

| Ruta | Contenido |
|------|-----------|
| `App.tsx` | Navegación entre pantallas (`AppScreen` enum) |
| `index.tsx` | Montaje React + `ToastProvider` |
| `components/` | UI: Landing, Login, Registration, QR, Profile, etc. |
| `services/` | Lógica de red y estado cliente |
| `utils/` | Funciones puras reutilizables |
| `types.ts` | Interfaces `UserData`, `IotData` |

### Pantallas (`AppScreen`)

| Valor | Pantalla |
|-------|----------|
| `LANDING` | Página de inicio / marketing |
| `LOGIN` | Usuario+contraseña o ID manual |
| `REGISTER` | Formulario de registro |
| `QR_VIEW` | QR tras registro exitoso |
| `PROFILE` | Monitoreo + datos médicos |

### Parámetros URL

| Parámetro | Efecto |
|-----------|--------|
| `?uid=<uuid>` | Abre perfil si el usuario existe en el servidor/local |
| `?data=<base64>` | Importa perfil público embebido en el enlace (modo portable) |

---

## Flujo de datos: registro de usuario

```mermaid
sequenceDiagram
  participant U as Usuario
  participant R as RegistrationForm
  participant S as storageService
  participant API as users.php
  participant DB as MariaDB

  U->>R: Completa formulario
  R->>R: validateRegistrationForm
  R->>S: saveUser(user)
  S->>API: POST JSON (sin password en respuestas)
  API->>DB: INSERT/UPDATE users
  API-->>S: success
  S-->>R: OK
  R->>U: Pantalla QR
```

---

## Flujo de datos: telemetría IoT

```mermaid
sequenceDiagram
  participant ESP as ESP32
  participant API as iot.php
  participant DB as iot_states
  participant W as iotService.ts
  participant P as UserProfile

  loop cada 5s aprox
    ESP->>API: POST + X-SmartVest-Api-Key
    API->>DB: UPSERT por device_id
  end

  loop cada 2s en perfil
    W->>API: GET ?deviceId=
    API->>DB: SELECT
    API-->>W: JSON data
    W->>P: actualiza mapa / SOS / obstáculos
  end
```

### Estado de conexión en la UI

`iotService.ts` clasifica:

| Estado | Significado |
|--------|-------------|
| `connecting` | Primera consulta en curso |
| `online` | Datos recibidos hace menos de 30 s |
| `stale` | Sin respuesta reciente o datos viejos |
| `offline` | Sin fila en BD para ese `deviceId` |

---

## Resolución de rutas API desde el navegador

El frontend **no** usa URL fija `http://localhost/api`. Calcula la base según la ruta actual:

- Si la URL es `http://localhost/Smartvest/` → API en `/Smartvest/api/`
- Implementado en `getApiBasePath()` dentro de `storageService.ts` e `iotService.ts`

---

## Despliegue en XAMPP

1. `npm run build` → genera `dist/`.
2. `scripts/deploy-xampp.sh` → copia todo el repo a `htdocs/Smartvest`, sustituye `index.html` y `assets/` por los de `dist/`.
3. Apache sirve PHP desde `api/` en la misma carpeta.

**Importante:** siempre **build antes de deploy** para no servir JavaScript antiguo.

---

## Spec Kit (organización del proyecto)

El repo incluye [GitHub Spec Kit](https://github.com/github/spec-kit):

- `.specify/` — plantillas y scripts SDD.
- `.cursor/skills/speckit-*` — comandos como `/speckit-specify`, `/speckit-plan`.
- `specs/001-web-platform-audit/` — auditoría web documentada.
- `.specify/memory/constitution.md` — principios del proyecto (seguridad, IoT, deploy).

---

## Siguiente lectura

- [API y base de datos](./API.md)
- [Funcionalidades](./FUNCIONALIDADES.md)
