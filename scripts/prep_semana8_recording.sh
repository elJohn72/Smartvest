#!/bin/zsh
# Prepara SmartVest para grabar el video del taller Semana 8.
# Requisito: Apache + MySQL encendidos en XAMPP Manager.
# Uso:
#   ./scripts/prep_semana8_recording.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PHP="/Applications/XAMPP/xamppfiles/bin/php"
MYSQL="/Applications/XAMPP/xamppfiles/bin/mysql"
BASE="http://localhost/Smartvest/api"
IOT_KEY="smartvest-local-dev-key"
USER="demo.s8@smartvest.local"
PASS='DemoS8!2026'
XAMPP_TARGET="/Applications/XAMPP/xamppfiles/htdocs/Smartvest"
ACADEMIC="/Volumes/Extreme SSD/Escritorio/AJENZA S.A.S./APRENDIZAJE/5to semestre/Aplicaciones Moviles - Paralelo D/Semana 08/_Taller_Optimizacion_SmartVest"

echo "==> Copiando API optimizada a XAMPP..."
mkdir -p "$XAMPP_TARGET/api/lib"
cp -f "$REPO_ROOT/api/config.php" "$XAMPP_TARGET/api/"
cp -f "$REPO_ROOT/api/users.php" "$XAMPP_TARGET/api/"
cp -f "$REPO_ROOT/api/iot.php" "$XAMPP_TARGET/api/"
cp -f "$REPO_ROOT/api/dashboard.php" "$XAMPP_TARGET/api/"
cp -f "$REPO_ROOT/api/worker.php" "$XAMPP_TARGET/api/"
cp -f "$REPO_ROOT/api/lib/"*.php "$XAMPP_TARGET/api/lib/"

echo "==> Comprobando MySQL..."
if ! "$MYSQL" -uroot -e "SELECT 1" >/dev/null 2>&1; then
  echo ""
  echo "❌ MySQL/MariaDB no está corriendo."
  echo "   En XAMPP Manager: Start en MySQL y Start en Apache."
  echo "   Luego: ./scripts/prep_semana8_recording.sh"
  open -a "XAMPP" 2>/dev/null || open /Applications/XAMPP/manager-osx.app 2>/dev/null || true
  exit 1
fi

echo "==> BD + usuario demo..."
"$MYSQL" -uroot < "$REPO_ROOT/database.sql" >/dev/null
"$MYSQL" -uroot < "$REPO_ROOT/scripts/seed_semana8_demo.sql"

echo "==> Comprobando Apache..."
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/users.php" || echo "000")
if [[ "$CODE" != "200" ]]; then
  echo "❌ Apache no responde (HTTP $CODE). Enciende Apache en XAMPP y reintenta."
  exit 1
fi

echo "==> Login demo..."
LOGIN=$(curl -s -X POST "$BASE/users.php" \
  -H "Content-Type: application/json" \
  -d "{\"action\":\"login\",\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(printf '%s' "$LOGIN" | "$PHP" -r '$d=json_decode(stream_get_contents(STDIN),true); echo $d["token"]??"";')
if [[ -z "$TOKEN" ]]; then
  echo "❌ Login falló:"
  echo "$LOGIN"
  exit 1
fi
echo "   token OK"

echo "==> Smoke tests (N+1 / eager / cache / cola)..."
N1=$(curl -s "$BASE/dashboard.php?mode=n1" -H "X-Smartvest-Token: $TOKEN")
EAGER=$(curl -s "$BASE/dashboard.php" -H "X-Smartvest-Token: $TOKEN")
CACHE=$(curl -s "$BASE/dashboard.php" -H "X-Smartvest-Token: $TOKEN")
IOT=$(curl -s -X POST "$BASE/iot.php" \
  -H "Content-Type: application/json" \
  -H "X-Smartvest-Api-Key: $IOT_KEY" \
  -d '{"deviceId":"VEST-DEMO","distanceCm":42,"latitude":-0.180653,"longitude":-78.467834,"sosActive":true,"batteryLevel":80}')
"$PHP" "$XAMPP_TARGET/api/worker.php" --once | tee /tmp/smartvest_worker_s8.txt >/dev/null

printf '%s' "$N1" | "$PHP" -r '$d=json_decode(stream_get_contents(STDIN),true); echo "   N+1     queryCount={$d["queryCount"]}  ms={$d["elapsedMs"]}\n";'
printf '%s' "$EAGER" | "$PHP" -r '$d=json_decode(stream_get_contents(STDIN),true); echo "   eager   queryCount={$d["queryCount"]}  cache={$d["cache"]}\n";'
printf '%s' "$CACHE" | "$PHP" -r '$d=json_decode(stream_get_contents(STDIN),true); echo "   2do GET queryCount={$d["queryCount"]}  cache={$d["cache"]}\n";'
printf '%s' "$IOT" | "$PHP" -r '$d=json_decode(stream_get_contents(STDIN),true); echo "   iot jobs=".count($d["queuedJobs"]??[])."\n";'
echo "   worker OK"

echo "$TOKEN" > /tmp/smartvest_s8_token.txt

echo ""
echo "============================================"
echo " LISTO PARA GRABAR"
echo "============================================"
echo "baseUrl:  $BASE"
echo "username: $USER"
echo "password: $PASS"
echo "iotKey:   $IOT_KEY"
echo ""
echo "1) Importa en Postman:"
echo "   $ACADEMIC/postman_semana8_smartvest.json"
echo "2) Lee en voz alta:"
echo "   $ACADEMIC/TELEPROMPTER_GRABACION.md"
echo "3) QuickTime → Nueva grabación de pantalla + micrófono"
echo "============================================"

open "$ACADEMIC/TELEPROMPTER_GRABACION.md" 2>/dev/null || true
open -a "Postman" 2>/dev/null || true
