<?php

declare(strict_types=1);

/**
 * Router para el servidor web embebido de pruebas (emula Nginx + PHP-FPM de infra/smartvest/nginx.conf)
 */

$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?? '/';

// 1. Bloquear archivos sensibles, SQL, migraciones, scripts y configuraciones:
// location ~* \.(sql|env|ini|log|sh|example|git|md|yml|yaml)$ { return 404; }
if (preg_match('/\.(sql|env|ini|log|sh|example|git|md|yml|yaml)$/i', $uri)) {
    http_response_code(404);
    echo "404 Not Found (blocked extension)";
    exit;
}

// 2. Archivos ocultos:
// location ~ /\.(?!well-known).* { deny all; }
if (preg_match('#/\.(?!well-known)#', $uri)) {
    http_response_code(403);
    echo "403 Forbidden";
    exit;
}

// 3. Bloquear explícitamente cualquier acceso directo a archivos en /api/ excepto api/v1
// location ~* ^/api/(?!v1(/|$)) { return 404; }
if (preg_match('#^/api/(?!v1(/|$))#', $uri)) {
    http_response_code(404);
    echo "404 Not Found (blocked legacy api script)";
    exit;
}

// 4. Enrutar /api/v1 y /emergency/{token} hacia el front controller /api/v1/index.php
if (preg_match('#^/api/v1#', $uri) || preg_match('#^/emergency/#', $uri)) {
    require __DIR__ . '/../api/v1/index.php';
    exit;
}

// 5. Denegar ejecución directa de cualquier otro archivo PHP
if (preg_match('/\.php$/i', $uri)) {
    http_response_code(404);
    echo "404 Not Found";
    exit;
}

// Archivos estáticos si existieran
return false;
