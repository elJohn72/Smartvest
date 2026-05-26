#!/bin/zsh
# Expone XAMPP (puerto 80) a Internet para abrir SmartVest desde el celular.
# Requiere: cloudflared (brew install cloudflared)
# Uso: ./scripts/tunnel-phone.sh

set -euo pipefail

if ! command -v cloudflared >/dev/null 2>&1; then
  echo "Instala cloudflared: brew install cloudflared"
  exit 1
fi

if ! curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1/Smartvest/" | grep -q 200; then
  echo "Apache/XAMPP no responde en http://127.0.0.1/Smartvest/"
  echo "Enciende Apache en XAMPP y ejecuta: npm run build && ./scripts/deploy-xampp.sh"
  exit 1
fi

echo "Iniciando túnel Cloudflare (Ctrl+C para detener)..."
echo ""
echo "En el celular abre la URL que aparece abajo + /Smartvest/"
echo "Ejemplo: https://xxxx.trycloudflare.com/Smartvest/"
echo ""

exec cloudflared tunnel --url "http://127.0.0.1:80"
