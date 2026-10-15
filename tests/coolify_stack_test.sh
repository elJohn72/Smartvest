#!/usr/bin/env bash
# Integración contra Nginx, PHP-FPM y MariaDB del compose de laboratorio.
# No sustituye el ensayo físico ni un deploy en el VPS.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${SMARTVEST_LAB_HTTP_PORT:-18080}"
PROJECT="${SMARTVEST_COMPOSE_PROJECT:-smartvest-ci-m1}"
export SMARTVEST_LAB_HTTP_PORT="$PORT"
BASE="http://127.0.0.1:${PORT}"
COMPOSE=(docker compose -p "$PROJECT" -f infra/smartvest/compose.lab.yml)

cleanup() {
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Levantando Nginx, PHP-FPM y MariaDB..."
"${COMPOSE[@]}" up -d --build

ready=0
for _ in $(seq 1 60); do
  code="$(curl -sS -o /tmp/smartvest-ready.json -w '%{http_code}' "$BASE/api/v1/health/ready" || true)"
  if [[ "$code" == "200" ]]; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" != "1" ]]; then
  echo "La pila no llegó a readiness."
  "${COMPOSE[@]}" logs || true
  exit 1
fi

python3 - <<'PY'
import json
payload = json.load(open("/tmp/smartvest-ready.json"))
assert payload.get("status") == "ready", payload
assert payload.get("database") == "connected", payload
PY

live="$(curl -sS -o /tmp/smartvest-live.json -w '%{http_code}' "$BASE/api/v1/health/live")"
[[ "$live" == "200" ]]
python3 - <<'PY'
import json
assert json.load(open("/tmp/smartvest-live.json")).get("status") == "ok"
PY

expect_status() {
  local path="$1"
  local want="$2"
  local got
  got="$(curl -sS -o /tmp/smartvest-body.txt -w '%{http_code}' "$BASE$path")"
  if [[ "$got" != "$want" ]]; then
    echo "Esperaba $want en $path y recibí $got"
    cat /tmp/smartvest-body.txt
    exit 1
  fi
}

expect_status /api/users.php 404
expect_status /api/iot.php 404
expect_status /api/config.php 404
expect_status /api/migrations/001_native.sql 404

invalid="$(curl -sS -o /tmp/smartvest-invalid.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -H 'X-SmartVest-Api-Key: clave-de-prueba' \
  -d '{"deviceId":"VEST-LAB","bootId":"boot-corto","sequence":"10","gpsFix":"si","sosActive":"true"}' \
  "$BASE/api/v1/telemetry")"
[[ "$invalid" == "400" ]]
python3 - <<'PY'
import json
assert json.load(open("/tmp/smartvest-invalid.json"))["error"]["code"] == "INVALID_PAYLOAD"
PY

wrong_key="$(curl -sS -o /tmp/smartvest-auth.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -H 'X-SmartVest-Api-Key: clave-incorrecta' \
  -d '{"deviceId":"VEST-001","bootId":"boot-lab-01","sequence":1,"gpsFix":false,"sosActive":false}' \
  "$BASE/api/v1/telemetry")"
[[ "$wrong_key" == "401" ]]

user="cuidador_pila_$(date +%s)"
register="$(curl -sS -o /tmp/smartvest-register.json -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$user\",\"password\":\"Prueba1234!\"}" \
  "$BASE/api/v1/auth/register")"
[[ "$register" == "201" ]]

token="$(curl -sS -H 'Content-Type: application/json' \
  -d "{\"username\":\"$user\",\"password\":\"Prueba1234!\"}" \
  "$BASE/api/v1/auth/login" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"

profile="$(curl -sS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
  -d '{"presentationName":"Paciente de laboratorio","emergencyPhone":"099000000","bloodType":"O+","medicalObservations":"dato que no debe verse"}' \
  "$BASE/api/v1/profiles")"
profile_id="$(printf '%s' "$profile" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"

share="$(curl -sS -D /tmp/smartvest-share.hdr -o /tmp/smartvest-share.json -w '%{http_code}' \
  -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
  -d '{"allowedFields":["presentationName"]}' \
  "$BASE/api/v1/profiles/$profile_id/shares")"
[[ "$share" == "201" ]]
share_token="$(python3 -c 'import json; print(json.load(open("/tmp/smartvest-share.json"))["token"])')"

page="$(curl -sS -o /tmp/smartvest-qr.html -w '%{http_code}' "$BASE/emergency/$share_token")"
[[ "$page" == "200" ]]
grep -q "Paciente de laboratorio" /tmp/smartvest-qr.html
if grep -q "dato que no debe verse" /tmp/smartvest-qr.html; then
  echo "La página pública expuso un dato no consentido."
  exit 1
fi

echo "Pila Nginx/PHP-FPM/MariaDB verificada."
