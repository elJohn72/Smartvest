# AGENTS Guide

This repository contains the SmartVest web app, local PHP API endpoints, MariaDB schema, and ESP32 PlatformIO firmware.

## Scope

- App frontend: Vite + React + TypeScript
- Local backend: PHP endpoints under `api/`
- Database: MariaDB schema in `database.sql`
- Firmware: ESP32 PlatformIO project under `firmware/esp32/platformio-smartvest`
- Local deployment target: XAMPP at `/Applications/XAMPP/xamppfiles/htdocs/Smartvest`

## Rules Files

- No `.cursorrules` file was found.
- No `.github/copilot-instructions.md` file was found.
- No `.cursor/rules/` directory was found.

## Primary Commands

### Frontend

- Install dependencies: `npm install`
- Start dev server: `npm run dev`
- Production build: `npm run build`
- Preview built app: `npm run preview`

### Deployment to XAMPP

- Deploy current repo to XAMPP: `./scripts/deploy-xampp.sh`
- Preferred safe sequence after frontend edits: `npm run build && ./scripts/deploy-xampp.sh`

Do not run build and deploy in parallel. This repo previously served stale bundles because deploy copied files before build completed.

### Firmware

Run these from `firmware/esp32/platformio-smartvest`.

- Build firmware: `"/Users/eljhon72/Library/Python/3.12/bin/pio" run`
- Upload firmware: `"/Users/eljhon72/Library/Python/3.12/bin/pio" run -t upload --upload-port /dev/cu.usbserial-0001`
- Monitor serial manually: use `pio device monitor` if available, or a Python serial script

The exact upload port can change. Check serial ports first with `ls /dev/cu.*`.

### Database / API Checks

- Query users API: `curl "http://localhost/Smartvest/api/users.php"`
- Query one IoT device: `curl "http://localhost/Smartvest/api/iot.php?deviceId=VEST-001"`
- Test login API: `curl -X POST "http://localhost/Smartvest/api/users.php" -H "Content-Type: application/json" -d '{"action":"login","username":"...","password":"..."}'`
- MariaDB via XAMPP client: `"/Applications/XAMPP/xamppfiles/bin/mysql" -h 127.0.0.1 -P 3306 -u root smartvest`

## Test / Lint Reality

This repository does not currently define formal lint, unit test, or E2E test scripts in `package.json`.

- Available npm scripts: `dev`, `build`, `preview`
- There is no `npm test`.
- There is no `npm run lint`.
- There is no built-in single-test command.

If asked to run a single test, state clearly that no test runner is configured in the repo today.

## Practical Validation Checklist

- Run `npm run build`
- If the user depends on XAMPP, run `./scripts/deploy-xampp.sh`
- Verify the served root page: `curl "http://localhost/Smartvest/"`

- Exercise the endpoint with `curl`
- Check the response payload shape
- If schema changed, update `database.sql` and apply the same change to the live MariaDB instance when appropriate

- Run PlatformIO build
- Upload only if hardware is connected and the user wants it
- Validate by querying the backend and/or reading serial output

## Architecture Notes

- Frontend base path is `/Smartvest/` in `vite.config.ts`.
- The frontend derives API paths relative to the served pathname.
- The app is intentionally written without a `src/` directory; top-level files like `App.tsx` and `index.tsx` are expected.
- `storageService.ts` talks to `api/users.php` and falls back to `localStorage` if the API is unavailable.
- `iotService.ts` polls `api/iot.php` and keeps an in-memory device cache.
- PHP endpoints use PDO and return JSON through `json_response()` in `api/config.php`.

## TypeScript Conventions

- TypeScript strict mode is enabled in `tsconfig.json`.
- `noUnusedLocals`, `noUnusedParameters`, and `noFallthroughCasesInSwitch` are enabled.
- Prefer explicit interfaces for props and shared domain shapes.
- Shared app types live in `types.ts`.
- Use `string | null` and `number | null` explicitly when null is a valid state.
- Avoid adding `any`. If unavoidable, keep it narrow and documented.

## React Conventions

- Components are function components, often typed as `React.FC<Props>`.
- Props interfaces are usually named `Props` near the component.
- Hooks are imported directly from React when used.
- Use early returns and small helper functions instead of deeply nested JSX logic.
- Prefer minimal, direct state transitions.
- Keep state local unless it is shared across screens/services.

## Import Conventions

- External imports first.
- Local component/service/type imports after externals.
- Relative imports are used throughout; no path alias system is configured.
- Keep import groups compact; do not introduce unnecessary reordering churn.

## Naming Conventions

- Components: PascalCase, exported by name, e.g. `RegistrationForm`
- Interfaces/types: PascalCase, e.g. `UserData`, `IotData`
- Variables/functions: camelCase
- Constants: camelCase for most local constants, `UPPER_SNAKE_CASE` for config-like firmware macros and PHP constants
- Database columns: snake_case
- API payload fields: camelCase

## Formatting Conventions

- TS/TSX uses semicolons.
- Strings are mixed in existing files; preserve local consistency when editing.
- Keep JSX readable; multiline props are preferred when a tag becomes crowded.
- Avoid large formatting-only edits.
- Follow the existing style in each file instead of forcing a new one.

## Error Handling

- Frontend services often use `try/catch` with fallback behavior.
- Preserve existing fallbacks unless the task explicitly removes them.
- For login and user-facing actions, prefer returning meaningful messages rather than generic alerts.
- PHP endpoints should fail with structured JSON and an appropriate HTTP status.
- Firmware should fail soft when network/API is unavailable and continue reporting locally.

## PHP / API Guidelines

- Reuse helpers from `api/config.php`.
- Normalize DB rows before returning them to the frontend.
- Keep DB schema fields aligned with payload fields. If one changes, update both API and schema.
- Avoid destructive schema changes unless clearly requested.
- Keep endpoint responses JSON-only.

- Default local DB connection is in `api/config.php`.
- Current app database is `smartvest`.
- Main tables are `users` and `iot_states`.
- IoT data from the vest is stored in `iot_states`, not in a separate telemetry history table.

- The current working firmware follows the older `.ino` pin choices for core peripherals.
- Firmware config lives in `include/smartvest_config.h`.
- WiFi backend posting is HTTP-based, not MQTT-based.
- Do not assume the connected hardware matches the schematic perfectly; verify against working behavior and current user instructions.

## When Changing Login / User Flow

- Check `components/Login.tsx`, `App.tsx`, `services/storageService.ts`, and `api/users.php` together.
- Be careful not to reintroduce the previous demo-only login behavior.
- Validate against live API responses, not just local component state.

## When Changing IoT Flow

- Check `types.ts`, `services/iotService.ts`, `api/iot.php`, `database.sql`, and firmware payload generation together.
- Keep payload keys consistent: e.g. `distanceCm` in API/frontend should map to `distance_cm` in MariaDB.

## Agent Workflow Guidance

- Build context by reading the relevant frontend, API, schema, and firmware files first.
- Prefer the smallest correct change.
- After changing deploy-sensitive frontend code, rebuild and redeploy sequentially.
- After changing API/schema contracts, validate with `curl`.
- After changing firmware payloads, confirm the backend actually receives the new fields.
