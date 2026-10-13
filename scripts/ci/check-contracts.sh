#!/usr/bin/env bash
set -euo pipefail

echo "=== Validando contrato OpenAPI 3.1 con Redocly CLI ==="
CONTRACT_FILE="specs/002-android-native-coolify/contracts/openapi.yaml"

if [ ! -f "$CONTRACT_FILE" ]; then
    echo "ERROR: Archivo de contrato $CONTRACT_FILE no encontrado."
    exit 1
fi

# Validación estricta con versión fijada de @redocly/cli
npx --yes @redocly/cli@2.63.0 lint "$CONTRACT_FILE"

echo "Contrato OpenAPI validado con éxito sin errores."
exit 0
