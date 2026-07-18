<?php

declare(strict_types=1);

/**
 * Listado users + estado IoT en UNA query (eager / anti N+1).
 * Comparar con: GET users.php + N × GET iot.php?deviceId=...
 *
 * Auth: preferir header X-Smartvest-Token (Apache a veces elimina Authorization).
 */
require __DIR__ . '/config.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    json_response(['success' => false, 'message' => 'Método no permitido.'], 405);
}

// Auth sin reconsultar password en BD (solo HMAC del token de login).
$session = require_auth_token();

$mode = strtolower((string) ($_GET['mode'] ?? 'eager'));
$pdo = get_pdo();
$started = hrtime(true);
$queryCount = 0;

if ($mode === 'n1' || $mode === 'lazy') {
    // Demostración del anti-patrón N+1 (solo para taller / Postman before).
    $statement = $pdo->query(
        'SELECT id, full_name, national_id, age, blood_type, address, emergency_phone,
                emergency_contact, medical_observations, created_at, photo, username, device_id
         FROM users ORDER BY created_at DESC'
    );
    $queryCount++;
    $rows = $statement->fetchAll();
    $items = [];

    foreach ($rows as $row) {
        $user = public_user_row($row);
        $iot = null;
        $deviceId = (string) ($user['deviceId'] ?? '');
        if ($deviceId !== '') {
            $iotStatement = $pdo->prepare('SELECT * FROM iot_states WHERE device_id = :device_id LIMIT 1');
            $iotStatement->execute(['device_id' => $deviceId]);
            $queryCount++;
            $iotRow = $iotStatement->fetch();
            if ($iotRow) {
                $iot = dashboard_map_iot($iotRow);
            }
        }
        $items[] = ['user' => $user, 'iot' => $iot];
    }

    $elapsedMs = (hrtime(true) - $started) / 1e6;
    json_response([
        'success' => true,
        'mode' => 'n1_lazy',
        'authUserId' => $session['userId'],
        'queryCount' => $queryCount,
        'elapsedMs' => round($elapsedMs, 3),
        'cache' => 'bypass',
        'items' => $items,
        'note' => '1 query users + N queries iot_states (anti-patrón).',
    ]);
}

// Eager + cache-aside
$cacheKey = 'dashboard:eager:v1';
$fromCache = cache_get($cacheKey);
if ($fromCache !== null) {
    $elapsedMs = (hrtime(true) - $started) / 1e6;
    json_response([
        'success' => true,
        'mode' => 'eager_cached',
        'authUserId' => $session['userId'],
        'queryCount' => 0,
        'elapsedMs' => round($elapsedMs, 3),
        'cache' => 'HIT',
        'items' => $fromCache,
        'note' => 'Respuesta servida desde cache-aside (TTL ' . SMARTVEST_CACHE_TTL_USERS . 's).',
    ]);
}

$statement = $pdo->query(
    'SELECT
        u.id, u.full_name, u.national_id, u.age, u.blood_type, u.address, u.emergency_phone,
        u.emergency_contact, u.medical_observations, u.created_at, u.photo, u.username, u.device_id,
        s.distance_cm, s.latitude, s.longitude, s.sos_active, s.battery_level, s.last_update
     FROM users u
     LEFT JOIN iot_states s ON s.device_id = u.device_id
     ORDER BY u.created_at DESC'
);
$queryCount++;
$rows = $statement->fetchAll();
$items = [];

foreach ($rows as $row) {
    $user = public_user_row($row);
    $iot = null;
    if (!empty($row['device_id']) && ($row['last_update'] !== null || $row['latitude'] !== null)) {
        $iot = dashboard_map_iot($row);
    }
    $items[] = ['user' => $user, 'iot' => $iot];
}

cache_set($cacheKey, $items, SMARTVEST_CACHE_TTL_USERS);
$elapsedMs = (hrtime(true) - $started) / 1e6;

json_response([
    'success' => true,
    'mode' => 'eager',
    'authUserId' => $session['userId'],
    'queryCount' => $queryCount,
    'elapsedMs' => round($elapsedMs, 3),
    'cache' => 'MISS',
    'items' => $items,
    'note' => '1 sola query con LEFT JOIN (eager loading).',
]);

function dashboard_map_iot(array $row): array
{
    $last = $row['last_update'] ?? $row['recorded_at'] ?? null;

    return [
        'deviceId' => $row['device_id'] ?? $row['deviceId'] ?? null,
        'distanceCm' => isset($row['distance_cm']) && $row['distance_cm'] !== null ? (float) $row['distance_cm'] : null,
        'latitude' => (float) ($row['latitude'] ?? 0),
        'longitude' => (float) ($row['longitude'] ?? 0),
        'sosActive' => (bool) ($row['sos_active'] ?? false),
        'lastUpdate' => $last ? strtotime((string) $last) * 1000 : null,
        'batteryLevel' => isset($row['battery_level']) && $row['battery_level'] !== null ? (int) $row['battery_level'] : null,
    ];
}
