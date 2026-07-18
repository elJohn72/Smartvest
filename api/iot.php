<?php

declare(strict_types=1);

require __DIR__ . '/config.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$pdo = get_pdo();

function map_iot_row(array $row): array
{
    return [
        'deviceId' => $row['device_id'],
        'distanceCm' => $row['distance_cm'] !== null ? (float) $row['distance_cm'] : null,
        'latitude' => (float) $row['latitude'],
        'longitude' => (float) $row['longitude'],
        'sosActive' => (bool) $row['sos_active'],
        'lastUpdate' => strtotime((string) ($row['last_update'] ?? $row['recorded_at'])) * 1000,
        'batteryLevel' => $row['battery_level'] !== null ? (int) $row['battery_level'] : null,
    ];
}

function prune_iot_history(PDO $pdo, string $deviceId, int $maxRows = 500): void
{
    $countStatement = $pdo->prepare('SELECT COUNT(*) FROM iot_history WHERE device_id = :device_id');
    $countStatement->execute(['device_id' => $deviceId]);
    $count = (int) $countStatement->fetchColumn();

    if ($count <= $maxRows) {
        return;
    }

    $deleteStatement = $pdo->prepare(
        'DELETE FROM iot_history
        WHERE device_id = :device_id
        AND id NOT IN (
            SELECT id FROM (
                SELECT id FROM iot_history
                WHERE device_id = :device_id_inner
                ORDER BY recorded_at DESC
                LIMIT :keep_rows
            ) AS recent_rows
        )'
    );

    $deleteStatement->bindValue('device_id', $deviceId);
    $deleteStatement->bindValue('device_id_inner', $deviceId);
    $deleteStatement->bindValue('keep_rows', $maxRows, PDO::PARAM_INT);
    $deleteStatement->execute();
}

if ($method === 'GET') {
    $deviceId = $_GET['deviceId'] ?? '';
    if ($deviceId === '') {
        json_response(['success' => false, 'message' => 'deviceId es obligatorio.'], 400);
    }

    if (isset($_GET['history'])) {
        $limit = (int) ($_GET['limit'] ?? 60);
        $limit = max(10, min(120, $limit));
        $cacheKey = "iot:history:{$deviceId}:{$limit}";

        $points = cache_remember($cacheKey, SMARTVEST_CACHE_TTL_IOT, static function () use ($pdo, $deviceId, $limit) {
            $historyStatement = $pdo->prepare(
                'SELECT device_id, distance_cm, latitude, longitude, sos_active, battery_level, recorded_at
                FROM iot_history
                WHERE device_id = :device_id
                ORDER BY recorded_at DESC
                LIMIT :limit_rows'
            );
            $historyStatement->bindValue('device_id', $deviceId);
            $historyStatement->bindValue('limit_rows', $limit, PDO::PARAM_INT);
            $historyStatement->execute();

            $mapped = [];
            foreach ($historyStatement->fetchAll() as $row) {
                $mapped[] = map_iot_row($row);
            }

            return array_reverse($mapped);
        });

        json_response([
            'success' => true,
            'data' => $points,
            'cacheTtlSeconds' => SMARTVEST_CACHE_TTL_IOT,
        ]);
    }

    $cacheKey = 'iot:state:' . $deviceId;
    $data = cache_remember($cacheKey, SMARTVEST_CACHE_TTL_IOT, static function () use ($pdo, $deviceId) {
        $statement = $pdo->prepare('SELECT * FROM iot_states WHERE device_id = :device_id LIMIT 1');
        $statement->execute(['device_id' => $deviceId]);
        $row = $statement->fetch();

        return $row ? map_iot_row($row) : null;
    });

    json_response([
        'success' => true,
        'data' => $data,
        'cacheTtlSeconds' => SMARTVEST_CACHE_TTL_IOT,
    ]);
}

if ($method !== 'POST') {
    json_response(['success' => false, 'message' => 'Método no permitido.'], 405);
}

require_iot_api_key();

$payload = read_json_body();
$deviceId = (string) ($payload['deviceId'] ?? '');

if ($deviceId === '') {
    json_response(['success' => false, 'message' => 'deviceId es obligatorio.'], 400);
}

$hasBatteryLevel = array_key_exists('batteryLevel', $payload) && $payload['batteryLevel'] !== null;

$updateFields = [
    'distance_cm = :distance_cm',
    'latitude = :latitude',
    'longitude = :longitude',
    'sos_active = :sos_active',
    'last_update = NOW()',
];

if ($hasBatteryLevel) {
    $updateFields[] = 'battery_level = :battery_level';
}

$statement = $pdo->prepare(
    'INSERT INTO iot_states (device_id, distance_cm, latitude, longitude, sos_active, battery_level, last_update)
    VALUES (:device_id, :distance_cm, :latitude, :longitude, :sos_active, :battery_level, NOW())
    ON DUPLICATE KEY UPDATE ' . implode(', ', $updateFields)
);

$params = [
    'device_id' => $deviceId,
    'distance_cm' => array_key_exists('distanceCm', $payload) ? (float) $payload['distanceCm'] : null,
    'latitude' => (float) ($payload['latitude'] ?? 0),
    'longitude' => (float) ($payload['longitude'] ?? 0),
    'sos_active' => !empty($payload['sosActive']) ? 1 : 0,
    'battery_level' => $hasBatteryLevel ? (int) $payload['batteryLevel'] : null,
];

$statement->execute($params);

$historyStatement = $pdo->prepare(
    'INSERT INTO iot_history (device_id, distance_cm, latitude, longitude, sos_active, battery_level)
    VALUES (:device_id, :distance_cm, :latitude, :longitude, :sos_active, :battery_level)'
);

$historyStatement->execute([
    'device_id' => $deviceId,
    'distance_cm' => $params['distance_cm'],
    'latitude' => $params['latitude'],
    'longitude' => $params['longitude'],
    'sos_active' => $params['sos_active'],
    'battery_level' => $hasBatteryLevel ? (int) $payload['batteryLevel'] : null,
]);

// Invalidación cache-aside tras escritura
cache_forget('iot:state:' . $deviceId);
cache_forget_prefix('iot:history:' . $deviceId . ':');
cache_forget('dashboard:eager:v1');

$queued = [];

// Trabajo pesado fuera del request HTTP (cola + worker)
$queued[] = queue_push('prune_iot_history', [
    'deviceId' => $deviceId,
    'maxRows' => 500,
]);

if (!empty($params['sos_active'])) {
    $queued[] = queue_push('notify_sos', [
        'deviceId' => $deviceId,
        'latitude' => $params['latitude'],
        'longitude' => $params['longitude'],
    ]);
}

json_response([
    'success' => true,
    'queuedJobs' => $queued,
    'queueStats' => queue_stats(),
    'note' => 'Prune/SOS van a cola async. Ejecuta: php api/worker.php --once',
]);
