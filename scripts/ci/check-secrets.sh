#!/usr/bin/env bash
set -euo pipefail

echo "=== Verificando posibles secretos o credenciales en git tracking ==="

ERRORS=0

# Buscar patrones sospechosos en archivos rastreados
SUSPECT_PATTERNS=(
    "AIzaSy[0-9A-Za-z_-]{33}" # Google API Key
    "-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"
    "ghp_[0-9a-zA-Z]{36}" # GitHub Token
)

for pattern in "${SUSPECT_PATTERNS[@]}"; do
    MATCHES=$(git grep -E -I "$pattern" -- ':(exclude)*.example*' ':(exclude)*.md' ':(exclude)*.json' 2>/dev/null || true)
    if [ -n "$MATCHES" ]; then
        echo "ALERTA: Posible secreto encontrado con patrón '$pattern':"
        echo "$MATCHES"
        ERRORS=$((ERRORS + 1))
    fi
done

# Verificar que config.local.php no esté versionado
if git ls-files --error-unmatch api/config.local.php >/dev/null 2>&1; then
    echo "ERROR: api/config.local.php está en el índice de git. Debe ser gitignored."
    ERRORS=$((ERRORS + 1))
fi

if [ "$ERRORS" -gt 0 ]; then
    echo "Fallo en verificación de secretos: $ERRORS incidencias."
    exit 1
fi

echo "Verificación de secretos completada con éxito. No se detectaron secretos."
exit 0
