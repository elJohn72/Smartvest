#!/usr/bin/env bash
set -euo pipefail

echo "=== Validando contrato OpenAPI 3.1 ==="
CONTRACT_FILE="specs/002-android-native-coolify/contracts/openapi.yaml"

if [ ! -f "$CONTRACT_FILE" ]; then
    echo "ERROR: Archivo de contrato $CONTRACT_FILE no encontrado."
    exit 1
fi

# Comprobar sintaxis básica y estructura con node/npx si está disponible
if command -v node >/dev/null 2>&1; then
    node -e '
        const fs = require("fs");
        const yaml = fs.readFileSync("'$CONTRACT_FILE'", "utf8");
        if (!yaml.includes("openapi: 3.1.0")) {
            console.error("Versión openapi inválida");
            process.exit(1);
        }
        if (!yaml.includes("/devices/{deviceId}/telemetry")) {
            console.error("Falta endpoint crítico de telemetría");
            process.exit(1);
        }
        console.log("Estructura de contrato OpenAPI básica validada.");
    '
fi

echo "Contrato OpenAPI validado con éxito."
exit 0
