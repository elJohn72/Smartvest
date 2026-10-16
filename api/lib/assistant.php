<?php

declare(strict_types=1);

/**
 * Asistente del cuidador: métricas de telemetría y relato opcional vía Groq.
 * La clave nunca sale del servidor.
 */

function smartvest_assistant_handle(PDO $pdo, array $profile, string $deviceId, array $payload): void
{
    $action = (string)($payload['action'] ?? 'analyze');
    $context = smartvest_assistant_context($pdo, $profile, $deviceId);

    header('Content-Type: application/json; charset=utf-8');

    if ($action === 'chat') {
        $messages = $payload['messages'] ?? [];
        if (!is_array($messages)) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'messages debe ser un arreglo.']);
            return;
        }
        $reply = smartvest_assistant_chat($context, $messages);
        echo json_encode([
            'success' => true,
            'reply' => $reply['text'],
            'aiPowered' => $reply['aiPowered'],
            'aiProvider' => $reply['aiProvider'],
            'aiModel' => $reply['aiModel'],
            'analytics' => $context['analytics'],
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return;
    }

    $insights = smartvest_assistant_insights($context);
    echo json_encode([
        'success' => true,
        'analytics' => $context['analytics'],
        'insights' => $insights['text'],
        'aiPowered' => $insights['aiPowered'],
        'aiProvider' => $insights['aiProvider'],
        'aiModel' => $insights['aiModel'],
        'generatedAt' => gmdate('c'),
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}

function smartvest_assistant_context(PDO $pdo, array $profile, string $deviceId): array
{
    $stateStmt = $pdo->prepare('SELECT * FROM telemetry_state WHERE device_id = :dev LIMIT 1');
    $stateStmt->execute(['dev' => $deviceId]);
    $stateRow = $stateStmt->fetch(PDO::FETCH_ASSOC);
    $current = $stateRow ? smartvest_assistant_map_row($stateRow) : null;

    $historyStmt = $pdo->prepare(
        'SELECT * FROM telemetry_history
         WHERE device_id = :dev
         ORDER BY received_at DESC, sequence DESC
         LIMIT 200'
    );
    $historyStmt->execute(['dev' => $deviceId]);
    $history = [];
    foreach ($historyStmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $history[] = smartvest_assistant_map_row($row);
    }
    $history = array_reverse($history);

    return [
        'deviceId' => $deviceId,
        'profile' => $profile,
        'currentState' => $current,
        'history' => $history,
        'analytics' => smartvest_assistant_analytics($profile, $current, $history, $deviceId),
    ];
}

function smartvest_assistant_map_row(array $row): array
{
    $stamp = (string)($row['received_at'] ?? '');
    $lastUpdate = $stamp !== '' ? (int)(strtotime($stamp . ' UTC') * 1000) : 0;

    return [
        'deviceId' => (string)$row['device_id'],
        'distanceCm' => $row['distance_cm'] !== null ? (float)$row['distance_cm'] : null,
        'latitude' => $row['latitude'] !== null ? (float)$row['latitude'] : null,
        'longitude' => $row['longitude'] !== null ? (float)$row['longitude'] : null,
        'gpsFix' => (bool)($row['gps_fix'] ?? false),
        'sosActive' => (bool)$row['sos_active'],
        'batteryLevel' => $row['battery_level'] !== null ? (int)$row['battery_level'] : null,
        'lastUpdate' => $lastUpdate,
    ];
}

function smartvest_assistant_valid_gps(?float $latitude, ?float $longitude): bool
{
    if ($latitude === null || $longitude === null) {
        return false;
    }
    return abs($latitude) > 0.0001 && abs($longitude) > 0.0001;
}

function smartvest_assistant_distance_level(?float $distanceCm): string
{
    if ($distanceCm === null || $distanceCm >= 9000) {
        return 'unknown';
    }
    if ($distanceCm <= 40) {
        return 'danger';
    }
    if ($distanceCm <= 100) {
        return 'alert';
    }
    if ($distanceCm <= 200) {
        return 'caution';
    }
    return 'clear';
}

function smartvest_assistant_place_risk(float $dangerRatio, float $alertRatio): string
{
    if ($dangerRatio >= 0.25 || $alertRatio >= 0.45) {
        return 'alto';
    }
    if ($dangerRatio >= 0.12 || $alertRatio >= 0.28) {
        return 'medio';
    }
    return 'bajo';
}

function smartvest_assistant_overall_risk(int $riskScore, int $sosCount): string
{
    if ($sosCount >= 2 || $riskScore >= 75) {
        return 'critico';
    }
    if ($sosCount >= 1 || $riskScore >= 55) {
        return 'alto';
    }
    if ($riskScore >= 35) {
        return 'medio';
    }
    return 'bajo';
}

function smartvest_assistant_analytics(?array $profile, ?array $current, array $history, string $deviceId): array
{
    $nowMs = (int)(microtime(true) * 1000);
    $staleAfterMs = 30000;
    $readingsWithDistance = 0;
    $dangerCount = 0;
    $alertCount = 0;
    $cautionCount = 0;
    $distanceSum = 0.0;
    $clusters = [];
    $sosEvents = [];
    $previousSos = false;

    foreach ($history as $point) {
        $distance = $point['distanceCm'] ?? null;
        if ($distance !== null && $distance < 9000) {
            $readingsWithDistance++;
            $distanceSum += $distance;
            $level = smartvest_assistant_distance_level($distance);
            if ($level === 'danger') {
                $dangerCount++;
            } elseif ($level === 'alert') {
                $alertCount++;
            } elseif ($level === 'caution') {
                $cautionCount++;
            }
        }

        $sosActive = !empty($point['sosActive']);
        if ($sosActive && !$previousSos) {
            $sosEvents[] = [
                'at' => (int)$point['lastUpdate'],
                'latitude' => $point['latitude'],
                'longitude' => $point['longitude'],
                'distanceCm' => $point['distanceCm'],
            ];
        }
        $previousSos = $sosActive;

        if (!smartvest_assistant_valid_gps($point['latitude'], $point['longitude'])) {
            continue;
        }

        $bucketLat = round((float)$point['latitude'], 3);
        $bucketLng = round((float)$point['longitude'], 3);
        $key = $bucketLat . ',' . $bucketLng;
        if (!isset($clusters[$key])) {
            $clusters[$key] = [
                'latitude' => $bucketLat,
                'longitude' => $bucketLng,
                'visitCount' => 0,
                'dangerCount' => 0,
                'alertCount' => 0,
                'distanceSum' => 0.0,
                'distanceSamples' => 0,
                'lastSeenAt' => (int)$point['lastUpdate'],
            ];
        }
        $clusters[$key]['visitCount']++;
        $clusters[$key]['lastSeenAt'] = max($clusters[$key]['lastSeenAt'], (int)$point['lastUpdate']);
        if ($distance !== null && $distance < 9000) {
            $clusters[$key]['distanceSum'] += $distance;
            $clusters[$key]['distanceSamples']++;
            $level = smartvest_assistant_distance_level($distance);
            if ($level === 'danger') {
                $clusters[$key]['dangerCount']++;
            } elseif ($level === 'alert') {
                $clusters[$key]['alertCount']++;
            }
        }
    }

    if ($current && !empty($current['sosActive'])) {
        $lastEvent = $sosEvents[count($sosEvents) - 1] ?? null;
        $isFresh = $lastEvent === null || abs((int)$current['lastUpdate'] - (int)$lastEvent['at']) > 5000;
        if ($isFresh) {
            $sosEvents[] = [
                'at' => (int)$current['lastUpdate'],
                'latitude' => $current['latitude'],
                'longitude' => $current['longitude'],
                'distanceCm' => $current['distanceCm'] ?? null,
            ];
        }
    }

    $frequentPlaces = [];
    foreach ($clusters as $cluster) {
        $samples = max(1, (int)$cluster['distanceSamples']);
        $dangerRatio = $cluster['dangerCount'] / $samples;
        $alertRatio = $cluster['alertCount'] / $samples;
        $frequentPlaces[] = [
            'latitude' => $cluster['latitude'],
            'longitude' => $cluster['longitude'],
            'visitCount' => $cluster['visitCount'],
            'avgDistanceCm' => $cluster['distanceSamples'] > 0
                ? round($cluster['distanceSum'] / $cluster['distanceSamples'], 1)
                : null,
            'riskLevel' => smartvest_assistant_place_risk($dangerRatio, $alertRatio),
            'lastSeenAt' => $cluster['lastSeenAt'],
            'mapsUrl' => 'https://www.google.com/maps/search/?api=1&query='
                . $cluster['latitude'] . ',' . $cluster['longitude'],
        ];
    }
    usort($frequentPlaces, static fn (array $a, array $b): int => $b['visitCount'] <=> $a['visitCount']);
    $frequentPlaces = array_slice($frequentPlaces, 0, 6);

    $dangerRatio = $readingsWithDistance > 0 ? $dangerCount / $readingsWithDistance : 0;
    $alertRatio = $readingsWithDistance > 0 ? $alertCount / $readingsWithDistance : 0;
    $avgDistance = $readingsWithDistance > 0 ? round($distanceSum / $readingsWithDistance, 1) : null;
    $sosCount = count($sosEvents);
    $riskScore = (int)min(100, round(
        ($dangerRatio * 55)
        + ($alertRatio * 25)
        + ($cautionCount / max(1, $readingsWithDistance) * 10)
        + ($sosCount * 12)
    ));

    $lastUpdate = $current['lastUpdate'] ?? null;
    $connectionStatus = 'offline';
    if ($lastUpdate) {
        $ageMs = $nowMs - (int)$lastUpdate;
        if ($ageMs <= $staleAfterMs) {
            $connectionStatus = 'online';
        } elseif ($ageMs <= $staleAfterMs * 4) {
            $connectionStatus = 'stale';
        }
    }

    $lastLocation = null;
    if ($current && smartvest_assistant_valid_gps($current['latitude'], $current['longitude'])) {
        $lastLocation = [
            'latitude' => $current['latitude'],
            'longitude' => $current['longitude'],
            'lastUpdate' => (int)$current['lastUpdate'],
            'mapsUrl' => 'https://www.google.com/maps/search/?api=1&query='
                . $current['latitude'] . ',' . $current['longitude'],
        ];
    }

    return [
        'deviceId' => $deviceId,
        'historyPoints' => count($history),
        'avgDistanceCm' => $avgDistance,
        'dangerRatio' => round($dangerRatio, 3),
        'alertRatio' => round($alertRatio, 3),
        'riskScore' => $riskScore,
        'dailyRiskLevel' => smartvest_assistant_overall_risk($riskScore, $sosCount),
        'frequentPlaces' => $frequentPlaces,
        'sosEvents' => array_reverse($sosEvents),
        'sosCount' => $sosCount,
        'connectionStatus' => $connectionStatus,
        'deviceOnline' => $connectionStatus === 'online',
        'sosActiveNow' => (bool)($current['sosActive'] ?? false),
        'lastLocation' => $lastLocation,
        'patientSummary' => [
            'fullName' => (string)($profile['presentation_name'] ?? 'Usuario SmartVest'),
            'bloodType' => $profile['blood_type'] ?? null,
            'medicalObservations' => $profile['medical_observations'] ?? null,
            'emergencyPhone' => $profile['emergency_phone'] ?? null,
        ],
        'currentTelemetry' => $current,
    ];
}

function smartvest_assistant_prompt(array $context): string
{
    $analytics = $context['analytics'];
    $payload = [
        'paciente' => $analytics['patientSummary'],
        'dispositivo' => [
            'deviceId' => $context['deviceId'],
            'enLinea' => $analytics['deviceOnline'],
            'estadoConexion' => $analytics['connectionStatus'],
            'sosActivoAhora' => $analytics['sosActiveNow'],
            'ultimaUbicacion' => $analytics['lastLocation'],
            'telemetriaActual' => $analytics['currentTelemetry'],
        ],
        'analitica' => [
            'puntuacionRiesgo' => $analytics['riskScore'],
            'nivelRiesgoDiario' => $analytics['dailyRiskLevel'],
            'distanciaPromedioCm' => $analytics['avgDistanceCm'],
            'ratioPeligro' => $analytics['dangerRatio'],
            'ratioAlerta' => $analytics['alertRatio'],
            'lugaresFrecuentes' => $analytics['frequentPlaces'],
            'eventosSos' => array_slice($analytics['sosEvents'], 0, 8),
            'totalEventosSos' => $analytics['sosCount'],
            'muestrasHistorial' => $analytics['historyPoints'],
        ],
    ];

    return json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}

function smartvest_assistant_groq(array $messages): ?array
{
    $apiKey = get_groq_api_key();
    if ($apiKey === '') {
        return null;
    }

    $models = [get_groq_model()];
    if (!in_array('llama-3.1-8b-instant', $models, true)) {
        $models[] = 'llama-3.1-8b-instant';
    }

    foreach ($models as $model) {
        $body = json_encode([
            'model' => $model,
            'messages' => $messages,
            'temperature' => 0.35,
            'max_tokens' => 900,
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if ($body === false) {
            return null;
        }

        $responseBody = smartvest_assistant_http_post(
            'https://api.groq.com/openai/v1/chat/completions',
            $body,
            $apiKey
        );
        if ($responseBody === null) {
            continue;
        }
        $response = json_decode($responseBody, true);
        if (!is_array($response) || !empty($response['error'])) {
            continue;
        }
        $text = trim((string)($response['choices'][0]['message']['content'] ?? ''));
        if ($text === '') {
            continue;
        }
        return [
            'text' => $text,
            'model' => (string)($response['model'] ?? $model),
        ];
    }

    return null;
}

function smartvest_assistant_http_post(string $url, string $body, string $apiKey): ?string
{
    if (function_exists('curl_init')) {
        $handle = curl_init($url);
        if ($handle === false) {
            return null;
        }
        curl_setopt_array($handle, [
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'Authorization: Bearer ' . $apiKey,
            ],
            CURLOPT_POSTFIELDS => $body,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 25,
        ]);
        $response = curl_exec($handle);
        $status = (int)curl_getinfo($handle, CURLINFO_HTTP_CODE);
        curl_close($handle);
        if (!is_string($response) || $status < 200 || $status >= 300) {
            return null;
        }
        return $response;
    }

    $context = stream_context_create([
        'http' => [
            'method' => 'POST',
            'header' => "Content-Type: application/json\r\nAuthorization: Bearer {$apiKey}\r\n",
            'content' => $body,
            'timeout' => 25,
            'ignore_errors' => true,
        ],
    ]);
    $response = @file_get_contents($url, false, $context);
    return is_string($response) ? $response : null;
}

function smartvest_assistant_insights(array $context): array
{
    $analytics = $context['analytics'];
    $prompt = "Eres el asistente de SmartVest para cuidadores de personas con discapacidad visual.\n"
        . "Analiza el JSON y responde en español, en markdown breve, con estas secciones:\n"
        . "## Resumen del día\n## Lugares habituales\n## Eventos SOS\n## Recomendaciones\n"
        . "Sé concreto. No inventes datos que no estén en el JSON. Si faltan muestras, dilo.\n\n"
        . smartvest_assistant_prompt($context);

    $ai = smartvest_assistant_groq([
        ['role' => 'system', 'content' => 'Eres el asistente SmartVest. Respondes en español con markdown breve.'],
        ['role' => 'user', 'content' => $prompt],
    ]);
    if ($ai !== null) {
        return ['text' => $ai['text'], 'aiPowered' => true, 'aiProvider' => 'groq', 'aiModel' => $ai['model']];
    }

    return [
        'text' => smartvest_assistant_fallback_insights($analytics),
        'aiPowered' => false,
        'aiProvider' => null,
        'aiModel' => null,
    ];
}

function smartvest_assistant_fallback_insights(array $analytics): string
{
    $labels = ['bajo' => 'bajo', 'medio' => 'medio', 'alto' => 'alto', 'critico' => 'crítico'];
    $level = $labels[$analytics['dailyRiskLevel']] ?? $analytics['dailyRiskLevel'];
    $online = $analytics['deviceOnline'] ? 'en línea' : ($analytics['connectionStatus'] === 'stale' ? 'con señal antigua' : 'sin señal reciente');
    $dangerPct = round($analytics['dangerRatio'] * 100, 1);
    $alertPct = round($analytics['alertRatio'] * 100, 1);

    $text = "## Resumen del día\n";
    $text .= "Riesgo general **{$level}** (puntuación {$analytics['riskScore']}/100). El chaleco está **{$online}**.\n\n";
    $text .= "## Lugares habituales\n";
    if ($analytics['frequentPlaces'] === []) {
        $text .= "Aún no hay suficientes coordenadas GPS en el historial.\n\n";
    } else {
        foreach (array_slice($analytics['frequentPlaces'], 0, 3) as $place) {
            $text .= "- Zona ~{$place['latitude']}, {$place['longitude']}: {$place['visitCount']} lecturas, riesgo **{$place['riskLevel']}**.\n";
        }
        $text .= "\n";
    }
    $text .= "## Eventos SOS\n";
    $text .= $analytics['sosCount'] === 0
        ? "No se registraron pulsaciones SOS en el periodo analizado.\n\n"
        : "Se detectaron **{$analytics['sosCount']}** activación(es) SOS.\n\n";
    $text .= "## Recomendaciones\n";
    $text .= "- Obstáculos críticos (≤40 cm): **{$dangerPct}%** de lecturas; alerta (≤100 cm): **{$alertPct}%**.\n";
    if ($analytics['sosActiveNow']) {
        $text .= "- **SOS activo ahora:** verifica a la persona y llama al contacto de emergencia.\n";
    }
    return $text;
}

function smartvest_assistant_chat(array $context, array $messages): array
{
    $clean = [];
    foreach (array_slice($messages, -12) as $message) {
        if (!is_array($message)) {
            continue;
        }
        $role = (string)($message['role'] ?? 'user');
        $content = trim((string)($message['content'] ?? ''));
        if ($content === '') {
            continue;
        }
        $clean[] = [
            'role' => $role === 'assistant' ? 'assistant' : 'user',
            'content' => mb_substr($content, 0, 1500),
        ];
    }

    $latest = '';
    foreach (array_reverse($clean) as $message) {
        if ($message['role'] === 'user') {
            $latest = $message['content'];
            break;
        }
    }
    if ($latest === '') {
        return [
            'text' => 'Escribe una pregunta sobre el estado de la persona, el chaleco, la ubicación o el riesgo.',
            'aiPowered' => false,
            'aiProvider' => null,
            'aiModel' => null,
        ];
    }

    $system = "Eres el asistente SmartVest. Respondes en español, de forma clara y breve, "
        . "usando SOLO el contexto JSON sobre la persona y su chaleco. "
        . "Si no hay dato suficiente, dilo. Si hay SOS activo, prioriza qué hacer ahora.\n\nCONTEXTO:\n"
        . smartvest_assistant_prompt($context);

    $ai = smartvest_assistant_groq(array_merge(
        [['role' => 'system', 'content' => $system]],
        $clean
    ));
    if ($ai !== null) {
        return ['text' => $ai['text'], 'aiPowered' => true, 'aiProvider' => 'groq', 'aiModel' => $ai['model']];
    }

    return [
        'text' => smartvest_assistant_fallback_chat($context['analytics'], $latest),
        'aiPowered' => false,
        'aiProvider' => null,
        'aiModel' => null,
    ];
}

function smartvest_assistant_fallback_chat(array $analytics, string $question): string
{
    $normalized = mb_strtolower($question, 'UTF-8');

    if (str_contains($normalized, 'sos') || str_contains($normalized, 'emergencia')) {
        if ($analytics['sosActiveNow']) {
            return '**SOS activo en este momento.** Revisa a la persona de inmediato y llama al contacto de emergencia.';
        }
        if ($analytics['sosCount'] === 0) {
            return 'No hay activaciones SOS recientes en el historial analizado.';
        }
        $last = $analytics['sosEvents'][0] ?? null;
        if ($last === null) {
            return "Se registraron {$analytics['sosCount']} evento(s) SOS en el periodo.";
        }
        $when = gmdate('d/m/Y H:i', (int)floor(((int)$last['at']) / 1000));
        return "Última activación SOS: **{$when}** UTC. Total en el periodo: **{$analytics['sosCount']}**.";
    }

    if (
        str_contains($normalized, 'ubicación')
        || str_contains($normalized, 'ubicacion')
        || str_contains($normalized, 'dónde')
        || str_contains($normalized, 'donde')
        || str_contains($normalized, 'gps')
    ) {
        $location = $analytics['lastLocation'];
        if ($location === null) {
            return 'No hay una ubicación GPS válida reciente.';
        }
        $when = gmdate('d/m/Y H:i', (int)floor(((int)$location['lastUpdate']) / 1000));
        return "Última ubicación conocida: **{$location['latitude']}, {$location['longitude']}** ({$when} UTC).";
    }

    if (
        str_contains($normalized, 'encendido')
        || str_contains($normalized, 'conectado')
        || str_contains($normalized, 'dispositivo')
        || str_contains($normalized, 'chaleco')
        || str_contains($normalized, 'online')
        || str_contains($normalized, 'línea')
        || str_contains($normalized, 'linea')
    ) {
        $labels = [
            'online' => '**en línea** (telemetría reciente)',
            'stale' => 'con señal **antigua**',
            'offline' => '**sin datos** recientes',
        ];
        return 'El chaleco está ' . ($labels[$analytics['connectionStatus']] ?? $analytics['connectionStatus']) . '.';
    }

    if (str_contains($normalized, 'riesgo') || str_contains($normalized, 'peligro')) {
        return "Riesgo del periodo: **{$analytics['dailyRiskLevel']}** "
            . "(puntuación {$analytics['riskScore']}/100). "
            . 'Lecturas en zona de peligro (≤40 cm): '
            . round($analytics['dangerRatio'] * 100, 1) . '%.';
    }

    $name = $analytics['patientSummary']['fullName'] ?? 'La persona';
    $distance = $analytics['currentTelemetry']['distanceCm'] ?? null;
    $distanceText = ($distance !== null && $distance < 9000)
        ? round($distance) . ' cm al obstáculo'
        : 'sin lectura de distancia';

    return "**{$name}**: chaleco {$analytics['connectionStatus']}, {$distanceText}. "
        . "Riesgo del periodo: **{$analytics['dailyRiskLevel']}**.";
}
