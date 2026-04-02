<?php

declare(strict_types=1);

require __DIR__ . '/config.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$pdo = get_pdo();

if ($method === 'GET') {
    $deviceId = $_GET['deviceId'] ?? '';
    if ($deviceId === '') {
        json_response(['success' => false, 'message' => 'deviceId es obligatorio.'], 400);
    }

    $statement = $pdo->prepare('SELECT * FROM iot_states WHERE device_id = :device_id LIMIT 1');
    $statement->execute(['device_id' => $deviceId]);
    $row = $statement->fetch();

    if (!$row) {
        json_response(['success' => true, 'data' => null]);
    }

    json_response([
        'success' => true,
        'data' => [
            'deviceId' => $row['device_id'],
            'distanceCm' => $row['distance_cm'] !== null ? (float) $row['distance_cm'] : null,
            'latitude' => (float) $row['latitude'],
            'longitude' => (float) $row['longitude'],
            'sosActive' => (bool) $row['sos_active'],
            'lastUpdate' => strtotime((string) $row['last_update']) * 1000,
            'batteryLevel' => $row['battery_level'] !== null ? (int) $row['battery_level'] : null,
        ],
    ]);
}

if ($method !== 'POST') {
    json_response(['success' => false, 'message' => 'Método no permitido.'], 405);
}

$payload = read_json_body();
$deviceId = (string) ($payload['deviceId'] ?? '');

if ($deviceId === '') {
    json_response(['success' => false, 'message' => 'deviceId es obligatorio.'], 400);
}

$statement = $pdo->prepare(
    'INSERT INTO iot_states (device_id, distance_cm, latitude, longitude, sos_active, battery_level, last_update)
    VALUES (:device_id, :distance_cm, :latitude, :longitude, :sos_active, :battery_level, NOW())
    ON DUPLICATE KEY UPDATE
        distance_cm = VALUES(distance_cm),
        latitude = VALUES(latitude),
        longitude = VALUES(longitude),
        sos_active = VALUES(sos_active),
        battery_level = VALUES(battery_level),
        last_update = NOW()'
);

$statement->execute([
    'device_id' => $deviceId,
    'distance_cm' => array_key_exists('distanceCm', $payload) ? (float) $payload['distanceCm'] : null,
    'latitude' => (float) ($payload['latitude'] ?? 0),
    'longitude' => (float) ($payload['longitude'] ?? 0),
    'sos_active' => !empty($payload['sosActive']) ? 1 : 0,
    'battery_level' => array_key_exists('batteryLevel', $payload) ? (int) $payload['batteryLevel'] : null,
]);

json_response(['success' => true]);
