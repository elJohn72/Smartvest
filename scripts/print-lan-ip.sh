#!/bin/zsh
set -euo pipefail

echo "IPs útiles para SMARTVEST_API_URL (misma red WiFi que el ESP32):"
echo ""

for iface in en0 en1; do
  ip=$(ipconfig getifaddr "$iface" 2>/dev/null || true)
  if [[ -n "$ip" ]]; then
    echo "  $iface: http://${ip}/Smartvest/api/iot.php"
  fi
done

echo ""
echo "Ejemplo en smartvest_config.h:"
echo '  #define SMARTVEST_API_URL "http://TU_IP/Smartvest/api/iot.php"'
