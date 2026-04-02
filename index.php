<?php

declare(strict_types=1);

$distIndexPath = __DIR__ . '/dist/index.html';

if (!file_exists($distIndexPath)) {
    http_response_code(500);
    header('Content-Type: text/plain; charset=utf-8');
    echo "No se encontró dist/index.html. Ejecuta 'npm install' y luego 'npm run build'.";
    exit;
}

$html = file_get_contents($distIndexPath);

if ($html === false) {
    http_response_code(500);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'No se pudo leer dist/index.html.';
    exit;
}

header('Content-Type: text/html; charset=utf-8');
echo $html;
