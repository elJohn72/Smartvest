<?php

declare(strict_types=1);

function smartvest_json_number(mixed $value): bool
{
    return is_int($value) || is_float($value);
}

/**
 * @return array{error?: string, deviceId?: string, bootId?: string, sequence?: int, distanceCm?: float|null, gpsFix?: bool, latitude?: float|null, longitude?: float|null, batteryLevel?: int|null, sosActive?: bool, sosEventId?: string|null}
 */
function smartvest_validate_telemetry(array $data, ?string $pathDeviceId): array
{
    $bodyDevice = $data['deviceId'] ?? null;
    if ($pathDeviceId !== null && $pathDeviceId !== '') {
        if ($bodyDevice !== null && $bodyDevice !== $pathDeviceId) {
            return ['error' => 'deviceId del cuerpo no coincide con la ruta.'];
        }
        $deviceId = $pathDeviceId;
    } else {
        $deviceId = is_string($bodyDevice) ? $bodyDevice : '';
    }

    if (!is_string($deviceId) || !preg_match('/^[A-Za-z0-9._-]{1,100}$/', $deviceId)) {
        return ['error' => 'deviceId inválido.'];
    }

    $bootId = $data['bootId'] ?? null;
    if (!is_string($bootId) || !preg_match('/^[A-Za-z0-9-]{8,36}$/', $bootId)) {
        return ['error' => 'bootId inválido.'];
    }

    if (!array_key_exists('sequence', $data) || !is_int($data['sequence']) || $data['sequence'] < 0) {
        return ['error' => 'sequence debe ser un entero mayor o igual a 0.'];
    }

    if (!array_key_exists('sosActive', $data) || !is_bool($data['sosActive'])) {
        return ['error' => 'sosActive debe ser booleano.'];
    }
    $sosActive = $data['sosActive'];

    $gpsFix = false;
    if (array_key_exists('gpsFix', $data)) {
        if (!is_bool($data['gpsFix'])) {
            return ['error' => 'gpsFix debe ser booleano.'];
        }
        $gpsFix = $data['gpsFix'];
    }

    $distance = null;
    if (array_key_exists('distanceCm', $data) && $data['distanceCm'] !== null) {
        if (!smartvest_json_number($data['distanceCm']) || $data['distanceCm'] < 0 || $data['distanceCm'] > 10000) {
            return ['error' => 'distanceCm fuera de rango o con tipo inválido.'];
        }
        $distance = (float)$data['distanceCm'];
    }

    $battery = null;
    if (array_key_exists('batteryLevel', $data) && $data['batteryLevel'] !== null) {
        if (!is_int($data['batteryLevel']) || $data['batteryLevel'] < 0 || $data['batteryLevel'] > 100) {
            return ['error' => 'batteryLevel debe ser un entero entre 0 y 100, o null.'];
        }
        $battery = $data['batteryLevel'];
    }

    $latitude = null;
    $longitude = null;
    $hasLat = array_key_exists('latitude', $data) && $data['latitude'] !== null;
    $hasLng = array_key_exists('longitude', $data) && $data['longitude'] !== null;
    if ($gpsFix) {
        if (!$hasLat || !$hasLng || !smartvest_json_number($data['latitude']) || !smartvest_json_number($data['longitude'])) {
            return ['error' => 'gpsFix verdadero exige latitud y longitud numéricas.'];
        }
        if ($data['latitude'] < -90 || $data['latitude'] > 90 || $data['longitude'] < -180 || $data['longitude'] > 180) {
            return ['error' => 'Coordenadas fuera de rango.'];
        }
        $latitude = (float)$data['latitude'];
        $longitude = (float)$data['longitude'];
    } elseif ($hasLat || $hasLng) {
        return ['error' => 'Sin gpsFix las coordenadas deben ser null.'];
    }

    $sosEventId = null;
    if ($sosActive) {
        $rawEvent = $data['sosEventId'] ?? null;
        if (!is_string($rawEvent) || !preg_match('/^[A-Za-z0-9-]{8,64}$/', $rawEvent)) {
            return ['error' => 'sosEventId es obligatorio cuando sosActive es verdadero.'];
        }
        $sosEventId = $rawEvent;
    } elseif (array_key_exists('sosEventId', $data) && $data['sosEventId'] !== null) {
        return ['error' => 'sosEventId debe ser null cuando no hay SOS.'];
    }

    return [
        'deviceId' => $deviceId,
        'bootId' => $bootId,
        'sequence' => $data['sequence'],
        'distanceCm' => $distance,
        'gpsFix' => $gpsFix,
        'latitude' => $latitude,
        'longitude' => $longitude,
        'batteryLevel' => $battery,
        'sosActive' => $sosActive,
        'sosEventId' => $sosEventId,
    ];
}
