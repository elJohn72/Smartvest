<?php

declare(strict_types=1);

require __DIR__ . '/config.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') {
    json_response(['success' => false, 'message' => 'Método no permitido.'], 405);
}

$payload = read_json_body();
$address = trim((string) ($payload['address'] ?? ''));

if ($address === '') {
    json_response(['success' => false, 'message' => 'La dirección es obligatoria.'], 400);
}

$apiKey = get_gemini_api_key();
if ($apiKey === '') {
    json_response([
        'success' => false,
        'message' => 'Verificación con IA no configurada en el servidor.',
        'address' => null,
    ], 503);
}

$prompt = 'Verifica si la siguiente dirección es válida y existe. '
    . 'Si existe, devuelve la dirección formateada y correcta. '
    . 'Si es ambigua o no se encuentra, sugiere la más probable. '
    . 'Dirección: "' . $address . '" '
    . 'Responde SOLAMENTE con la dirección corregida. Si no encuentras nada, responde "No encontrada".';

$requestBody = json_encode([
    'contents' => [
        [
            'parts' => [
                ['text' => $prompt],
            ],
        ],
    ],
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

$endpoint = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=' . urlencode($apiKey);

$context = stream_context_create([
    'http' => [
        'method' => 'POST',
        'header' => "Content-Type: application/json\r\n",
        'content' => $requestBody,
        'timeout' => 20,
    ],
]);

$responseBody = @file_get_contents($endpoint, false, $context);
if ($responseBody === false) {
    json_response([
        'success' => false,
        'message' => 'No se pudo contactar al servicio de verificación.',
        'address' => null,
    ], 502);
}

$response = json_decode($responseBody, true);
$text = trim((string) ($response['candidates'][0]['content']['parts'][0]['text'] ?? ''));

if ($text === '' || stripos($text, 'no encontrada') !== false) {
    json_response([
        'success' => false,
        'message' => 'No se encontró una dirección verificada.',
        'address' => null,
    ]);
}

json_response([
    'success' => true,
    'address' => $text,
]);
